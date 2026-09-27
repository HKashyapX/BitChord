"""Unauthenticated YouTube Music search, isolated from the desktop UI."""

from dataclasses import dataclass
from urllib.parse import urlparse


@dataclass(frozen=True)
class Track:
    video_id: str
    title: str
    artist: str
    album: str = ""
    duration: str = ""
    artwork_url: str = ""


def parse_track(item: dict) -> Track | None:
    video_id = item.get("videoId")
    if not isinstance(video_id, str) or not video_id or not all(
        char.isascii() and (char.isalnum() or char in "_-") for char in video_id
    ) or len(video_id) > 32:
        return None
    artists = item.get("artists") or []
    names = [a.get("name", "") for a in artists if isinstance(a, dict)]
    album = item.get("album") or {}
    thumbnails = item.get("thumbnails") or []
    artwork_url = ""
    if isinstance(thumbnails, list):
        for thumbnail in reversed(thumbnails):
            url = thumbnail.get("url") if isinstance(thumbnail, dict) else None
            if isinstance(url, str):
                parsed = urlparse(url)
                if parsed.scheme == "https" and parsed.hostname in {
                    "lh3.googleusercontent.com", "i.ytimg.com", "yt3.ggpht.com"
                }:
                    artwork_url = url
                    break
    return Track(
        video_id=video_id,
        title=str(item.get("title") or "Untitled"),
        artist=", ".join(filter(None, names)) or "Unknown artist",
        album=str(album.get("name") or "") if isinstance(album, dict) else "",
        duration=str(item.get("duration") or ""),
        artwork_url=artwork_url,
    )


def search(query: str) -> list[Track]:
    if not query.strip():
        return []
    from ytmusicapi import YTMusic

    results = YTMusic().search(query.strip(), filter="songs", limit=30)
    return [track for item in results if (track := parse_track(item)) is not None]
