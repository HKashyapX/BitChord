package com.music.bitchord.desktop

import java.io.File
import java.util.Base64
import javax.swing.SwingUtilities

/** Connects the UI to the local Python catalog and audio player. */
internal class DesktopAudio(private val state: DesktopState) : AutoCloseable {
    private val project = generateSequence(File(System.getProperty("user.dir")).absoluteFile) { it.parentFile }
        .firstOrNull { File(it, "desktop/bitchord_desktop/bridge.py").isFile }
        ?: error("Run BitChord from the repository directory")
    private val python = listOf(
        File(project, ".venv-desktop/bin/python"), File(project, ".venv/bin/python")
    ).firstOrNull { it.canExecute() }?.absolutePath ?: "python3"
    private val process = ProcessBuilder(python, "-m", "bitchord_desktop.bridge")
        .directory(project)
        .apply {
            environment()["PYTHONPATH"] = File(project, "desktop").absolutePath
            redirectError(ProcessBuilder.Redirect.INHERIT)
        }.start()
    private val writer = process.outputStream.bufferedWriter()
    private var searchId = 0
    private var playbackId = 0
    private var requestId = 0
    private val pending = mutableMapOf<Int, MutableList<MockTrack>>()
    private val pendingLyrics = mutableMapOf<Int, MutableList<LyricLine>>()

    init {
        Thread({
            try {
                process.inputStream.bufferedReader().forEachLine { line ->
                    SwingUtilities.invokeLater { receive(line) }
                }
            } finally {
                SwingUtilities.invokeLater { state.audioError = "Audio helper stopped. Check Python dependencies." }
            }
        }, "bitchord-audio-reader").apply { isDaemon = true }.start()
    }

    private fun send(vararg fields: String) {
        try {
            synchronized(writer) {
                writer.write(fields.joinToString("\t"))
                writer.newLine()
                writer.flush()
            }
        } catch (exc: Exception) {
            state.audioError = "Audio helper unavailable: ${exc.message}"
            state.searchLoading = false
            state.isAudioLoading = false
        }
    }

    fun search(query: String) {
        val id = ++requestId
        searchId = id
        pending.clear()
        pending[id] = mutableListOf()
        state.searchLoading = true
        state.audioError = null
        send("SEARCH", id.toString(), encode(query))
    }

    fun play(track: MockTrack) {
        if (!track.id.matches(Regex("[A-Za-z0-9_-]{1,32}"))) return
        val id = ++requestId
        playbackId = id
        state.audioError = null
        state.isAudioLoading = true
        state.audioSeekAvailable = false
        state.clearLyrics()
        state.lyricsLoading = true
        pendingLyrics.clear()
        pendingLyrics[id] = mutableListOf()
        send("PLAY", id.toString(), track.id)
        send("LYRICS", id.toString(), encode(track.title), encode(track.artist), track.durationSeconds.toString())
    }

    fun pause() = send("PAUSE", playbackId.toString())
    fun seek(seconds: Float) = send("SEEK", playbackId.toString(), seconds.toString())
    fun stop() {
        playbackId = ++requestId
        state.audioStreamActive = false
        state.audioSeekAvailable = false
        state.isAudioLoading = false
        state.setActualPlayback(false)
        send("STOP", playbackId.toString())
    }

    private fun receive(line: String) {
        val fields = line.split('\t')
        if (fields.size < 2) return
        val id = fields[1].toIntOrNull() ?: return
        when (fields[0]) {
            "TRACK" -> if (id == searchId && fields.size >= 7) {
                pending[id]?.add(MockTrack(fields[2], decode(fields[3]), decode(fields[4]),
                    decode(fields[5]), parseDuration(decode(fields[6])),
                    androidx.compose.ui.graphics.Color(0xFF303039),
                    androidx.compose.ui.graphics.Color(0xFF45404A),
                    fields.getOrNull(7)?.let(::decode).orEmpty()))
            }
            "DONE" -> if (id == searchId) {
                state.liveResults = pending.remove(id).orEmpty()
                state.rememberTracks(state.liveResults.orEmpty())
                state.searchLoading = false
            }
            "PLAYING" -> if (id == playbackId) {
                state.isAudioLoading = false
                state.audioStreamActive = true
                state.audioSeekAvailable = fields.getOrNull(2) == "seek"
                state.setActualPlayback(true)
            }
            "POSITION" -> if (id == playbackId) fields.getOrNull(2)?.toFloatOrNull()?.let(state::updatePosition)
            "LYRIC" -> if (id == playbackId && fields.size >= 4) {
                val time = fields[2].toIntOrNull()
                if (time != null) pendingLyrics[id]?.add(LyricLine(time, decode(fields[3])))
            }
            "LYRICS_DONE" -> if (id == playbackId) {
                state.lyricLines.clear()
                state.lyricLines.addAll(pendingLyrics.remove(id).orEmpty())
                state.lyricsSource = if (state.lyricLines.isEmpty()) "" else fields.getOrNull(2).orEmpty()
                state.lyricsLoading = false
            }
            "LYRICS_ERROR" -> if (id == playbackId) {
                pendingLyrics.remove(id)
                state.lyricsLoading = false
            }
            "SEEK_ERROR" -> if (id == playbackId) state.audioError = fields.getOrNull(2)?.let(::decode)
            "PAUSED" -> if (id == playbackId) state.setActualPlayback(false)
            "RESUMED" -> if (id == playbackId) state.setActualPlayback(true)
            "ENDED" -> if (id == playbackId) {
                state.audioStreamActive = false
                state.audioSeekAvailable = false
                state.isAudioLoading = false
                state.setActualPlayback(false)
                state.advanceOnEnd()
            }
            "STOPPED" -> if (id == playbackId) {
                state.setActualPlayback(false)
                state.audioStreamActive = false
                state.audioSeekAvailable = false
            }
            "ERROR" -> if (id == searchId && state.searchLoading) {
                state.searchLoading = false
                state.liveResults = emptyList()
                state.audioError = fields.getOrNull(2)?.let(::decode)
            } else if (id == playbackId) {
                state.isAudioLoading = false
                state.audioStreamActive = false
                state.audioSeekAvailable = false
                state.setActualPlayback(false)
                state.audioError = fields.getOrNull(2)?.let(::decode)
            }
        }
    }

    override fun close() {
        runCatching { stop(); writer.close() }
        process.destroy()
    }

    private fun encode(value: String) = Base64.getEncoder().encodeToString(value.toByteArray(Charsets.UTF_8))
    private fun decode(value: String) = String(Base64.getDecoder().decode(value), Charsets.UTF_8)
}

internal fun parseDuration(value: String): Int = value.split(':').mapNotNull(String::toIntOrNull)
    .fold(0) { seconds, unit -> seconds * 60 + unit }
