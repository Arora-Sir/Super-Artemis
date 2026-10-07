# Super Artemis

Super Artemis is a personal build of [Artemis](https://github.com/ClassicOldSong/moonlight-android) (ClassicOldSong's Moonlight fork) that adds upstream microphone streaming with a low-power toggle and a few community improvements. It installs as `com.mohitarora.superartemis` (debug builds as `com.mohitarora.superartemis.debug`), so it runs side by side with Artemis and Artemis-mic.

## What is added on top of Artemis

| Feature | Where it comes from | Notes |
| :--- | :--- | :--- |
| Microphone streaming to the host | logabell `codex/mic-android` (`e01ab88`, `f16c1b5`) and `logabell/moonlight-common-c` | Needs a host that negotiates the mic stream, such as Apollo with `stream_mic = enabled`. |
| Microphone at stream start (Always off, Always on, Follow last state) | Modeled on Moonlight V+ PR #639 | The mic channel is negotiated at connect, but AudioRecord and the Opus encoder only run while unmuted. |
| Mute and unmute from the in-stream Quick Menu and Advanced menu | New | Muting stops capture, so the mic hardware powers down and the privacy indicator turns off. |
| Post-start low-latency decoder parameter | derflacco `d8f6f620` | Android 11 and newer. |
| Stylus isolation on the stream view | Moonlight V+ PR #634 | Stops the handwriting IME from taking S Pen strokes (Android 13 and newer). |
| Network drop grace period (10 to 60 seconds) | KAST `25ffe75` (common-c) and `b277878` (wiring) | The host must also allow the same silence, see Host setup. |
| Settings search | `ByteHamster/SearchPreference` | Results inside collapsed categories expand before they are highlighted. |
| Backup and restore of settings, paired hosts and pairing keys | New, modeled on DarQ-Reborn | Single JSON file through the Storage Access Framework. Settings only by default, pairing keys are an explicit opt-in at export. |
| Remember mouse mode by default | Artemis setting with a new default | |
| Customizable in-stream menu | New | Pick and reorder Quick Menu actions (Settings or Advanced Menu > Customize menu), choose List, Grid with icons or Compact. Actions left out stay under Advanced Menu. |
| Five-finger tap has its own setting | New | Turning off Back Menu no longer disables the five-finger menu gesture. |

The native core lives in [`Arora-Sir/moonlight-common-c`](https://github.com/Arora-Sir/moonlight-common-c) on branch `super-artemis`, which is the Artemis core (`c999436`) plus logabell's three microphone commits and KAST's peer timeout commit.

Menu icons are [Material Symbols](https://github.com/google/material-design-icons) (Apache License 2.0).

## Host setup

Enable the microphone in Apollo's `sunshine.conf` with `stream_mic = enabled` and `mic_backend = steam_streaming_microphone`. For the network drop grace period to help, set Apollo's ping timeout to at least the grace period chosen in the app, otherwise the host ends the session on its own timeout first.

## Building

Local debug build (JDK 17 or newer, NDK `27.0.12077973`):

```powershell
.\gradlew.bat assembleNonRoot_gameDebug
```

Release builds run on GitHub Actions (`.github/workflows/build-unified.yml`) when a `v*` tag is pushed, and publish a draft release with the signed arm64 APK. Run `scripts/setup-release-signing.ps1` once to create the release keystore and store the four signing secrets in the repository. Keep the keystore and its password backed up, because a lost key blocks updates of the installed app.

## Backup file contents

A backup contains every app SharedPreferences file except device-specific crash and GPU state, framework internals and settings search history, the profiles file, and the paired host list with server certificates. Only when "Include pairing keys" is checked (it is off by default) does it also contain the client certificate, private key and unique ID, and anyone holding that file can connect to your paired PCs as this device. Every backup records a SHA-256 fingerprint of the public client certificate, so a restore can tell whether added PCs need pairing again. Restoring shows a preview first. Settings are always replaced. Without pairing keys the PCs already on the device are kept and only new PCs are added. With pairing keys the PC list and the pairing identity are replaced by the backup's. The app then restarts.
