package com.kk.reader.ui.reader

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kk.reader.ui.theme.ReaderTheme

@Composable
fun ReaderSettingsSheet(
    fontSize: Float,
    lineSpacing: Float,
    theme: ReaderTheme,
    onFontSizeChange: (Float) -> Unit,
    onLineSpacingChange: (Float) -> Unit,
    onThemeChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        Text("Reader Settings", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(20.dp))

        // Font size
        Text("Font Size: ${fontSize.toInt()}sp", style = MaterialTheme.typography.bodyMedium)
        Slider(
            value = fontSize,
            onValueChange = onFontSizeChange,
            valueRange = 12f..32f,
            steps = 19
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Line spacing
        Text("Line Spacing: ${"%.1f".format(lineSpacing)}", style = MaterialTheme.typography.bodyMedium)
        Slider(
            value = lineSpacing,
            onValueChange = onLineSpacingChange,
            valueRange = 1.0f..3.0f,
            steps = 19
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Theme
        Text("Theme", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            ReaderTheme.entries.forEach { t ->
                FilterChip(
                    selected = theme == t,
                    onClick = { onThemeChange(t.name.lowercase()) },
                    label = {
                        Text(
                            when (t) {
                                ReaderTheme.LIGHT -> "Light"
                                ReaderTheme.DARK -> "Dark"
                                ReaderTheme.SEPIA -> "Sepia"
                            }
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}
