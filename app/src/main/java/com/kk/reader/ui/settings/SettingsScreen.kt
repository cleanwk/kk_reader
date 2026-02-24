package com.kk.reader.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.kk.reader.data.preferences.ReaderPreferences
import com.kk.reader.tts.model.ModelDownloadManager
import com.kk.reader.tts.model.TtsModelRegistry
import com.kk.reader.ui.theme.ReaderTheme
import com.kk.reader.update.AppUpdateManager
import com.kk.reader.update.ReleaseInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class UpdateUiState {
    data object Idle : UpdateUiState()
    data object Checking : UpdateUiState()
    data object UpToDate : UpdateUiState()
    data class UpdateAvailable(val release: ReleaseInfo) : UpdateUiState()
    data class Downloading(val progress: Float) : UpdateUiState()
    data class Error(val message: String) : UpdateUiState()
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferences: ReaderPreferences,
    private val downloadManager: ModelDownloadManager,
    val updateManager: AppUpdateManager
) : ViewModel() {
    val fontSize = preferences.fontSize.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 16f)
    val theme = preferences.theme.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), "light")
    val ttsModelId = preferences.ttsModelId.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), "system")

    private val _updateState = MutableStateFlow<UpdateUiState>(UpdateUiState.Idle)
    val updateState: StateFlow<UpdateUiState> = _updateState.asStateFlow()

    fun setFontSize(size: Float) { viewModelScope.launch { preferences.setFontSize(size) } }
    fun setTheme(theme: String) { viewModelScope.launch { preferences.setTheme(theme) } }
    fun setTtsModel(modelId: String) { viewModelScope.launch { preferences.setTtsModelId(modelId) } }

    fun checkForUpdate() {
        viewModelScope.launch {
            _updateState.value = UpdateUiState.Checking
            val result = updateManager.checkForUpdate()
            result.fold(
                onSuccess = { release ->
                    _updateState.value = if (release != null) {
                        UpdateUiState.UpdateAvailable(release)
                    } else {
                        UpdateUiState.UpToDate
                    }
                },
                onFailure = { e ->
                    _updateState.value = UpdateUiState.Error(e.message ?: "Check failed")
                }
            )
        }
    }

    fun downloadAndInstall(release: ReleaseInfo) {
        viewModelScope.launch {
            updateManager.downloadApk(release.apkDownloadUrl).collect { progress ->
                when {
                    progress.error != null -> {
                        _updateState.value = UpdateUiState.Error(progress.error)
                    }
                    progress.isComplete && progress.apkFile != null -> {
                        _updateState.value = UpdateUiState.Idle
                        updateManager.installApk(progress.apkFile)
                    }
                    else -> {
                        val pct = if (progress.totalBytes > 0) {
                            progress.bytesDownloaded.toFloat() / progress.totalBytes
                        } else 0f
                        _updateState.value = UpdateUiState.Downloading(pct)
                    }
                }
            }
        }
    }

    fun dismissUpdate() {
        _updateState.value = UpdateUiState.Idle
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val fontSize by viewModel.fontSize.collectAsStateWithLifecycle()
    val theme by viewModel.theme.collectAsStateWithLifecycle()
    val ttsModelId by viewModel.ttsModelId.collectAsStateWithLifecycle()
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Reading section
            Text("Reading", style = MaterialTheme.typography.titleMedium)

            // Default font size
            Column {
                Text("Default Font Size: ${fontSize.toInt()}sp", style = MaterialTheme.typography.bodyMedium)
                Slider(
                    value = fontSize,
                    onValueChange = viewModel::setFontSize,
                    valueRange = 12f..32f,
                    steps = 19
                )
            }

            // Default theme
            Column {
                Text("Default Theme", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReaderTheme.entries.forEach { t ->
                        FilterChip(
                            selected = theme == t.name.lowercase(),
                            onClick = { viewModel.setTheme(t.name.lowercase()) },
                            label = { Text(t.name.lowercase().replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }
            }

            HorizontalDivider()

            // TTS section
            Text("Text-to-Speech", style = MaterialTheme.typography.titleMedium)

            // Default TTS model
            Column {
                Text("Default TTS Model", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
                val availableModels = TtsModelRegistry.getAllModels()
                availableModels.forEach { model ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        RadioButton(
                            selected = ttsModelId == model.id,
                            onClick = { viewModel.setTtsModel(model.id) }
                        )
                        Column(modifier = Modifier.padding(start = 8.dp)) {
                            Text(model.displayName, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                model.languages.joinToString(", ").uppercase(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            HorizontalDivider()

            // About & Update section
            Text("About", style = MaterialTheme.typography.titleMedium)
            Column {
                val packageInfo = try {
                    context.packageManager.getPackageInfo(context.packageName, 0)
                } catch (_: Exception) { null }
                Text(
                    "KK Reader v${packageInfo?.versionName ?: "0.1.0"}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "A free, open-source book reader with text-to-speech support.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Update section
            UpdateSection(
                state = updateState,
                onCheckUpdate = viewModel::checkForUpdate,
                onDownload = viewModel::downloadAndInstall,
                onDismiss = viewModel::dismissUpdate
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun UpdateSection(
    state: UpdateUiState,
    onCheckUpdate: () -> Unit,
    onDownload: (ReleaseInfo) -> Unit,
    onDismiss: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.SystemUpdate,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("App Update", style = MaterialTheme.typography.titleSmall)
            }
            Spacer(modifier = Modifier.height(12.dp))

            when (state) {
                is UpdateUiState.Idle -> {
                    Button(onClick = onCheckUpdate) {
                        Text("Check for Updates")
                    }
                }
                is UpdateUiState.Checking -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Checking for updates...", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                is UpdateUiState.UpToDate -> {
                    Text(
                        "You're on the latest version.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(onClick = onCheckUpdate) {
                        Text("Check Again")
                    }
                }
                is UpdateUiState.UpdateAvailable -> {
                    Text(
                        "New version available: v${state.release.versionName}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (state.release.releaseNotes.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            state.release.releaseNotes.take(200),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { onDownload(state.release) }) {
                            Text("Download & Install")
                        }
                        OutlinedButton(onClick = onDismiss) {
                            Text("Later")
                        }
                    }
                }
                is UpdateUiState.Downloading -> {
                    Text("Downloading update...", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { state.progress },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "${(state.progress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                is UpdateUiState.Error -> {
                    Text(
                        "Update check failed: ${state.message}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(onClick = onCheckUpdate) {
                        Text("Retry")
                    }
                }
            }
        }
    }
}
