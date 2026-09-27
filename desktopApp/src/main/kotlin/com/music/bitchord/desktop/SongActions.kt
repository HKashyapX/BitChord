package com.music.bitchord.desktop

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun SongActionsDialog(state: DesktopState, track: MockTrack) {
    AlertDialog(
        onDismissRequest = state::closeActions,
        title = { Text(track.title) },
        text = {
            Column(Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState())) {
                ActionLine(if (state.isLiked(track)) "Remove from Liked Music" else "Like") {
                    state.toggleLike(track); state.closeActions()
                }
                ActionLine(if (track.id in state.dislikedIds) "Undo dislike" else "Dislike") {
                    state.toggleDislike(track); state.closeActions()
                }
                ActionLine("Add to playlist") { state.openPlaylistPicker(track) }
                ActionLine("Play next") { state.playNext(track); state.closeActions() }
                ActionLine("Add to queue") { state.addToQueue(track); state.closeActions() }
                if (track.id !in mockTracks.map { it.id }) {
                    ActionLine(when (track.id) {
                        in state.downloadingIds -> "Downloading…"
                        in state.downloadedIds -> "Downloaded"
                        else -> "Download"
                    }) { state.downloadTrack(track); state.closeActions() }
                    ActionLine("Start radio") { state.startRadio(track); state.closeActions() }
                }
                ActionLine("Open album") { state.openAlbum(track) }
                ActionLine("Open artist") { state.openArtist(track) }
                if (track.id !in mockTracks.map { it.id }) {
                    ActionLine("Share link") { state.copyShareLink(track); state.closeActions() }
                }
                ActionLine("Copy playback log") { state.copyPlaybackLog(track); state.closeActions() }
                if (track.id == state.currentTrack?.id) {
                    if (state.isRealTrack) {
                        ActionLine("Upgrade quality / audio pipeline") { state.closeActions(); state.openPipeline() }
                    }
                    ActionLine("Sleep timer · 15 minutes") { state.setSleepTimer(15); state.closeActions() }
                    ActionLine("Sleep timer · 30 minutes") { state.setSleepTimer(30); state.closeActions() }
                    ActionLine("Sleep timer · 60 minutes") { state.setSleepTimer(60); state.closeActions() }
                    if (state.sleepTimerEndsAt != null) {
                        ActionLine("Cancel sleep timer") { state.setSleepTimer(0); state.closeActions() }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = state::closeActions) { Text("Close") } },
    )
}

@Composable
private fun ActionLine(label: String, onClick: () -> Unit) {
    Text(label, modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 13.dp))
}

@Composable
internal fun PlaylistPickerDialog(state: DesktopState, track: MockTrack) {
    var name by remember(track.id) { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = state::closePlaylistPicker,
        title = { Text("Add ${track.title} to playlist") },
        text = {
            Column {
                state.playlists.keys.sorted().forEach { playlist ->
                    ActionLine(playlist) {
                        state.addToPlaylist(playlist, track)
                        state.closePlaylistPicker()
                    }
                }
                Row(Modifier.fillMaxWidth()) {
                    OutlinedTextField(name, onValueChange = { name = it }, label = { Text("New playlist") },
                        singleLine = true, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = {
                        if (state.createPlaylist(name)) {
                            state.addToPlaylist(name.trim(), track)
                            state.closePlaylistPicker()
                        }
                    }, enabled = name.isNotBlank() && name.trim() !in state.playlists) { Text("Create") }
                }
            }
        },
        confirmButton = { TextButton(onClick = state::closePlaylistPicker) { Text("Close") } },
    )
}
