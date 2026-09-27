package com.music.bitchord.desktop

import androidx.compose.ui.graphics.Color
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

class DesktopLibraryTest {
    @Test
    fun libraryRoundTripPreservesSelectionAndDoesNotAutoplay() {
        val directory = Files.createTempDirectory("bitchord-library-test")
        val path = directory.resolve("library.properties")
        try {
            val storage = DesktopLibrary(path)
            val live = MockTrack("abc123_-", "A song with ünicode", "Artist", "Album", 210,
                Color(0xFF303039), Color(0xFF45404A), "https://i.ytimg.com/vi/abc/default.jpg")
            val original = DesktopState()
            original.rememberTracks(listOf(live))
            original.searchQuery = "Artist"
            original.recordSearch()
            original.toggleLike(live)
            original.createPlaylist("My playlist")
            original.addToPlaylist("My playlist", live)
            original.selectTrack(live, listOf(live), "Search")
            original.cycleRepeat()
            original.toggleShuffle()

            storage.save(original.snapshot())
            val restored = DesktopState()
            restored.restore(assertNotNull(storage.load()))
            assertEquals(live.id, restored.currentTrack?.id)
            assertEquals(live.title, restored.currentTrack?.title)
            assertEquals(live.artworkUrl, restored.currentTrack?.artworkUrl)
            assertEquals(listOf(live.id), restored.playlists["My playlist"]?.map { it.id })
            assertEquals("Artist", restored.recentSearches.first())
            assertEquals(RepeatMode.ALL, restored.repeatMode)
            assertEquals(true, restored.shuffleEnabled)
            assertFalse(restored.isPlaying)
        } finally {
            Files.deleteIfExists(path)
            Files.deleteIfExists(directory)
        }
    }
}
