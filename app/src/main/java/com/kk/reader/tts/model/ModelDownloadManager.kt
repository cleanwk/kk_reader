package com.kk.reader.tts.model

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Request
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

data class DownloadProgress(
    val modelId: String,
    val bytesDownloaded: Long,
    val totalBytes: Long,
    val isExtracting: Boolean = false,
    val isComplete: Boolean = false,
    val error: String? = null
)

@Singleton
class ModelDownloadManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val modelsDir: File
        get() = File(context.filesDir, "tts_models").apply { mkdirs() }

    fun getModelDir(modelId: String): File = File(modelsDir, modelId)

    fun isModelDownloaded(modelId: String): Boolean {
        if (modelId == "system") return true
        val modelDir = getModelDir(modelId)
        return modelDir.exists() && modelDir.listFiles()?.isNotEmpty() == true
    }

    fun getDownloadedModels(): List<String> {
        return modelsDir.listFiles()
            ?.filter { it.isDirectory && it.listFiles()?.isNotEmpty() == true }
            ?.map { it.name }
            ?: emptyList()
    }

    fun deleteModel(modelId: String): Boolean {
        val modelDir = getModelDir(modelId)
        return modelDir.deleteRecursively()
    }

    fun downloadModel(model: TtsModelInfo): Flow<DownloadProgress> = flow {
        val modelDir = getModelDir(model.id)
        modelDir.mkdirs()

        val tempFile = File(context.cacheDir, "${model.id}.tar.bz2")
        try {
            // Download
            val request = Request.Builder().url(model.downloadUrl).build()
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                emit(DownloadProgress(model.id, 0, 0, error = "HTTP ${response.code}"))
                return@flow
            }

            val body = response.body ?: run {
                emit(DownloadProgress(model.id, 0, 0, error = "Empty response"))
                return@flow
            }

            val totalBytes = body.contentLength().takeIf { it > 0 } ?: model.sizeBytes
            var downloaded = 0L

            FileOutputStream(tempFile).use { output ->
                body.byteStream().use { input ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloaded += bytesRead
                        emit(DownloadProgress(model.id, downloaded, totalBytes))
                    }
                }
            }

            // Extract
            emit(DownloadProgress(model.id, downloaded, totalBytes, isExtracting = true))
            extractTarBz2(tempFile, modelsDir)

            // Rename extracted dir if needed
            val expectedDir = File(modelsDir, model.dataDir)
            if (expectedDir.exists() && expectedDir.path != modelDir.path) {
                if (modelDir.exists()) modelDir.deleteRecursively()
                expectedDir.renameTo(modelDir)
            }

            emit(DownloadProgress(model.id, totalBytes, totalBytes, isComplete = true))
        } catch (e: Exception) {
            emit(DownloadProgress(model.id, 0, 0, error = e.message ?: "Unknown error"))
        } finally {
            tempFile.delete()
        }
    }.flowOn(Dispatchers.IO)

    private fun extractTarBz2(archive: File, destDir: File) {
        FileInputStream(archive).use { fis ->
            BufferedInputStream(fis).use { bis ->
                BZip2CompressorInputStream(bis).use { bzis ->
                    TarArchiveInputStream(bzis).use { tar ->
                        var entry = tar.nextTarEntry
                        while (entry != null) {
                            val outFile = File(destDir, entry.name)
                            // Prevent zip slip
                            if (!outFile.canonicalPath.startsWith(destDir.canonicalPath)) {
                                throw SecurityException("Zip slip detected: ${entry.name}")
                            }
                            if (entry.isDirectory) {
                                outFile.mkdirs()
                            } else {
                                outFile.parentFile?.mkdirs()
                                FileOutputStream(outFile).use { output ->
                                    tar.copyTo(output)
                                }
                            }
                            entry = tar.nextTarEntry
                        }
                    }
                }
            }
        }
    }
}
