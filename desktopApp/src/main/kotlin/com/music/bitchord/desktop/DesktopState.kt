package com.music.bitchord.desktop

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

internal data class MockTrack(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationSeconds: Int,
    val coverStart: Color,
    val coverEnd: Color,
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

internal enum class Destination { HOME, SEARCH, LIKED_MUSIC, SONGS, NOW_PLAYING }
internal enum class PlayerPane { MAIN, LYRICS, QUEUE }

/** All desktop interactions use one local session; live audio is optional. */
internal class DesktopState {
    var audio by mutableStateOf<DesktopAudio?>(null)
    var audioError by mutableStateOf<String?>(null)
    var isAudioLoading by mutableStateOf(false)
    var audioStreamActive by mutableStateOf(false)
    var searchLoading by mutableStateOf(false)
    var liveResults by mutableStateOf<List<MockTrack>?>(null)
    val isRealTrack: Boolean get() = currentTrack?.let { it !in mockTracks } == true
    fun setActualPlayback(playing: Boolean) { isPlaying = playing }
    var destination by mutableStateOf(Destination.HOME)
        private set
    private val backStack = mutableStateListOf<Destination>()

    val queue = mutableStateListOf<MockTrack>().apply { addAll(mockTracks) }
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

    val likedIds = mutableStateListOf("midnight", "less", "after", "space", "chamber")
    val knownTracks = mutableStateListOf<MockTrack>().apply { addAll(mockTracks) }
    val likedTracks: List<MockTrack> get() = knownTracks.filter { it.id in likedIds }
    fun rememberTracks(tracks: List<MockTrack>) {
        tracks.forEach { track -> if (knownTracks.none { it.id == track.id }) knownTracks.add(track) }
    }
    val recentSearches = mutableStateListOf("M83", "Beach House")
    var searchQuery by mutableStateOf("")
    val searchResults: List<MockTrack> get() {
        val term = searchQuery.trim()
        return if (term.isBlank()) emptyList() else liveResults ?: mockTracks.filter {
            it.title.contains(term, ignoreCase = true) ||
                it.artist.contains(term, ignoreCase = true) ||
                it.album.contains(term, ignoreCase = true)
        }
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
        val context = from.ifEmpty { listOf(track) }
        queue.clear()
        queue.addAll(context)
        currentIndex = queue.indexOfFirst { it.id == track.id }.takeIf { it >= 0 } ?: 0
        playbackOrigin = origin
        progress = 0f
        isPlaying = audio == null
        if (audio != null) {
            if (track in mockTracks) {
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
        currentIndex = index
        progress = 0f
        isPlaying = audio == null
        if (audio != null) {
            val track = currentTrack
            if (track != null && track !in mockTracks) audio?.play(track)
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
    fun seekTo(fraction: Float) { progress = fraction.coerceIn(0f, 1f) }
    fun previous() {
        if (currentIndex > 0) currentIndex--
        progress = 0f
        if (audio != null) restartAudio()
    }
    fun next() {
        if (currentIndex < queue.lastIndex) {
            currentIndex++
            progress = 0f
            isPlaying = audio == null
            if (audio != null) restartAudio()
        }
    }

    private fun restartAudio() {
        val track = currentTrack
        if (track != null && track !in mockTracks) audio?.play(track)
        else { audio?.stop(); audioError = "Sample track: search for a real song to play audio." }
    }

    fun addToQueue(track: MockTrack) { queue.add(track) }
    fun removeUpcoming(index: Int) { if (index > currentIndex && index in queue.indices) queue.removeAt(index) }
    fun clearUpcoming() { while (queue.size > currentIndex + 1) queue.removeAt(queue.lastIndex) }
    fun isLiked(track: MockTrack): Boolean = track.id in likedIds
    fun toggleLike(track: MockTrack) {
        if (isLiked(track)) likedIds.remove(track.id) else likedIds.add(track.id)
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
