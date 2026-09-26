package com.music.bitchord.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun LikedMusicScreen(state: DesktopState) {
    val tracks = state.likedTracks
    Box(Modifier.fillMaxSize().background(Black)) {
        CoverWash(tracks.firstOrNull(), Modifier.fillMaxWidth().height(400.dp))
        LazyColumn(
            contentPadding = PaddingValues(start = 28.dp, end = 28.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            item {
                IconButton(onClick = state::back, modifier = Modifier.padding(top = 12.dp).focusOutline(CircleShape)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back to previous page", tint = Color.White)
                }
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val narrow = maxWidth < 630.dp
                    if (narrow) {
                        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            CoverPlaceholder(tracks.firstOrNull(), Modifier.size(148.dp), 13.dp)
                            Spacer(Modifier.height(15.dp))
                            PlaylistCredits(tracks.size)
                            PlaylistActions(state, tracks)
                        }
                    } else {
                        Row(Modifier.fillMaxWidth().padding(vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                            CoverPlaceholder(tracks.firstOrNull(), Modifier.size(174.dp), 13.dp)
                            Column(Modifier.padding(start = 24.dp)) {
                                PlaylistCredits(tracks.size)
                                PlaylistActions(state, tracks)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("Songs", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
            }
            if (tracks.isEmpty()) {
                item { Text("Songs you like will appear here.", color = Muted, fontSize = 13.sp, modifier = Modifier.padding(vertical = 18.dp)) }
            } else {
                items(tracks, key = { it.id }) { track ->
                    DesktopTrackRow(track, state, tracks, "Liked Music", showLike = true)
                }
            }
        }
    }
}

@Composable
private fun PlaylistCredits(count: Int) {
    Text("Liked Music", fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.8).sp)
    Text("Auto playlist · $count songs", color = Color.White.copy(alpha = 0.65f), fontSize = 13.sp)
}

@Composable
private fun PlaylistActions(state: DesktopState, tracks: List<MockTrack>) {
    Row(Modifier.padding(top = 18.dp, bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Button(
            onClick = { tracks.firstOrNull()?.let { state.selectTrack(it, tracks, "Liked Music") } },
            enabled = tracks.isNotEmpty(),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Black),
            shape = RoundedCornerShape(22.dp),
        ) {
            Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(19.dp))
            Text("Play", fontWeight = FontWeight.SemiBold)
        }
        IconButton(
            onClick = {
                val shuffled = tracks.shuffled()
                shuffled.firstOrNull()?.let { state.selectTrack(it, shuffled, "Liked Music") }
            },
            enabled = tracks.isNotEmpty(),
            modifier = Modifier.focusOutline(CircleShape).background(Charcoal, CircleShape),
        ) { Icon(Icons.Default.Shuffle, "Shuffle Liked Music", tint = Color.White) }
    }
}
