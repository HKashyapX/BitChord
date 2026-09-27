# BitChord for Linux (early preview)

This is the local Python service used by the independent desktop client. It
loads a guest YouTube Music home feed, searches songs, resolves playback, and
supports the Compose client's queue, downloads, lyrics, radio, and player
controls. Google sign-in and a synchronized account library are not available.

## Run

Install Python 3.10+, Tk (`python3-tkinter` on Fedora, `python3-tk` on
Debian/Ubuntu), and FFmpeg's `ffplay` (`ffmpeg-free` on Fedora, `ffmpeg` on
Debian/Ubuntu). In a terminal at the repository root:

```sh
python3 -m venv .venv-desktop
. .venv-desktop/bin/activate
python -m pip install -e ./desktop
bitchord-desktop
```

Search for a song, double-click a result to play, or select it and press
**Add to queue**. Songs in **Up next** play automatically when the current song
ends; select a queued song and press **Remove queued** to take it out. Playback
needs internet access. Availability may change with YouTube Music's
unofficial API and stream extraction. No account credentials are collected or
stored by this preview.

If a song stops unexpectedly, try another result and check that `ffplay` runs
on your system. Stream extraction errors appear below the player controls.
The standalone Tk preview remains intentionally smaller than the Compose app.

Run the dependency-free unit tests with:

```sh
cd desktop
python3 -m unittest discover -s tests -v
```

## Architecture

`catalog.py` normalizes search results from `ytmusicapi` into `Track` records.
`player.py` resolves a stream through `yt-dlp` and plays it with `ffplay`.
`app.py` keeps the Tk UI responsive while network and playback work run in
background workers. This desktop module is isolated from Android Gradle builds.

Next milestones: authenticated library and playlists, progress/seek controls,
MPRIS media keys, lyrics, then Linux packaging.
Keep any shared source rules aligned with the Android project when extending it.
