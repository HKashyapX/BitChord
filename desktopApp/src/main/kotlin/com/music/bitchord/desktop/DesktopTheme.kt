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
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.concurrent.ConcurrentHashMap
import javax.imageio.ImageIO
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

/** Some YouTube video thumbnails contain solid black letterbox bars. */
private fun trimVideoLetterbox(bytes: ByteArray): ByteArray {
    val image = ImageIO.read(ByteArrayInputStream(bytes)) ?: return bytes
    val width = image.width
    val height = image.height
    if (width < 100 || height < 100) return bytes
    fun darkRow(y: Int): Boolean {
        val samples = (1..19).count { index ->
            val pixel = image.getRGB(index * width / 20, y)
            val red = pixel shr 16 and 0xff
            val green = pixel shr 8 and 0xff
            val blue = pixel and 0xff
            red < 24 && green < 24 && blue < 24
        }
        return samples >= 17
    }
    var top = 0
    var bottom = height - 1
    while (top < height / 4 && darkRow(top)) top++
    while (bottom > height * 3 / 4 && darkRow(bottom)) bottom--
    if (top < height / 20 || height - 1 - bottom < height / 20) return bytes
    val cropped = image.getSubimage(0, top, width, bottom - top + 1)
    return ByteArrayOutputStream().use { output ->
        ImageIO.write(cropped, "png", output)
        output.toByteArray()
    }
}

internal fun artworkCandidates(track: MockTrack?): List<String> {
    if (track == null || mockTracks.any { it.id == track.id }) return emptyList()
    val fallback = track.id.takeIf { it.matches(Regex("[A-Za-z0-9_-]{1,32}")) }
        ?.let { "https://i.ytimg.com/vi/$it/hqdefault.jpg" }
    return listOfNotNull(track.artworkUrl.takeIf { it.isNotBlank() }, fallback).distinct()
}

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
            if (bytes.size > 2_000_000) null else {
                val imageBytes = if (uri.host == "i.ytimg.com" && uri.path.endsWith("/hqdefault.jpg")) {
                    trimVideoLetterbox(bytes)
                } else bytes
                imageBytes.decodeToImageBitmap()
            }
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
    val urls = artworkCandidates(track)
    val artwork by produceState<ImageBitmap?>(initialValue = null, urls) {
        value = urls.firstNotNullOfOrNull(coverCache::get)
        if (value == null) {
            for (url in urls) {
                val bitmap = withContext(Dispatchers.IO) { coverCache[url] ?: loadCover(url) }
                if (bitmap != null) {
                    if (coverCache.size >= 128) coverCache.clear()
                    coverCache[url] = bitmap
                    value = bitmap
                    break
                }
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
