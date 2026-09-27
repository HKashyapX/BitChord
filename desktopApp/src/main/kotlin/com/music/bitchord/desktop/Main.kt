package com.music.bitchord.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Surface
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import java.awt.Dimension
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.withContext

fun main(args: Array<String>) = application {
    val large = "--large" in args
    val narrow = "--narrow" in args
    val search = args.firstOrNull { it.startsWith("--search=") }
    Window(
        onCloseRequest = ::exitApplication,
        title = "BitChord",
        state = WindowState(
            width = if (large) 1920.dp else if (narrow) 900.dp else 1280.dp,
            height = if (large) 1080.dp else if (narrow) 680.dp else 720.dp,
        ),
    ) {
        window.minimumSize = Dimension(800, 620)
        DesktopTheme {
            val library = remember { DesktopLibrary.default() }
            val state = remember {
                DesktopState().apply {
                    restore(library.load())
                    when {
                        "--now-playing" in args -> {
                            navigate(Destination.LIKED_MUSIC)
                            val liked = likedTracks
                            selectTrack(liked[1], liked, "Liked Music")
                            if ("--lyrics" in args) showPane(PlayerPane.LYRICS)
                            if ("--queue" in args) showPane(PlayerPane.QUEUE)
                        }
                        "--liked" in args -> navigate(Destination.LIKED_MUSIC)
                        search != null -> {
                            searchQuery = search.substringAfter('=')
                            navigate(Destination.SEARCH)
                        }
                    }
                }
            }
            LaunchedEffect(state) {
                snapshotFlow { state.snapshot() }.collectLatest { snapshot ->
                    delay(400)
                    withContext(Dispatchers.IO) {
                        runCatching { library.save(snapshot) }.onFailure { it.printStackTrace() }
                    }
                }
            }
            LaunchedEffect(state) {
                while (true) {
                    state.checkSleepTimer()
                    delay(1000)
                }
            }
            DisposableEffect(state) {
                if ("--offline" !in args) {
                    runCatching {
                        state.audio = DesktopAudio(state)
                        if (state.currentTrack?.let { it in mockTracks } == true) state.setActualPlayback(false)
                    }
                        .onFailure { state.audioError = "Audio helper unavailable: ${it.message}" }
                }
                onDispose {
                    state.audio?.close()
                    runCatching { library.save(state.snapshot()) }.onFailure { it.printStackTrace() }
                }
            }
            DesktopApp(state)
        }
    }
}

@Composable
private fun DesktopApp(state: DesktopState) {
    val keyboard = Modifier.onPreviewKeyEvent { event ->
        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
        when {
            event.isAltPressed && event.key == Key.DirectionLeft -> { state.back(); true }
            event.key == Key.Escape && state.destination == Destination.NOW_PLAYING -> { state.back(); true }
            event.isCtrlPressed && event.key == Key.F -> { state.navigate(Destination.SEARCH); true }
            else -> false
        }
    }
    Surface(Modifier.fillMaxSize().then(keyboard).focusable(), color = Black) {
        if (state.destination == Destination.NOW_PLAYING) {
            NowPlayingScreen(state)
        } else {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val showSideQueue = maxWidth >= 1180.dp
                Column {
                    Row(Modifier.weight(1f).fillMaxWidth()) {
                        Sidebar(state)
                        VerticalDivider(Modifier.fillMaxHeight(), color = Stroke)
                        Box(Modifier.weight(1f).fillMaxHeight()) {
                            when (state.destination) {
                                Destination.HOME -> HomeScreen(state)
                                Destination.SEARCH -> SearchScreen(state)
                                Destination.LIKED_MUSIC -> LikedMusicScreen(state)
                                Destination.SONGS -> SongsScreen(state)
                                Destination.PLAYLISTS -> PlaylistsScreen(state)
                                Destination.PLAYLIST -> PlaylistScreen(state)
                                Destination.ALBUM, Destination.ARTIST -> DetailScreen(state)
                                Destination.NOW_PLAYING -> Unit
                            }
                        }
                        if (showSideQueue) {
                            VerticalDivider(Modifier.fillMaxHeight(), color = Stroke)
                            QueueSidebar(state)
                        }
                    }
                    BottomPlayer(state)
                }
            }
        }
        state.actionTrack?.let { SongActionsDialog(state, it) }
        state.playlistPickerTrack?.let { PlaylistPickerDialog(state, it) }
        if (state.outputDialogOpen) AudioOutputDialog(state)
        if (state.pipelineDialogOpen) AudioPipelineDialog(state)
        if (state.lyricsProviderDialogOpen) LyricsProviderDialog(state)
    }
}
