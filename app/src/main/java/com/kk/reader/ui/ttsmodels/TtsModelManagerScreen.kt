package com.kk.reader.ui.ttsmodels

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kk.reader.tts.model.ModelDownloadStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TtsModelManagerScreen(
    onBack: () -> Unit,
    viewModel: TtsModelManagerViewModel = hiltViewModel()
) {
    val models by viewModel.models.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("TTS Models") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(models, key = { it.model.id }) { modelState ->
                ModelCard(
                    state = modelState,
                    onDownload = { viewModel.downloadModel(modelState.model.id) },
                    onDelete = { viewModel.deleteModel(modelState.model.id) },
                    onSelect = { viewModel.setActiveModel(modelState.model.id) }
                )
            }
        }
    }
}

@Composable
private fun ModelCard(
    state: ModelUiState,
    onDownload: () -> Unit,
    onDelete: () -> Unit,
    onSelect: () -> Unit
) {
    val model = state.model

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = if (state.isActive) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = model.displayName,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = model.languages.joinToString(", ").uppercase(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (model.description.isNotEmpty()) {
                        Text(
                            text = model.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (state.isActive) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Active",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Size and speakers info
            if (model.sizeBytes > 0) {
                Text(
                    text = "Size: ${model.sizeBytes / 1_000_000}MB | Speakers: ${model.numSpeakers}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Download progress
            if (state.status == ModelDownloadStatus.DOWNLOADING) {
                LinearProgressIndicator(
                    progress = { state.downloadProgress },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (state.isExtracting) "Extracting..." else "${(state.downloadProgress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Action buttons
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                when (state.status) {
                    ModelDownloadStatus.NOT_DOWNLOADED -> {
                        Button(onClick = onDownload) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Download")
                        }
                    }
                    ModelDownloadStatus.DOWNLOADING -> {
                        OutlinedButton(onClick = {}, enabled = false) {
                            Text("Downloading...")
                        }
                    }
                    ModelDownloadStatus.DOWNLOADED -> {
                        if (!state.isActive) {
                            Button(onClick = onSelect) {
                                Text("Use")
                            }
                        }
                        if (model.sizeBytes > 0) { // Don't show delete for system TTS
                            OutlinedButton(onClick = onDelete) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Delete")
                            }
                        }
                    }
                }
            }
        }
    }
}
