"""Resolve a music stream and play it through mpv (or ffplay as fallback)."""

import shutil
import json
import os
import signal
import socket
import subprocess
import tempfile
import threading
import time
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
        self.backend = ""
        self._ipc_dir: str | None = None

    @property
    def supports_seek(self) -> bool:
        return self.backend == "mpv"

    def play(self, track: Track) -> None:
        backend = "mpv" if shutil.which("mpv") else "ffplay"
        if not shutil.which(backend):
            raise RuntimeError("Install mpv (recommended) or ffplay with your Linux package manager.")
        url, headers = resolve_stream(track.video_id)
        with self._lock:
            if self._closed:
                return
            self._stop_locked()
            self.backend = backend
            if backend == "mpv":
                self._ipc_dir = tempfile.mkdtemp(prefix="bitchord-mpv-")
                socket_path = os.path.join(self._ipc_dir, "ipc")
                command = ["mpv", "--no-config", "--no-video", "--terminal=no",
                           f"--input-ipc-server={socket_path}"]
                lower_headers = {key.lower(): value for key, value in headers.items()}
                if "user-agent" in lower_headers:
                    command += [f"--user-agent={lower_headers['user-agent']}"]
                if "referer" in lower_headers:
                    command += [f"--referrer={lower_headers['referer']}"]
                if "origin" in lower_headers:
                    command += [f"--http-header-fields-append=Origin: {lower_headers['origin']}"]
            else:
                header_arg = "".join(f"{key}: {value}\r\n" for key, value in headers.items())
                command = ["ffplay", "-nodisp", "-autoexit", "-loglevel", "error"]
                if header_arg:
                    command += ["-headers", header_arg]
            command += [url]
            try:
                self.process = subprocess.Popen(command, stdin=subprocess.DEVNULL,
                                                stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
            except Exception:
                self._cleanup_ipc_locked()
                raise
            self.paused = False

    def _command_locked(self, *args):
        if not self._ipc_dir or not self.process or self.process.poll() is not None:
            raise RuntimeError("No active mpv stream")
        path = os.path.join(self._ipc_dir, "ipc")
        # mpv creates its listening socket shortly after process startup.
        for _ in range(20):
            if os.path.exists(path):
                break
            if self.process.poll() is not None:
                raise RuntimeError("mpv exited before its control socket was ready")
            time.sleep(0.05)
        with socket.socket(socket.AF_UNIX, socket.SOCK_STREAM) as connection:
            connection.settimeout(1.0)
            connection.connect(path)
            connection.sendall((json.dumps({"command": list(args), "request_id": 1}) + "\n").encode())
            with connection.makefile("r", encoding="utf-8") as response:
                for line in response:
                    frame = json.loads(line)
                    if frame.get("request_id") == 1:
                        if frame.get("error") != "success":
                            raise RuntimeError(f"mpv command failed: {frame.get('error')}")
                        return frame.get("data")
        raise RuntimeError("mpv did not respond")

    def position(self) -> float | None:
        with self._lock:
            if not self.supports_seek:
                return None
            try:
                value = self._command_locked("get_property", "time-pos")
                return max(0.0, float(value)) if value is not None else None
            except (OSError, ValueError, RuntimeError):
                return None

    def seek(self, seconds: float) -> bool:
        with self._lock:
            if not self.supports_seek or not 0 <= seconds < float("inf"):
                return False
            self._command_locked("seek", seconds, "absolute")
            return True

    def toggle_pause(self) -> bool | None:
        """Toggle a live process; return None if there is nothing to pause."""
        with self._lock:
            if not self.process or self.process.poll() is not None:
                return None
            if self.supports_seek:
                self._command_locked("set_property", "pause", not self.paused)
                self.paused = not self.paused
                return self.paused
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
            if was_paused and self.backend != "mpv":
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
        self._cleanup_ipc_locked()
        self.backend = ""

    def _cleanup_ipc_locked(self) -> None:
        if self._ipc_dir:
            path = os.path.join(self._ipc_dir, "ipc")
            if os.path.exists(path):
                os.unlink(path)
            os.rmdir(self._ipc_dir)
            self._ipc_dir = None
