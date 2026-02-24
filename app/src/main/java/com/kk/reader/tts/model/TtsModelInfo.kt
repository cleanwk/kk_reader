package com.kk.reader.tts.model

data class TtsModelInfo(
    val id: String,
    val displayName: String,
    val family: ModelFamily,
    val languages: List<String>,
    val downloadUrl: String,
    val sizeBytes: Long,
    val numSpeakers: Int,
    val modelFileName: String,
    val tokensFileName: String,
    val dataDir: String,
    val description: String = ""
)

enum class ModelFamily {
    PIPER, KOKORO, VITS_CHINESE, SYSTEM
}

enum class ModelDownloadStatus {
    NOT_DOWNLOADED, DOWNLOADING, DOWNLOADED
}
