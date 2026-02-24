package com.kk.reader.ui.ttsmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kk.reader.data.preferences.ReaderPreferences
import com.kk.reader.tts.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ModelUiState(
    val model: TtsModelInfo,
    val status: ModelDownloadStatus,
    val downloadProgress: Float = 0f,
    val isExtracting: Boolean = false,
    val isActive: Boolean = false
)

@HiltViewModel
class TtsModelManagerViewModel @Inject constructor(
    private val downloadManager: ModelDownloadManager,
    private val preferences: ReaderPreferences
) : ViewModel() {

    private val _models = MutableStateFlow<List<ModelUiState>>(emptyList())
    val models: StateFlow<List<ModelUiState>> = _models.asStateFlow()

    private val activeModelId = preferences.ttsModelId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), "system")

    init {
        refreshModels()
        viewModelScope.launch {
            activeModelId.collect { refreshModels() }
        }
    }

    private fun refreshModels() {
        val active = activeModelId.value
        _models.value = TtsModelRegistry.getAllModels().map { model ->
            ModelUiState(
                model = model,
                status = when {
                    model.id == "system" -> ModelDownloadStatus.DOWNLOADED
                    downloadManager.isModelDownloaded(model.id) -> ModelDownloadStatus.DOWNLOADED
                    else -> ModelDownloadStatus.NOT_DOWNLOADED
                },
                isActive = model.id == active
            )
        }
    }

    fun downloadModel(modelId: String) {
        val model = TtsModelRegistry.getModelById(modelId) ?: return
        updateModelStatus(modelId, ModelDownloadStatus.DOWNLOADING)

        viewModelScope.launch {
            downloadManager.downloadModel(model).collect { progress ->
                val downloadProgress = if (progress.totalBytes > 0) {
                    progress.bytesDownloaded.toFloat() / progress.totalBytes
                } else 0f

                if (progress.error != null) {
                    updateModelStatus(modelId, ModelDownloadStatus.NOT_DOWNLOADED)
                } else if (progress.isComplete) {
                    updateModelStatus(modelId, ModelDownloadStatus.DOWNLOADED)
                } else {
                    _models.value = _models.value.map { state ->
                        if (state.model.id == modelId) {
                            state.copy(
                                status = ModelDownloadStatus.DOWNLOADING,
                                downloadProgress = downloadProgress,
                                isExtracting = progress.isExtracting
                            )
                        } else state
                    }
                }
            }
        }
    }

    fun deleteModel(modelId: String) {
        downloadManager.deleteModel(modelId)
        // If active model was deleted, fall back to system
        if (activeModelId.value == modelId) {
            setActiveModel("system")
        }
        refreshModels()
    }

    fun setActiveModel(modelId: String) {
        viewModelScope.launch {
            preferences.setTtsModelId(modelId)
        }
    }

    private fun updateModelStatus(modelId: String, status: ModelDownloadStatus) {
        _models.value = _models.value.map { state ->
            if (state.model.id == modelId) state.copy(status = status, downloadProgress = 0f, isExtracting = false)
            else state
        }
    }
}
