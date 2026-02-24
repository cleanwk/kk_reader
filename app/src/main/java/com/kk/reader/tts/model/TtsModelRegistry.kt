package com.kk.reader.tts.model

object TtsModelRegistry {

    val models: List<TtsModelInfo> = listOf(
        TtsModelInfo(
            id = "piper-libritts-medium",
            displayName = "Piper English (LibriTTS Medium)",
            family = ModelFamily.PIPER,
            languages = listOf("en"),
            downloadUrl = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-piper-en_US-libritts-high.tar.bz2",
            sizeBytes = 75_000_000L,
            numSpeakers = 904,
            modelFileName = "en_US-libritts-high.onnx",
            tokensFileName = "tokens.txt",
            dataDir = "vits-piper-en_US-libritts-high",
            description = "High quality English multi-speaker model"
        ),
        TtsModelInfo(
            id = "piper-amy-low",
            displayName = "Piper English (Amy Low)",
            family = ModelFamily.PIPER,
            languages = listOf("en"),
            downloadUrl = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-piper-en_US-amy-low.tar.bz2",
            sizeBytes = 20_000_000L,
            numSpeakers = 1,
            modelFileName = "en_US-amy-low.onnx",
            tokensFileName = "tokens.txt",
            dataDir = "vits-piper-en_US-amy-low",
            description = "Lightweight English single-speaker model"
        ),
        TtsModelInfo(
            id = "kokoro-multi-v1",
            displayName = "Kokoro Multi-Language v1.0",
            family = ModelFamily.KOKORO,
            languages = listOf("en", "zh", "ja", "ko", "fr", "de", "es", "it"),
            downloadUrl = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/kokoro-multi-lang-v1_0.tar.bz2",
            sizeBytes = 80_000_000L,
            numSpeakers = 53,
            modelFileName = "model.onnx",
            tokensFileName = "tokens.txt",
            dataDir = "kokoro-multi-lang-v1_0",
            description = "Multi-language model supporting 8 languages"
        ),
        TtsModelInfo(
            id = "vits-zh-fanchen",
            displayName = "VITS Chinese (Fanchen)",
            family = ModelFamily.VITS_CHINESE,
            languages = listOf("zh"),
            downloadUrl = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-zh-fanchen.tar.bz2",
            sizeBytes = 115_000_000L,
            numSpeakers = 187,
            modelFileName = "model.onnx",
            tokensFileName = "tokens.txt",
            dataDir = "vits-zh-fanchen",
            description = "Chinese multi-speaker model"
        )
    )

    val systemModel = TtsModelInfo(
        id = "system",
        displayName = "Android System TTS",
        family = ModelFamily.SYSTEM,
        languages = listOf("*"),
        downloadUrl = "",
        sizeBytes = 0,
        numSpeakers = 0,
        modelFileName = "",
        tokensFileName = "",
        dataDir = "",
        description = "Uses the device's built-in TTS engine"
    )

    fun getModelById(id: String): TtsModelInfo? {
        if (id == "system") return systemModel
        return models.find { it.id == id }
    }

    fun getAllModels(): List<TtsModelInfo> = models + systemModel
}
