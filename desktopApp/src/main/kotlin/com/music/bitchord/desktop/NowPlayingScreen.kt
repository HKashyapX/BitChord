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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
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
                val side = minOf(maxWidth, maxHeight)
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
        PlayerPane.LYRICS -> LyricsPane()
        PlayerPane.QUEUE -> PlayerQueuePane(state)
    }
}

@Composable
private fun MainControls(state: DesktopState) {
    val track = state.currentTrack ?: return
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
        }
        Spacer(Modifier.height(24.dp))
        Slider(
            value = state.progress,
            onValueChange = state::seekTo,
            enabled = !state.isRealTrack,
            colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.White, inactiveTrackColor = Color.White.copy(alpha = 0.25f)),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatTime((track.durationSeconds * state.progress).toInt()), color = Muted, fontSize = 11.sp)
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
            state.audioError ?: if (state.isAudioLoading) "Loading audio…" else if (state.isRealTrack) "Live audio · seeking is not available yet" else "Sample track · search for a real song to play",
            color = if (state.audioError != null) Red else Muted, fontSize = 11.sp,
        )
    }
}

@Composable
private fun LyricsPane() {
    Column(Modifier.fillMaxWidth().height(320.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("Lyrics", fontSize = 25.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Text("Lyrics are not available in this local preview.", color = Color.White.copy(alpha = 0.62f), fontSize = 14.sp)
    }
}

@Composable
private fun PlayerQueuePane(state: DesktopState) {
    Column(Modifier.fillMaxWidth().height(380.dp).verticalScroll(rememberScrollState())) {
        Text("Up next", fontSize = 25.sp, fontWeight = FontWeight.Bold)
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
            }
        }
    }
}
