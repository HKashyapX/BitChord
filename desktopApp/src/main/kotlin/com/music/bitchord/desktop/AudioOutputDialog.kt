package com.music.bitchord.desktop

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun AudioOutputDialog(state: DesktopState) {
    var volume by remember { mutableFloatStateOf(state.outputVolume) }
    LaunchedEffect(state.outputVolume) { volume = state.outputVolume }
    AlertDialog(
        onDismissRequest = state::closeOutput,
        title = { Text("Audio output") },
        text = {
            Column(Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState())) {
                if (state.outputLoading) Text("Loading devices…")
                state.outputError?.let { Text(it, color = Red) }
                if (!state.outputLoading && state.outputError == null) {
                    state.outputDevices.forEach { device ->
                        Text(
                            "${if (device.name == state.outputSelected) "✓  " else ""}${device.description}",
                            Modifier.fillMaxWidth().clickable { state.audio?.selectOutput(device.name) }
                                .padding(vertical = 11.dp),
                        )
                    }
                    Text("Volume ${volume.toInt()}%")
                    Slider(value = volume, onValueChange = { volume = it }, valueRange = 0f..100f,
                        onValueChangeFinished = { state.audio?.setVolume(volume) })
                    Text("Device names and volume are reported by mpv; system controls may also affect the final level.")
                }
            }
        },
        confirmButton = { TextButton(onClick = state::closeOutput) { Text("Done") } },
    )
}
