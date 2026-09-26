package com.music.bitchord.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal val Black = Color(0xFF000000)
internal val Charcoal = Color(0xFF1C1C1E)
internal val Raised = Color(0xFF262628)
internal val Stroke = Color(0xFF333336)
internal val Muted = Color(0xFF8E8E93)
internal val Red = Color(0xFFFA2D48)

private val colors = darkColorScheme(
    primary = Color.White,
    onPrimary = Black,
    background = Black,
    onBackground = Color.White,
    surface = Color(0xFF0D0D0F),
    onSurface = Color.White,
    surfaceVariant = Charcoal,
    onSurfaceVariant = Muted,
    outline = Stroke,
)

@Composable
internal fun DesktopTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, content = content)
}

/** Placeholder artwork stays bundled in code; no image or network request is made. */
@Composable
internal fun CoverPlaceholder(track: MockTrack?, modifier: Modifier = Modifier, radius: Dp = 10.dp) {
    val shape = RoundedCornerShape(radius)
    val start = track?.coverStart ?: Color(0xFF353539)
    val end = track?.coverEnd ?: Color(0xFF4A4A4E)
    Box(
        modifier.clip(shape).background(Brush.linearGradient(listOf(start, end)))
            .border(1.dp, Color.White.copy(alpha = 0.07f), shape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Default.MusicNote, null, tint = Color.White.copy(alpha = 0.24f), modifier = Modifier.fillMaxSize(0.29f))
    }
}

/** A still Gaussian wash sampled from the placeholder colors; foreground content is never blurred. */
@Composable
internal fun CoverWash(track: MockTrack?, modifier: Modifier = Modifier) {
    val start = track?.coverStart ?: Color(0xFF303039)
    val end = track?.coverEnd ?: Color(0xFF45404A)
    Box(modifier) {
        Box(Modifier.fillMaxSize().blur(64.dp).background(Brush.linearGradient(listOf(start, end, Black))))
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Black.copy(alpha = 0.28f), Black.copy(alpha = 0.78f), Black))))
    }
}

@Composable
internal fun Modifier.focusOutline(shape: Shape = RoundedCornerShape(9.dp)): Modifier {
    var focused by remember { mutableStateOf(false) }
    return this.onFocusChanged { focused = it.hasFocus }
        .border(if (focused) 2.dp else 1.dp, if (focused) Color.White else Color.Transparent, shape)
}
