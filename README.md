# Super Artemis

**Stream games, apps or your whole desktop from your PC to your Android phone or tablet, with your voice, your own menu and your own controls.**

[![Latest release](https://img.shields.io/github/v/release/Arora-Sir/Super-Artemis?label=release)](https://github.com/Arora-Sir/Super-Artemis/releases/latest)
[![License: GPL v3](https://img.shields.io/badge/license-GPLv3-blue)](LICENSE.txt)
![Android 5.0+](https://img.shields.io/badge/Android-5.0%2B-3ddc84)
![Host: Apollo](https://img.shields.io/badge/host-Apollo-8a2be2)

Super Artemis is a fork of [Artemis](https://github.com/ClassicOldSong/moonlight-android) by ClassicOldSong, which is a fork of [Moonlight](https://github.com/moonlight-stream/moonlight-android). It keeps everything Artemis does, adds the features its community kept asking for, and brings in proven fixes from Moonlight and other forks, with credit to everyone involved.

**[Download](https://github.com/Arora-Sir/Super-Artemis/releases/latest)** · **[Get updates with Obtainium](https://apps.obtainium.imranr.dev/redirect?r=obtainium://app/%7B%22id%22%3A%22com.mohitarora.superartemis%22%2C%22url%22%3A%22https%3A%2F%2Fgithub.com%2FArora-Sir%2FSuper-Artemis%22%2C%22author%22%3A%22Arora-Sir%22%2C%22name%22%3A%22Super%20Artemis%22%2C%22additionalSettings%22%3A%22%7B%5C%22apkFilterRegEx%5C%22%3A%5C%22arm64-v8a%5C%22%2C%5C%22versionExtractionRegEx%5C%22%3A%5C%22v%28.%2B%29%5C%22%2C%5C%22matchGroupToUse%5C%22%3A%5C%22%241%5C%22%7D%22%7D)** · **[Report an issue](https://github.com/Arora-Sir/Super-Artemis/issues/new/choose)**

## What you would miss going back

Artemis already improves a lot on Moonlight. These are the things you lose if you switch from Super Artemis back to Artemis or Moonlight.

| Feature | Moonlight | Artemis | Super Artemis |
| :--- | :---: | :---: | :---: |
| Microphone streaming to the PC, off until you unmute it | No | No | Yes |
| Microphone state at stream start (off, on or last state, per PC) | No | No | Yes |
| Quick Menu you reorder, in list, grid or compact style with a live preview | No | Fixed menu | Yes |
| Quick-action bars anywhere on screen, as grids, in switchable sets | No | No | Yes |
| Special keys you hide, reorder and give icons | No | No | Yes |
| Record your own key shortcuts on the built-in keyboard, with 75 icons | No | JSON file only | Yes |
| Special keys as step flows: shortcuts, typed text in any language, waits, holds and repeats | No | No | Yes |
| 24 everyday Windows shortcuts ready in More keys | No | No | Yes |
| Choose what 3, 4 and 5-finger taps, double taps, holds and swipes do | No | Fixed taps | Yes |
| Gestures that also work in pan and zoom and the other touch modes | No | No | Yes |
| Windows touchpad swipes (task view, desktops, app switch) in one tap | No | No | Yes |
| Held keys released when the built-in keyboard closes | No | No | Yes |
| When you leave: disconnect, keep in Recents and resume, or picture-in-picture on any app switch | Partial | Partial | Yes |
| Connect to the last PC when the app starts | No | No | Yes |
| Network drop grace period for short Wi-Fi drops | No | No | Yes |
| Frame pacing setting honoured, frame-dropping drain loop removed | Yes | No | Yes |
| Encrypted backup file you save or share, with selective restore | No | No | Yes |
| Restore on another device with warnings for what it cannot use | No | No | Yes |
| Crash report and diagnostic log you can share without root, Shizuku or a PC | No | No | Yes |
| Settings search that stays on top and understands everyday words | No | No | Yes |
| Rounded centered dialogs and Material You with color schemes | No | No | Yes |
| Installs next to Artemis and Moonlight | Yes | Yes | Yes |

Everything else from Artemis stays: custom virtual buttons, custom resolutions and bitrates, mouse modes, gamepad skins, external display mode, Apollo virtual display, server commands and clipboard sync.

## Features

### Voice

| Feature | What it does |
| :--- | :--- |
| Microphone streaming | Sends your microphone to Apollo, so you can talk in games and calls on the PC. |
| Microphone at stream start | Always off, always on, or the state you left it in, remembered per PC. Off keeps the microphone hardware and the privacy indicator off. |
| Mute and unmute anywhere | From the Quick Menu, a quick-action bar or a gesture. |
| Input device and preview | Pick which microphone to use and check its level before you stream. |

### Menus and quick-action bars

| Feature | What it does |
| :--- | :--- |
| Quick Menu editor | Choose the actions and special keys, drag to reorder, hide actions you never use from the Advanced Menu, and pick list, grid or compact with a live preview. |
| Sections | Actions and session controls are separate, and Disconnect and Quit sit in their own red section. |
| Quick-action bars | Any number of bars, each at one of nine positions with a nudge, as a grid of 1 to 20 rows and columns. Bars live in named sets (game, desktop and so on) that you switch from the Quick Menu, and each settings profile keeps its own sets. Toggles show when they are on and the bars fade when idle. Edit them from the Quick Menu during a stream. |
| Floating menu button | Opens the Quick Menu even with the Back menu turned off. |
| Settings from a stream | Settings in the Quick Menu (or on a bar or gesture) opens Settings and reconnects to the same app when you close it, with the new settings. |
| Profile copies | Duplicate a settings profile, or copy all of its settings into another profile, from the copy button in Profiles. |

### Keys and keyboard

| Feature | What it does |
| :--- | :--- |
| Special keys | Hide, reorder and give icons to the built-in keys, or reset them. |
| Recorded keys | Tap a combination on the built-in keyboard or a physical one, name it and pick one of 75 icons. Sticky keys stay pressed until tapped again. |
| Keys everywhere | Recorded keys work in Send special keys, quick-action bars and gestures. |
| Phone hotkeys | Give a key or flow a hotkey such as Ctrl + Alt + 1. During a stream that combo on a keyboard connected to the phone runs it instead of reaching the PC, with an option to send it to the PC as well. |
| F13 to F24 | The recorder offers F13 to F24, free keys for PC hotkeys in AHK, OBS or Discord. |
| Release on close | Keys held on the built-in keyboard are released when it closes. |
| Keyboard fixes | Print Screen, the Samsung keyboard screenshot key and vendor keyboards with unusual key codes. |

### Gestures

| Feature | What it does |
| :--- | :--- |
| Taps and double taps | Choose what 3, 4 and 5-finger taps and double taps do, or set Nothing so the touch reaches the PC. A gesture tap never also clicks on the PC. |
| Holds | Hold 3, 4 or 5 fingers still for 0.5, 1 or 2 seconds to run another action. |
| Swipes | Up, down, left and right with 3 and 4 fingers, mapped to an action or a key combination. |
| Windows preset | One tap maps the swipes to the Windows touchpad gestures. |
| Every touch mode | Gestures work in multi-touch, trackpad, mouse and pan and zoom, so a 3-finger double tap can always leave pan and zoom. |

### Sessions and video

| Feature | What it does |
| :--- | :--- |
| When you leave the stream | Disconnect, resume the same app when you come back within 30 minutes, or keep it playing in picture-in-picture. |
| Connect on app start | Opening the app from the launcher starts your last stream. |
| Network drop grace period | Keeps the session alive through short Wi-Fi drops. |
| Smoother video | Frame pacing works again, the frame-dropping drain loop is gone, the refresh rate is tracked, and native core fixes recover faster from packet loss. |

### Backup and restore

| Feature | What it does |
| :--- | :--- |
| One backup file | Every setting, layout, profile and PC in a `.sabackup` file that opens in the app from a file manager or the share sheet. |
| Save or share | Save it on the device or share it straight to another app or device. |
| Password protection | AES-256-GCM encryption with your password, required when the file includes pairing keys. |
| Pairing keys (opt-in) | Restore on a new device and connect to your PCs without pairing again. |
| Choose what to restore | Tick settings, layouts, profiles, PCs and pairing keys separately. |
| Device checks | Warns about settings the new device cannot use (resolution, frame rate, HDR, motion sensors, vibration, microphone) and keeps its own value unless you tick them. |
| Old and new versions | Newer backups open in older versions when they can, and unknown values are skipped instead of failing the restore. |

### Look and settings

| Feature | What it does |
| :--- | :--- |
| Settings search | A search bar that stays on top, finds settings by everyday words and jumps straight to the result. |
| Clear sections | Settings grouped by what you want to do, with a section list on tablets. |
| Modern dialogs | Rounded, centered dialogs with switches, rounded text fields and layouts that fit phones and tablets. |
| Material You | The default style on Android 12 and later, in your wallpaper colors or a color scheme (Blue, Teal, Green, Purple, Orange, Rose, Mono), chosen from live previews. Classic stays one tap away. Older Android versions use Classic. |
| About | Support development (UPI QR and PayPal), developer links and in-app credits for every contributor. |

## Get started

1. Install [Apollo](https://github.com/ClassicOldSong/Apollo/releases) on your Windows PC and finish its first-time setup in the web UI.
2. Install Super Artemis from [Releases](https://github.com/Arora-Sir/Super-Artemis/releases/latest), or follow it with Obtainium. It installs as `com.mohitarora.superartemis`, next to Artemis and Moonlight.
3. Tap your PC in the app and enter the PIN in the Apollo web UI.
4. Start streaming. Open the menu with the Back key, a five-finger tap or the floating button.

### Host setup

| Goal | What to set |
| :--- | :--- |
| Microphone | In Apollo's `sunshine.conf` set `stream_mic = enabled` and `mic_backend = steam_streaming_microphone`, then turn on "Enable microphone streaming" in the app. |
| Network drop grace period | Set Apollo's ping timeout at least as long as the grace period chosen in the app, or the host ends the session first. |
| Actions on connect and disconnect | Apollo's per-client Do and Undo commands run on every session start and stop. SETUP.md has examples for mouse speed and for making Steam Streaming Microphone the default PC microphone only while you stream. |

## If something goes wrong

| Situation | What to do |
| :--- | :--- |
| The app closed unexpectedly | The next launch offers the saved report. Tap Share report and attach it to a [new issue](https://github.com/Arora-Sir/Super-Artemis/issues/new/choose). |
| Something misbehaves without a crash | Settings > More > Share diagnostic log sends the app log, the last crash and the last native crash or freeze (Android 11 and later). |
| The app does not even start | Turn on USB debugging, connect the device to a PC with [platform tools](https://developer.android.com/tools/releases/platform-tools) and run the commands below, then attach `super-artemis-log.txt` to the issue. |

```
adb logcat -c
adb shell monkey -p com.mohitarora.superartemis -c android.intent.category.LAUNCHER 1
adb logcat -d -b main,crash -v threadtime > super-artemis-log.txt
```

## Building

You need JDK 17 or newer, the Android SDK (platform 36) and NDK `27.0.12077973`.

```
git clone --recurse-submodules https://github.com/Arora-Sir/Super-Artemis.git
cd Super-Artemis
./gradlew assembleNonRoot_gameDebug
```

The APK lands in `app/build/outputs/apk/nonRoot_game/debug/`. SETUP.md covers devices, pairing, tests, release signing and the doc checks. ARCHITECTURE.md explains how the app is built, CONTEXT.md shows the current state and UPSTREAM.md how the fork stays current with upstream.

## Support

Super Artemis is free, open source and has no ads. If it makes your streaming better:

- Star this repository. It helps other people find it.
- Support development in the app (Settings > About > Support development), with [PayPal](https://paypal.me/arorasir) or by UPI (`mohit1998arora@yescred`).

## Credits and thanks

Super Artemis stands on the work of these people and projects. The table is generated from `app/src/main/res/raw/credits.json`, which also feeds Settings > About > Credits in the app.

<!-- credits:start (generated by scripts/render_credits.py, edit credits.json instead) -->

| Feature | Credit | Contribution | Links |
| :--- | :--- | :--- | :--- |
| Artemis, the app Super Artemis is built on, and the Apollo host | [@ClassicOldSong](https://github.com/ClassicOldSong) | Base project | [ClassicOldSong/moonlight-android](https://github.com/ClassicOldSong/moonlight-android), [ClassicOldSong/Apollo](https://github.com/ClassicOldSong/Apollo) |
| Moonlight, the GameStream client and core library Artemis is built on | [@cgutman](https://github.com/cgutman) | Base project | [moonlight-stream/moonlight-android](https://github.com/moonlight-stream/moonlight-android), [moonlight-stream/moonlight-common-c](https://github.com/moonlight-stream/moonlight-common-c) |
| Microphone streaming to the host (client, native core and Apollo host side) | [@logabell](https://github.com/logabell) | Code ported | [ClassicOldSong/moonlight-android#537](https://github.com/ClassicOldSong/moonlight-android/pull/537), [ClassicOldSong/moonlight-common-c#1](https://github.com/ClassicOldSong/moonlight-common-c/pull/1), [ClassicOldSong/Apollo#1428](https://github.com/ClassicOldSong/Apollo/pull/1428) |
| Stylus (S Pen) isolation from the handwriting keyboard | [@Yundi339](https://github.com/Yundi339) | Code ported | [qiin2333/moonlight-vplus#634](https://github.com/qiin2333/moonlight-vplus/pull/634) |
| Low-latency decoder parameter after decoder start | [@derflacco](https://github.com/derflacco) | Code ported | [derflacco/moonlight-android@d8f6f62](https://github.com/derflacco/moonlight-android/commit/d8f6f620b1f7569ff1f80b2514eafda1e28d6858) |
| Network drop grace period (configurable control stream timeout) | [@MikalaiKryvusha](https://github.com/MikalaiKryvusha) | Code ported | [MikalaiKryvusha/moonlight-common-c@25ffe75](https://github.com/MikalaiKryvusha/moonlight-common-c/commit/25ffe75ad655d6404bf326ab9e30009161aa1fa1), [MikalaiKryvusha/KAST@b277878](https://github.com/MikalaiKryvusha/KAST/commit/b277878cd1ae6cfb48bd96538951dfa194c3314f) |
| Frame pacing setting honoured again, no frame-dropping drain loop, refresh rate tracked during the stream | [@coroiu](https://github.com/coroiu) | Code ported | [coroiu/twilight-android@a3d872503](https://github.com/coroiu/twilight-android/commit/a3d872503534f731c85331fa6e72e249773d2eb7), [coroiu/twilight-android@e34cf77c5](https://github.com/coroiu/twilight-android/commit/e34cf77c5ea2a925e1cbc3bf6c1be26966f36410), [coroiu/twilight-android@98298aa6e](https://github.com/coroiu/twilight-android/commit/98298aa6ec56645f9096ebf85b9d599e20c25b49), [ClassicOldSong/moonlight-android#416](https://github.com/ClassicOldSong/moonlight-android/issues/416), [ClassicOldSong/moonlight-android#415](https://github.com/ClassicOldSong/moonlight-android/issues/415), [ClassicOldSong/moonlight-android#376](https://github.com/ClassicOldSong/moonlight-android/issues/376) |
| Surface frame rate and fixed size hack removed | [@derflacco](https://github.com/derflacco) | Code ported | [derflacco/moonlight-android@e3eaf4939](https://github.com/derflacco/moonlight-android/commit/e3eaf49395f3ab7db63d2a1ebd2272e6d9c0c9e2), [derflacco/moonlight-android@5c0d3981a](https://github.com/derflacco/moonlight-android/commit/5c0d3981aee27d29b7a8334aa4727a50ded432a8), [ClassicOldSong/moonlight-android#443](https://github.com/ClassicOldSong/moonlight-android/issues/443) |
| Decoder setup hang guard, audio effects setting, private key kept out of Android backups, poster provider path checks | [@Rafael-CN](https://github.com/Rafael-CN) | Code ported | [Rafael-CN/VibeArtemis@a502acb23](https://github.com/Rafael-CN/VibeArtemis/commit/a502acb23982841e176d4ddffcbdab852aff78db) |
| Joy-Con fix setting applies again | [@CoolRobloxDeveloper](https://github.com/CoolRobloxDeveloper) | Code ported | [ClassicOldSong/moonlight-android#570](https://github.com/ClassicOldSong/moonlight-android/pull/570), [ClassicOldSong/moonlight-android#450](https://github.com/ClassicOldSong/moonlight-android/issues/450) |
| 4K streams keep the current display mode | [@Mad-Melon](https://github.com/Mad-Melon) | Code ported | [ClassicOldSong/moonlight-android#601](https://github.com/ClassicOldSong/moonlight-android/pull/601), [ClassicOldSong/moonlight-android#505](https://github.com/ClassicOldSong/moonlight-android/issues/505) |
| Print Screen key and Samsung keyboard cover screenshot key | [@Lian-CrescentMoon](https://github.com/Lian-CrescentMoon) | Code ported | [ClassicOldSong/moonlight-android#606](https://github.com/ClassicOldSong/moonlight-android/pull/606) |
| No crash with vendor keyboards that report out-of-range key codes | [@qiin2333](https://github.com/qiin2333) | Code ported | [qiin2333/moonlight-vplus#591](https://github.com/qiin2333/moonlight-vplus/pull/591) |
| Adding a PC from an art:// pairing link works on slow networks | [@Swanand01](https://github.com/Swanand01) | Code ported | [Swanand01/moonlight-android@350a7461c](https://github.com/Swanand01/moonlight-android/commit/350a7461c3120f7b74b454c3fa3e5ac1be9005ba) |
| Controller LED, rumble and keyboard fixes from Moonlight 12.2 | [@cgutman](https://github.com/cgutman), [@T2Fat2Fly](https://github.com/T2Fat2Fly), [@bladeoner](https://github.com/bladeoner), [@ReenigneArcher](https://github.com/ReenigneArcher) | Code ported | [moonlight-stream/moonlight-android@abde6021e](https://github.com/moonlight-stream/moonlight-android/commit/abde6021e83e4689fcbd3872afb86998aff16f2f), [moonlight-stream/moonlight-android#1454](https://github.com/moonlight-stream/moonlight-android/pull/1454), [moonlight-stream/moonlight-android@280454fdb](https://github.com/moonlight-stream/moonlight-android/commit/280454fdb018bbeff3dbac4b54dccef524a11f92), [moonlight-stream/moonlight-android#1582](https://github.com/moonlight-stream/moonlight-android/pull/1582), [moonlight-stream/moonlight-android@8974dcda6](https://github.com/moonlight-stream/moonlight-android/commit/8974dcda6e448a0abb9281af4349aa4f992f6743) |
| Video loss recovery, RTT and leak fixes and RTSP hardening in moonlight-common-c | [@cgutman](https://github.com/cgutman), [@foxirain](https://github.com/foxirain), [@JustinZeus](https://github.com/JustinZeus) | Code ported | [ClassicOldSong/moonlight-common-c#2](https://github.com/ClassicOldSong/moonlight-common-c/pull/2), [moonlight-stream/moonlight-common-c@d85371cd8](https://github.com/moonlight-stream/moonlight-common-c/commit/d85371cd8cb6304e66ef90a02e2e1609a8621906), [moonlight-stream/moonlight-common-c@20c05eda6](https://github.com/moonlight-stream/moonlight-common-c/commit/20c05eda6a0d410d84fe97a3a79d04fb70609d48), [moonlight-stream/moonlight-common-c@7b026e77b](https://github.com/moonlight-stream/moonlight-common-c/commit/7b026e77be62175104640e7e722b758df6d3d0d7) |
| Microphone state at stream start (off, on, follow last state) | [@Yundi339](https://github.com/Yundi339) | Design adapted | [qiin2333/moonlight-vplus#639](https://github.com/qiin2333/moonlight-vplus/pull/639) |
| In-stream microphone toggle action | [@ShadowLemoon](https://github.com/ShadowLemoon) | Design adapted | [qiin2333/moonlight-vplus#314](https://github.com/qiin2333/moonlight-vplus/pull/314) |
| Windows touchpad swipes on 3 and 4 fingers (Task View, desktop, app and desktop switching) | [@Lian-CrescentMoon](https://github.com/Lian-CrescentMoon) | Prior art | [ClassicOldSong/moonlight-android#608](https://github.com/ClassicOldSong/moonlight-android/pull/608) |
| Microphone redirection in Moonlight V+ | [@qiin2333](https://github.com/qiin2333) | Prior art | [qiin2333/moonlight-vplus#20](https://github.com/qiin2333/moonlight-vplus/pull/20) |
| Settings search | [@ByteHamster](https://github.com/ByteHamster) | Library | [ByteHamster/SearchPreference](https://github.com/ByteHamster/SearchPreference) |
| Menu and action icons (Material Symbols, Apache License 2.0) | Google | Assets | [google/material-design-icons](https://github.com/google/material-design-icons) |
| Brand icons for the developer links, from Primer Octicons (MIT), Bootstrap Icons (MIT) and Simple Icons (CC0) | Google | Assets | [primer/octicons](https://github.com/primer/octicons), [twbs/icons](https://github.com/twbs/icons), [simple-icons/simple-icons](https://github.com/simple-icons/simple-icons) |
| Large touch targets for menu tiles | [@Rafael-CN](https://github.com/Rafael-CN) | Idea | [Rafael-CN/VibeArtemis@600a068](https://github.com/Rafael-CN/VibeArtemis/commit/600a068a6cfff40ea6dcf3e50737df3bd652b105) |
| Middle mouse click as a gesture action | [@wefcdse](https://github.com/wefcdse) | Idea | [ClassicOldSong/moonlight-android#409](https://github.com/ClassicOldSong/moonlight-android/pull/409), [ClassicOldSong/moonlight-android@16d218341](https://github.com/ClassicOldSong/moonlight-android/commit/16d218341663c5ce1e61e742a96e695ba39db523) |
| Reorderable Quick Menu and floating buttons | [@Sx1Fl0NdqlkM6N4s2pswXKUI0k9FKTaOckLbNXu](https://github.com/Sx1Fl0NdqlkM6N4s2pswXKUI0k9FKTaOckLbNXu) | Requested | [ClassicOldSong/moonlight-android#241](https://github.com/ClassicOldSong/moonlight-android/issues/241) |
| Quick Menu modes | [@r0l1ka](https://github.com/r0l1ka) | Requested | [ClassicOldSong/moonlight-android#146](https://github.com/ClassicOldSong/moonlight-android/issues/146) |
| Microphone support | [@jackson00w](https://github.com/jackson00w), [@Moby812](https://github.com/Moby812) | Requested | [ClassicOldSong/moonlight-android#582](https://github.com/ClassicOldSong/moonlight-android/issues/582), [ClassicOldSong/moonlight-android#403](https://github.com/ClassicOldSong/moonlight-android/issues/403) |
| Custom keys recorded in the app, with names and icons | [@Senjar](https://github.com/Senjar), [@Sx1Fl0NdqlkM6N4s2pswXKUI0k9FKTaOckLbNXu](https://github.com/Sx1Fl0NdqlkM6N4s2pswXKUI0k9FKTaOckLbNXu), [@loudcricketts](https://github.com/loudcricketts) | Requested | [ClassicOldSong/moonlight-android#348](https://github.com/ClassicOldSong/moonlight-android/issues/348), [ClassicOldSong/moonlight-android#243](https://github.com/ClassicOldSong/moonlight-android/issues/243), [ClassicOldSong/moonlight-android#239](https://github.com/ClassicOldSong/moonlight-android/issues/239) |
| Duplicate keys as separate custom keys | [@voguelike](https://github.com/voguelike) | Requested | [ClassicOldSong/moonlight-android#521](https://github.com/ClassicOldSong/moonlight-android/issues/521) |
| On-screen control layouts carried in the backup file and restored on any device | [@bonelag](https://github.com/bonelag) | Requested | [ClassicOldSong/moonlight-android#238](https://github.com/ClassicOldSong/moonlight-android/issues/238) |
| Resume when reopened and connect to the last PC on app start | [@webibi](https://github.com/webibi) | Requested | [ClassicOldSong/moonlight-android#159](https://github.com/ClassicOldSong/moonlight-android/issues/159) |
| Material You app style with color schemes | [@xDoge26](https://github.com/xDoge26) | Requested | [ClassicOldSong/moonlight-android#358](https://github.com/ClassicOldSong/moonlight-android/issues/358), [ClassicOldSong/moonlight-android#394](https://github.com/ClassicOldSong/moonlight-android/issues/394) |
| Configurable 3, 4 and 5-finger taps and double taps, with Nothing passing taps to the PC | [@funkholio](https://github.com/funkholio), [@junoxxx67](https://github.com/junoxxx67) | Reported | [ClassicOldSong/moonlight-android#540](https://github.com/ClassicOldSong/moonlight-android/issues/540), [ClassicOldSong/moonlight-android#470](https://github.com/ClassicOldSong/moonlight-android/issues/470) |

<!-- credits:end -->

## License

Super Artemis is licensed under the GNU General Public License v3, like Artemis and Moonlight. See LICENSE.txt. The privacy policy is in PRIVACY.md.

Moonlight was created by [Cameron Gutman](https://github.com/cgutman), [Diego Waxemberg](https://github.com/dwaxemberg), [Aaron Neyer](https://github.com/Aaronneyer) and [Andrew Hennessy](https://github.com/yetanothername), and Artemis and Apollo by [ClassicOldSong](https://github.com/ClassicOldSong). Super Artemis is an independent fork and is not affiliated with those projects.
