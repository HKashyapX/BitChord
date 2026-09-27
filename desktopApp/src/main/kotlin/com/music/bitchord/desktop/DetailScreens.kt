package com.music.bitchord.desktop

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun DetailScreen(state: DesktopState) {
    val selected = state.detailTrack ?: return
    val isAlbum = state.destination == Destination.ALBUM
    val title = if (isAlbum) selected.album.ifBlank { "Unknown album" } else selected.artist
    TrackCollectionScreen(state, title, if (isAlbum) "Album · ${selected.artist}" else "Artist",
        state.detailTracks, if (isAlbum) "Album" else "Artist")
}

@Composable
internal fun PlaylistsScreen(state: DesktopState) {
    var newName by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(28.dp)) {
        Text("Playlists", fontSize = 34.sp, fontWeight = FontWeight.Bold)
        Text("Local desktop playlists · saved on this device", color = Muted, fontSize = 12.sp)
        Spacer(Modifier.height(18.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(newName, { newName = it }, label = { Text("New playlist name") },
                singleLine = true, modifier = Modifier.weight(1f))
            TextButton(onClick = { if (state.createPlaylist(newName)) newName = "" },
                enabled = newName.isNotBlank() && newName.trim() !in state.playlists) { Text("Create") }
        }
        LazyColumn {
            items(state.playlists.keys.sorted()) { name ->
                Row(Modifier.fillMaxWidth().clickable { state.openPlaylist(name) }.padding(vertical = 17.dp)) {
                    Text(name, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                    Text("${state.playlists[name].orEmpty().size} songs", color = Muted)
                }
            }
        }
    }
}

@Composable
internal fun PlaylistScreen(state: DesktopState) {
    val name = state.selectedPlaylist ?: return
    TrackCollectionScreen(state, name, "Local playlist", state.playlists[name].orEmpty(), name)
}

@Composable
private fun TrackCollectionScreen(state: DesktopState, title: String, subtitle: String,
    tracks: List<MockTrack>, origin: String) {
    Column(Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 14.dp)) {
        IconButton(onClick = state::back) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
        Text(title, fontSize = 34.sp, fontWeight = FontWeight.Bold)
        Text("$subtitle · ${tracks.size} songs", color = Muted, fontSize = 13.sp)
        Spacer(Modifier.height(14.dp))
        Button(onClick = { tracks.firstOrNull()?.let { state.selectTrack(it, tracks, origin) } },
            enabled = tracks.isNotEmpty()) {
            Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(20.dp))
            Text("Play")
        }
        Spacer(Modifier.height(18.dp))
        if (tracks.isEmpty()) Text("No songs here yet.", color = Muted)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            items(tracks, key = { it.id }) { track ->
                DesktopTrackRow(track, state, tracks, origin, showLike = true)
            }
        }
    }
}
