# Changelog

All notable changes to Super Artemis are listed here. The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and versions follow [Semantic Versioning](https://semver.org/). Super Artemis is based on Artemis 20.2.6 (ClassicOldSong/moonlight-android, branch `moonlight-noir`).

## [Unreleased]

### Added

- Microphone streaming to Apollo hosts that negotiate a mic stream (Steam Streaming Microphone on Windows), ported from @logabell's work in ClassicOldSong/moonlight-android#537.
- "Microphone at stream start" setting with Always off (default), Always on and Follow last state, remembered per PC.
- Mute and unmute from the in-stream Quick Menu and Advanced Menu. Muting stops the microphone hardware and the Opus encoder, so the privacy indicator turns off and no battery is spent on audio capture.
- "Network drop grace period" setting (10 to 60 seconds, default 30) so short Wi-Fi drops do not end the session, from @MikalaiKryvusha's KAST.
- Settings search with results inside collapsed categories.
- Backup and restore of all settings, profiles and the PC list to a single JSON file through the system file picker. Pairing keys are an explicit opt-in with a clear caution.
- Restore shows a preview first (date, app version, settings and PC counts, what happens to your PCs). A backup without pairing keys keeps the PCs already on the device and only adds new ones.
- Customizable in-stream Quick Menu: choose and reorder actions in an editor, with List, Grid with icons and Compact styles. The editor has a style picker and a live preview, and "Customize menu" sits right in the Quick Menu.
- The Quick Menu is split into Actions, Session and Menu sections. Disconnect and Quit are always in the Session section, drawn in red. The Advanced Menu has "Back to Quick Menu".
- Separate "Five-finger tap opens menu" setting.
- Credits for everyone whose code, design or request shaped the fork, in the README and release notes.

### Changed

- Stylus (S Pen) strokes are no longer taken over by the handwriting keyboard on Android 13+, from @Yundi339's fix in qiin2333/moonlight-vplus#634.
- The decoder requests low latency again right after it starts on Android 11+, from @derflacco.
- "Remember mouse mode" is on by default.
- The app installs side by side with Artemis, Artemis-mic and Moonlight under its own package ID.
- Release APKs are built and signed by GitHub Actions.

### Fixed

- Turning off Back Menu no longer disables the five-finger menu tap.
- Microphone capture falls back to the next audio source on devices that reject the first one instead of failing.
- Backups no longer include Android framework files or settings search history.
