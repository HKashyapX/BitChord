# Compose Desktop UI

This Compose Desktop preview uses one local session for navigation, selection,
queue, likes, searches, and playback controls. Search and audio for **real search
results** now use the existing Python desktop catalog/player through a local
subprocess. No server, Docker, account, or credentials are needed. The Android
app and Listen Together server remain separate.

On Fedora, install Python 3.10+, `mpv` for elapsed time and seeking, and the
Python package. `ffplay` remains a fallback for playback without seeking:

```sh
sudo dnf install python3 mpv
python3 -m venv .venv-desktop
.venv-desktop/bin/python -m pip install -e ./desktop
```

If you already set up `.venv-desktop`, only install the package if it is missing.
The app prefers `.venv-desktop/bin/python` and otherwise tries `python3`.

Run it from the repository root:

```sh
./gradlew --configure-on-demand :desktopApp:run
```

Configure-on-demand keeps this desktop-only task independent of local Android
SDK configuration.

Open Search, type a song or artist, then select a result to hear it. Search
requires internet. Play/pause and next/previous operate on the real results
in the queue. The Home, Songs, and initial Liked Music tracks remain **sample
data** and cannot be played; the player explains this when one is selected.

The song actions menu offers like/dislike, play next, add to queue, add to a
local playlist, a session-only sleep timer for the current song, and open album
or artist detail pages for tracks seen in this
session. Likes, playlists, known search tracks, recents, and queue state are
stored locally at `$XDG_CONFIG_HOME/bitchord/desktop-library.properties` (or
`~/.config/bitchord/desktop-library.properties`). The Up next panel supports
moving and removing upcoming tracks; shuffle chooses from the remaining queue
when advancing, and repeat cycles off → all → one. Queue actions are shared
between Now Playing and the browsing sidebar.
Now Playing displays the album when known. Song actions can copy a share link
or a diagnostic log, while the Lyrics pane has a line timing offset. With mpv,
the Audio output dialog lists available devices and controls player volume.
Real tracks can be downloaded to `$XDG_DATA_HOME/bitchord/downloads` (or
`~/.local/share/bitchord/downloads`) and played from there on future selections.
Start radio builds a fresh queue from YouTube Music's watch radio response.
Real search results advance to the next queued track when audio ends. With
`mpv`, the player reports elapsed time and supports seeking. With only `ffplay`,
the real-track seek control stays disabled and elapsed time is unavailable.
Real search tracks show the largest provider artwork when available. Older saved
tracks refresh their artwork when played; if the provider has no matching music
cover, the video thumbnail is cropped and shown at a bounded size. Sample tracks
retain placeholders. The
Lyrics pane tries LRCLIB for line-synchronized lyrics, highlights
the current line, and lets you select a line when mpv supports seeking. Provider
coverage varies. Sign-in and playback from the sample catalog are still pending. The
underlying Python playback preview remains usable independently.

For a UI-only demo without starting Python, run with `--offline`:

```sh
./gradlew --configure-on-demand :desktopApp:run --args="--offline"
```

Follow the flow from Home's Listen again shelf into Liked Music. Selecting a
track opens Now Playing; its Lyrics and Up next buttons switch the right pane.
The back arrow returns to the playlist. Search has recent queries and live
local-catalog results. Selecting any result updates the same player session.
Lyrics display a labeled unavailable state when the provider has no match.

Keyboard: Tab and Shift+Tab move focus, Enter/Space activate focused controls,
Alt+Left goes back, Escape leaves Now Playing, and Ctrl+F opens Search.

For layout checks, use `--large` (1920×1080) or `--narrow` (900×680). The
following optional flags open a preview state directly: `--liked`,
`--now-playing` (which accepts `--lyrics` or `--queue`), or `--search=term`.
For example:

```sh
./gradlew --configure-on-demand :desktopApp:run --args="--narrow --now-playing --queue"
```
