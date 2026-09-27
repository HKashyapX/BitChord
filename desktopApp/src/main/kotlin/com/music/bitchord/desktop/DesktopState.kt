package com.music.bitchord.desktop

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

internal data class MockTrack(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationSeconds: Int,
    val coverStart: Color,
    val coverEnd: Color,
    val artworkUrl: String = "",
)

internal val mockTracks = listOf(
    MockTrack("midnight", "Midnight City", "M83", "Hurry Up, We're Dreaming", 243, Color(0xFF303039), Color(0xFF45404A)),
    MockTrack("less", "The Less I Know The Better", "Tame Impala", "Currents", 216, Color(0xFF38313C), Color(0xFF51444B)),
    MockTrack("after", "After Dark", "Mr.Kitty", "Time", 257, Color(0xFF2D353D), Color(0xFF44505A)),
    MockTrack("505", "505", "Arctic Monkeys", "Favourite Worst Nightmare", 253, Color(0xFF303033), Color(0xFF4A4A4D)),
    MockTrack("space", "Space Song", "Beach House", "Depression Cherry", 320, Color(0xFF3C3137), Color(0xFF55424A)),
    MockTrack("eventually", "Eventually", "Tame Impala", "Currents", 319, Color(0xFF303B3C), Color(0xFF465353)),
    MockTrack("sweater", "Sweater Weather", "The Neighbourhood", "I Love You.", 240, Color(0xFF303236), Color(0xFF494B50)),
    MockTrack("chamber", "Chamber of Reflection", "Mac DeMarco", "Salad Days", 231, Color(0xFF37392F), Color(0xFF4D5040)),
)

internal enum class Destination { HOME, SEARCH, LIKED_MUSIC, SONGS, PLAYLISTS, ALBUM, ARTIST, PLAYLIST, NOW_PLAYING }
internal enum class PlayerPane { MAIN, LYRICS, QUEUE }
internal enum class RepeatMode { OFF, ALL, ONE }
internal data class LyricLine(val timeMs: Int, val text: String)
internal data class OutputDevice(val name: String, val description: String)
internal data class HomeShelf(val title: String, val tracks: List<MockTrack>)

