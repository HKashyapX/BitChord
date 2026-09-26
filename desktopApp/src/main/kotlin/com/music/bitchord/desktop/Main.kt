package com.music.bitchord.desktop

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import java.awt.Dimension

private val Background = Color(0xFF000000)
private val Surface = Color(0xFF0D0D0F)
private val SurfaceRaised = Color(0xFF1C1C1E)
private val Outline = Color(0xFF2C2C2E)
private val Secondary = Color(0xFF8E8E93)
private val AccentRed = Color(0xFFFA2D48)

private val BitChordColors = darkColorScheme(
    primary = Color.White,
    onPrimary = Color.Black,
    background = Background,
    onBackground = Color.White,
    surface = Surface,
    onSurface = Color.White,
    surfaceVariant = SurfaceRaised,
    onSurfaceVariant = Secondary,
    outline = Outline,
    error = AccentRed,
)

private data class Track(
    val title: String,
    val artist: String,
    val album: String,
    val duration: String,
    val colors: List<Color>,
)

private val searchResults = listOf(
    Track("Midnight City", "M83", "Hurry Up, We're Dreaming", "4:03", listOf(Color(0xFF292253), Color(0xFFED4C67))),
    Track("The Less I Know The Better", "Tame Impala", "Currents", "3:36", listOf(Color(0xFF6D246D), Color(0xFFF0835D))),
    Track("After Dark", "Mr.Kitty", "Time", "4:17", listOf(Color(0xFF17233D), Color(0xFF3C6E91))),
    Track("505", "Arctic Monkeys", "Favourite Worst Nightmare", "4:13", listOf(Color(0xFF3A3A3A), Color(0xFFA8A8A8))),
    Track("Space Song", "Beach House", "Depression Cherry", "5:20", listOf(Color(0xFF401821), Color(0xFFD35170))),
    Track("Eventually", "Tame Impala", "Currents", "5:19", listOf(Color(0xFF135D67), Color(0xFFE26A73))),
    Track("Sweater Weather", "The Neighbourhood", "I Love You.", "4:00", listOf(Color(0xFF141414), Color(0xFF777777))),
    Track("Chamber of Reflection", "Mac DeMarco", "Salad Days", "3:51", listOf(Color(0xFF394D2B), Color(0xFFD6B65E))),
)

private val initialQueue = listOf(
    searchResults[1], searchResults[4], searchResults[5], searchResults[7],
)

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "BitChord",
        state = WindowState(width = 1240.dp, height = 800.dp),
    ) {
        window.minimumSize = Dimension(960, 640)
        MaterialTheme(colorScheme = BitChordColors) {
            BitChordDesktop()
        }
    }
}

@Composable
private fun BitChordDesktop() {
    var query by remember { mutableStateOf("midnight") }
    var selectedSection by remember { mutableStateOf("Search") }
    var currentTrack by remember { mutableStateOf(searchResults.first()) }
    var isPlaying by remember { mutableStateOf(false) }
    var queue by remember { mutableStateOf(initialQueue) }
    val filtered = remember(query) {
        if (query.isBlank()) searchResults else searchResults.filter {
            it.title.contains(query, ignoreCase = true) ||
                it.artist.contains(query, ignoreCase = true) ||
                it.album.contains(query, ignoreCase = true)
        }.ifEmpty { searchResults }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Background) {
        Column {
            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                NavigationSidebar(
                    selected = selectedSection,
                    onSelect = { selectedSection = it },
                )
                VerticalDivider(modifier = Modifier.fillMaxHeight().width(1.dp), color = Outline)
                SearchPane(
                    modifier = Modifier.weight(1f),
                    query = query,
                    onQueryChange = { query = it },
                    results = filtered,
                    currentTrack = currentTrack,
                    isPlaying = isPlaying,
                    onPlay = {
                        currentTrack = it
                        isPlaying = true
                    },
                    onQueue = { track -> if (track !in queue) queue = queue + track },
                )
                VerticalDivider(modifier = Modifier.fillMaxHeight().width(1.dp), color = Outline)
                QueuePane(
                    queue = queue,
                    onPlay = {
                        currentTrack = it
                        isPlaying = true
                    },
                    onClear = { queue = emptyList() },
                )
            }
            HorizontalDivider(color = Outline)
            PlayerBar(
                track = currentTrack,
                isPlaying = isPlaying,
                onTogglePlay = { isPlaying = !isPlaying },
            )
        }
    }
}

