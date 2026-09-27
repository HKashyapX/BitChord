package com.music.bitchord.desktop

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun AudioPipelineDialog(state: DesktopState) {
    AlertDialog(
        onDismissRequest = { state.pipelineDialogOpen = false },
        title = { Text("Stream preference and pipeline") },
        text = {
            Column {
                Text("Choose a stream preference. The provider may offer the same format for both choices.")
                TextButton(onClick = { state.changeQuality("best") }) {
                    Text("${if (state.qualityMode == "best") "✓  " else ""}Best available")
                }
                TextButton(onClick = { state.changeQuality("standard") }) {
                    Text("${if (state.qualityMode == "standard") "✓  " else ""}Standard when available")
                }
                if (state.pipelineLoading) Text("Reading playback details…")
                state.pipelineError?.let { Text(it, color = Red) }
                state.pipelineFields.forEach { (name, value) ->
                    if (value.isNotBlank()) Text("$name · $value", Modifier.padding(vertical = 3.dp))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { state.pipelineDialogOpen = false }) { Text("Done") }
        },
    )
}
