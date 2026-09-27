package com.music.bitchord.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Image
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
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.decodeToImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.net.HttpURLConnection
import java.net.URI
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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

private val coverCache = ConcurrentHashMap<String, ImageBitmap>()

private fun loadCover(url: String): ImageBitmap? {
    val uri = runCatching { URI(url) }.getOrNull() ?: return null
    if (uri.scheme != "https" || uri.host !in setOf(
            "lh3.googleusercontent.com", "i.ytimg.com", "yt3.ggpht.com"
        )) return null
    return runCatching {
        val connection = uri.toURL().openConnection() as HttpURLConnection
        connection.connectTimeout = 4000
        connection.readTimeout = 4000
        connection.instanceFollowRedirects = false
        try {
            if (connection.responseCode != 200 ||
                !connection.contentType.orEmpty().startsWith("image/") ||
                connection.contentLengthLong > 2_000_000) return null
            val bytes = connection.inputStream.use { it.readNBytes(2_000_001) }
            if (bytes.size > 2_000_000) null else bytes.decodeToImageBitmap()
        } finally {
            connection.disconnect()
        }
    }.getOrNull()
}

/** Real search results show their artwork; sample and unavailable covers use a gradient. */
@Composable
internal fun CoverPlaceholder(track: MockTrack?, modifier: Modifier = Modifier, radius: Dp = 10.dp) {
    val shape = RoundedCornerShape(radius)
    val start = track?.coverStart ?: Color(0xFF353539)
    val end = track?.coverEnd ?: Color(0xFF4A4A4E)
    val url = track?.artworkUrl.orEmpty()
    val artwork by produceState<ImageBitmap?>(initialValue = coverCache[url], url) {
        if (url.isNotBlank() && value == null) {
            value = withContext(Dispatchers.IO) { loadCover(url) }
            value?.let {
                if (coverCache.size >= 128) coverCache.clear()
                coverCache[url] = it
            }
        }
    }
    Box(
        modifier.clip(shape).background(Brush.linearGradient(listOf(start, end)))
            .border(1.dp, Color.White.copy(alpha = 0.07f), shape),
        contentAlignment = Alignment.Center,
    ) {
        if (artwork != null) {
            Image(artwork!!, track?.title, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Icon(Icons.Default.MusicNote, null, tint = Color.White.copy(alpha = 0.24f), modifier = Modifier.fillMaxSize(0.29f))
        }
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
