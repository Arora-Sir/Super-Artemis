# Artemis Android

Previously named Moonlight Noir

An open source client for [Apollo](https://github.com/ClassicOldSong/Apollo)/[Sunshine](https://github.com/LizardByte/Sunshine).

Artemis Android will allow you to stream your collection of games from your Windows PC to your Android device,
whether in your own home or over the internet.

Artemis is currently the best fork of Moonlight with loads of optimizations for office usage.

A more seamless experience with virtual display will be Artemis paired with [Apollo](https://github.com/ClassicOldSong/Apollo).

# Features

If you switch back to the main stream version, you'll be missing the following awesome features which are very unlikely to be added there:

1. Custom virtual buttons with import and export support.
2. [Custom resolutions](https://github.com/moonlight-stream/moonlight-android/pull/1349).
3. Custom bitrates.
4. [Multiple mouse mode switching](https://github.com/moonlight-stream/moonlight-android/pull/1304) (normal mouse, [multi-touch](https://github.com/moonlight-stream/moonlight-android/pull/1364), touchpad, disabled, local cursor mode).
5. Optimized virtual gamepad skins and free joystick.
6. External monitor mode.
7. Joycon D-pad support.
8. Simplified performance information display.
9. [Game back menu](https://github.com/moonlight-stream/moonlight-android/pull/1171).
10. Custom shortcut commands.
11. Easy soft keyboard switching.
12. Portrait mode.
13. Display on top mode, useful for foldable phones.
14. [Virtual touchpad space and sensitivity adjustment](https://github.com/moonlight-stream/moonlight-android/issues/1348#issuecomment-2236344729) for playing right-click view games, such as Warcraft.
15. Force use device's own vibration motor (in case your gamepad's vibration is not effective).
16. Gamepad debugging page to view gamepad vibration and gyroscope information, as well as Android kernel version information.
17. Trackpad tap/scrolling support
18. Natural track pad mode with touch screen
19. Non-QWERTY keyboard layout support
20. Quick Meta key with physical BACK button
21. Frame rate lock fix for some devices
22. Video scale mode: Fit/Fill/Stretch
23. View pan/zoom support
24. Rotate screen in-game
25. Add option to quit app directly
26. Samsung DeX scrolling support
27. Proper click/scroll/right-click for trackpad on generic Android tablet when using local cursor
28. Virtual Display integration with [Apollo](https://github.com/ClassicOldSong/Apollo)
29. Server Command integration with [Apollo](https://github.com/ClassicOldSong/Apollo)
30. Clipboard sync (requires Apollo)
31. SBS 3D for external Displays (Using AI MiDaS v2 Lite)

# Disclaimer

This is the `go away` version of Moonlight Android.

I got kicked from Moonlight and Sunshine's Discord server literally for helping people out.

This is what I got for finding a bug, opened an issue, getting no response, troubleshoot myself, fixed the issue myself, shared it by PR to the main repo hoping my efforts can help someone else during the maintainance gap.

Yes, I'm going away. Fixes and improvements on this fork are not necessarily be merged to the main repo either. I have also started [a fork of Sunshine called Apollo](https://github.com/ClassicOldSong/Apollo) and will add useful features that will never get merged by the main repo shortly. [Apollo](https://github.com/ClassicOldSong/Apollo) and [Moonlight Noir](https://github.com/ClassicOldSong/moonlight-android) will no longer be compatible with OG Sunshine and OG Moonlight eventually, but they'll work even better with much more carefully designed features.

The main repo had stayed silent for 5 months, with nobody actually responding to issues, and people are getting totally no help besides the limited FAQ in their Discord server. I tried to answer issues and questions, solve problems within my ablilty but I got kicked out just for helping others.

**PRs for feature improvements are welcomed here unlike the main repo, your ideas are more likely to be appreciated and your efforts are actually being respected. We welcome people who can and willing to share their efforts, helping yourselves and other people in need.**

**Update**: They have contacted me and apologized for this incident, but the fact it **happened** still motivated me to start my own fork.

## Downloads
* [Download APK directly](https://github.com/ClassicOldSong/moonlight-android/releases)
* [Use Obtainium](https://apps.obtainium.imranr.dev/redirect?r=obtainium://app/%7B%22id%22%3A%22com.limelight.noir%22%2C%22url%22%3A%22https%3A%2F%2Fgithub.com%2FClassicOldSong%2Fmoonlight-android%22%2C%22author%22%3A%22ClassicOldSong%22%2C%22name%22%3A%22Artemis%22%2C%22additionalSettings%22%3A%22%7B%5C%22apkFilterRegEx%5C%22%3A%5C%22nonRoot%5C%22%2C%5C%22matchGroutToUse%5C%22%3A%5C%22%241%5C%22%2C%5C%22versionExtractionRegEx%5C%22%3A%5C%22v(.%2B)%5C%22%7D%22%7D) (recommended)

## Building
* Install Android Studio and the Android NDK
* Run ‘git submodule update --init --recursive’ from within moonlight-android/
* In moonlight-android/, create a file called ‘local.properties’. Add an ‘ndk.dir=’ property to the local.properties file and set it equal to your NDK directory.
* Build the APK using Android Studio or gradle

## Authors

* [Cameron Gutman](https://github.com/cgutman)  
* [Diego Waxemberg](https://github.com/dwaxemberg)  
* [Aaron Neyer](https://github.com/Aaronneyer)  
* [Andrew Hennessy](https://github.com/yetanothername)

Moonlight is the work of students at [Case Western](http://case.edu) and was
started as a project at [MHacks](http://mhacks.org).

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
| Microphone state at stream start (off, on, follow last state) | [@Yundi339](https://github.com/Yundi339) | Design adapted | [qiin2333/moonlight-vplus#639](https://github.com/qiin2333/moonlight-vplus/pull/639) |
| In-stream microphone toggle action | [@ShadowLemoon](https://github.com/ShadowLemoon) | Design adapted | [qiin2333/moonlight-vplus#314](https://github.com/qiin2333/moonlight-vplus/pull/314) |
| Microphone redirection in Moonlight V+ | [@qiin2333](https://github.com/qiin2333) | Prior art | [qiin2333/moonlight-vplus#20](https://github.com/qiin2333/moonlight-vplus/pull/20) |
| Settings search | [@ByteHamster](https://github.com/ByteHamster) | Library | [ByteHamster/SearchPreference](https://github.com/ByteHamster/SearchPreference) |
| Menu and action icons (Material Symbols, Apache License 2.0) | Google | Assets | [google/material-design-icons](https://github.com/google/material-design-icons) |
| Large touch targets for menu tiles | [@Rafael-CN](https://github.com/Rafael-CN) | Idea | [Rafael-CN/VibeArtemis@600a068](https://github.com/Rafael-CN/VibeArtemis/commit/600a068a6cfff40ea6dcf3e50737df3bd652b105) |
| Reorderable Quick Menu and floating buttons | [@Sx1Fl0NdqlkM6N4s2pswXKUI0k9FKTaOckLbNXu](https://github.com/Sx1Fl0NdqlkM6N4s2pswXKUI0k9FKTaOckLbNXu) | Requested | [ClassicOldSong/moonlight-android#241](https://github.com/ClassicOldSong/moonlight-android/issues/241) |
| Quick Menu modes | [@r0l1ka](https://github.com/r0l1ka) | Requested | [ClassicOldSong/moonlight-android#146](https://github.com/ClassicOldSong/moonlight-android/issues/146) |
| Microphone support | [@jackson00w](https://github.com/jackson00w), [@Moby812](https://github.com/Moby812) | Requested | [ClassicOldSong/moonlight-android#582](https://github.com/ClassicOldSong/moonlight-android/issues/582), [ClassicOldSong/moonlight-android#403](https://github.com/ClassicOldSong/moonlight-android/issues/403) |
| Custom keys recorded in the app, with names and icons | [@Senjar](https://github.com/Senjar), [@Sx1Fl0NdqlkM6N4s2pswXKUI0k9FKTaOckLbNXu](https://github.com/Sx1Fl0NdqlkM6N4s2pswXKUI0k9FKTaOckLbNXu), [@loudcricketts](https://github.com/loudcricketts) | Requested | [ClassicOldSong/moonlight-android#348](https://github.com/ClassicOldSong/moonlight-android/issues/348), [ClassicOldSong/moonlight-android#243](https://github.com/ClassicOldSong/moonlight-android/issues/243), [ClassicOldSong/moonlight-android#239](https://github.com/ClassicOldSong/moonlight-android/issues/239) |
| Duplicate keys as separate custom keys | [@voguelike](https://github.com/voguelike) | Requested | [ClassicOldSong/moonlight-android#521](https://github.com/ClassicOldSong/moonlight-android/issues/521) |

<!-- credits:end -->
