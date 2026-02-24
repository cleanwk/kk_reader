package com.kk.reader.ui.reader

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kk.reader.tts.TtsPlaybackState

@Composable
fun TtsControlPanel(
    playbackState: TtsPlaybackState,
    speed: Float,
    speakerId: Int,
    numSpeakers: Int,
    modelName: String,
    onTogglePlayback: () -> Unit,
    onStop: () -> Unit,
    onSpeedChange: (Float) -> Unit,
    onSpeakerChange: (Int) -> Unit,
    onChangeModel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        Text("Text-to-Speech", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        // Model info
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Model: $modelName",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onChangeModel) {
                Text("Change")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        // Playback controls
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onStop) {
                Icon(Icons.Default.Stop, contentDescription = "Stop")
            }
            Spacer(modifier = Modifier.width(16.dp))
            FloatingActionButton(
                onClick = onTogglePlayback,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    when (playbackState) {
                        TtsPlaybackState.PLAYING -> Icons.Default.Pause
                        TtsPlaybackState.LOADING -> Icons.Default.HourglassTop
                        else -> Icons.Default.PlayArrow
                    },
                    contentDescription = "Play/Pause"
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Speed control
        Text("Speed: ${"%.1f".format(speed)}x", style = MaterialTheme.typography.bodyMedium)
        Slider(
            value = speed,
            onValueChange = onSpeedChange,
            valueRange = 0.5f..3.0f,
            steps = 24
        )

        // Speaker selector (only if model has multiple speakers)
        if (numSpeakers > 1) {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Speaker: $speakerId", style = MaterialTheme.typography.bodyMedium)
            Slider(
                value = speakerId.toFloat(),
                onValueChange = { onSpeakerChange(it.toInt()) },
                valueRange = 0f..(numSpeakers - 1).toFloat(),
                steps = maxOf(0, numSpeakers - 2)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
