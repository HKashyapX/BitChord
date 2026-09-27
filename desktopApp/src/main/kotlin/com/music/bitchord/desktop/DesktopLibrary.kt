package com.music.bitchord.desktop

import androidx.compose.ui.graphics.Color
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.Properties

internal data class LibrarySnapshot(
    val tracks: List<MockTrack>,
    val likedIds: List<String>,
    val dislikedIds: List<String>,
    val recentSearches: List<String>,
    val playlists: Map<String, List<String>>,
    val queueIds: List<String>,
    val currentIndex: Int,
    val repeatMode: RepeatMode,
    val shuffleEnabled: Boolean,
)

/** A local, account-free library file. No cookies, streams, or credentials are written. */
internal class DesktopLibrary(private val path: Path) {
    companion object {
        fun default(): DesktopLibrary {
            val config = System.getenv("XDG_CONFIG_HOME")?.takeIf { it.isNotBlank() }
                ?: Path.of(System.getProperty("user.home"), ".config").toString()
            return DesktopLibrary(Path.of(config, "bitchord", "desktop-library.properties"))
        }
    }

    fun load(): LibrarySnapshot? {
        if (!Files.isRegularFile(path)) return null
        return runCatching {
            val data = Properties()
            Files.newInputStream(path).use { data.load(it) }
            if (data.getProperty("format") != "1") return null
            val tracks = (0 until data.count("tracks", 5000)).mapNotNull { index ->
                val id = data.getProperty("tracks.$index.id") ?: return@mapNotNull null
                val title = data.getProperty("tracks.$index.title") ?: return@mapNotNull null
                MockTrack(id, title, data.getProperty("tracks.$index.artist", ""),
                    data.getProperty("tracks.$index.album", ""),
                    data.getProperty("tracks.$index.duration", "0").toIntOrNull() ?: 0,
                    Color(0xFF303039), Color(0xFF45404A))
            }
            val playlists = (0 until data.count("playlists", 200)).mapNotNull { index ->
                val name = data.getProperty("playlists.$index.name") ?: return@mapNotNull null
                name to data.getProperty("playlists.$index.ids", "").ids()
            }.toMap()
            LibrarySnapshot(tracks, data.getProperty("liked", "").ids(),
                data.getProperty("disliked", "").ids(),
                (0 until data.count("searches", 20)).mapNotNull { data.getProperty("searches.$it") },
                playlists,
                data.getProperty("queue", "").ids(),
                data.getProperty("currentIndex", "0").toIntOrNull() ?: 0,
                runCatching { RepeatMode.valueOf(data.getProperty("repeat", "OFF")) }.getOrDefault(RepeatMode.OFF),
                data.getProperty("shuffle") == "true")
        }.getOrNull()
    }

    fun save(snapshot: LibrarySnapshot) {
        Files.createDirectories(path.parent)
        val data = Properties().apply {
            setProperty("format", "1")
            setProperty("tracks.count", snapshot.tracks.size.toString())
            snapshot.tracks.forEachIndexed { index, track ->
                setProperty("tracks.$index.id", track.id)
                setProperty("tracks.$index.title", track.title)
                setProperty("tracks.$index.artist", track.artist)
                setProperty("tracks.$index.album", track.album)
                setProperty("tracks.$index.duration", track.durationSeconds.toString())
            }
            setProperty("liked", snapshot.likedIds.joinToString(","))
            setProperty("disliked", snapshot.dislikedIds.joinToString(","))
            setProperty("searches.count", snapshot.recentSearches.size.toString())
            snapshot.recentSearches.forEachIndexed { index, term -> setProperty("searches.$index", term) }
            setProperty("playlists.count", snapshot.playlists.size.toString())
            snapshot.playlists.entries.forEachIndexed { index, entry ->
                setProperty("playlists.$index.name", entry.key)
                setProperty("playlists.$index.ids", entry.value.joinToString(","))
            }
            setProperty("queue", snapshot.queueIds.joinToString(","))
            setProperty("currentIndex", snapshot.currentIndex.toString())
            setProperty("repeat", snapshot.repeatMode.name)
            setProperty("shuffle", snapshot.shuffleEnabled.toString())
        }
        val temporary = Files.createTempFile(path.parent, "desktop-library-", ".tmp")
        try {
            Files.newOutputStream(temporary).use { data.store(it, "BitChord desktop library") }
            try {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING)
            }
        } finally {
            Files.deleteIfExists(temporary)
        }
    }

    private fun Properties.count(prefix: String, max: Int) =
        (getProperty("$prefix.count")?.toIntOrNull() ?: 0).coerceIn(0, max)
    private fun String.ids() = split(',').filter { it.isNotBlank() }
}
