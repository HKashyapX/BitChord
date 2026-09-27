"""Desktop lyric providers following Android BitChord's ordered fallback model."""

import html
import json
import re
from urllib.error import HTTPError, URLError
from urllib.parse import urlencode
from urllib.request import Request, urlopen
from xml.etree import ElementTree

STAMP = re.compile(r"\[(\d{1,3}):(\d{2})(?:\.(\d{1,3}))?\]")
LRCLIB_BASE = "https://lrclib.net/api"
BETTER_LYRICS_BASE = "https://lyrics-api.boidu.dev/getLyrics"
PROVIDERS = ("BetterLyrics", "LRCLIB")
MAX_RESPONSE = 1_000_000
_LYRIC_CREDITS = (
    re.compile(r"\s*[\[(]\s*(?:feat|ft|featuring|with)\b[^)\]]*[)\]]", re.IGNORECASE),
    re.compile(r"\s+(?:feat|ft|featuring)\.?\s+.*$", re.IGNORECASE),
    re.compile(r"\s*[\[(]\s*(?:official\s*)?(?:music\s*)?"
               r"(?:video|audio|visuali[sz]er|lyrics?\s*video|lyrics?|m/?v|hd|hq|4k|full\s*song)"
               r"\s*[)\]]", re.IGNORECASE),
    re.compile(r"\s*[\[(]\s*official\s*[)\]]", re.IGNORECASE),
)


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


def _time_ms(value: str | None) -> int | None:
    if not value:
        return None
    raw = value.strip()
    if raw.endswith("ms"):
        try:
            return int(float(raw[:-2]))
        except ValueError:
            return None
    raw = raw.removesuffix("s")
    try:
        parts = [float(part) for part in raw.split(":")]
    except ValueError:
        return None
    if len(parts) == 1:
        seconds = parts[0]
    elif len(parts) == 2:
        seconds = parts[0] * 60 + parts[1]
    elif len(parts) == 3:
        seconds = parts[0] * 3600 + parts[1] * 60 + parts[2]
    else:
        return None
    return max(0, int(seconds * 1000))


def parse_ttml(source: str) -> list[tuple[int, str]]:
    """Read line timestamps from the TTML used by Android's Apple lyric providers."""
    try:
        root = ElementTree.fromstring(source)
    except ElementTree.ParseError:
        return []
    lines: list[tuple[int, str]] = []
    for paragraph in root.iter():
        if paragraph.tag.rsplit("}", 1)[-1] != "p":
            continue
        role = next((value for key, value in paragraph.attrib.items()
                     if key.rsplit("}", 1)[-1] == "role"), "")
        if role in {"x-translation", "x-roman"}:
            continue
        when = _time_ms(paragraph.attrib.get("begin"))
        text = " ".join("".join(paragraph.itertext()).split())
        if when is not None and text:
            lines.append((when, text))
    return sorted(lines, key=lambda item: item[0])[:500]


def _extract_content(value):
    if isinstance(value, str):
        stripped = value.strip()
        try:
            nested = json.loads(stripped)
        except (ValueError, TypeError):
            return stripped
        return _extract_content(nested)
    if isinstance(value, list):
        parts = [_extract_content(item) for item in value]
        return "\n".join(part for part in parts if part)
    if isinstance(value, dict):
        if value.get("isError") is True or value.get("ok") is False or value.get("error"):
            return None
        for key in ("ttml", "ttmlContent", "lyrics", "lrc", "content", "text",
                    "plainLyrics", "syncedLyrics", "data", "result", "response"):
            if key in value and (found := _extract_content(value[key])):
                return found
    return None


def parse_provider_payload(raw: str) -> list[tuple[int, str]]:
    content = _extract_content(raw) or raw.strip()
    if "&lt;tt" in content.lower():
        content = html.unescape(content)
    if "<tt" in content.lower() or "www.w3.org/ns/ttml" in content.lower():
        return parse_ttml(content)
    return parse_lrc(content)


def _read(url: str, accept: str = "application/json") -> str | None:
    request = Request(url, headers={
        "User-Agent": "BitChord desktop (https://github.com/kushagrasinghx/BitChord)",
        "Accept": accept,
    })
    try:
        with urlopen(request, timeout=7) as response:
            data = response.read(MAX_RESPONSE + 1)
            return data.decode("utf-8") if len(data) <= MAX_RESPONSE else None
    except (HTTPError, URLError, TimeoutError, ValueError, UnicodeError):
        return None


def _lrclib(title: str, artist: str, duration: int, album: str = "") -> list[tuple[int, str]]:
    params = {"track_name": title, "artist_name": artist, "duration": duration}
    if album:
        params["album_name"] = album
    raw = _read(f"{LRCLIB_BASE}/get?{urlencode(params)}")
    if raw:
        try:
            exact = json.loads(raw)
        except ValueError:
            exact = None
        if isinstance(exact, dict) and (lines := parse_lrc(exact.get("syncedLyrics") or "")):
            return lines
    raw = _read(f"{LRCLIB_BASE}/search?{urlencode({'track_name': title, 'artist_name': artist})}")
    try:
        candidates = json.loads(raw) if raw else None
    except ValueError:
        candidates = None
    if not isinstance(candidates, list):
        return []
    matches = [item for item in candidates if isinstance(item, dict)
               and item.get("syncedLyrics") and isinstance(item.get("duration"), (int, float))
               and (duration <= 0 or abs(item["duration"] - duration) <= 15)]
    matches.sort(key=lambda item: abs(item["duration"] - duration) if duration > 0 else 0)
    return parse_lrc(matches[0]["syncedLyrics"]) if matches else []


def _better_lyrics(title: str, artist: str, duration: int, album: str = "") -> list[tuple[int, str]]:
    params = {"s": title, "a": artist}
    if duration > 0:
        params["d"] = duration
    if album:
        params["al"] = album
    raw = _read(f"{BETTER_LYRICS_BASE}?{urlencode(params)}", "application/json, text/plain, */*")
    return parse_provider_payload(raw) if raw else []


def fetch_from_provider(title: str, artist: str, duration: int, album: str = "",
                        provider: str = "auto") -> tuple[str, list[tuple[int, str]]]:
    if not title.strip() or not artist.strip():
        return "", []
    clean_title = title
    for pattern in _LYRIC_CREDITS:
        clean_title = pattern.sub(" ", clean_title)
    clean_title = " ".join(clean_title.split()).strip(" ,-–—") or title.strip()
    clean_artist = re.sub(r"\s*-\s*Topic$", "", artist, flags=re.IGNORECASE).strip() or artist.strip()
    requested = PROVIDERS if provider == "auto" else (provider,)
    for source in requested:
        if source == "BetterLyrics":
            lines = _better_lyrics(clean_title, clean_artist, duration, album)
        elif source == "LRCLIB":
            lines = _lrclib(clean_title, clean_artist, duration, album)
        else:
            continue
        if lines:
            return source, lines
    return "", []


def fetch_lyrics(title: str, artist: str, duration: int) -> list[tuple[int, str]]:
    """Compatibility wrapper used by existing callers and tests."""
    return fetch_from_provider(title, artist, duration)[1]
