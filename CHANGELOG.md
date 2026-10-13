# Changelog

All notable changes to Super Artemis are listed here. The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and versions follow [Semantic Versioning](https://semver.org/). Super Artemis is based on Artemis (ClassicOldSong/moonlight-android, branch `moonlight-noir` at `c5cf27f`), which is version 20.2.6 plus the renderer work of the 20.3.0 experimental builds.

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
- Quick-action bar: pin actions (such as Pan/Zoom or the microphone) and custom keys as small buttons over the stream, in the top left, top right, bottom left or bottom right corner. Switched-on toggles show green, and the bar fades while idle. Set it up in Settings > UI or from the Advanced Menu during a stream.
- Custom keys without JSON: record a key combination on the built-in keyboard or a physical keyboard, name it, mark it sticky and pick one of 28 icons. Manage keys in Settings > Special Key Layout > Custom keys, or add them during a stream from Send special keys. They keep working with import, export, backup and the on-screen key layer.
- Opt-in "Material You" app style on Android 12+: wallpaper-based colors, switches instead of checkboxes in Settings, and rounded dialogs, including the in-stream menus. Classic stays the default.
- "When you leave the stream" setting: Disconnect (default), Disconnect and resume when reopened (Recents or the app icon reconnects to the same app within 30 minutes, with no battery use while away), or Keep streaming in picture-in-picture (replaces the old PiP switch, which carries over). Picture-in-picture now also starts when quick switching to the previous app.
- "Connect to the last PC on app start" setting (off by default).
- Multi-finger gestures: choose what 3, 4 and 5-finger taps and double taps do (any action, a custom key, the Quick Menu, a middle mouse click, or Nothing so the tap reaches the PC). Defaults match Artemis, and the old five-finger switch carries over.
- Settings > About: Support development (UPI app, UPI QR code, PayPal, a GitHub star), developer profiles, in-app credits with links to every person and source, source code, issue reporting, privacy policy and version.
- Credits for everyone whose code, design or request shaped the fork, in the app, the README and the release notes.

### Changed

- Stylus (S Pen) strokes are no longer taken over by the handwriting keyboard on Android 13+, from @Yundi339's fix in qiin2333/moonlight-vplus#634.
- The decoder requests low latency again right after it starts on Android 11+, from @derflacco.
- "Remember mouse mode" is on by default.
- The app installs side by side with Artemis, Artemis-mic and Moonlight under its own package ID.
- Release APKs are built and signed by GitHub Actions.
- The app names itself Super Artemis everywhere, in English and in the French, Russian, Vietnamese and Chinese translations. Store metadata, issue templates and the pull request template describe this fork and its credit rules.
- The native resolution warning explains how Apollo handles resolution instead of warning about custom NVIDIA modes.
- Sharing performance logs with the upstream Artemis dataset and its dashboard link are hidden. Local performance logging stays.

### Fixed

- Turning off Back Menu no longer disables the five-finger menu tap.
- A three-finger tap in trackpad mode no longer sends a middle click and toggles the keyboard at the same time. Middle click is now a gesture action you can choose.
- Microphone capture falls back to the next audio source on devices that reject the first one instead of failing.
- Backups no longer include Android framework files or settings search history.
- The frame pacing setting is honoured again instead of always being forced to Balanced, a renderer loop that dropped about half the frames after any hiccup is gone, and the drop thresholds follow refresh rate changes during a stream, from @coroiu. The broken "Warp Drive" pacing options, which asked the host for double or quadruple the frame rate, are removed. This addresses the Samsung render rate reports in ClassicOldSong/moonlight-android#416, #415 and #376.
- The forced surface frame rate and fixed surface size, which caused stutter, are removed, from @derflacco.
- Decoder setup can no longer loop forever on NVIDIA and MediaTek decoders, the audio effects setting is no longer tied to "Play audio on PC", Android cloud and device transfer backups no longer copy the pairing private key, and the exported poster provider only serves files from its own cache, from @Rafael-CN.
- The Joy-Con fix setting applies again (@CoolRobloxDeveloper, ClassicOldSong/moonlight-android#570), 4K streams no longer switch display modes needlessly (@Mad-Melon, #601), Print Screen sends the right key and the Samsung keyboard cover screenshot key reaches the PC (@Lian-CrescentMoon, #606), and vendor keyboards with out-of-range key codes no longer crash stream start (@qiin2333, qiin2333/moonlight-vplus#591).
- Adding a PC from an `art://` pairing link no longer fails on slow networks, from @Swanand01.
- Controller LED crashes, rumble on newer Android versions, Xbox Series controller IDs, Shift for the plus key and Android 15 and 16 key codes, from Moonlight 12.2 (@cgutman, @T2Fat2Fly, @bladeoner, @ReenigneArcher).
- Native core: recovery after multi-block video loss and partially dropped IDR frames, no speculative loss reports with RFI off, RTT queries without the ENet lock, thread and pthread attribute leaks, and RTSP hardening against malformed or oversized responses (@cgutman, @foxirain, picked as in ClassicOldSong/moonlight-common-c#2 by @JustinZeus).
- The local mouse cursor setting lists the Ctrl+Alt+Shift shortcuts.
