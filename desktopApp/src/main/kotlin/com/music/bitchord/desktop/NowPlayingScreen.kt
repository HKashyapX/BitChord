package com.music.bitchord.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun NowPlayingScreen(state: DesktopState) {
    val track = state.currentTrack ?: return
    Box(Modifier.fillMaxSize().background(Black)) {
        CoverWash(track, Modifier.fillMaxSize())
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = state::back, modifier = Modifier.focusOutline(CircleShape)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back to previous page", tint = Color.White)
                }
                Spacer(Modifier.width(11.dp))
                Text("Now Playing", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val wide = maxWidth >= 1000.dp && maxHeight >= 520.dp
                if (wide) {
                    Row(
                        Modifier.fillMaxSize().widthIn(max = 1240.dp).padding(horizontal = 56.dp, vertical = 28.dp),
                        horizontalArrangement = Arrangement.spacedBy(56.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ArtworkColumn(state, Modifier.weight(1f).fillMaxHeight())
                        Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                            PlayerContent(state)
                        }
                    }
                } else {
                    Column(
                        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 28.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        ArtworkColumn(state, Modifier.fillMaxWidth().height(300.dp), compact = true)
                        Spacer(Modifier.height(20.dp))
                        Box(Modifier.fillMaxWidth().widthIn(max = 560.dp), contentAlignment = Alignment.Center) { PlayerContent(state) }
                        Spacer(Modifier.height(28.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ArtworkColumn(state: DesktopState, modifier: Modifier, compact: Boolean = false) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        if (compact) {
            CoverPlaceholder(state.currentTrack, Modifier.size(220.dp), 13.dp)
        } else {
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                val side = minOf(maxWidth, maxHeight, 560.dp)
                CoverPlaceholder(state.currentTrack, Modifier.size(side), 13.dp)
            }
        }
        Spacer(Modifier.height(17.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
            PaneButton("Lyrics", state.playerPane == PlayerPane.LYRICS, {
                state.showPane(PlayerPane.LYRICS)
            }) { Icon(Icons.Default.FormatQuote, null, modifier = Modifier.size(19.dp)) }
            PaneButton("Up next", state.playerPane == PlayerPane.QUEUE, {
                state.showPane(PlayerPane.QUEUE)
            }) { Icon(Icons.AutoMirrored.Filled.QueueMusic, null, modifier = Modifier.size(19.dp)) }
        }
    }
}

@Composable
private fun PaneButton(label: String, active: Boolean, onClick: () -> Unit, icon: @Composable () -> Unit) {
    Row(
        Modifier.background(if (active) Color.White.copy(alpha = 0.14f) else Charcoal.copy(alpha = 0.68f), RoundedCornerShape(20.dp))
            .focusOutline(RoundedCornerShape(20.dp)).clickable(onClick = onClick).padding(horizontal = 13.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon()
        Spacer(Modifier.width(7.dp))
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun PlayerContent(state: DesktopState) {
    when (state.playerPane) {
        PlayerPane.MAIN -> MainControls(state)
        PlayerPane.LYRICS -> LyricsPane(state)
        PlayerPane.QUEUE -> PlayerQueuePane(state)
    }
}

@Composable
private fun MainControls(state: DesktopState) {
    val track = state.currentTrack ?: return
    var dragging by remember(track.id) { mutableStateOf(false) }
    var draggedProgress by remember(track.id) { mutableFloatStateOf(0f) }
    Column(Modifier.fillMaxWidth().widthIn(max = 530.dp)) {
        Text("PLAYING FROM ${state.playbackOrigin.uppercase()}", color = Color.White.copy(alpha = 0.62f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp)
        Spacer(Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(track.title, fontSize = 28.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(track.artist, color = Color.White.copy(alpha = 0.62f), fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            IconButton(onClick = { state.toggleLike(track) }, modifier = Modifier.focusOutline(CircleShape)) {
                Icon(
                    if (state.isLiked(track)) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    if (state.isLiked(track)) "Remove from Liked Music" else "Like",
                    tint = if (state.isLiked(track)) Red else Color.White,
                )
            }
            IconButton(onClick = { state.openActions(track) }, modifier = Modifier.focusOutline(CircleShape)) {
                Icon(Icons.Default.MoreHoriz, "More actions for ${track.title}")
            }
        }
        Spacer(Modifier.height(24.dp))
        Slider(
            value = if (dragging) draggedProgress else state.progress,
            onValueChange = { draggedProgress = it; dragging = true },
            onValueChangeFinished = {
                if (dragging) state.seekTo(draggedProgress)
                dragging = false
            },
            enabled = !state.isRealTrack || state.audioSeekAvailable,
            colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.White, inactiveTrackColor = Color.White.copy(alpha = 0.25f)),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatTime((track.durationSeconds * (if (dragging) draggedProgress else state.progress)).toInt()), color = Muted, fontSize = 11.sp)
            Text(formatTime(track.durationSeconds), color = Muted, fontSize = 11.sp)
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = state::previous, modifier = Modifier.size(58.dp).focusOutline(CircleShape)) {
                Icon(Icons.Default.SkipPrevious, "Previous", modifier = Modifier.size(32.dp))
            }
            Spacer(Modifier.width(20.dp))
            IconButton(onClick = state::togglePlayback, modifier = Modifier.size(68.dp).background(Color.White, CircleShape).focusOutline(CircleShape)) {
                Icon(if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, if (state.isPlaying) "Pause" else "Play", tint = Black, modifier = Modifier.size(36.dp))
            }
            Spacer(Modifier.width(20.dp))
            IconButton(onClick = state::next, enabled = state.currentIndex < state.queue.lastIndex, modifier = Modifier.size(58.dp).focusOutline(CircleShape)) {
                Icon(Icons.Default.SkipNext, "Next", modifier = Modifier.size(32.dp))
            }
        }
        Spacer(Modifier.height(18.dp))
        Text(
            state.audioError ?: if (state.isAudioLoading) "Loading audio…" else if (state.isRealTrack) {
                if (state.audioSeekAvailable) "Live audio · drag to seek" else "Live audio · install mpv to seek"
            } else "Sample track · search for a real song to play",
            color = if (state.audioError != null) Red else Muted, fontSize = 11.sp,
        )
    }
}

@Composable
private fun LyricsPane(state: DesktopState) {
    val track = state.currentTrack ?: return
    val lines = state.lyricLines
    val positionMs = (track.durationSeconds * state.progress * 1000).toInt()
    val activeIndex = lines.indexOfLast { it.timeMs <= positionMs }.coerceAtLeast(0)
    val scroll = rememberLazyListState()
    LaunchedEffect(activeIndex, track.id) {
        if (lines.isNotEmpty()) scroll.animateScrollToItem((activeIndex - 1).coerceAtLeast(0))
    }
    Column(Modifier.fillMaxWidth().height(390.dp)) {
        Text(track.title, fontSize = 19.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        Text(if (state.lyricsSource.isNotEmpty()) "Lyrics by ${state.lyricsSource}" else "Lyrics",
            color = Muted, fontSize = 12.sp)
        Spacer(Modifier.height(12.dp))
        if (lines.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(if (state.lyricsLoading) "Finding lyrics…" else "No synchronized lyrics found for this song.",
                    color = Muted, fontSize = 14.sp)
            }
        } else {
            LazyColumn(Modifier.weight(1f).fillMaxWidth(), state = scroll) {
                itemsIndexed(lines) { index, line ->
                    Text(line.text,
                        color = if (index == activeIndex) Color.White else Color.White.copy(alpha = 0.42f),
                        fontSize = if (index == activeIndex) 22.sp else 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.fillMaxWidth().clickable(enabled = state.audioSeekAvailable) {
                            if (track.durationSeconds > 0) state.seekTo(line.timeMs / (track.durationSeconds * 1000f))
                        }.padding(vertical = 9.dp),
                    )
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(formatTime(positionMs / 1000), color = Muted, fontSize = 12.sp)
            IconButton(onClick = state::previous) { Icon(Icons.Default.SkipPrevious, "Previous") }
            IconButton(onClick = state::togglePlayback) {
                Icon(if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    if (state.isPlaying) "Pause" else "Play")
            }
            IconButton(onClick = state::next, enabled = state.currentIndex < state.queue.lastIndex) {
                Icon(Icons.Default.SkipNext, "Next")
            }
            Text(formatTime(track.durationSeconds), color = Muted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun PlayerQueuePane(state: DesktopState) {
    Column(Modifier.fillMaxWidth().height(380.dp).verticalScroll(rememberScrollState())) {
        Text("Up next", fontSize = 25.sp, fontWeight = FontWeight.Bold)
        QueueModeControls(state)
        Spacer(Modifier.height(16.dp))
        state.queue.forEachIndexed { index, track ->
            Row(
                Modifier.fillMaxWidth().height(58.dp).focusOutline().clickable { state.selectQueued(index) }.padding(5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CoverPlaceholder(track, Modifier.size(43.dp), 7.dp)
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f)) {
                    Text(track.title, color = if (index == state.currentIndex) Color.White else Color.White.copy(alpha = 0.75f), fontSize = 13.sp, fontWeight = if (index == state.currentIndex) FontWeight.Bold else FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(if (index == state.currentIndex) "Now playing · ${track.artist}" else track.artist, color = Muted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (index > state.currentIndex) {
                    QueueMoveButtons(state, index)
                    IconButton(onClick = { state.removeUpcoming(index) }, modifier = Modifier.size(27.dp)) {
                        Icon(androidx.compose.material.icons.Icons.Default.Close, "Remove ${track.title}", modifier = Modifier.size(17.dp))
                    }
                }
            }
        }
    }
}