@Composable
private fun NavigationSidebar(selected: String, onSelect: (String) -> Unit) {
    Column(
        modifier = Modifier.width(210.dp).fillMaxHeight().background(Surface).padding(horizontal = 14.dp, vertical = 22.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp)) {
            Box(
                modifier = Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(AccentRed),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color.White, modifier = Modifier.size(21.dp))
            }
            Spacer(Modifier.width(10.dp))
            Text("BitChord", fontSize = 20.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.4).sp)
        }
        Spacer(Modifier.height(28.dp))
        Text("DISCOVER", color = Secondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp, modifier = Modifier.padding(start = 10.dp, bottom = 7.dp))
        NavItem("Home", Icons.Default.Home, selected, onSelect)
        NavItem("Search", Icons.Default.Search, selected, onSelect)
        Spacer(Modifier.height(24.dp))
        Text("LIBRARY", color = Secondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp, modifier = Modifier.padding(start = 10.dp, bottom = 7.dp))
        NavItem("Recently Added", Icons.Default.Album, selected, onSelect)
        NavItem("Artists", Icons.Default.Person, selected, onSelect)
        NavItem("Albums", Icons.Default.LibraryMusic, selected, onSelect)
        NavItem("Songs", Icons.Default.MusicNote, selected, onSelect)
        Spacer(Modifier.weight(1f))
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { }.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(28.dp).clip(CircleShape).background(SurfaceRaised), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Person, null, tint = Secondary, modifier = Modifier.size(17.dp))
            }
            Spacer(Modifier.width(9.dp))
            Column {
                Text("Desktop preview", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text("Mock library", fontSize = 10.sp, color = Secondary)
            }
        }
    }
}

@Composable
private fun NavItem(label: String, icon: ImageVector, selected: String, onSelect: (String) -> Unit) {
    val active = selected == label
    val background by animateColorAsState(if (active) SurfaceRaised else Color.Transparent)
    Row(
        modifier = Modifier.fillMaxWidth().height(38.dp).clip(RoundedCornerShape(8.dp)).background(background).clickable { onSelect(label) }.padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = if (active) AccentRed else Secondary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(11.dp))
        Text(label, fontSize = 13.sp, fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal, color = if (active) Color.White else Color(0xFFD0D0D2))
    }
}

@Composable
private fun SearchPane(
    modifier: Modifier,
    query: String,
    onQueryChange: (String) -> Unit,
    results: List<Track>,
    currentTrack: Track,
    isPlaying: Boolean,
    onPlay: (Track) -> Unit,
    onQueue: (Track) -> Unit,
) {
    Column(modifier = modifier.fillMaxHeight().padding(horizontal = 30.dp, vertical = 22.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = {}) { Icon(Icons.Default.ArrowBackIosNew, "Back", tint = Secondary, modifier = Modifier.size(16.dp)) }
            IconButton(onClick = {}) { Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, "Forward", tint = Color(0xFF48484A), modifier = Modifier.size(16.dp)) }
            Spacer(Modifier.width(6.dp))
            SearchField(query, onQueryChange, Modifier.weight(1f).widthIn(max = 520.dp))
        }
        Spacer(Modifier.height(30.dp))
        Text("Search", fontSize = 34.sp, lineHeight = 38.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.8).sp)
        Spacer(Modifier.height(5.dp))
        Text(
            if (query.isBlank()) "Browse the mock catalog" else "Top results for “$query”",
            color = Secondary,
            fontSize = 14.sp,
        )
        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("#", color = Secondary, fontSize = 11.sp, modifier = Modifier.width(34.dp))
            Text("TITLE", color = Secondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp, modifier = Modifier.weight(1.2f))
            Text("ALBUM", color = Secondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp, modifier = Modifier.weight(0.9f))
            Text("TIME", color = Secondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp, modifier = Modifier.width(44.dp))
            Spacer(Modifier.width(58.dp))
        }
        HorizontalDivider(color = Outline)
        LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(vertical = 5.dp)) {
            items(results) { track ->
                TrackRow(
                    track = track,
                    index = searchResults.indexOf(track) + 1,
                    active = track == currentTrack,
                    isPlaying = isPlaying,
                    onPlay = { onPlay(track) },
                    onQueue = { onQueue(track) },
                )
            }
        }
    }
}

