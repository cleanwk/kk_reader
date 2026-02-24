package com.kk.reader.tts

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.kk.reader.data.preferences.ReaderPreferences
import com.kk.reader.tts.engine.SherpaOnnxTtsEngine
import com.kk.reader.tts.engine.SystemTtsEngine
import com.kk.reader.tts.model.ModelDownloadManager
import com.kk.reader.tts.model.TtsModelRegistry
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

enum class TtsPlaybackState {
    IDLE, LOADING, PLAYING, PAUSED
}

@Singleton
class TtsManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferences: ReaderPreferences,
    private val downloadManager: ModelDownloadManager
) {
    private val sherpaEngine = SherpaOnnxTtsEngine()
    private val systemEngine by lazy { SystemTtsEngine(context) }

    private val _playbackState = MutableStateFlow(TtsPlaybackState.IDLE)
    val playbackState: StateFlow<TtsPlaybackState> = _playbackState.asStateFlow()

    private var audioTrack: AudioTrack? = null
    private var playbackJob: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var currentSpeed = 1.0f
    private var currentSpeaker = 0
    private var currentModelId = "system"
    private var isPaused = false
    private var pendingText: String? = null

    init {
        scope.launch {
            preferences.ttsModelId.collect { modelId ->
                currentModelId = modelId
            }
        }
        scope.launch {
            preferences.ttsSpeed.collect { speed ->
                currentSpeed = speed
            }
        }
        scope.launch {
            preferences.ttsSpeakerId.collect { id ->
                currentSpeaker = id
            }
        }
    }

    fun startReading(text: String) {
        if (text.isBlank()) return
        pendingText = text
        _playbackState.value = TtsPlaybackState.LOADING

        playbackJob?.cancel()
        playbackJob = scope.launch {
            try {
                if (currentModelId == "system") {
                    playWithSystemTts(text)
                } else {
                    playWithSherpa(text)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                e.printStackTrace()
                _playbackState.value = TtsPlaybackState.IDLE
            }
        }
    }

    private suspend fun playWithSystemTts(text: String) {
        _playbackState.value = TtsPlaybackState.PLAYING
        val sentences = splitIntoSentences(text)
        for (sentence in sentences) {
            if (!isActive) break
            val deferred = CompletableDeferred<Unit>()
            withContext(Dispatchers.Main) {
                systemEngine.speak(sentence, currentSpeed) {
                    deferred.complete(Unit)
                }
            }
            deferred.await()
        }
        _playbackState.value = TtsPlaybackState.IDLE
    }

    private suspend fun playWithSherpa(text: String) {
        // Ensure model is loaded
        if (sherpaEngine.currentModelId() != currentModelId) {
            val modelInfo = TtsModelRegistry.getModelById(currentModelId) ?: run {
                _playbackState.value = TtsPlaybackState.IDLE
                return
            }
            val modelDir = downloadManager.getModelDir(currentModelId)
            if (!modelDir.exists()) {
                _playbackState.value = TtsPlaybackState.IDLE
                return
            }
            val loaded = sherpaEngine.loadModel(modelInfo, modelDir)
            if (!loaded) {
                _playbackState.value = TtsPlaybackState.IDLE
                return
            }
        }

        _playbackState.value = TtsPlaybackState.PLAYING
        val sampleRate = sherpaEngine.sampleRate()

        val sentences = splitIntoSentences(text)
        for (sentence in sentences) {
            if (!isActive || isPaused) {
                if (isPaused) {
                    _playbackState.value = TtsPlaybackState.PAUSED
                    return
                }
                break
            }

            val samples = sherpaEngine.generate(sentence, currentSpeaker, currentSpeed) ?: continue
            playAudio(samples, sampleRate)
        }

        _playbackState.value = TtsPlaybackState.IDLE
    }

    private suspend fun playAudio(samples: FloatArray, sampleRate: Int) {
        val shortSamples = ShortArray(samples.size) { i ->
            (samples[i] * 32767).toInt().coerceIn(-32768, 32767).toShort()
        }

        val bufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .build()
            )
            .setBufferSizeInBytes(maxOf(bufferSize, shortSamples.size * 2))
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        audioTrack = track
        track.write(shortSamples, 0, shortSamples.size)
        track.play()

        // Wait for playback to finish
        val durationMs = (samples.size.toLong() * 1000) / sampleRate
        delay(durationMs + 100)

        track.stop()
        track.release()
        audioTrack = null
    }

    fun pause() {
        isPaused = true
        audioTrack?.pause()
        systemEngine.pause()
        _playbackState.value = TtsPlaybackState.PAUSED
    }

    fun resume() {
        isPaused = false
        val text = pendingText ?: return
        startReading(text)
    }

    fun stop() {
        isPaused = false
        pendingText = null
        playbackJob?.cancel()
        playbackJob = null
        audioTrack?.let {
            try {
                it.stop()
                it.release()
            } catch (_: Exception) {}
        }
        audioTrack = null
        systemEngine.stop()
        _playbackState.value = TtsPlaybackState.IDLE
    }

    fun setSpeed(speed: Float) {
        currentSpeed = speed
    }

    fun setSpeaker(speakerId: Int) {
        currentSpeaker = speakerId
    }

    fun numSpeakers(): Int {
        return if (currentModelId == "system") 0
        else sherpaEngine.numSpeakers()
    }

    fun currentModelName(): String {
        return TtsModelRegistry.getModelById(currentModelId)?.displayName ?: "System TTS"
    }

    fun release() {
        stop()
        sherpaEngine.release()
        systemEngine.release()
        scope.cancel()
    }

    private fun splitIntoSentences(text: String): List<String> {
        return text.split(Regex("[.!?。！？\n]+"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    private val isActive: Boolean
        get() = playbackJob?.isActive == true
}
