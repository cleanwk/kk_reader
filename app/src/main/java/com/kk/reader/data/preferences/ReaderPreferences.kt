package com.kk.reader.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "reader_prefs")

@Singleton
class ReaderPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    companion object {
        val FONT_SIZE = floatPreferencesKey("font_size")
        val LINE_SPACING = floatPreferencesKey("line_spacing")
        val THEME = stringPreferencesKey("theme")
        val TTS_SPEED = floatPreferencesKey("tts_speed")
        val TTS_MODEL_ID = stringPreferencesKey("tts_model_id")
        val TTS_SPEAKER_ID = intPreferencesKey("tts_speaker_id")
    }

    val fontSize: Flow<Float> = dataStore.data.map { it[FONT_SIZE] ?: 16f }
    val lineSpacing: Flow<Float> = dataStore.data.map { it[LINE_SPACING] ?: 1.5f }
    val theme: Flow<String> = dataStore.data.map { it[THEME] ?: "light" }
    val ttsSpeed: Flow<Float> = dataStore.data.map { it[TTS_SPEED] ?: 1.0f }
    val ttsModelId: Flow<String> = dataStore.data.map { it[TTS_MODEL_ID] ?: "system" }
    val ttsSpeakerId: Flow<Int> = dataStore.data.map { it[TTS_SPEAKER_ID] ?: 0 }

    suspend fun setFontSize(size: Float) = dataStore.edit { it[FONT_SIZE] = size }
    suspend fun setLineSpacing(spacing: Float) = dataStore.edit { it[LINE_SPACING] = spacing }
    suspend fun setTheme(theme: String) = dataStore.edit { it[THEME] = theme }
    suspend fun setTtsSpeed(speed: Float) = dataStore.edit { it[TTS_SPEED] = speed }
    suspend fun setTtsModelId(modelId: String) = dataStore.edit { it[TTS_MODEL_ID] = modelId }
    suspend fun setTtsSpeakerId(speakerId: Int) = dataStore.edit { it[TTS_SPEAKER_ID] = speakerId }
}