/** All desktop interactions use one local session; live audio is optional. */
internal class DesktopState(private val sampleMode: Boolean = false) {
    var audio by mutableStateOf<DesktopAudio?>(null)
    var audioError by mutableStateOf<String?>(null)
    var isAudioLoading by mutableStateOf(false)
    var audioStreamActive by mutableStateOf(false)
    var audioSeekAvailable by mutableStateOf(false)
    val lyricLines = mutableStateListOf<LyricLine>()
    var lyricsLoading by mutableStateOf(false)
    var lyricsSource by mutableStateOf("")
    var lyricsProviderDialogOpen by mutableStateOf(false)
    val lyricsProviders = listOf("Automatic" to "auto", "BetterLyrics" to "BetterLyrics", "LRCLIB" to "LRCLIB")
    fun chooseLyricsProvider(provider: String) {
        val track = currentTrack ?: return
        lyricLines.clear()
        lyricsSource = ""
        audio?.requestLyrics(track, provider)
    }
    var lyricsOffsetMs by mutableIntStateOf(0)
    fun adjustLyricsOffset(deltaMs: Int) { lyricsOffsetMs = (lyricsOffsetMs + deltaMs).coerceIn(-10_000, 10_000) }
    var outputDialogOpen by mutableStateOf(false)
    var outputLoading by mutableStateOf(false)
    var outputError by mutableStateOf<String?>(null)
    val outputDevices = mutableStateListOf<OutputDevice>()
    var outputSelected by mutableStateOf("auto")
    var outputVolume by mutableFloatStateOf(100f)
    var pipelineDialogOpen by mutableStateOf(false)
    var pipelineLoading by mutableStateOf(false)
    var pipelineError by mutableStateOf<String?>(null)
    val pipelineFields = mutableStateMapOf<String, String>()
    var qualityMode by mutableStateOf("best")
    var videoVersionActive by mutableStateOf(false)
        private set
    var versionSwitchLoading by mutableStateOf(false)
    private var originalAudioTrack by mutableStateOf<MockTrack?>(null)
    fun toggleVideoVersion() {
        val track = currentTrack ?: return
        if (videoVersionActive) {
            val original = originalAudioTrack ?: return
            val seconds = track.durationSeconds * progress
            queue[currentIndex] = original
            videoVersionActive = false
            versionSwitchLoading = true
            audio?.playAt(original, seconds)
            statusMessage = "Returning to audio version…"
        } else {
            originalAudioTrack = track
            audio?.playVideo(track)
            statusMessage = "Finding matching music video…"
        }
    }
    fun activateVideoVersion(video: MockTrack) {
        queue[currentIndex] = video
        rememberTracks(listOf(video))
        videoVersionActive = true
        versionSwitchLoading = false
        statusMessage = "Playing video version in mpv"
    }
    fun openPipeline() {
        pipelineDialogOpen = true
        pipelineError = null
        pipelineFields.clear()
        pipelineLoading = true
        if (audioStreamActive) audio?.queryPipeline()
        else { pipelineLoading = false; pipelineError = "Play a real song with mpv to inspect the audio pipeline" }
    }
    fun changeQuality(mode: String) {
        val track = currentTrack ?: return
        if (isSampleTrack(track) || audio == null) return
        audio?.changeQuality(mode, track)
        qualityMode = mode
        statusMessage = "Stream preference: ${if (mode == "best") "best available" else "standard when available"}"
        pipelineDialogOpen = false
    }
    var statusMessage by mutableStateOf<String?>(null)
    var radioLoading by mutableStateOf(false)
    val downloadingIds = mutableStateListOf<String>()
    val downloadedIds = mutableStateListOf<String>()
    fun startRadio(track: MockTrack) {
        if (isSampleTrack(track) || audio == null) {
            statusMessage = "Radio needs a real song and an active audio helper"
            return
        }
        radioLoading = true
        statusMessage = "Loading radio for ${track.title}…"
        audio?.radio(track)
    }
    fun downloadTrack(track: MockTrack) {
        if (isSampleTrack(track) || audio == null) {
            statusMessage = "Search for a real song to download"
            return
        }
        if (track.id !in downloadingIds) {
            downloadingIds.add(track.id)
            statusMessage = "Downloading ${track.title}…"
            audio?.download(track)
        }
    }
    fun openOutput() {
        outputDialogOpen = true
        outputError = null
        outputDevices.clear()
        outputLoading = true
        if (audio != null) audio?.queryOutputs()
        else { outputLoading = false; outputError = "Audio output is available during live playback" }
    }
    fun closeOutput() { outputDialogOpen = false }
    fun copyShareLink(track: MockTrack) {
        copyText("https://music.youtube.com/watch?v=${track.id}")
    }
    fun copyPlaybackLog(track: MockTrack) {
        copyText("BitChord Desktop\nSong ID: ${track.id}\nTitle: ${track.title}\n" +
            "Duration: ${track.durationSeconds}s\nPosition: ${(track.durationSeconds * progress).toInt()}s\n" +
            "Player: ${if (audioSeekAvailable) "mpv" else "ffplay or inactive"}\n" +
            "Error: ${audioError.orEmpty()}")
    }
    private fun copyText(value: String) {
        runCatching { Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(value), null) }
            .onSuccess { statusMessage = "Copied to clipboard" }
            .onFailure { statusMessage = "Clipboard unavailable: ${it.message}" }
    }
    var sleepTimerEndsAt by mutableStateOf<Long?>(null)
        private set
    fun setSleepTimer(minutes: Int, nowMillis: Long = System.currentTimeMillis()) {
        sleepTimerEndsAt = if (minutes > 0) nowMillis + minutes * 60_000L else null
    }
    fun checkSleepTimer(nowMillis: Long = System.currentTimeMillis()) {
        val end = sleepTimerEndsAt ?: return
        if (nowMillis >= end) {
            sleepTimerEndsAt = null
            audio?.stop()
            setActualPlayback(false)
        }
    }
    fun clearLyrics() {
        lyricLines.clear()
        lyricsLoading = false
        lyricsSource = ""
    }
    var searchLoading by mutableStateOf(false)
    var liveResults by mutableStateOf<List<MockTrack>?>(null)
    val homeShelves = mutableStateListOf<HomeShelf>().apply {
        if (sampleMode) add(HomeShelf("Offline preview", mockTracks))
    }
    var homeLoading by mutableStateOf(false)
    var homeError by mutableStateOf<String?>(null)
    fun refreshHome() {
        val helper = audio
        if (helper == null) {
            homeLoading = false
            homeError = if (sampleMode) null else "The music service is unavailable."
        } else {
            homeLoading = true
            homeError = null
            helper.loadHome()
        }
    }
    private fun isSampleTrack(track: MockTrack) = sampleMode && mockTracks.any { it.id == track.id }
    val isRealTrack: Boolean get() = currentTrack?.let { !isSampleTrack(it) } == true
    fun setActualPlayback(playing: Boolean) { isPlaying = playing }
    var destination by mutableStateOf(Destination.HOME)
        private set
    private val backStack = mutableStateListOf<Destination>()

    val queue = mutableStateListOf<MockTrack>().apply { if (sampleMode) addAll(mockTracks) }
    var currentIndex by mutableIntStateOf(0)
        private set
    val currentTrack: MockTrack? get() = queue.getOrNull(currentIndex)
    var isPlaying by mutableStateOf(false)
        private set
    var progress by mutableFloatStateOf(0f)
        private set
    var playbackOrigin by mutableStateOf("Listen Now")
        private set
    var playerPane by mutableStateOf(PlayerPane.MAIN)
        private set
    var repeatMode by mutableStateOf(RepeatMode.OFF)
        private set
    var shuffleEnabled by mutableStateOf(false)
        private set
    var actionTrack by mutableStateOf<MockTrack?>(null)
        private set
    var playlistPickerTrack by mutableStateOf<MockTrack?>(null)
        private set
    var detailTrack by mutableStateOf<MockTrack?>(null)
        private set
    var selectedPlaylist by mutableStateOf<String?>(null)
        private set
    val playlists = mutableStateMapOf<String, List<MockTrack>>()

    val likedIds = mutableStateListOf<String>().apply {
        if (sampleMode) addAll(listOf("midnight", "less", "after", "space", "chamber"))
    }
    val dislikedIds = mutableStateListOf<String>()
    val knownTracks = mutableStateListOf<MockTrack>().apply { if (sampleMode) addAll(mockTracks) }
    val likedTracks: List<MockTrack> get() = knownTracks.filter { it.id in likedIds }
    fun rememberTracks(tracks: List<MockTrack>) {
        tracks.forEach { track ->
            val index = knownTracks.indexOfFirst { it.id == track.id }
            if (index < 0) knownTracks.add(track)
            else if (knownTracks[index] != track) {
                knownTracks[index] = track
                queue.indices.filter { queue[it].id == track.id }
                    .forEach { queue[it] = track }
                playlists.toMap().forEach { (name, songs) ->
                    if (songs.any { it.id == track.id }) playlists[name] = songs.map {
                        if (it.id == track.id) track else it
                    }
                }
            }
        }
    }
    fun snapshot(): LibrarySnapshot = LibrarySnapshot(
        knownTracks.filter { known -> mockTracks.none { it.id == known.id } }.toList(),
        likedIds.toList(), dislikedIds.toList(), recentSearches.toList(),
        playlists.mapValues { (_, tracks) -> tracks.map { it.id } },
        queue.map { it.id }, currentIndex, repeatMode, shuffleEnabled,
    )

    fun restore(saved: LibrarySnapshot?) {
        if (saved == null) return
        rememberTracks(saved.tracks)
        val tracksById = knownTracks.associateBy { it.id }
        likedIds.clear(); likedIds.addAll(saved.likedIds.filter(tracksById::containsKey).distinct())
        dislikedIds.clear(); dislikedIds.addAll(saved.dislikedIds.filter(tracksById::containsKey).distinct())
        recentSearches.clear(); recentSearches.addAll(saved.recentSearches.take(20))
        playlists.clear()
        saved.playlists.forEach { (name, ids) ->
            playlists[name] = ids.mapNotNull(tracksById::get).distinctBy { it.id }
        }
        val restoredQueue = saved.queueIds.mapNotNull(tracksById::get)
        if (restoredQueue.isNotEmpty()) {
            queue.clear(); queue.addAll(restoredQueue)
            currentIndex = saved.currentIndex.coerceIn(queue.indices)
        }
        repeatMode = saved.repeatMode
        shuffleEnabled = saved.shuffleEnabled
        isPlaying = false // Never start audio from a restored session.
    }
    val recentSearches = mutableStateListOf<String>().apply {
        if (sampleMode) addAll(listOf("M83", "Beach House"))
    }
    var searchQuery by mutableStateOf("")
    val searchResults: List<MockTrack> get() {
        val term = searchQuery.trim()
        return if (term.isBlank()) emptyList() else liveResults ?: if (sampleMode) mockTracks.filter {
            it.title.contains(term, ignoreCase = true) ||
                it.artist.contains(term, ignoreCase = true) ||
                it.album.contains(term, ignoreCase = true)
        } else emptyList()
    }

    fun navigate(to: Destination) {
        if (to == destination) return
        backStack.add(destination)
        destination = to
    }

    fun back() {
        destination = if (backStack.isNotEmpty()) backStack.removeAt(backStack.lastIndex) else Destination.HOME
        if (destination != Destination.NOW_PLAYING) playerPane = PlayerPane.MAIN
    }

    fun selectTrack(track: MockTrack, from: List<MockTrack>, origin: String) {
        videoVersionActive = false
        versionSwitchLoading = false
        originalAudioTrack = null
        clearLyrics()
        val context = from.ifEmpty { listOf(track) }
        queue.clear()
        queue.addAll(context)
        currentIndex = queue.indexOfFirst { it.id == track.id }.takeIf { it >= 0 } ?: 0
        playbackOrigin = origin
        progress = 0f
        isPlaying = audio == null
        if (audio != null) {
            if (isSampleTrack(track)) {
                audio?.stop()
                audioError = "Sample track: search for a real song to play audio."
            }
            else audio?.play(track)
        }
        playerPane = PlayerPane.MAIN
        navigate(Destination.NOW_PLAYING)
    }

    fun selectQueued(index: Int) {
        if (index !in queue.indices) return
        videoVersionActive = false
        versionSwitchLoading = false
        originalAudioTrack = null
        clearLyrics()
        currentIndex = index
        progress = 0f
        isPlaying = audio == null
        if (audio != null) {
            val track = currentTrack
            if (track != null && !isSampleTrack(track)) audio?.play(track)
            else {
                audio?.stop()
                audioError = "Sample track: search for a real song to play audio."
            }
        }
        playerPane = PlayerPane.MAIN
    }

    fun togglePlayback() {
        if (audio == null) isPlaying = !isPlaying
        else if (isRealTrack) {
            if (isAudioLoading) return
            if (audioStreamActive) audio?.pause() else currentTrack?.let { audio?.play(it) }
        }
    }
    fun seekTo(fraction: Float) {
        progress = fraction.coerceIn(0f, 1f)
        if (isRealTrack && audioSeekAvailable) {
            val duration = currentTrack?.durationSeconds ?: 0
            if (duration > 0) audio?.seek(duration * progress)
        }
    }

    fun updatePosition(seconds: Float) {
        val duration = currentTrack?.durationSeconds ?: 0
        if (duration > 0) progress = (seconds / duration).coerceIn(0f, 1f)
    }
    fun previous() {
        if (currentIndex > 0) currentIndex--
        clearLyrics()
        progress = 0f
        if (audio != null) restartAudio()
    }
    fun next() {
        clearLyrics()
        if (currentIndex < queue.lastIndex) {
            if (shuffleEnabled) {
                val nextIndex = (currentIndex + 1..queue.lastIndex).random()
                val chosen = queue[nextIndex]
                queue[nextIndex] = queue[currentIndex + 1]
                queue[currentIndex + 1] = chosen
            }
            currentIndex++
            progress = 0f
            isPlaying = audio == null
            if (audio != null) restartAudio()
        } else if (repeatMode == RepeatMode.ALL && queue.isNotEmpty()) {
            currentIndex = 0
            progress = 0f
            isPlaying = audio == null
            if (audio != null) restartAudio()
        }
    }

    fun advanceOnEnd() {
        if (repeatMode == RepeatMode.ONE && currentTrack != null) {
            progress = 0f
            isPlaying = audio == null
            if (audio != null) restartAudio()
        } else if (currentIndex < queue.lastIndex || repeatMode == RepeatMode.ALL) {
            next()
        } else {
            isPlaying = false
        }
    }

    fun toggleShuffle() { shuffleEnabled = !shuffleEnabled }
    fun cycleRepeat() {
        repeatMode = when (repeatMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
    }

    private fun restartAudio() {
        val track = currentTrack
        if (track != null && !isSampleTrack(track)) audio?.play(track)
        else { audio?.stop(); isPlaying = false; audioError = "Sample track: search for a real song to play audio." }
    }

    fun addToQueue(track: MockTrack) { queue.add(track) }
    fun playNext(track: MockTrack) { queue.add((currentIndex + 1).coerceAtMost(queue.size), track) }
    fun moveUpcoming(from: Int, to: Int) {
        if (from <= currentIndex || to <= currentIndex || from !in queue.indices || to !in queue.indices) return
        val track = queue.removeAt(from)
        queue.add(to, track)
    }
    fun removeUpcoming(index: Int) { if (index > currentIndex && index in queue.indices) queue.removeAt(index) }
    fun clearUpcoming() { while (queue.size > currentIndex + 1) queue.removeAt(queue.lastIndex) }
    fun isLiked(track: MockTrack): Boolean = track.id in likedIds
    fun toggleLike(track: MockTrack) {
        if (isLiked(track)) likedIds.remove(track.id) else {
            dislikedIds.remove(track.id)
            likedIds.add(track.id)
        }
    }
    fun toggleDislike(track: MockTrack) {
        if (track.id in dislikedIds) dislikedIds.remove(track.id) else {
            likedIds.remove(track.id)
            dislikedIds.add(track.id)
        }
    }
    fun openActions(track: MockTrack) { actionTrack = track }
    fun closeActions() { actionTrack = null }
    fun openPlaylistPicker(track: MockTrack) { actionTrack = null; playlistPickerTrack = track }
    fun closePlaylistPicker() { playlistPickerTrack = null }
    fun openAlbum(track: MockTrack) { detailTrack = track; actionTrack = null; navigate(Destination.ALBUM) }
    fun openArtist(track: MockTrack) { detailTrack = track; actionTrack = null; navigate(Destination.ARTIST) }
    val detailTracks: List<MockTrack> get() = detailTrack?.let { selected ->
        knownTracks.filter {
            if (destination == Destination.ALBUM) it.album == selected.album && it.artist == selected.artist
            else it.artist == selected.artist
        }
    }.orEmpty()
    fun openPlaylist(name: String) { selectedPlaylist = name; navigate(Destination.PLAYLIST) }
    fun addToPlaylist(name: String, track: MockTrack) {
        val existing = playlists[name].orEmpty()
        if (existing.none { it.id == track.id }) playlists[name] = existing + track
    }
    fun createPlaylist(name: String): Boolean {
        val cleaned = name.trim()
        if (cleaned.isEmpty() || cleaned in playlists) return false
        playlists[cleaned] = emptyList()
        return true
    }

    fun showPane(pane: PlayerPane) { playerPane = if (playerPane == pane) PlayerPane.MAIN else pane }
    fun recordSearch(term: String = searchQuery) {
        val cleaned = term.trim()
        if (cleaned.isEmpty()) return
        recentSearches.removeAll { it.equals(cleaned, ignoreCase = true) }
        recentSearches.add(0, cleaned)
        while (recentSearches.size > 6) recentSearches.removeAt(recentSearches.lastIndex)
    }
    fun removeSearch(term: String) { recentSearches.remove(term) }
    fun clearSearches() { recentSearches.clear() }
}

internal fun formatTime(seconds: Int): String = "%d:%02d".format(seconds / 60, seconds % 60)
