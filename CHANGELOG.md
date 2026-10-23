# Changelog

All notable changes to Super Artemis are listed here. The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and versions follow [Semantic Versioning](https://semver.org/). Super Artemis is based on Artemis (ClassicOldSong/moonlight-android, branch `moonlight-noir` at `c5cf27f`), which is version 20.2.6 plus the renderer work of the 20.3.0 experimental builds.

## [Unreleased]

Nothing yet.

## [1.0.0]

The first Super Artemis release, not published yet. The date is set when the `v1.0.0` tag is published, and the release notes are in `.github/release-notes/v1.0.0.md`.

### Added

- Microphone streaming to Apollo hosts that negotiate a mic stream (Steam Streaming Microphone on Windows), ported from @logabell's work in ClassicOldSong/moonlight-android#537.
- "Microphone at stream start" setting with Always off (default), Always on and Follow last state, remembered per PC.
- Mute and unmute from the in-stream Quick Menu and Advanced Menu. Muting stops the microphone hardware and the Opus encoder, so the privacy indicator turns off and no battery is spent on audio capture.
- "Network drop grace period" setting (10 to 60 seconds, default 30) so short Wi-Fi drops do not end the session, from @MikalaiKryvusha's KAST.
- Settings search from a bar that stays at the top of Settings, with results inside collapsed sections and everyday words ("three finger", "pip", "upi") that find the right setting.
- Backup and restore of all settings, layouts, profiles and the PC list to one `.sabackup` file, saved on the device or shared to another app or device. The file opens in the app from a file manager or the share sheet.
- Password protected backups (AES-256-GCM, PBKDF2-HMAC-SHA256 with 310,000 iterations). Pairing keys are an explicit opt-in with a clear caution and always need a password.
- Restore shows a preview first and lets you tick which parts to restore (settings, layouts, profiles, PCs, pairing keys). On another device it lists the settings that device cannot use (resolution, frame rate, HDR, motion sensors, vibration, microphone) and keeps its own value unless you tick them. A backup without pairing keys keeps the PCs already on the device and only adds new ones.
- 3, 4 and 5-finger holds as gestures, with the hold time (0.5, 1 or 2 seconds) chosen in the gesture editor.
- Crash reports: after a crash the next launch offers a report, and Settings > More > Share diagnostic log sends the app log at any time, without root, Shizuku or a PC. Both open one list: a new bug report on GitHub with the device filled in, an existing issue, a file in Downloads, any app, or the clipboard.
- Long press on a quick-action bar button opens a menu for that bar: record a new key straight onto the bar, pin an existing special key, edit the bar, or manage special keys.
- Backups stay compatible across versions: a newer backup opens in an older version when its minimum reader version allows it, and unknown values are skipped.
- Customizable in-stream Quick Menu: choose and reorder actions in an editor, with List, Grid with icons and Compact styles picked right in the editor next to a live preview. "Customize menu" sits right in the Quick Menu.
- The Quick Menu is split into Actions, Session and Menu sections. Disconnect and Quit are always in the Session section, drawn in red. The Advanced Menu has "Back to Quick Menu".
- Quick-action bars: pin actions (such as Pan/Zoom or the microphone) and special keys as small buttons over the stream. Each bar sits at one of nine positions (corners, edge centers or the center), can be nudged sideways and up or down, and shows its buttons in lines of 1 to 20, across in rows or down in columns, starting a new line instead of hiding a button. Switched-on toggles show green, and the bars fade while idle. The editor draws every bar to scale on a screen preview. Set them up in Settings > In-stream controls or from the Quick Menu during a stream.
- Quick-action bar sets: keep several named sets of bars, such as one for games and one for the desktop, and switch between them from the Quick Menu. Sets can be added, renamed, duplicated and deleted, and a bar can be copied into another set. Bars from earlier test builds become the Default set.
- Each settings profile keeps its own quick-action bar sets, and the bar editor names the profile it edits. Profiles can be duplicated, or all of one profile's settings copied into another after a confirmation.
- Special keys without JSON: the built-in keys can be hidden, reordered, given another icon or reset, and your own keys are recorded on the built-in keyboard or a physical keyboard with a name, a sticky option and one of 75 icons. Manage them in Settings > In-stream controls > Special keys, or during a stream from Send special keys. Any key can be pinned to a quick-action bar or bound to a gesture, and custom keys keep working with import, export, backup and the on-screen key layer.
- Special keys as step flows: a key runs its steps one after another, such as a shortcut, typed text (any language and emoji, sent as Unicode), a wait, holding and releasing a key, repeating the previous step, or a stream action. A one-step key works exactly like a plain shortcut, and older versions still read the first shortcut of a flow.
- More keys: 24 everyday Windows shortcuts on their own page in Send special keys, each with an icon (select all, copy, cut, undo, redo, save, find, clipboard history, snip, Alt + Tab, Task Manager, tabs, File Explorer, lock, Run, Home, End, Page Up, Page Down, Delete, Tab and Enter). They can be hidden, reordered, pinned to a bar or bound to a gesture like any special key.
- "Add key or flow" in every place that picks a key: the quick-action bar editor and its long-press menu, the gesture editor, the menu editor and Send special keys. The new key is pinned or bound right there and shows up everywhere else too.
- Special keys and flows can be pinned to the Quick Menu next to actions, from the menu editor.
- Settings from a stream: the Quick Menu has Settings next to Customize menu, and it can be pinned to a bar or bound to a gesture. Closing Settings reconnects to the same app, so changed stream settings apply right away.
- Reconnect after the connection ends: the "Connection terminated" dialog has a Reconnect button that starts the same stream again.
- Reconnect after closing picture-in-picture: closing the small stream window with its X offers to reconnect the next time the app opens.
- "Starting video" status between the connection and the first picture, instead of a plain black screen while the PC starts its encoder.
- Print Screen as a built-in More key and a PrtSc key on the built-in keyboard and the key recorder.
- F13 to F24 in the key recorder, for macro and streaming hotkeys that no Android keyboard has (requested in ClassicOldSong/moonlight-android#574).
- Phone hotkeys: a key or flow can have a hotkey such as Ctrl + Alt + 1. During a stream, that combo on a keyboard connected to the phone runs the key instead of reaching the PC, or reaches the PC as well when "Also send the hotkey to the PC" is on. The editor warns about shortcuts the PC uses all the time and refuses a hotkey another key already has. Hotkeys are part of the key file, so backups keep them.
- "Landscape side" setting: follow the sensor, normal, or upside down for a stand or a dock (requested in ClassicOldSong/moonlight-android#492).
- "System key capture" in the keyboard settings shows whether the keyboard service is on and opens the Accessibility page, so Alt+Tab and the Windows key reach the PC (as proposed in ClassicOldSong/moonlight-android#556).
- Hide actions you never use from the Advanced Menu with the eye in the menu editor. Gestures and quick-action bars can still run them, and Disconnect and Quit always stay.
- "Material You" app style, the default on Android 12+, chosen from two live preview cards: switches instead of checkboxes and each settings section in one rounded container, including the in-stream menus. Classic stays available and is kept for anyone who picks it, and Android 11 and older use Classic.
- Color schemes for Material You: Wallpaper or a fixed Blue, Teal, Green, Purple, Orange, Rose or Mono palette, picked from swatches drawn in each scheme's real color. A change applies right away.
- "When you leave the stream" setting: Disconnect (default), Disconnect and resume when reopened (Recents or the app icon reconnects to the same app within 30 minutes, with no battery use while away), or Keep streaming in picture-in-picture (replaces the old PiP switch, which carries over). Picture-in-picture now also starts when quick switching to the previous app.
- "Connect to the last PC on app start" setting (off by default).
- "Release held keys when the keyboard closes" (on by default): hiding the built-in keyboard lets go of every key still held, including Ctrl, Alt, Shift or Win latched with a long press, so no key stays pressed out of sight.
- Multi-finger gestures: choose what 3, 4 and 5-finger taps and double taps and 3 and 4-finger swipes (up, down, left, right) do: any action, a special key, the Quick Menu, a middle mouse click, or Nothing so the touch reaches the PC. Gestures work in every touch mode, including pan and zoom. One tap sets the Windows touchpad swipes (Task View, desktop, switch apps and desktops). Defaults match Artemis, and the old five-finger switch carries over.
- Settings > About: Support development (a UPI card with the QR code, the UPI ID to copy and a UPI app button, and PayPal), developer links with their icons, in-app credits as cards with a chip for every PR, commit and issue, source code, issue reporting, privacy policy and version.
- Credits for everyone whose code, design or request shaped the fork, in the app, the README and the release notes.

### Changed

- Stylus (S Pen) strokes are no longer taken over by the handwriting keyboard on Android 13+, from @Yundi339's fix in qiin2333/moonlight-vplus#634.
- The decoder requests low latency again right after it starts on Android 11+, from @derflacco.
- "Remember mouse mode" is on by default.
- The app installs side by side with Artemis, Artemis-mic and Moonlight under its own package ID.
- Release APKs are built and signed by GitHub Actions.
- A weekly upstream watch reports new commits, releases and security advisories from Artemis, Moonlight, moonlight-common-c, Apollo and the forks the app takes code from, and Dependabot proposes dependency updates.
- The app names itself Super Artemis everywhere, in English and in the French, Russian, Vietnamese and Chinese translations. Store metadata, issue templates and the pull request template describe this fork and its credit rules.
- The native resolution warning explains how Apollo handles resolution instead of warning about custom NVIDIA modes.
- Sharing performance logs with the upstream Artemis dataset and its dashboard link are hidden. Local performance logging stays.
- Every dialog is the same rounded, centered dialog with one type scale, sized for the screen (full width on phones, a readable width on tablets). Material You no longer uses Samsung's bottom-anchored dialogs.
- Settings sections are regrouped and renamed: In-stream controls, Starting and leaving a stream, Appearance, Built-in keyboard and Virtual keys on screen are new, and rarely used items moved to More. Tablets show a section list beside the settings.
- Duplicate settings are gone: the menu style lives in the menu editor, "Disable default special keys" became per-key hiding (the old switch carries over), and the overlay zoom button became a Pan/Zoom pin on the top right quick-action bar (also carried over).
- The Support dialog has no GitHub button any more, since About has one "Source code on GitHub" entry.
- "Enable multi-touch gestures" became one switch on the Multi-finger gestures row in Settings, after a divider: the row opens the gesture editor and the switch turns every multi-finger gesture on or off in every touch mode, multi-touch included. It is on by default. With it off, every touch reaches the touch mode as it would without Super Artemis.
- SETUP.md shows how Apollo's Do and Undo commands make Steam Streaming Microphone the default PC microphone only while a stream runs.
- The quick-action bar editor uses two panes on tablets: the preview and bar settings scroll on the left, and the button list fills the right side down to its Add key or flow button.
- Profiles has a title bar with back, each profile is a rounded card with tinted copy, edit and delete buttons, and its dialogs use the same icon headers as the rest of the app.
- Send special keys and More keys list the keys and the menu entries (More keys, Add key or flow, Manage special keys, Cancel) in separate sections.
- The mouse mode picker shows each mode with an icon and a check on the mode in use.
- Every entry that creates a key is named "Add key or flow", and the editor is titled "New key or flow".
- The automatic name of a flow is its first step plus the number of further steps, such as "Ctrl + C (+2)", and the special keys list shows a flow's step count and first steps.
- The color scheme in use carries a check on its swatch.
- Settings appears once in the in-stream menus, in the Quick Menu's Menu section. It no longer also shows in the Advanced Menu and the menu editor list, and it can still go on a quick-action bar or a gesture.
- The eye in the Special keys list shows each key's state like the menu editor: open while the key is shown, crossed while it is hidden.
- The menu editor action list is taller on phones.
- The Touchscreen Mode summary is rewritten, since it had a typo and old mode names.
- Quick-action bars have one "Buttons per line" slider and a Fill across or down choice instead of separate rows and columns, so a button is never cut off. Bars from earlier versions convert by themselves.
- The profile editor is the Settings screen in profile mode: the same sections, with the section list on tablets, and settings that apply to the whole app stay on screen greyed out with a note instead of vanishing.
- The home screen has a title bar with round tonal buttons, search results show as cards, and the bar, menu and key editors have an icon title.
- On a phone in landscape the menu and bar editors scroll as one page with a smaller preview (dragging the menu editor list moves the header away first), and the in-stream menus use more of the width.
- Rough setting descriptions left from earlier versions are rewritten in plain words.
- Dialog buttons, Quick Menu tiles and the PC and app grids show a rounded Material 3 ripple when pressed or focused, instead of a sharp box.
- Every picker (mouse mode, gesture actions, hold time, app action step, copy profile) is built by one shared choice list, so they all look and behave the same.
- The key recorder on a phone in portrait has its own layout in sections: Function keys with an F1 to F12 and F13 to F24 switch, Typing laid out like a phone keyboard, Symbols, and Navigation with an arrow cluster. Keys in a row share one width and every label one size. Landscape and tablets keep the full PC keyboard, now with even gaps, one label size and a height that fits a phone in landscape.
- Save in the key editor stays off while the recorder holds keys not yet confirmed with Done.

### Fixed
- Text fields in the restore password prompt, the key step values (text, wait, repeat) and set names open the keyboard again when tapped. They took focus only from code, so the keyboard never showed.
- Dialogs with one text field open the keyboard by themselves.
- The key recorder scrolls into view when a key already has several steps.
- A key added after the keys were reordered goes before the More keys instead of after all of them.
- The key editor opened from the bar, menu or gesture editor keeps its chip and segment styles.
- The menu editor preview names pinned keys the way the Quick Menu does, without the "Key:" prefix.
- The physical keyboard accessibility service declared itself a screen reader, so apps such as YouTube read their screens aloud while it was on. It now declares generic feedback, as reported in ClassicOldSong/moonlight-android#609.
- The microphone permission prompt no longer shrinks the stream into picture-in-picture.
- A running key flow stops at once when the stream ends, even in a long wait, and lets go of held keys. Actions from a flow no longer run after the stream closed, and open in-stream menus close with the stream.
- Gestures, bars and flows that wait for the stream to get focus back give up after 2 seconds instead of firing much later.
- PC names on the home screen wrap at a hyphen instead of mid-word.
- A search result keeps its section header in view below the search bar, and the section list on tablets marks the section you are in.
- The "Bottom right" position in the bar editor has the same text size as the other eight on phones.
- One Hand Operation+ "previous app" and other app switches without a Home gesture keep the stream in picture-in-picture, and the Home swipe slides smoothly into the small window.
- Opening Recents with a picture-in-picture stream no longer ends it for good: Android cannot shrink a stream shown as a Recents card, so tapping the card reconnects to the same app.
- The profile editor no longer crashes when Android rebuilds it after a theme change, a rotation or the app being closed in the background.
- Starting a stream while the phone turns from portrait to landscape no longer crashes.
- With "Keep streaming in picture-in-picture", switching apps in any way now keeps the stream in picture-in-picture. Before, an app switch without a Home or Recents gesture, such as One Hand Operation+ opening the previous app, ended the stream. Screen off, the lock screen, split screen and the app's own screens still pause it normally.
- The "resume when reopened" choice is named "Keep in Recents, resume when reopened" and says that the stream's card stays in Recents and reconnects to the same app within 30 minutes.
- Settings search shows every match instead of the first 10, so "mouse" finds Touchscreen Mode, Remember mouse mode and Remote desktop mouse mode. Titles rank first, then the section name, then descriptions, and everyday words find their settings too ("cursor" and "pointer" for mouse settings, "touchpad" for trackpad, "sound" for audio and more).
- Dragging a row to the top or bottom of a list (special keys, menu editor, quick-action bar editor) keeps scrolling at a useful speed instead of crawling, and a fast drag over several rows saves exactly the order shown on screen.
- The mouse mode picker, the quit confirmation, connection errors and the connecting spinner now follow the app style (Material You or Classic) like every other dialog. The mouse mode picker also has a Cancel button.
- A 3, 4 or 5-finger tap no longer also sends a right click to the PC in the mouse and trackpad modes. The touch modes track two fingers, so the gesture now takes the touch at the first lift, before they can read it as a two-finger tap.
- Opening the quick-action bar editor no longer closes the app, and it can be opened from the Quick Menu during a stream.
- Toggles run from a gesture (Pan/Zoom, microphone) now also update their quick-action bar button.
- Crash fixes from an audit of the new features: multi-finger gestures with touch input disabled, changing the app style after the system recreated Settings, colors on Android 5 and 6, credit links on devices without a browser, custom key files with empty entries, dialogs left open after a fold or display change, and backups shared from apps that send something other than a file.
- "Report an issue" opens in the default browser instead of the GitHub app, which cannot show the issue form chooser.
- "Show more" replaces "Advanced" on collapsed settings groups for English in every region.
- Dialogs on phones: the support card stacks its QR above the UPI details, the menu style buttons no longer clip their labels, the recorder keyboard keeps every key label on one line, and dialogs with password fields resize above the keyboard.
- Scrolling dialog content shows a thin divider on the edge that hides more content, instead of cutting the next card in half or dimming the last row.
- Cancel in Send special keys has its icon like every other menu.
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
- Multi-finger gestures work in pan and zoom mode, so the gesture that turned it on can turn it off again.
- The floating menu button no longer needs Back Menu to be on.

### Credits

Everyone credited for this release is listed in the README credits table and in Settings > About > Credits, both generated from `app/src/main/res/raw/credits.json`.
