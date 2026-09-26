package com.music.bitchord.desktop

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DesktopStateTest {
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
}