@Composable
private fun SearchField(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.height(38.dp).clip(RoundedCornerShape(9.dp)).background(SurfaceRaised).border(1.dp, Outline, RoundedCornerShape(9.dp)).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Default.Search, null, tint = Secondary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
            cursorBrush = SolidColor(AccentRed),
            modifier = Modifier.weight(1f),
            decorationBox = { input ->
                if (value.isEmpty()) Text("Artists, songs, albums", color = Secondary, fontSize = 14.sp)
                input()
            },
        )
        if (value.isNotEmpty()) {
            Icon(
                Icons.Default.Clear,
                "Clear search",
                tint = Secondary,
                modifier = Modifier.size(17.dp).clip(CircleShape).clickable { onValueChange("") },
            )
        }
    }
}

@Composable
private fun TrackRow(track: Track, index: Int, active: Boolean, isPlaying: Boolean, onPlay: () -> Unit, onQueue: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier.fillMaxWidth().height(60.dp).clip(RoundedCornerShape(8.dp)).clickable(interactionSource = interaction, indication = null, onClick = onPlay).padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(34.dp), contentAlignment = Alignment.CenterStart) {
            Text(if (active && isPlaying) "♪" else index.toString(), color = if (active) AccentRed else Secondary, fontSize = 12.sp)
        }
        Artwork(track.colors, 42.dp)
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1.2f)) {
            Text(track.title, color = if (active) AccentRed else Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(track.artist, color = Secondary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(track.album, color = Secondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(0.9f).padding(end = 10.dp))
        Text(track.duration, color = Secondary, fontSize = 11.sp, modifier = Modifier.width(44.dp))
        IconButton(onClick = onQueue, modifier = Modifier.size(30.dp)) {
            Icon(Icons.Default.Add, "Add ${track.title} to queue", tint = Secondary, modifier = Modifier.size(17.dp))
        }
        IconButton(onClick = {}, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.MoreHoriz, "More", tint = Secondary, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun QueuePane(queue: List<Track>, onPlay: (Track) -> Unit, onClear: () -> Unit) {
    Column(modifier = Modifier.width(290.dp).fillMaxHeight().background(Surface).padding(horizontal = 18.dp, vertical = 24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.AutoMirrored.Filled.QueueMusic, null, tint = AccentRed, modifier = Modifier.size(21.dp))
            Spacer(Modifier.width(9.dp))
            Text("Up Next", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Text(
                "Clear",
                color = if (queue.isEmpty()) Color(0xFF48484A) else AccentRed,
                fontSize = 12.sp,
                modifier = Modifier.clip(RoundedCornerShape(5.dp)).clickable(enabled = queue.isNotEmpty(), onClick = onClear).padding(5.dp),
            )
        }
        Spacer(Modifier.height(5.dp))
        Text("${queue.size} songs · Mock queue", color = Secondary, fontSize = 11.sp, modifier = Modifier.padding(start = 30.dp))
        Spacer(Modifier.height(20.dp))
        if (queue.isEmpty()) {
            Column(Modifier.fillMaxWidth().weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Icon(Icons.AutoMirrored.Filled.QueueMusic, null, tint = Outline, modifier = Modifier.size(42.dp))
                Spacer(Modifier.height(12.dp))
                Text("Your queue is empty", color = Secondary, fontSize = 13.sp)
                Text("Add a song from search", color = Color(0xFF5A5A5E), fontSize = 11.sp)
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(queue) { track -> QueueRow(track, onPlay = { onPlay(track) }) }
            }
        }
        Surface(color = SurfaceRaised, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Shuffle, null, tint = Secondary, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Autoplay is off", color = Secondary, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun QueueRow(track: Track, onPlay: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable(onClick = onPlay).padding(7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Artwork(track.colors, 39.dp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(track.title, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(track.artist, color = Secondary, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Icon(Icons.Default.MoreHoriz, "More", tint = Secondary, modifier = Modifier.size(17.dp))
    }
}

@Composable
private fun PlayerBar(track: Track, isPlaying: Boolean, onTogglePlay: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(94.dp).background(Color(0xFF111113)).padding(horizontal = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Artwork(track.colors, 58.dp)
            Spacer(Modifier.width(13.dp))
            Column(Modifier.widthIn(max = 240.dp)) {
                Text(track.title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(track.artist, color = Secondary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            IconButton(onClick = {}, modifier = Modifier.padding(start = 6.dp).size(32.dp)) {
                Icon(Icons.Default.MoreHoriz, "More", tint = Secondary, modifier = Modifier.size(18.dp))
            }
        }
        Column(modifier = Modifier.weight(1.3f), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                IconButton(onClick = {}) { Icon(Icons.Default.Shuffle, "Shuffle", tint = Secondary, modifier = Modifier.size(17.dp)) }
                IconButton(onClick = {}) { Icon(Icons.Default.SkipPrevious, "Previous", tint = Color.White, modifier = Modifier.size(25.dp)) }
                Button(
                    onClick = onTogglePlay,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, if (isPlaying) "Pause mock playback" else "Play mock playback", modifier = Modifier.size(24.dp))
                }
                IconButton(onClick = {}) { Icon(Icons.Default.SkipNext, "Next", tint = Color.White, modifier = Modifier.size(25.dp)) }
                IconButton(onClick = {}) { Icon(Icons.Default.Repeat, "Repeat", tint = Secondary, modifier = Modifier.size(17.dp)) }
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(0.84f)) {
                Text(if (isPlaying) "1:16" else "0:00", color = Secondary, fontSize = 9.sp, modifier = Modifier.width(28.dp))
                Box(Modifier.weight(1f).height(3.dp).clip(CircleShape).background(Outline)) {
                    Box(Modifier.fillMaxWidth(if (isPlaying) 0.31f else 0f).fillMaxHeight().background(Color(0xFFB8B8BA)))
                }
                Text("−2:47", color = Secondary, fontSize = 9.sp, modifier = Modifier.width(34.dp).padding(start = 7.dp))
            }
        }
        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.AutoMirrored.Filled.VolumeUp, null, tint = Secondary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(9.dp))
            Box(Modifier.width(92.dp).height(3.dp).clip(CircleShape).background(Outline)) {
                Box(Modifier.fillMaxWidth(0.68f).fillMaxHeight().background(Color(0xFFB8B8BA)))
            }
            Spacer(Modifier.width(12.dp))
            Surface(color = AccentRed.copy(alpha = 0.13f), shape = RoundedCornerShape(6.dp)) {
                Text("MOCK", color = AccentRed, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.7.sp, modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp))
            }
        }
    }
}

@Composable
private fun Artwork(colors: List<Color>, size: androidx.compose.ui.unit.Dp) {
    Canvas(modifier = Modifier.size(size).clip(RoundedCornerShape(6.dp))) {
        drawRect(brush = Brush.linearGradient(colors, Offset.Zero, Offset(this.size.width, this.size.height)))
        drawCircle(color = Color.White.copy(alpha = 0.14f), radius = this.size.minDimension * 0.33f, center = Offset(this.size.width * 0.68f, this.size.height * 0.30f))
        drawArc(color = Color.Black.copy(alpha = 0.22f), startAngle = 15f, sweepAngle = 220f, useCenter = false, topLeft = Offset(this.size.width * 0.05f, this.size.height * 0.34f), size = Size(this.size.width * 0.75f, this.size.height * 0.75f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = this.size.width * 0.08f))
    }
}
