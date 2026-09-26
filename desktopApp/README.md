# Compose Desktop UI

This module is a UI-only Compose Desktop slice for BitChord. It uses one local
session for navigation, selection, queue, likes, searches, and playback controls.
It deliberately has no network or audio playback integration.
The existing Android app (`:app`) and Python prototype (`desktop/`) remain
independent.

Run it from the repository root:

```sh
./gradlew --configure-on-demand :desktopApp:run
```

Configure-on-demand keeps this desktop-only task independent of local Android
SDK configuration.

Follow the flow from Home's Listen again shelf into Liked Music. Selecting a
track opens Now Playing; its Lyrics and Up next buttons switch the right pane.
The back arrow returns to the playlist. Search has recent queries and live
local-catalog results. Selecting any result updates the same player session.
The player clock and seek position change only through UI controls; no song is
actually played. Lyrics display a labeled unavailable state. Covers are local
neutral placeholders.

Keyboard: Tab and Shift+Tab move focus, Enter/Space activate focused controls,
Alt+Left goes back, Escape leaves Now Playing, and Ctrl+F opens Search.

For layout checks, use `--large` (1920×1080) or `--narrow` (900×680). The
following optional flags open a preview state directly: `--liked`,
`--now-playing` (which accepts `--lyrics` or `--queue`), or `--search=term`.
For example:

```sh
./gradlew --configure-on-demand :desktopApp:run --args="--narrow --now-playing --queue"
```
