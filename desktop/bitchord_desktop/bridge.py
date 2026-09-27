"""Line-oriented local bridge for the Compose Desktop preview.

Fields containing user/provider text are base64 encoded. This process never listens
on a socket or stores credentials. stdout is reserved for protocol frames.
"""

import base64
from concurrent.futures import ThreadPoolExecutor
import sys
import threading
import time

from .catalog import Track, lookup_metadata, radio, search
from .downloads import download_audio
from .lyrics import fetch_lyrics
from .player import Player


def unique_tracks(tracks: list[Track]):
    seen_ids: set[str] = set()
    for track in tracks:
        if track.video_id not in seen_ids:
            seen_ids.add(track.video_id)
            yield track


def encode(value: str) -> str:
    return base64.b64encode(value.encode("utf-8")).decode("ascii")


def decode(value: str) -> str:
    return base64.b64decode(value, validate=True).decode("utf-8")


def main() -> None:
    player = Player()
    output_lock = threading.Lock()
    search_pool = ThreadPoolExecutor(max_workers=2)
    lyrics_pool = ThreadPoolExecutor(max_workers=2)
    download_pool = ThreadPoolExecutor(max_workers=1)
    play_pool = ThreadPoolExecutor(max_workers=1)
    playback_lock = threading.Lock()
    playback_serial = 0

    def emit(*fields: str) -> None:
        with output_lock:
            print("\t".join(fields), flush=True)

    def do_search(request: str, query: str) -> None:
        try:
            for track in unique_tracks(search(query)):
                emit("TRACK", request, track.video_id, encode(track.title),
                     encode(track.artist), encode(track.album), encode(track.duration),
                     encode(track.artwork_url))
            emit("DONE", request)
        except Exception as exc:
            emit("ERROR", request, encode(str(exc)))

    def do_lyrics(request: str, title: str, artist: str, duration: int) -> None:
        try:
            for when, line in fetch_lyrics(title, artist, duration):
                emit("LYRIC", request, str(when), encode(line))
            emit("LYRICS_DONE", request, "LRCLIB")
        except Exception as exc:
            emit("LYRICS_ERROR", request, encode(str(exc)))

    def do_artwork(request: str, track_id: str, title: str, artist: str) -> None:
        try:
            track = lookup_metadata(track_id, title, artist)
            if track:
                emit("METADATA", request, encode(track.album), encode(track.artwork_url))
        except Exception:
            # Artwork is optional; a failed lookup must not interrupt audio.
            pass

    def do_radio(request: str, track_id: str) -> None:
        try:
            for track in unique_tracks(radio(track_id)):
                emit("RADIO_TRACK", request, track.video_id, encode(track.title),
                     encode(track.artist), encode(track.album), encode(track.duration),
                     encode(track.artwork_url))
            emit("RADIO_DONE", request)
        except Exception as exc:
            emit("RADIO_ERROR", request, encode(str(exc)))

    def do_download(request: str, track_id: str) -> None:
        try:
            path = download_audio(track_id)
            emit("DOWNLOAD_DONE", request, track_id, encode(str(path)))
        except Exception as exc:
            emit("DOWNLOAD_ERROR", request, track_id, encode(str(exc)))

    def watch_playback(request: str, serial: int) -> None:
        try:
            while True:
                time.sleep(0.5)
                with playback_lock:
                    if serial != playback_serial:
                        return
                code = player.exit_code()
                if code is not None:
                    emit("ENDED" if code == 0 else "ERROR", request,
                         "" if code == 0 else encode(f"Audio player exited with code {code}"))
                    return
                position = player.position()
                if position is not None:
                    emit("POSITION", request, str(position))
        except Exception as exc:
            emit("ERROR", request, encode(str(exc)))

    def do_play(request: str, track_id: str, serial: int) -> None:
        with playback_lock:
            if serial != playback_serial:
                return
        try:
            player.play(Track(track_id, "", ""))
            with playback_lock:
                if serial != playback_serial:
                    player.stop()
                    return
            emit("PLAYING", request, "seek" if player.supports_seek else "")
            threading.Thread(target=watch_playback, args=(request, serial), daemon=True).start()
        except Exception as exc:
            emit("ERROR", request, encode(str(exc)))

    try:
        for line in sys.stdin:
            fields = line.rstrip("\n").split("\t")
            if len(fields) < 2:
                continue
            action, request, *values = fields
            try:
                if action == "SEARCH" and len(values) == 1:
                    search_pool.submit(do_search, request, decode(values[0]))
                elif action == "LYRICS" and len(values) == 3:
                    lyrics_pool.submit(do_lyrics, request, decode(values[0]),
                                       decode(values[1]), int(values[2]))
                elif action == "ARTWORK_LOOKUP" and len(values) == 3:
                    track_id = values[0]
                    if not track_id or len(track_id) > 32 or not all(
                        char.isascii() and (char.isalnum() or char in "_-") for char in track_id
                    ):
                        raise ValueError("Invalid track ID")
                    search_pool.submit(do_artwork, request, track_id, decode(values[1]), decode(values[2]))
                elif action in {"RADIO", "DOWNLOAD"} and len(values) == 1:
                    track_id = values[0]
                    if not track_id or len(track_id) > 32 or not all(
                        char.isascii() and (char.isalnum() or char in "_-") for char in track_id
                    ):
                        raise ValueError("Invalid track ID")
                    if action == "RADIO":
                        search_pool.submit(do_radio, request, track_id)
                    else:
                        download_pool.submit(do_download, request, track_id)
                elif action == "PLAY" and len(values) == 1:
                    track_id = values[0]
                    if not track_id or len(track_id) > 32 or not all(
                        char.isascii() and (char.isalnum() or char in "_-") for char in track_id
                    ):
                        raise ValueError("Invalid track ID")
                    with playback_lock:
                        playback_serial += 1
                        serial = playback_serial
                    player.stop()
                    play_pool.submit(do_play, request, track_id, serial)
                elif action == "PAUSE":
                    paused = player.toggle_pause()
                    emit("PAUSED" if paused else "RESUMED" if paused is False else "ERROR",
                         request, "" if paused is not None else encode("No active audio stream"))
                elif action == "SEEK" and len(values) == 1:
                    try:
                        seconds = float(values[0])
                        if not player.seek(seconds):
                            raise ValueError("Seeking requires mpv and an active stream")
                        emit("POSITION", request, str(seconds))
                    except (ValueError, RuntimeError, OSError) as exc:
                        emit("SEEK_ERROR", request, encode(str(exc)))
                elif action == "OUTPUTS":
                    try:
                        info = player.output_info()
                        if info is None:
                            raise ValueError("Install mpv and play a real track to select audio output")
                        devices, selected, volume = info
                        for name, description in devices:
                            emit("OUTPUT", request, encode(name), encode(description))
                        emit("OUTPUT_DONE", request, encode(selected), str(volume))
                    except (ValueError, RuntimeError, OSError) as exc:
                        emit("OUTPUT_ERROR", request, encode(str(exc)))
                elif action == "VOLUME" and len(values) == 1:
                    try:
                        player.set_volume(float(values[0]))
                        emit("VOLUME_SET", request, values[0])
                    except (ValueError, RuntimeError, OSError) as exc:
                        emit("OUTPUT_ERROR", request, encode(str(exc)))
                elif action == "OUTPUT_SELECT" and len(values) == 1:
                    try:
                        device = decode(values[0])
                        player.set_output(device)
                        emit("OUTPUT_SELECTED", request, encode(device))
                    except (ValueError, RuntimeError, OSError) as exc:
                        emit("OUTPUT_ERROR", request, encode(str(exc)))
                elif action == "QUALITY" and len(values) == 1:
                    try:
                        player.set_quality(values[0])
                        emit("QUALITY_SET", request, values[0])
                    except ValueError as exc:
                        emit("QUALITY_ERROR", request, encode(str(exc)))
                elif action == "PIPELINE":
                    try:
                        for key, value in player.pipeline().items():
                            emit("PIPELINE_FIELD", request, encode(key), encode(value))
                        emit("PIPELINE_DONE", request)
                    except (ValueError, RuntimeError, OSError) as exc:
                        emit("PIPELINE_ERROR", request, encode(str(exc)))
                elif action == "STOP":
                    with playback_lock:
                        playback_serial += 1
                    player.stop()
                    emit("STOPPED", request)
            except Exception as exc:
                emit("ERROR", request, encode(str(exc)))
    finally:
        player.close()
        search_pool.shutdown(wait=False, cancel_futures=True)
        lyrics_pool.shutdown(wait=False, cancel_futures=True)
        download_pool.shutdown(wait=False, cancel_futures=True)
        play_pool.shutdown(wait=False, cancel_futures=True)


if __name__ == "__main__":
    main()
