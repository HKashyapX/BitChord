package com.music.bitchord.desktop

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
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

/** All desktop interactions use one local session; live audio is optional. */
internal class DesktopState {
    var audio by mutableStateOf<DesktopAudio?>(null)
    var audioError by mutableStateOf<String?>(null)
    var isAudioLoading by mutableStateOf(false)
    var audioStreamActive by mutableStateOf(false)
    var audioSeekAvailable by mutableStateOf(false)
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
    val playlists = mutableStateMapOf("Desktop Mix" to listOf<MockTrack>())

    val likedIds = mutableStateListOf("midnight", "less", "after", "space", "chamber")
    val dislikedIds = mutableStateListOf<String>()
    val knownTracks = mutableStateListOf<MockTrack>().apply { addAll(mockTracks) }
    val likedTracks: List<MockTrack> get() = knownTracks.filter { it.id in likedIds }
    fun rememberTracks(tracks: List<MockTrack>) {
        tracks.forEach { track -> if (knownTracks.none { it.id == track.id }) knownTracks.add(track) }
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
        if (playlists.isEmpty()) playlists["Desktop Mix"] = emptyList()
        val restoredQueue = saved.queueIds.mapNotNull(tracksById::get)
        if (restoredQueue.isNotEmpty()) {
            queue.clear(); queue.addAll(restoredQueue)
            currentIndex = saved.currentIndex.coerceIn(queue.indices)
        }
        repeatMode = saved.repeatMode
        shuffleEnabled = saved.shuffleEnabled
        isPlaying = false // Never start audio from a restored session.
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
        progress = 0f
        if (audio != null) restartAudio()
    }
    fun next() {
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
        if (track != null && track !in mockTracks) audio?.play(track)
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
