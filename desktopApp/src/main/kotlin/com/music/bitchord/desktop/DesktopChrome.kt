package com.music.bitchord.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun Sidebar(state: DesktopState) {
    Column(Modifier.width(210.dp).fillMaxHeight().background(Color(0xFF0D0D0F)).padding(horizontal = 14.dp, vertical = 18.dp)) {
        Row(Modifier.padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(34.dp).background(Red, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.MusicNote, null, tint = Color.White, modifier = Modifier.size(21.dp))
            }
            Spacer(Modifier.width(10.dp))
            Text("BitChord", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(23.dp))
        SidebarLabel("DISCOVER")
        SidebarItem("Home", Icons.Default.Home, state.destination == Destination.HOME) { state.navigate(Destination.HOME) }
        SidebarItem("Search", Icons.Default.Search, state.destination == Destination.SEARCH) { state.navigate(Destination.SEARCH) }
        Spacer(Modifier.height(22.dp))
        SidebarLabel("LIBRARY")
        SidebarItem("Liked Music", Icons.Default.Favorite, state.destination == Destination.LIKED_MUSIC) { state.navigate(Destination.LIKED_MUSIC) }
        SidebarItem("Songs", Icons.Default.LibraryMusic, state.destination == Destination.SONGS) { state.navigate(Destination.SONGS) }
        SidebarItem("Playlists", Icons.Default.QueueMusic, state.destination == Destination.PLAYLISTS || state.destination == Destination.PLAYLIST) { state.navigate(Destination.PLAYLISTS) }
        Spacer(Modifier.weight(1f))
        Text("YouTube Music · local library", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(8.dp))
    }
}

@Composable
private fun SidebarLabel(label: String) {
    Text(label, color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp, modifier = Modifier.padding(start = 10.dp, bottom = 7.dp))
}

@Composable
private fun SidebarItem(label: String, icon: ImageVector, active: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(38.dp).clip(RoundedCornerShape(8.dp))
            .background(if (active) Charcoal else Color.Transparent).focusOutline().clickable(onClick = onClick).padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = if (active) Red else Muted, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(11.dp))
        Text(label, color = if (active) Color.White else Color(0xFFD0D0D2), fontSize = 13.sp, fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal)
    }
}

@Composable
internal fun QueueSidebar(state: DesktopState) {
    Column(Modifier.width(270.dp).fillMaxHeight().background(Color(0xFF0D0D0F)).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.AutoMirrored.Filled.QueueMusic, null, tint = Red, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(9.dp))
            Text("Up next", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("Clear", color = Muted, fontSize = 11.sp, modifier = Modifier.focusOutline().clickable { state.clearUpcoming() }.padding(7.dp))
        }
        Text("${(state.queue.size - state.currentIndex - 1).coerceAtLeast(0)} songs", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(start = 29.dp, top = 3.dp, bottom = 13.dp))
        QueueModeControls(state)
        val upcoming = state.queue.drop(state.currentIndex + 1)
        if (upcoming.isEmpty()) {
            Text("Nothing up next", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 22.dp))
        } else {
            LazyColumn(Modifier.weight(1f)) {
                itemsIndexed(upcoming) { offset, track ->
                    val index = state.currentIndex + offset + 1
                    Row(
                        Modifier.fillMaxWidth().height(55.dp).focusOutline().clickable {
                            state.selectQueued(index)
                            state.navigate(Destination.NOW_PLAYING)
                        }.padding(5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CoverPlaceholder(track, Modifier.size(38.dp), 6.dp)
                        Spacer(Modifier.width(9.dp))
                        Column(Modifier.weight(1f)) {
                            Text(track.title, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(track.artist, color = Muted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        QueueMoveButtons(state, index)
                        IconButton(onClick = { state.removeUpcoming(index) }, modifier = Modifier.size(27.dp)) {
                            Icon(Icons.Default.Close, "Remove ${track.title} from queue", tint = Muted, modifier = Modifier.size(15.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun BottomPlayer(state: DesktopState) {
    val track = state.currentTrack ?: return
    Box(Modifier.fillMaxWidth().height(82.dp).background(Black).padding(horizontal = 10.dp, vertical = 5.dp)) {
        val shape = RoundedCornerShape(18.dp)
        Box(Modifier.fillMaxSize().clip(shape).background(Charcoal).focusOutline(shape)) {
            CoverWash(track, Modifier.fillMaxSize().blur(24.dp))
            Box(Modifier.fillMaxSize().background(Color(0xE817171A)))
            Row(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Row(
                    Modifier.weight(1f).focusOutline().clickable { state.navigate(Destination.NOW_PLAYING) },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CoverPlaceholder(track, Modifier.size(46.dp), 7.dp)
                    Spacer(Modifier.width(11.dp))
                    Column(Modifier.weight(1f)) {
                        Text(track.title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(track.artist, color = Muted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = state::previous, modifier = Modifier.size(34.dp).focusOutline(CircleShape)) {
                            Icon(Icons.Default.SkipPrevious, "Previous", modifier = Modifier.size(23.dp))
                        }
                        IconButton(onClick = state::togglePlayback, modifier = Modifier.size(38.dp).focusOutline(CircleShape)) {
                            Icon(if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, if (state.isPlaying) "Pause" else "Play", modifier = Modifier.size(26.dp))
                        }
                        IconButton(onClick = state::next, modifier = Modifier.size(34.dp).focusOutline(CircleShape)) {
                            Icon(Icons.Default.SkipNext, "Next", modifier = Modifier.size(23.dp))
                        }
                    }
                    Box(Modifier.fillMaxWidth(0.85f).height(2.dp).background(Color.White.copy(alpha = 0.16f), CircleShape)) {
                        Box(Modifier.fillMaxWidth(state.progress).height(2.dp).background(Color.White, CircleShape))
                    }
                }
                Text("${formatTime((track.durationSeconds * state.progress).toInt())} / ${formatTime(track.durationSeconds)}", color = Muted, fontSize = 11.sp, modifier = Modifier.weight(1f).padding(end = 6.dp), textAlign = androidx.compose.ui.text.style.TextAlign.End)
            }
        }
    }
}

@Composable
internal fun SongsScreen(state: DesktopState) {
    val tracks = state.knownTracks.filter { known -> mockTracks.none { it.id == known.id } }
    Column(Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 24.dp)) {
        Text("Songs", fontSize = 34.sp, fontWeight = FontWeight.ExtraBold)
        Text("${tracks.size} songs", color = Muted, fontSize = 12.sp)
        Spacer(Modifier.height(18.dp))
        HorizontalDivider(color = Stroke)
        LazyColumn(contentPadding = PaddingValues(bottom = 20.dp)) {
            if (tracks.isEmpty()) {
                item { Text("Songs you play will appear here.", color = Muted, fontSize = 13.sp, modifier = Modifier.padding(vertical = 18.dp)) }
            } else {
                itemsIndexed(tracks) { _, track -> DesktopTrackRow(track, state, tracks, "Songs", showLike = true) }
            }
        }
    }
}
