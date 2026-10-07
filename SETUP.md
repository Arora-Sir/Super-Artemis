# Development setup

This guide covers building Super Artemis, installing it on a device, pairing it with Apollo, producing signed releases and the repository's documentation checks.

## Prerequisites

- JDK 17 or newer (the JetBrains Runtime bundled with Android Studio works).
- Android SDK with platform 36 and build tools.
- Android NDK `27.0.12077973` (install with `sdkmanager --install "ndk;27.0.12077973"`).
- Python 3.10 or newer for the repository scripts.
- A Windows, Linux or macOS host running Apollo for streaming tests.

## Clone and build

1. Clone with the native submodule: `git clone --recurse-submodules https://github.com/Arora-Sir/Super-Artemis.git`
2. If you cloned without submodules, run `git submodule update --init --recursive`. The core lives in `app/src/main/jni/moonlight-core/moonlight-common-c` on branch `super-artemis` of `Arora-Sir/moonlight-common-c`.
3. Create `local.properties` in the repository root with `sdk.dir=<path to your Android SDK>`.
4. Build the debug APKs: `./gradlew assembleNonRoot_gameDebug` (on Windows `gradlew.bat assembleNonRoot_gameDebug`).
5. The arm64 APK is `app/build/outputs/apk/nonRoot_game/debug/app-nonRoot_game-arm64-v8a-debug.apk`.

Debug and release builds use different package IDs, so both install next to Artemis, Artemis-mic and stock Moonlight.

## Install on a device

- USB: `adb install -r app/build/outputs/apk/nonRoot_game/debug/app-nonRoot_game-arm64-v8a-debug.apk`
- Wireless: `adb connect <device-ip>:5555`, then the same install command with `-s <device-ip>:5555`.
- Debug builds are debuggable, so settings can be inspected with `adb shell run-as <applicationId> cat shared_prefs/<applicationId>_preferences.xml`.

## Pair with Apollo

1. Open the app, tap the PC (it appears automatically on the same network, or add it by IP).
2. The app shows a PIN. Enter it in the Apollo web UI (`https://<host>:47990`, PIN tab).
3. Each build has its own pairing identity, so a new debug install needs pairing once.

## Apollo host settings used by the fork

- Microphone: in `sunshine.conf` set `stream_mic = enabled` and `mic_backend = steam_streaming_microphone`.
- Network drop grace period: the app setting only helps if Apollo's ping timeout is at least as long.
- Connect and disconnect actions: Apollo web UI, Clients, select the device, then add Do commands (run on every session start) and Undo commands (run on every session stop, which includes disconnect and quit).

## Tests

- Unit tests: `./gradlew testNonRoot_gameDebugUnitTest`
- Five upstream tests fail on untouched upstream code too (see BUGS.md, BUG-009). Any failure beyond those five is a regression.

## Release builds and signing

- Release builds are signed only when `KEYSTORE_FILE`, `KEYSTORE_PASSWORD`, `KEY_ALIAS` and `KEY_PASSWORD` are set. Without them the release APK stays unsigned.
- Run `scripts/setup-release-signing.ps1` once (PowerShell 7). It creates the keystore outside the repository and stores the four GitHub Actions secrets. The password is read as a secure string and never echoed.
- Keep the keystore and its password backed up. Losing either blocks updates of the installed app.
- Pushing a `v*` tag runs `.github/workflows/build-unified.yml`, which builds, verifies the signature and package, and creates a draft release with the arm64 APK.

## Documentation checks

- Enable the repository hook once: `git config core.hooksPath .githooks`. It checks only the lines a commit adds, so upstream text is never rewritten.
- The hook blocks em dashes, en dashes and spaced or joined double hyphens in added comment and documentation lines, personal Windows user paths and the machine name, and a stale credits table.
- Fix dashes in specific files with `python scripts/clean_dashes.py <file>` (use `--check` to only report). Do not run it on whole upstream files.
- Refresh the README credits table after editing `app/src/main/res/raw/credits.json`: `python scripts/render_credits.py --write`.
