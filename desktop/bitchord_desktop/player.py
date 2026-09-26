"""Resolve a music stream and hand it to ffplay for Linux audio output."""

import shutil
import subprocess
import threading
from urllib.parse import urlparse

from .catalog import Track


def resolve_stream(video_id: str) -> tuple[str, dict[str, str]]:
    from yt_dlp import YoutubeDL

    with YoutubeDL({"format": "bestaudio/best", "quiet": True, "no_warnings": True, "noplaylist": True}) as dl:
        info = dl.extract_info(f"https://www.youtube.com/watch?v={video_id}", download=False)
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
        if process and process.poll() is None:
            process.terminate()
            try:
                process.wait(timeout=2)
            except subprocess.TimeoutExpired:
                process.kill()
                process.wait(timeout=2)
