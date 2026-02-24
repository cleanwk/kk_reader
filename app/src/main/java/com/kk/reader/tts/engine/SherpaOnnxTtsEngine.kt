package com.kk.reader.tts.engine

import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig
import com.kk.reader.tts.model.ModelFamily
import com.kk.reader.tts.model.TtsModelInfo
import java.io.File

class SherpaOnnxTtsEngine {

    private var tts: OfflineTts? = null
    private var currentModelId: String? = null
    private var _sampleRate: Int = 22050
    private var _numSpeakers: Int = 1

    val isLoaded: Boolean get() = tts != null

    fun loadModel(modelInfo: TtsModelInfo, modelDir: File): Boolean {
        release()
        return try {
            val modelPath = File(modelDir, modelInfo.modelFileName).absolutePath
            val tokensPath = File(modelDir, modelInfo.tokensFileName).absolutePath
            val dataDir = if (File(modelDir, "espeak-ng-data").exists()) {
                File(modelDir, "espeak-ng-data").absolutePath
            } else {
                ""
            }

            val vitsConfig = OfflineTtsVitsModelConfig(
                model = modelPath,
                tokens = tokensPath,
                dataDir = dataDir
            )

            val modelConfig = OfflineTtsModelConfig(
                vits = vitsConfig,
                numThreads = 2,
                debug = false
            )

            val config = OfflineTtsConfig(model = modelConfig)
            tts = OfflineTts(config = config)
            currentModelId = modelInfo.id
            _sampleRate = tts?.sampleRate() ?: 22050
            _numSpeakers = tts?.numSpeakers() ?: 1
            true
        } catch (e: Exception) {
            e.printStackTrace()
            release()
            false
        }
    }

    fun generate(text: String, speakerId: Int = 0, speed: Float = 1.0f): FloatArray? {
        val engine = tts ?: return null
        return try {
            val audio = engine.generate(text = text, sid = speakerId, speed = speed)
            audio.samples
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun sampleRate(): Int = _sampleRate

    fun numSpeakers(): Int = _numSpeakers

    fun release() {
        // OfflineTts doesn't have an explicit close, but we null the reference
        tts = null
        currentModelId = null
    }

    fun currentModelId(): String? = currentModelId
}
