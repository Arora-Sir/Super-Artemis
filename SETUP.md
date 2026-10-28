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
5. The arm64 APK is `app/build/outputs/apk/nonRoot_game/debug/SuperArtemis-<version>-arm64-v8a-debug.apk`.

Release builds install as `com.mohitarora.superartemis` and debug builds as `com.mohitarora.superartemis.debug`, so both install next to Artemis, Artemis-mic and stock Moonlight.

## Install on a device

- USB: `adb install -r app/build/outputs/apk/nonRoot_game/debug/SuperArtemis-<version>-arm64-v8a-debug.apk`
- Wireless: `adb connect <device-ip>:5555`, then the same install command with `-s <device-ip>:5555`.
- Debug builds are debuggable, so settings can be inspected with `adb shell run-as <applicationId> cat shared_prefs/<applicationId>_preferences.xml`.

## Pair with Apollo

1. Open the app, tap the PC (it appears automatically on the same network, or add it by IP).
2. The app shows a PIN. Enter it in the Apollo web UI (`https://<host>:47990`, PIN tab).
3. Each build has its own pairing identity, so a new debug install needs pairing once.

## Apollo host settings used by the fork

- Microphone: in `sunshine.conf` set `stream_mic = enabled` and `mic_backend = steam_streaming_microphone`.
- Network drop grace period: the app setting only helps if Apollo's ping timeout is at least as long.
- Connect and disconnect actions: see the next section.

## Run commands on the PC when a stream starts and stops

Apollo runs per-client commands on the host: Do commands when a session starts and Undo commands when it stops, which includes Disconnect, Quit and a dropped connection. This replaces watching logs on the PC (for example with AutoHotkey) to react to disconnects, and nothing is needed in the app.

1. Open the Apollo web UI (`https://<host>:47990`), go to Clients and select this device.
2. Add a Do command and an Undo command, and enable "Run as administrator" only when the command needs it.
3. Save. The commands run for this device only, so other clients are not affected.

Example: a faster mouse while streaming from a tablet, and the normal speed afterwards. Save this as `mouse-speed.ps1` on the PC:

```powershell
param([int]$Speed = 10)
$signature = '[DllImport("user32.dll")] public static extern bool SystemParametersInfo(int action, int param, IntPtr value, int update);'
$user32 = Add-Type -MemberDefinition $signature -Name User32 -Namespace Win32 -PassThru
# SPI_SETMOUSESPEED (0x71), saved to the profile and broadcast to running apps
$user32::SystemParametersInfo(0x71, 0, [IntPtr]$Speed, 3) | Out-Null
```

- Do: `powershell -NoProfile -ExecutionPolicy Bypass -File C:\Scripts\mouse-speed.ps1 -Speed 16`
- Undo: `powershell -NoProfile -ExecutionPolicy Bypass -File C:\Scripts\mouse-speed.ps1 -Speed 10`

Windows mouse speed ranges from 1 to 20, and 10 is the default.

Example: make Steam Streaming Microphone the default Windows microphone while a stream runs, so voice chat apps on the PC hear the tablet, and put the previous microphone back on Disconnect or Quit. It needs the AudioDeviceCmdlets module once (`Install-Module AudioDeviceCmdlets -Scope CurrentUser`). Save this as `stream-mic.ps1` on the PC:

```powershell
param([switch]$Restore)
$state = Join-Path $env:LOCALAPPDATA 'SuperArtemis\default-mic.txt'
if ($Restore) {
    # Put back the microphone that was the default before the stream started
    if (Test-Path $state) {
        Set-AudioDevice -ID (Get-Content $state) | Out-Null
        Remove-Item $state
    }
    return
}
$current = Get-AudioDevice -Recording
if ($current.Name -notlike '*Steam Streaming Microphone*') {
    New-Item -ItemType Directory -Force (Split-Path $state) | Out-Null
    Set-Content $state $current.ID
}
$steam = Get-AudioDevice -List | Where-Object { $_.Type -eq 'Recording' -and $_.Name -like '*Steam Streaming Microphone*' } | Select-Object -First 1
if ($steam) {
    Set-AudioDevice -ID $steam.ID | Out-Null
}
```

- Do: `powershell -NoProfile -ExecutionPolicy Bypass -File C:\Scripts\stream-mic.ps1`
- Undo: `powershell -NoProfile -ExecutionPolicy Bypass -File C:\Scripts\stream-mic.ps1 -Restore`

Leave "Run as administrator" off, because the default microphone belongs to your Windows user. The Steam microphone stays silent until the microphone is unmuted in the app, so keeping it as the default for the whole session is harmless. Apollo only creates it when `mic_backend = steam_streaming_microphone` is set.

## Lock the PC from a button

The built-in Lock key sends Windows + L. Windows treats that shortcut specially, and some hosts ignore it when it is injected, so a reliable way is an Apollo command that locks the session. In the Apollo web UI add a command to the client (Clients > this device > Do, or an application entry) with:

```
rundll32.exe user32.dll,LockWorkStation
```

Leave "Run as administrator" off, since the lock belongs to your Windows user. The Lock key keeps working as a shortcut on hosts that accept it.

Windows ignores a Win+L that a remote session injects, even though the Lock key sends the right keys. The simplest fix is an AutoHotkey v2 script on the PC. The repository has it as `scripts/host/SuperArtemisLock.ahk`, and Settings > Host > "Lock key does nothing?" shows the same two lines with a copy button. Save the script, run it, and put it in the Startup folder (Win+R, then `shell:startup`).

## Keeping up with upstream

Follow UPSTREAM.md. The weekly "Upstream watch" issue lists new commits, releases and security advisories from every project the fork builds on, and `python scripts/upstream_watch.py` produces the same report locally.

## Tests

- Unit tests: `./gradlew testNonRoot_gameDebugUnitTest`
- Five upstream tests fail on untouched upstream code too (see BUGS.md, BUG-009). Any failure beyond those five is a regression.

## Release builds and signing

- Release builds are signed only when `KEYSTORE_FILE`, `KEYSTORE_PASSWORD`, `KEY_ALIAS` and `KEY_PASSWORD` are set. Without them the release APK stays unsigned.
- Follow RELEASING.md: run `python scripts/release/generate_keystore.py --out-dir <folder outside the repo>` once, then store the four secrets in the protected `release` environment. The password is read with a hidden prompt and never printed.
- Keep the keystore and its password backed up. Losing either blocks updates of the installed app.
- [RELEASING.md](RELEASING.md) has the full step-by-step guide: keystore, protected environment, certificate check, checksums and each release.
- Pushing a `v*` tag runs `.github/workflows/build-unified.yml`, which builds, verifies the signature, the package and that the tag equals `v<versionName>`, and creates a draft release with `SuperArtemis-v<version>-arm64-v8a.apk`.

## Documentation checks

- Enable the repository hook once: `git config core.hooksPath .githooks`. It checks only the lines a commit adds, so upstream text is never rewritten.
- The hook blocks em dashes, en dashes and spaced or joined double hyphens in added comment and documentation lines, personal Windows user paths and the machine name, and a stale credits table.
- Fix dashes in specific files with `python scripts/clean_dashes.py <file>` (use `--check` to only report). Do not run it on whole upstream files.
- Refresh the README credits table after editing `app/src/main/res/raw/credits.json`: `python scripts/render_credits.py --write`.
