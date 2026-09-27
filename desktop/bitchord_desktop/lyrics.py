"""Fetch line-timed lyrics from LRCLIB, using the same source as Android."""

import json
import re
from urllib.error import HTTPError, URLError
from urllib.parse import urlencode
from urllib.request import Request, urlopen

STAMP = re.compile(r"\[(\d{1,3}):(\d{2})(?:\.(\d{1,3}))?\]")
BASE = "https://lrclib.net/api"


def parse_lrc(source: str) -> list[tuple[int, str]]:
    lines = []
    for raw in source.splitlines():
        match = STAMP.search(raw)
        if not match:
            continue
        minutes, seconds, fraction = match.groups()
        fraction_ms = int(fraction.ljust(3, "0")) if fraction else 0
        when = (int(minutes) * 60 + int(seconds)) * 1000 + fraction_ms
        text = STAMP.sub("", raw).strip()
        if text:
            lines.append((when, text))
    return sorted(lines, key=lambda item: item[0])[:500]


def _get(endpoint: str, **params):
    request = Request(f"{BASE}/{endpoint}?{urlencode(params)}",
                      headers={"User-Agent": "BitChord desktop (https://github.com/kushagrasinghx/BitChord)"})
    try:
        with urlopen(request, timeout=7) as response:
            return json.loads(response.read(1_000_001))
    except (HTTPError, URLError, TimeoutError, ValueError):
        return None


def fetch_lyrics(title: str, artist: str, duration: int) -> list[tuple[int, str]]:
    if not title.strip() or not artist.strip():
        return []
    exact = _get("get", track_name=title, artist_name=artist, duration=duration)
    if isinstance(exact, dict):
        lines = parse_lrc(exact.get("syncedLyrics") or "")
        if lines:
            return lines
    candidates = _get("search", track_name=title, artist_name=artist)
    if not isinstance(candidates, list):
        return []
    candidates = [item for item in candidates if isinstance(item, dict)
                  and item.get("syncedLyrics") and isinstance(item.get("duration"), (int, float))
                  and abs(item["duration"] - duration) <= 15]
    candidates.sort(key=lambda item: abs(item["duration"] - duration))
    return parse_lrc(candidates[0]["syncedLyrics"]) if candidates else []
