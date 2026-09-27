"""Line-oriented local bridge for the Compose Desktop preview.

Fields containing user/provider text are base64 encoded. This process never listens
on a socket or stores credentials. stdout is reserved for protocol frames.
"""

import base64
from concurrent.futures import ThreadPoolExecutor
import sys
import threading
import time

from .catalog import Track, search
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
        play_pool.shutdown(wait=False, cancel_futures=True)


if __name__ == "__main__":
    main()
