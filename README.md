# Super Artemis

Stream games, apps or your whole desktop from a PC running [Apollo](https://github.com/ClassicOldSong/Apollo) to your Android phone or tablet, with your microphone, your own menu and your own controls.

Super Artemis is a fork of [Artemis](https://github.com/ClassicOldSong/moonlight-android) by ClassicOldSong, itself a fork of [Moonlight](https://github.com/moonlight-stream/moonlight-android). It keeps everything Artemis does and adds the features its community kept asking for, plus proven fixes from upstream Moonlight and other forks, with credit to everyone involved.

[Download the latest release](https://github.com/Arora-Sir/Super-Artemis/releases/latest) · [Get updates with Obtainium](https://apps.obtainium.imranr.dev/redirect?r=obtainium://app/%7B%22id%22%3A%22com.mohitarora.superartemis%22%2C%22url%22%3A%22https%3A%2F%2Fgithub.com%2FArora-Sir%2FSuper-Artemis%22%2C%22author%22%3A%22Arora-Sir%22%2C%22name%22%3A%22Super%20Artemis%22%2C%22additionalSettings%22%3A%22%7B%5C%22apkFilterRegEx%5C%22%3A%5C%22arm64-v8a%5C%22%2C%5C%22versionExtractionRegEx%5C%22%3A%5C%22v%28.%2B%29%5C%22%2C%5C%22matchGroupToUse%5C%22%3A%5C%22%241%5C%22%7D%22%7D) · [Report an issue](https://github.com/Arora-Sir/Super-Artemis/issues/new/choose)

## Why Super Artemis

- **Talk while you play.** Your microphone streams to Apollo, and it stays off (no battery, no privacy indicator) until you unmute it from the menu.
- **Your menu, your way.** Pick and reorder Quick Menu actions, choose list, grid or compact, and see the result live while editing. Disconnect and Quit always sit in their own red section.
- **One tap, always on screen.** Pin actions and custom keys to a small quick-action bar in any screen corner. Toggles show green while on, and the bar fades when idle.
- **Custom keys without JSON.** Record a shortcut on the built-in keyboard, name it and give it an icon.
- **Gestures you choose.** Decide what 3, 4 and 5-finger taps and double taps do, or let them reach the PC.
- **Leave and come back.** Disconnect, resume the same app when you return, or keep it in picture-in-picture, including on a quick app switch.
- **Settings you never lose.** Back up every setting to one file and restore it with a preview that keeps the PCs already on your device.
- **Smoother video.** The frame pacing setting works again, a renderer loop that dropped frames is gone, and native core fixes improve recovery after packet loss.
- **Find anything.** Search all settings, and choose an optional Material You style on Android 12 and later.

## Features

| Area | What you get |
| :--- | :--- |
| Voice | Microphone streaming to Apollo, "Microphone at stream start" (always off, always on, follow last state, remembered per PC), mute and unmute from the menu or the quick-action bar |
| Menus | Sectioned Quick Menu, Advanced Menu with Back to Quick Menu, list, grid and compact styles, editor with live preview, quick-action bar in a corner you pick |
| Input | Custom keys with a keyboard recorder and 28 icons, configurable multi-finger taps and double taps, S Pen strokes reach the PC on Android 13+, Print Screen and Samsung keyboard cover screenshot key |
| Sessions | When you leave the stream: disconnect, resume when reopened (within 30 minutes) or picture-in-picture, optional connect to the last PC on app start, network drop grace period for short Wi-Fi drops |
| Performance | Honoured frame pacing, no frame-dropping drain loop, refresh rate tracking, post-start low latency on Android 11+, decoder setup that cannot hang |
| Backup | One JSON file through the system file picker, settings only by default, pairing keys as an explicit opt-in, restore preview that keeps existing PCs |
| Look | Settings search, optional Material You (wallpaper colors, switches, rounded dialogs) |
| About | Support development, developer profiles and in-app credits with links to every contributor |

Everything from Artemis is still there: custom virtual buttons, custom resolutions and bitrates, multiple mouse modes, virtual gamepad skins, external display mode, Apollo virtual display, server commands and clipboard sync.

## Get started

1. Install [Apollo](https://github.com/ClassicOldSong/Apollo/releases) on your Windows PC and finish its first-time setup in the web UI.
2. Install Super Artemis from [Releases](https://github.com/Arora-Sir/Super-Artemis/releases/latest) (or follow it with Obtainium). It installs as `com.mohitarora.superartemis`, next to Artemis and Moonlight.
3. Tap your PC in the app and enter the PIN in the Apollo web UI.
4. Start streaming. Open the menu with the Back key, a five-finger tap or the floating button.

### Host setup

- **Microphone:** in Apollo's `sunshine.conf` set `stream_mic = enabled` and `mic_backend = steam_streaming_microphone`, then turn on "Enable microphone streaming" in the app.
- **Network drop grace period:** set Apollo's ping timeout at least as long as the grace period chosen in the app, or the host ends the session first.
- **Actions on connect and disconnect:** Apollo's per-client Do and Undo commands run on every session start and stop. SETUP.md has a ready mouse speed example.

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

- Star this repository, it helps other people find it.
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
| Resume when reopened and connect to the last PC on app start | [@webibi](https://github.com/webibi) | Requested | [ClassicOldSong/moonlight-android#159](https://github.com/ClassicOldSong/moonlight-android/issues/159) |
| Opt-in Material You app style | [@xDoge26](https://github.com/xDoge26) | Requested | [ClassicOldSong/moonlight-android#358](https://github.com/ClassicOldSong/moonlight-android/issues/358), [ClassicOldSong/moonlight-android#394](https://github.com/ClassicOldSong/moonlight-android/issues/394) |
| Configurable 3, 4 and 5-finger taps and double taps, with Nothing passing taps to the PC | [@funkholio](https://github.com/funkholio), [@nightshadecf](https://github.com/nightshadecf), [@junoxxx67](https://github.com/junoxxx67) | Reported | [ClassicOldSong/moonlight-android#540](https://github.com/ClassicOldSong/moonlight-android/issues/540), [ClassicOldSong/moonlight-android#504](https://github.com/ClassicOldSong/moonlight-android/issues/504), [ClassicOldSong/moonlight-android#470](https://github.com/ClassicOldSong/moonlight-android/issues/470) |

<!-- credits:end -->

## License

Super Artemis is licensed under the GNU General Public License v3, like Artemis and Moonlight. See LICENSE.txt. The privacy policy is in PRIVACY.md.

Moonlight was created by [Cameron Gutman](https://github.com/cgutman), [Diego Waxemberg](https://github.com/dwaxemberg), [Aaron Neyer](https://github.com/Aaronneyer) and [Andrew Hennessy](https://github.com/yetanothername), and Artemis and Apollo by [ClassicOldSong](https://github.com/ClassicOldSong). Super Artemis is an independent fork and is not affiliated with those projects.
