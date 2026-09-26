# Compose Desktop UI

This module is the first UI-only Compose Desktop slice for BitChord. It uses
local mock tracks and deliberately has no network or audio playback integration.
The existing Android app (`:app`) and Python prototype (`desktop/`) remain
independent.

Run it from the repository root:

```sh
./gradlew --configure-on-demand :desktopApp:run
```

Configure-on-demand keeps this desktop-only task independent of local Android
SDK configuration.

The screen contains an interactive navigation sidebar, local search filtering,
mock search results, a queue, and a persistent mock player bar.
