package com.music.bitchord.desktop

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun LyricsProviderDialog(state: DesktopState) {
    AlertDialog(
        onDismissRequest = { state.lyricsProviderDialogOpen = false },
        title = { Text("Choose lyrics provider") },
        text = {
            Column {
                state.lyricsProviders.forEach { (label, value) ->
                    val current = label == state.lyricsSource || value == "auto" && state.lyricsSource.isBlank()
                    Text(
                        "${if (current) "✓  " else ""}$label",
                        Modifier.fillMaxWidth().clickable { state.chooseLyricsProvider(value) }
                            .padding(vertical = 13.dp),
                    )
                }
                if (state.lyricsLoading) Text("Searching…", color = Muted)
            }
        },
        confirmButton = {
            TextButton(onClick = { state.lyricsProviderDialogOpen = false }) { Text("Close") }
        },
    )
}
