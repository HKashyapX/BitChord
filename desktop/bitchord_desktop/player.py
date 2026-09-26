"""Resolve a music stream and hand it to ffplay for Linux audio output."""

import shutil
import os
import signal
import subprocess
import threading
from urllib.parse import urlparse

from .catalog import Track


def resolve_stream(video_id: str) -> tuple[str, dict[str, str]]:
    from yt_dlp import YoutubeDL
    from yt_dlp.utils import DownloadError

    try:
        with YoutubeDL({"format": "bestaudio/best", "quiet": True, "no_warnings": True,
                        "noplaylist": True, "socket_timeout": 10,
                        "retries": 1, "extractor_retries": 1}) as dl:
            info = dl.extract_info(f"https://www.youtube.com/watch?v={video_id}", download=False)
    except DownloadError as exc:
        raise RuntimeError(f"Could not load the stream: {exc}") from exc
    if not isinstance(info, dict):
        raise RuntimeError("No playable stream was returned")
    url = info.get("url")
    if not isinstance(url, str) or urlparse(url).scheme != "https":
        raise RuntimeError("No HTTPS audio stream was returned")
    headers = info.get("http_headers") or {}
    if not isinstance(headers, dict):
        headers = {}
    safe_headers = {
        key: value for key, value in headers.items()
        if isinstance(key, str) and isinstance(value, str)
        and key.lower() in {"user-agent", "referer", "origin"}
        and "\r" not in value and "\n" not in value
    }
    return url, safe_headers


class Player:
    def __init__(self) -> None:
        self.process: subprocess.Popen | None = None
        self._lock = threading.Lock()
        self._closed = False
        self.paused = False

    def play(self, track: Track) -> None:
        if not shutil.which("ffplay"):
            raise RuntimeError("ffplay is required. Install FFmpeg with your Linux package manager.")
        url, headers = resolve_stream(track.video_id)
        header_arg = "".join(f"{key}: {value}\r\n" for key, value in headers.items())
        command = ["ffplay", "-nodisp", "-autoexit", "-loglevel", "error"]
        if header_arg:
            command += ["-headers", header_arg]
        command += [url]
        with self._lock:
            if self._closed:
                return
            self._stop_locked()
            self.process = subprocess.Popen(command, stdin=subprocess.DEVNULL,
                                            stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
            self.paused = False

    def toggle_pause(self) -> bool | None:
        """Toggle a live ffplay process; return None if there is nothing to pause."""
        with self._lock:
            if not self.process or self.process.poll() is not None:
                return None
            try:
                os.kill(self.process.pid, signal.SIGCONT if self.paused else signal.SIGSTOP)
            except ProcessLookupError:
                return None
            self.paused = not self.paused
            return self.paused

    def exit_code(self) -> int | None:
        with self._lock:
            return self.process.poll() if self.process else None

    def stop(self) -> None:
        with self._lock:
            self._stop_locked()

    def close(self) -> None:
        with self._lock:
            self._closed = True
            self._stop_locked()

    def _stop_locked(self) -> None:
        process = self.process
        self.process = None
        was_paused = self.paused
        self.paused = False
        if process and process.poll() is None:
            # Resume a suspended process so it can handle SIGTERM and exit.
            if was_paused:
                try:
                    os.kill(process.pid, signal.SIGCONT)
                except ProcessLookupError:
                    pass
            process.terminate()
            try:
                process.wait(timeout=2)
            except subprocess.TimeoutExpired:
                process.kill()
                process.wait(timeout=2)
