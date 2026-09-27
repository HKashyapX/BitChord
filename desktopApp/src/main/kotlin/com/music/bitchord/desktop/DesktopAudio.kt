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
    private var homeId = 0
    private var playbackId = 0
    private var pendingQualitySeek: Float? = null
    private var requestId = 0
    private val pending = mutableMapOf<Int, MutableList<MockTrack>>()
    private val pendingHome = mutableMapOf<Int, LinkedHashMap<String, MutableList<MockTrack>>>()
    private val pendingLyrics = mutableMapOf<Int, MutableList<LyricLine>>()
    private val pendingRadio = mutableMapOf<Int, MutableList<MockTrack>>()
    private val radioSeeds = mutableMapOf<Int, MockTrack>()
    private val downloadRequests = mutableMapOf<Int, MockTrack>()
    private val videoRequests = mutableSetOf<Int>()

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
            state.homeLoading = false
            if (state.homeShelves.isEmpty()) state.homeError = state.audioError
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

    fun loadHome() {
        val id = ++requestId
        homeId = id
        pendingHome.clear()
        pendingHome[id] = linkedMapOf()
        state.homeLoading = true
        state.homeError = null
        send("HOME", id.toString())
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
        requestLyrics(track, "auto", id)
        send("ARTWORK_LOOKUP", id.toString(), track.id, encode(track.title), encode(track.artist))
    }

    fun pause() = send("PAUSE", playbackId.toString())
    fun seek(seconds: Float) = send("SEEK", playbackId.toString(), seconds.toString())
    fun queryOutputs() = send("OUTPUTS", playbackId.toString())
    fun setVolume(value: Float) = send("VOLUME", playbackId.toString(), value.toString())
    fun selectOutput(name: String) = send("OUTPUT_SELECT", playbackId.toString(), encode(name))
    fun requestLyrics(track: MockTrack, provider: String = "auto", id: Int = playbackId) {
        pendingLyrics[id] = mutableListOf()
        state.lyricsLoading = true
        send("LYRICS", id.toString(), encode(track.title), encode(track.artist),
            track.durationSeconds.toString(), encode(track.album), provider)
    }
    fun queryPipeline() = send("PIPELINE", playbackId.toString())
    fun changeQuality(mode: String, track: MockTrack) {
        if (mode !in setOf("standard", "best")) return
        val position = track.durationSeconds * state.progress
        pendingQualitySeek = position.takeIf { state.audioSeekAvailable && it > 0f }
        send("QUALITY", playbackId.toString(), mode)
        if (state.audioStreamActive) play(track)
    }
    fun playVideo(track: MockTrack) {
        val id = ++requestId
        videoRequests.add(id)
        pendingQualitySeek = (track.durationSeconds * state.progress)
            .takeIf { state.audioSeekAvailable && it > 0f }
        state.versionSwitchLoading = true
        send("VIDEO", id.toString(), track.id, encode(track.title), encode(track.artist),
            encode(track.album), formatTime(track.durationSeconds))
    }
    fun playAt(track: MockTrack, seconds: Float) {
        pendingQualitySeek = seconds.takeIf { it > 0f }
        play(track)
    }
    fun radio(track: MockTrack) {
        val id = ++requestId
        pendingRadio.clear(); radioSeeds.clear()
        pendingRadio[id] = mutableListOf()
        radioSeeds[id] = track
        send("RADIO", id.toString(), track.id, encode(track.title), encode(track.artist))
    }
    fun download(track: MockTrack) {
        val id = ++requestId
        downloadRequests[id] = track
        send("DOWNLOAD", id.toString(), track.id)
    }
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
            "TRACK" -> if (id == searchId && fields.size >= 7) pending[id]?.add(parseTrack(fields))
            "HOME_TRACK" -> if (id == homeId && fields.size >= 9) {
                val title = decode(fields[2])
                pendingHome[id]?.getOrPut(title) { mutableListOf() }?.add(parseTrack(fields, 3))
            }
            "HOME_DONE" -> if (id == homeId) {
                val shelves = pendingHome.remove(id).orEmpty().map { (title, tracks) ->
                    HomeShelf(title, tracks.distinctBy { it.id })
                }.filter { it.tracks.isNotEmpty() }
                state.homeShelves.clear()
                state.homeShelves.addAll(shelves)
                state.rememberTracks(shelves.flatMap { it.tracks })
                state.homeLoading = false
                if (shelves.isEmpty()) state.homeError = "YouTube Music returned no playable home sections."
            }
            "HOME_ERROR" -> if (id == homeId) {
                pendingHome.remove(id)
                state.homeLoading = false
                state.homeError = fields.getOrNull(2)?.let(::decode) ?: "Could not load Home."
            }
            "VIDEO_TRACK" -> if (videoRequests.remove(id) && fields.size >= 7) {
                playbackId = id
                state.activateVideoVersion(parseTrack(fields))
            }
            "VIDEO_ERROR" -> if (videoRequests.remove(id)) {
                state.versionSwitchLoading = false
                state.statusMessage = fields.getOrNull(2)?.let(::decode)
            }
            "RADIO_TRACK" -> if (id in pendingRadio && fields.size >= 7) pendingRadio[id]?.add(parseTrack(fields))
            "RADIO_DONE" -> {
                val seed = radioSeeds.remove(id)
                if (seed != null) {
                    val tracks = pendingRadio.remove(id).orEmpty().filter { it.id != seed.id }
                    state.radioLoading = false
                    if (tracks.isEmpty()) state.statusMessage = "No radio tracks were returned"
                    else {
                        state.rememberTracks(tracks)
                        state.selectTrack(seed, listOf(seed) + tracks, "Radio")
                        state.statusMessage = "Radio · ${tracks.size} songs"
                    }
                }
            }
            "RADIO_ERROR" -> if (id in radioSeeds) {
                pendingRadio.remove(id); radioSeeds.remove(id)
                state.radioLoading = false
                state.statusMessage = fields.getOrNull(2)?.let(::decode)
            }
            "DOWNLOAD_DONE" -> downloadRequests.remove(id)?.let { track ->
                state.downloadingIds.remove(track.id)
                if (track.id !in state.downloadedIds) state.downloadedIds.add(track.id)
                state.statusMessage = "Saved ${track.title} for offline playback"
            }
            "DOWNLOAD_ERROR" -> downloadRequests.remove(id)?.let { track ->
                state.downloadingIds.remove(track.id)
                state.statusMessage = "Download failed: ${fields.getOrNull(3)?.let(::decode).orEmpty()}"
            }
            "DONE" -> if (id == searchId) {
                state.liveResults = pending.remove(id).orEmpty()
                state.rememberTracks(state.liveResults.orEmpty())
                state.searchLoading = false
            }
            "PLAYING" -> if (id == playbackId) {
                state.isAudioLoading = false
                state.versionSwitchLoading = false
                state.audioStreamActive = true
                state.audioSeekAvailable = fields.getOrNull(2) == "seek"
                state.setActualPlayback(true)
                pendingQualitySeek?.let { seconds ->
                    if (state.audioSeekAvailable) seek(seconds)
                    pendingQualitySeek = null
                }
            }
            "QUALITY_SET" -> if (id == playbackId) state.qualityMode = fields.getOrNull(2).orEmpty()
            "QUALITY_ERROR" -> if (id == playbackId) state.statusMessage = fields.getOrNull(2)?.let(::decode)
            "PIPELINE_FIELD" -> if (id == playbackId && fields.size >= 4) {
                state.pipelineFields[decode(fields[2])] = decode(fields[3])
            }
            "PIPELINE_DONE" -> if (id == playbackId) state.pipelineLoading = false
            "PIPELINE_ERROR" -> if (id == playbackId) {
                state.pipelineLoading = false
                state.pipelineError = fields.getOrNull(2)?.let(::decode)
            }
            "POSITION" -> if (id == playbackId) fields.getOrNull(2)?.toFloatOrNull()?.let(state::updatePosition)
            "OUTPUT" -> if (id == playbackId && fields.size >= 4) {
                state.outputDevices.add(OutputDevice(decode(fields[2]), decode(fields[3])))
            }
            "OUTPUT_DONE" -> if (id == playbackId) {
                state.outputSelected = fields.getOrNull(2)?.let(::decode).orEmpty()
                state.outputVolume = fields.getOrNull(3)?.toFloatOrNull() ?: 100f
                state.outputLoading = false
            }
            "OUTPUT_SELECTED" -> if (id == playbackId) state.outputSelected = fields.getOrNull(2)?.let(::decode).orEmpty()
            "VOLUME_SET" -> if (id == playbackId) state.outputVolume = fields.getOrNull(2)?.toFloatOrNull() ?: state.outputVolume
            "OUTPUT_ERROR" -> if (id == playbackId) {
                state.outputLoading = false
                state.outputError = fields.getOrNull(2)?.let(::decode)
            }
            "METADATA" -> if (id == playbackId) {
                val track = state.currentTrack
                val album = fields.getOrNull(2)?.let(::decode).orEmpty()
                val url = fields.getOrNull(3)?.let(::decode).orEmpty()
                if (track != null && (album.isNotBlank() || url.isNotBlank())) {
                    state.rememberTracks(listOf(track.copy(
                        album = album.ifBlank { track.album }, artworkUrl = url.ifBlank { track.artworkUrl }
                    )))
                }
            }
            "LYRIC" -> if (id == playbackId && fields.size >= 4) {
                val time = fields[2].toIntOrNull()
                if (time != null) pendingLyrics[id]?.add(LyricLine(time, decode(fields[3])))
            }
            "LYRICS_DONE" -> if (id == playbackId) {
                state.lyricLines.clear()
                state.lyricLines.addAll(pendingLyrics.remove(id).orEmpty())
                state.lyricsSource = if (state.lyricLines.isEmpty()) "" else fields.getOrNull(2)?.let(::decode).orEmpty()
                state.lyricsLoading = false
                if (state.lyricLines.isNotEmpty()) state.lyricsProviderDialogOpen = false
                else state.statusMessage = "This provider did not find synchronized lyrics"
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
    private fun parseTrack(fields: List<String>, start: Int = 2) = MockTrack(fields[start], decode(fields[start + 1]), decode(fields[start + 2]),
        decode(fields[start + 3]), parseDuration(decode(fields[start + 4])),
        androidx.compose.ui.graphics.Color(0xFF303039), androidx.compose.ui.graphics.Color(0xFF45404A),
        fields.getOrNull(start + 5)?.let(::decode).orEmpty())
}

internal fun parseDuration(value: String): Int = value.split(':').mapNotNull(String::toIntOrNull)
    .fold(0) { seconds, unit -> seconds * 60 + unit }
