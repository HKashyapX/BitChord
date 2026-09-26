package com.music.bitchord.desktop

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
internal fun SectionTitle(title: String, subtitle: String? = null) {
    Text(title, fontSize = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.4).sp)
    if (subtitle != null) Text(subtitle, color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.5.sp)
}

@Composable
internal fun DesktopTrackRow(
    track: MockTrack,
    state: DesktopState,
    context: List<MockTrack>,
    origin: String,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    showLike: Boolean = false,
    onSelected: (() -> Unit)? = null,
) {
    val rowShape = RoundedCornerShape(9.dp)
    Row(
        modifier.fillMaxWidth().height(if (compact) 58.dp else 66.dp)
            .focusOutline(rowShape).clickable { onSelected?.invoke() ?: state.selectTrack(track, context, origin) }
            .padding(horizontal = if (compact) 2.dp else 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CoverPlaceholder(track, Modifier.size(if (compact) 48.dp else 50.dp), 7.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(track.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                if (compact) track.artist else "${track.artist} · ${track.album}",
                color = Muted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        if (!compact) Text(formatTime(track.durationSeconds), color = Muted, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp))
        if (showLike) {
            IconButton(onClick = { state.toggleLike(track) }, modifier = Modifier.size(34.dp).focusOutline(RoundedCornerShape(17.dp))) {
                Icon(
                    if (state.isLiked(track)) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    if (state.isLiked(track)) "Remove from Liked Music" else "Like",
                    tint = if (state.isLiked(track)) Red else Muted,
                    modifier = Modifier.size(17.dp),
                )
            }
        }
        IconButton(onClick = { state.addToQueue(track) }, modifier = Modifier.size(34.dp).focusOutline(RoundedCornerShape(17.dp))) {
            Icon(Icons.Default.Add, "Add ${track.title} to queue", tint = Muted, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
internal fun CoverCard(
    title: String,
    subtitle: String,
    track: MockTrack?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(modifier.focusOutline(RoundedCornerShape(11.dp)).clickable(onClick = onClick)) {
        CoverPlaceholder(track, Modifier.fillMaxWidth().aspectRatio(1f), 11.dp)
        Spacer(Modifier.height(8.dp))
        Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(subtitle, color = Muted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
internal fun CoverShelf(items: List<MockTrack>, state: DesktopState, count: Int, origin: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(13.dp)) {
        items.take(count).forEach { track ->
            CoverCard(track.title, track.artist, track, Modifier.weight(1f)) {
                state.selectTrack(track, items, origin)
            }
        }
    }
}
