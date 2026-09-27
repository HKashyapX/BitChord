package com.music.bitchord.desktop

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import androidx.compose.ui.graphics.Color

class DesktopStateTest {
    @Test
    fun lyricsOffsetIsBounded() {
        val state = DesktopState()
        state.adjustLyricsOffset(500)
        assertEquals(500, state.lyricsOffsetMs)
        state.adjustLyricsOffset(100_000)
        assertEquals(10_000, state.lyricsOffsetMs)
    }
    @Test
    fun refreshedSearchTrackUpdatesOldQueueArtwork() {
        val state = DesktopState()
        val old = MockTrack("abc123", "Song", "Artist", "Album", 180,
            Color.Black, Color.DarkGray)
        state.rememberTracks(listOf(old))
        state.selectTrack(old, listOf(old), "Search")
        val refreshed = old.copy(artworkUrl = "https://i.ytimg.com/vi/abc123/hqdefault.jpg")
        state.rememberTracks(listOf(refreshed))
        assertEquals(refreshed.artworkUrl, state.currentTrack?.artworkUrl)
        assertEquals(refreshed.artworkUrl, state.knownTracks.last().artworkUrl)
        assertEquals(listOf(refreshed.artworkUrl), artworkCandidates(old))
        assertEquals(emptyList(), artworkCandidates(mockTracks.first()))
    }
    @Test
    fun sleepTimerStopsPlaybackAtDeadline() {
        val state = DesktopState()
        state.selectTrack(mockTracks[0], mockTracks, "Test")
        state.setSleepTimer(15, nowMillis = 1_000L)
        state.checkSleepTimer(nowMillis = 900_000L)
        assertEquals(true, state.isPlaying)
        state.checkSleepTimer(nowMillis = 901_000L)
        assertEquals(false, state.isPlaying)
        assertEquals(null, state.sleepTimerEndsAt)
    }
    @Test
    fun playlistSelectionSharesPlayerAndBackDestination() {
        val state = DesktopState()
        state.navigate(Destination.LIKED_MUSIC)
        val liked = state.likedTracks
        state.selectTrack(liked[1], liked, "Liked Music")

        assertEquals(Destination.NOW_PLAYING, state.destination)
        assertEquals(liked[1], state.currentTrack)
        assertEquals("Liked Music", state.playbackOrigin)
        assertTrue(state.isPlaying)
        assertEquals(liked, state.queue.toList())

        state.showPane(PlayerPane.LYRICS)
        assertEquals(PlayerPane.LYRICS, state.playerPane)
        state.showPane(PlayerPane.QUEUE)
        assertEquals(PlayerPane.QUEUE, state.playerPane)
        state.selectQueued(2)
        assertEquals(liked[2], state.currentTrack)
        assertEquals(PlayerPane.MAIN, state.playerPane)
        state.selectQueued(1)
        state.seekTo(0.5f)
        state.next()
        assertEquals(liked[2], state.currentTrack)
        assertEquals(0f, state.progress)
        state.previous()
        assertEquals(liked[1], state.currentTrack)
        state.back()
        assertEquals(Destination.LIKED_MUSIC, state.destination)
    }

    @Test
    fun queueLikeAndSearchRemainInOneSession() {
        val state = DesktopState()
        val track = mockTracks.last()
        state.addToQueue(track)
        assertEquals(track, state.queue.last())
        state.selectQueued(state.queue.lastIndex)
        assertEquals(track, state.currentTrack)

        assertTrue(state.isLiked(track))
        state.toggleLike(track)
        assertFalse(state.isLiked(track))
        assertFalse(track in state.likedTracks)

        state.navigate(Destination.SEARCH)
        state.searchQuery = "beach"
        assertEquals(listOf(mockTracks[4]), state.searchResults)
        state.recordSearch()
        assertEquals("beach", state.recentSearches.first())
        state.selectTrack(state.searchResults.first(), state.searchResults, "Search")
        assertEquals(mockTracks[4], state.currentTrack)
        state.back()
        assertEquals(Destination.SEARCH, state.destination)
        assertEquals("beach", state.searchQuery)
    }

    @Test
    fun playNextReorderAndRepeatKeepQueueConsistent() {
        val state = DesktopState()
        val tracks = mockTracks.take(4)
        state.selectTrack(tracks[0], tracks, "Songs")
        state.playNext(mockTracks[7])
        assertEquals(mockTracks[7], state.queue[1])
        state.moveUpcoming(1, 2)
        assertEquals(tracks[1], state.queue[1])
        assertEquals(mockTracks[7], state.queue[2])
        state.moveUpcoming(2, 0) // Never displace the current track.
        assertEquals(tracks[0], state.currentTrack)

        state.cycleRepeat()
        assertEquals(RepeatMode.ALL, state.repeatMode)
        state.selectQueued(state.queue.lastIndex)
        state.advanceOnEnd()
        assertEquals(tracks[0], state.currentTrack)
        state.cycleRepeat()
        assertEquals(RepeatMode.ONE, state.repeatMode)
        state.advanceOnEnd()
        assertEquals(tracks[0], state.currentTrack)
        state.cycleRepeat()
        assertEquals(RepeatMode.OFF, state.repeatMode)
        state.selectQueued(state.queue.lastIndex)
        state.advanceOnEnd()
        assertFalse(state.isPlaying)
    }

    @Test
    fun actionsPlaylistAndDetailsUseKnownTracks() {
        val state = DesktopState()
        val track = mockTracks[1]
        state.openActions(track)
        assertEquals(track, state.actionTrack)
        state.openPlaylistPicker(track)
        assertEquals(null, state.actionTrack)
        assertTrue(state.createPlaylist("Favorites"))
        assertFalse(state.createPlaylist("Favorites"))
        state.addToPlaylist("Favorites", track)
        state.addToPlaylist("Favorites", track)
        assertEquals(listOf(track), state.playlists["Favorites"])
        state.openPlaylist("Favorites")
        assertEquals(Destination.PLAYLIST, state.destination)
        state.openAlbum(track)
        assertEquals(listOf(track, mockTracks[5]), state.detailTracks)
        state.back()
        assertEquals(Destination.PLAYLIST, state.destination)
        state.openArtist(track)
        assertEquals(listOf(track, mockTracks[5]), state.detailTracks)
        state.toggleDislike(track)
        assertFalse(state.isLiked(track))
        state.toggleLike(track)
        assertFalse(track.id in state.dislikedIds)
    }
}
