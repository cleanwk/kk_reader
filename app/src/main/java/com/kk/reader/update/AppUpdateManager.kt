package com.kk.reader.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

data class ReleaseInfo(
    val tagName: String,
    val versionName: String,
    val releaseNotes: String,
    val apkDownloadUrl: String,
    val publishedAt: String
)

data class DownloadProgress(
    val bytesDownloaded: Long,
    val totalBytes: Long,
    val isComplete: Boolean = false,
    val apkFile: File? = null,
    val error: String? = null
)

@Singleton
class AppUpdateManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val GITHUB_OWNER = "anthropics"
        private const val GITHUB_REPO = "kk_reader"
        private const val API_URL = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun checkForUpdate(): Result<ReleaseInfo?> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(API_URL)
                .header("Accept", "application/vnd.github+json")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("HTTP ${response.code}"))
            }

            val body = response.body?.string()
                ?: return@withContext Result.failure(Exception("Empty response"))

            val json = JSONObject(body)
            val tagName = json.getString("tag_name")
            val versionName = tagName.removePrefix("v")
            val releaseNotes = json.optString("body", "")
            val publishedAt = json.optString("published_at", "")

            // Find APK asset
            val assets = json.getJSONArray("assets")
            var apkUrl = ""
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                val name = asset.getString("name")
                if (name.endsWith(".apk")) {
                    apkUrl = asset.getString("browser_download_url")
                    break
                }
            }

            if (apkUrl.isEmpty()) {
                return@withContext Result.failure(Exception("No APK found in release"))
            }

            val currentVersion = getCurrentVersion()
            val release = ReleaseInfo(
                tagName = tagName,
                versionName = versionName,
                releaseNotes = releaseNotes,
                apkDownloadUrl = apkUrl,
                publishedAt = publishedAt
            )

            if (isNewerVersion(versionName, currentVersion)) {
                Result.success(release)
            } else {
                Result.success(null) // Already up to date
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun downloadApk(url: String): Flow<DownloadProgress> = flow {
        val apkDir = File(context.cacheDir, "updates").apply { mkdirs() }
        val apkFile = File(apkDir, "kk_reader_update.apk")
        apkFile.delete() // Clean previous download

        try {
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                emit(DownloadProgress(0, 0, error = "HTTP ${response.code}"))
                return@flow
            }

            val body = response.body ?: run {
                emit(DownloadProgress(0, 0, error = "Empty response"))
                return@flow
            }

            val totalBytes = body.contentLength()
            var downloaded = 0L

            FileOutputStream(apkFile).use { output ->
                body.byteStream().use { input ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloaded += bytesRead
                        emit(DownloadProgress(downloaded, totalBytes))
                    }
                }
            }

            emit(DownloadProgress(downloaded, totalBytes, isComplete = true, apkFile = apkFile))
        } catch (e: Exception) {
            apkFile.delete()
            emit(DownloadProgress(0, 0, error = e.message ?: "Download failed"))
        }
    }.flowOn(Dispatchers.IO)

    fun installApk(apkFile: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        context.startActivity(intent)
    }

    fun getCurrentVersion(): String {
        return try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0.0.0"
        } catch (_: Exception) {
            "0.0.0"
        }
    }

    /**
     * Compare semantic versions: "0.2.0" > "0.1.0"
     */
    internal fun isNewerVersion(remote: String, local: String): Boolean {
        val r = remote.split(".").mapNotNull { it.toIntOrNull() }
        val l = local.split(".").mapNotNull { it.toIntOrNull() }
        for (i in 0 until maxOf(r.size, l.size)) {
            val rv = r.getOrElse(i) { 0 }
            val lv = l.getOrElse(i) { 0 }
            if (rv > lv) return true
            if (rv < lv) return false
        }
        return false
    }
}
