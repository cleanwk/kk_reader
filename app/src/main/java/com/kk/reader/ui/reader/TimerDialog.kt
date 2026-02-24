package com.kk.reader.ui.reader

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun TimerDialog(
    onDismiss: () -> Unit,
    onSetTimer: (minutes: Int) -> Unit
) {
    var customMinutes by remember { mutableStateOf("") }
    val presets = listOf(15, 30, 45, 60)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sleep Timer") },
        text = {
            Column {
                Text("Stop reading after:", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(12.dp))

                // Preset buttons
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    presets.forEach { minutes ->
                        AssistChip(
                            onClick = {
                                onSetTimer(minutes)
                                onDismiss()
                            },
                            label = { Text("${minutes}m") }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                // Custom input
                OutlinedTextField(
                    value = customMinutes,
                    onValueChange = { customMinutes = it.filter { c -> c.isDigit() } },
                    label = { Text("Custom (minutes)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val mins = customMinutes.toIntOrNull()
                    if (mins != null && mins > 0) {
                        onSetTimer(mins)
                        onDismiss()
                    }
                },
                enabled = customMinutes.toIntOrNull()?.let { it > 0 } ?: false
            ) {
                Text("Set")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
