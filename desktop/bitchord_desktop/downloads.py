"""Local, account-free audio downloads for the desktop player."""

import os
from pathlib import Path

MEDIA_SUFFIXES = {".webm", ".m4a", ".opus", ".mp3", ".mp4"}


def downloads_dir() -> Path:
    base = os.environ.get("XDG_DATA_HOME") or str(Path.home() / ".local" / "share")
    return Path(base) / "bitchord" / "downloads"


def saved_audio(video_id: str) -> Path | None:
    if not video_id or len(video_id) > 32 or not all(
        char.isascii() and (char.isalnum() or char in "_-") for char in video_id
    ):
        return None
    directory = downloads_dir()
    for file in directory.glob(f"{video_id}.*"):
        if file.suffix.lower() in MEDIA_SUFFIXES and file.is_file() and file.stat().st_size > 0:
            return file
    return None


def download_audio(video_id: str) -> Path:
    from yt_dlp import YoutubeDL

    existing = saved_audio(video_id)
    if existing is not None:
        return existing
    if not video_id or len(video_id) > 32 or not all(
        char.isascii() and (char.isalnum() or char in "_-") for char in video_id
    ):
        raise ValueError("Invalid song ID")
    directory = downloads_dir()
    directory.mkdir(parents=True, exist_ok=True)
    with YoutubeDL({"format": "bestaudio/best", "outtmpl": str(directory / f"{video_id}.%(ext)s"),
                    "quiet": True, "no_warnings": True, "noplaylist": True,
                    "socket_timeout": 15, "retries": 2}) as dl:
        dl.download([f"https://www.youtube.com/watch?v={video_id}"])
    result = saved_audio(video_id)
    if result is None:
        raise RuntimeError("Download finished without a playable audio file")
    return result
