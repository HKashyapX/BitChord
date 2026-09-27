"""Unauthenticated YouTube Music search, isolated from the desktop UI."""

from dataclasses import dataclass
import re
from urllib.parse import urlparse


@dataclass(frozen=True)
class Track:
    video_id: str
    title: str
    artist: str
    album: str = ""
    duration: str = ""
    artwork_url: str = ""
    is_video: bool = False


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
        choices = []
        for thumbnail in thumbnails:
            url = thumbnail.get("url") if isinstance(thumbnail, dict) else None
            if isinstance(url, str):
                parsed = urlparse(url)
                if parsed.scheme == "https" and parsed.hostname in {
                    "lh3.googleusercontent.com", "i.ytimg.com", "yt3.ggpht.com"
                }:
                    width = thumbnail.get("width", 0)
                    height = thumbnail.get("height", 0)
                    size = width * height if isinstance(width, int) and isinstance(height, int) else 0
                    choices.append((size, url))
        if choices:
            artwork_url = max(choices, key=lambda candidate: candidate[0])[1]
    return Track(
        video_id=video_id,
        title=str(item.get("title") or "Untitled"),
        artist=", ".join(filter(None, names)) or "Unknown artist",
        album=str(album.get("name") or "") if isinstance(album, dict) else "",
        duration=str(item.get("duration") or ""),
        artwork_url=artwork_url,
        is_video=item.get("resultType") == "video" or item.get("videoType") == "MUSIC_VIDEO_TYPE_OMV",
    )


def search(query: str) -> list[Track]:
    if not query.strip():
        return []
    from ytmusicapi import YTMusic

    results = YTMusic().search(query.strip(), filter="songs", limit=30)
    return [track for item in results if (track := parse_track(item)) is not None]


def lookup_artwork(video_id: str, title: str, artist: str) -> str:
    """Refresh metadata for a saved track without substituting another song's art."""
    for track in search(f"{title} {artist}"):
        if track.video_id == video_id and track.artwork_url:
            return track.artwork_url
    return ""


def lookup_metadata(video_id: str, title: str, artist: str) -> Track | None:
    """Refresh artwork and album only for the same song ID."""
    return next((track for track in search(f"{title} {artist}") if track.video_id == video_id), None)


def radio(video_id: str) -> list[Track]:
    from ytmusicapi import YTMusic

    results = YTMusic().get_watch_playlist(videoId=video_id, limit=25, radio=True)
    items = results.get("tracks") or []
    if not isinstance(items, list):
        return []
    return [track for item in items if isinstance(item, dict)
            and (track := parse_track(item)) is not None]


_TITLE_NOISE = re.compile(
    r'\((?:official|lyric|lyrics|lyrical|audio|video|visuali[sz]er|full song|hd|4k)[^)]*\)'
    r'|\(from[^)]*\)|\[[^]]*]|\b(?:official (?:video|audio|music video)|lyrical video|full video|4k video)\b',
    re.IGNORECASE,
)
_PUNCTUATION = re.compile(r'[^\w]+', re.UNICODE)


def normalized_title(value: str) -> str:
    value = value.lower().split(" | ", 1)[0]
    return " ".join(_PUNCTUATION.sub(" ", _TITLE_NOISE.sub(" ", value)).split())


def artist_set(value: str) -> set[str]:
    value = re.sub(r"\s*-\s*topic\b", " ", value.lower(), flags=re.IGNORECASE)
    parts = re.split(r",|&|·|•|;|\bfeat\b|\bft\.?\b|\bx\b|\bwith\b", value)
    return {" ".join(_PUNCTUATION.sub(" ", part).split()) for part in parts
            if " ".join(_PUNCTUATION.sub(" ", part).split())}


def same_recording(left: Track, right: Track) -> bool:
    if left.video_id == right.video_id:
        return True
    if not normalized_title(left.title) or normalized_title(left.title) != normalized_title(right.title):
        return False
    left_artists, right_artists = artist_set(left.artist), artist_set(right.artist)
    return not left_artists or not right_artists or bool(left_artists & right_artists)


def build_radio(seed: Track, candidates: list[Track], limit: int = 20) -> list[Track]:
    """Port Android QueueBuilder's recording de-duplication and artist caps."""
    chosen: list[Track] = []
    counts: dict[str, int] = {}
    seed_artists = artist_set(seed.artist)
    for candidate in candidates:
        if len(chosen) >= limit or any(same_recording(item, candidate) for item in [seed, *chosen]):
            continue
        artists = artist_set(candidate.artist)
        key = min(artists) if artists else ""
        if key:
            cap = 4 if artists & seed_artists else 2
            if counts.get(key, 0) >= cap:
                continue
            counts[key] = counts.get(key, 0) + 1
        chosen.append(candidate)
    return chosen


def _duration_seconds(value: str) -> int:
    try:
        return sum(int(part) * 60 ** power
                   for power, part in enumerate(reversed(value.split(":"))))
    except ValueError:
        return 0


def find_video_version(track: Track) -> Track | None:
    """Port Android resolveVideo: search videos and accept the closest real track match."""
    from ytmusicapi import YTMusic

    results = YTMusic().search(f"{track.title} {track.artist}", filter="videos", limit=20)
    candidates = [candidate for item in results
                  if (candidate := parse_track(item)) is not None
                  and candidate.video_id != track.video_id
                  and same_recording(track, candidate)]
    if not candidates:
        return None
    target_duration = _duration_seconds(track.duration)
    return min(candidates, key=lambda candidate: (
        abs(_duration_seconds(candidate.duration) - target_duration)
        if target_duration and _duration_seconds(candidate.duration) else 10_000
    ))
