# Changelog

All notable changes to Super Artemis are listed here. The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and versions follow [Semantic Versioning](https://semver.org/). Super Artemis is based on Artemis (ClassicOldSong/moonlight-android, branch `moonlight-noir` at `c5cf27f`), which is version 20.2.6 plus the renderer work of the 20.3.0 experimental builds.

## [Unreleased]

### Added

- A profile can be marked as the start for new profiles (Copy profile menu, Use as the start for new profiles). A new profile then starts from the current settings with that profile's options laid over them, and existing profiles never change. The marked profile shows a star after its name, and deleting it clears the mark (issue 5). The Copy profile menu is the app's own choice list now, so it has its own surface in Pure black instead of merging with the screen.

- The Custom Resolution setting opens a structured dialog with five aspect ratio buttons (Native, 16:9, 16:10, 21:9, 4:3), numeric width and height fields with minus and plus stepping buttons, a labelled resolution stops bar, and real-time decoder capability validation across 22 supported locales.
- The slider bar in settings dialogs features 16 dp side insets so the thumb aligns with the dialog text indent and action button edges at minimum and maximum values.
- Every slider dialog in Settings has a minus button, a number field and a plus button above the slider. Long pressing either button accelerates value stepping with haptic feedback.
- The video bitrate and metered bitrate sliders use a calibrated power curve that places the 35 to 40 Mbps streaming sweet spot at the 50 percent center mark. Stepping supports 0.5 Mbps decimal precision below 50 Mbps.
- The video resolution picker organizes display modes into three distinct sections: Standard Resolutions (360p through 4K with 16:9 subtitles), Native Display (Full-Screen and Cutout safe area with aspect ratio subtitles), and Custom Resolution at the bottom with clean section dividers.
- Samsung Book Cover Keyboard touchpads are captured on demand when uncaptured finger input occurs on mouse-class devices, restoring two-finger scrolling and preventing tablet system gestures from interrupting the stream session.
- Configurable controller rumble rate limiting with a 0 to 100 ms minimum interval prevents Android 14 system server crashes during dense vibration bursts on Bluetooth gamepads and DualSense controllers. Stop commands dispatch immediately. Trigger rumble honors the Enable Rumble toggle. Native translations are provided across all 11 supported locales.
- The Amlogic Codec2 HEVC decoder (`c2.amlogic.hevc.decoder`) skips `KEY_LOW_LATENCY` and falls through to vendor low-latency controls, resolving the slideshow rendering bug on S905X5, S905X5M, and S905Y4 TV devices. Reference frame invalidation is disabled on this decoder to eliminate frame error bursts.
- Official PowerA Pro controllers for Nintendo Switch with zeroed vendor and product IDs are recognized and mapped using the Switch Pro controller key layout.
- Physical Japanese JIS keyboards map Hankaku/Zenkaku, Henkan, Muhenkan, and Katakana/Hiragana keys directly to Windows virtual keys without forced QWERTY normalization, enabling full host IME switching.
- Native translations for resolution picker section headers are provided across all 11 supported locales. European Portuguese localization is brought to full parity for network drop recovery.

### Changed

- The Quick bar style, the Hide quick bars when idle dialog and the Quick Menu section order no longer have their own Settings rows, because each is part of an editor that is also reachable from the stream. The Quick-action bars editor holds the style buttons and an Hide quick bars when idle button, and the In-stream menu editor holds the section order. Settings search still finds all three through the rows of those editors.
- The Tap to expand style is drawn in the bars editor preview as its small handle, like in the stream. A tap on the handle of the selected bar opens or closes it in the preview, and a tap on another bar selects it.
- The Video resolution picker always has a Custom resolution row at the end that opens the custom size dialog, so that setting is no longer hidden in a collapsed group.

- The standard resolutions in the Video resolution picker are equal-size cells in an even grid (six become 3 by 2, styled like the segmented buttons, the chosen one filled), with the shared ratio in the heading (Standard Resolutions (16:9)), so the picker is much shorter on a phone. Native display and custom sizes keep their rows.
- The custom accent color picker replaces the app style dialog instead of opening on top of it, and the app style dialog comes back with the choice, or unchanged when the picker is cancelled.

- The Settings list shows the active value inline in the title for Resolution, Frame rate, and Bitrate settings, keeping descriptive text full-width underneath.
- Preference titles wrap cleanly across up to two lines on compact phone displays to prevent truncation of active values.

### Fixed

- Ported six fixes from moonlight-common-c into the native core. A video frame can no longer grow without a limit from a flood of continuation packets without a start of frame (32 MB and 65536 fragments per frame), clearing a POSIX event takes its mutex, a failed audio start no longer closes its socket twice, the frame index is compared as 32-bit, a new stream resets the RTSP encryption counter and the frame drop count, and a full or shutting down queue frees what it could not take. Thanks to [@repparw](https://github.com/repparw) and [@cgutman](https://github.com/cgutman). The larger upstream changes (the depacketizer lock, the RTT race, ENet short headers) are not taken yet because they need a longer stream test.
- Fixed the Stream closed reconnect dialog being wider than the screen, with its message and buttons cut off, when the app opened in portrait right after a landscape stream. Dialogs now take the width from the window manager at show time and fit it again once the screen has settled.
- Fixed the quick bars dimming or hiding while a finger was on the stream, for example during a scroll or a long press. The idle time now starts when the last finger lifts.
- Fixed the hard white lines across the Quick-action bars editor on a tablet. The two panes fade out softly at their edges instead.

- Fixed the 1080p entry disappearing from the Video resolution picker when the saved custom resolution was also 1920 by 1080.
- Fixed the Copy profile menu running past the edge of the screen on a phone and blending into the black background in Pure black.

- Fixed long titles of Settings rows with a switch being cut off with dots on a phone in portrait. A title now wraps to a second line before it is cut.

## [1.1.0] - 2026-11-06

### Added

- Profiles can be moved up and down in the Profiles list: drag a profile by the handle at the start of its row, or long press the row and drag it. The new order is saved when the drag ends and stays after a restart. The active profile does not change.
- While the host is silent for 1.5 s, a label over the last frame says "Connection lost, waiting for the network" and counts the seconds. It goes away when the host answers again, and when the network drop grace period runs out the usual "connection terminated" dialog shows without the label.

- When the network drops, a card over the last frame shows the state (waiting for the stream, waiting for the network, reaching the PC, starting) with Cancel and Reconnect now. The stream reconnects by itself once the network is back and the PC answers, with a short backoff and a five minute limit, and the usual terminated dialog shows if that runs out. A new Settings switch, Reconnect after a network drop, turns it off.
- The quick bars have a Show or Hide Quick Bars action (`toggle_quick_bar`) for buttons, the Quick Menu, key flows and gestures. A bar hidden this way stays hidden until the action runs again and a touch does not bring it back.
- A new Settings row, Hide quick bars when idle, opens a dialog with two stages and four values, each a slider with a number field: dim after X seconds to a chosen opacity, hide completely after Y seconds, and the opacity of the edge tab that stays where a bar hid (0 means no tab). A time of 0 skips that stage. The bars come back when you tap the tab or touch the stream. The defaults dim to 40 percent after 3 seconds and never hide, as before.
- Gestures: two-finger taps, double taps, holds and swipes, and one-finger double taps and holds. Nothing is bound at first, so a two-finger tap stays a right click and one-finger touches reach the PC without delay. A one-finger hold takes the touch over only when it fires.
- A new Settings choice, Quick bar style, picks the look of the quick bars: Dock rail (one translucent rounded container, the default), Round buttons (a circle per button, as before) or Tap to expand (one small handle that opens the rail and closes again after a pause).
- A Find panel searches the keys, flows and actions of the quick bar editor (the Find button next to Add key or flow) and the gesture picker (the Search row at its top). It is a full-screen layer on a phone and a centered panel on a tablet. Type to filter by name, or switch to the in-app keyboard and tap keys to list everything that uses them. A gesture or bar item that is already used elsewhere shows a notice.
- The menu style, the quick bar style and the Quick Menu section order are actions now, so the Advanced Menu, the Quick Menu, a bar or a gesture can hold them. The Advanced Menu also has a Customize menu entry.
- The Quick Menu opens with a picker for the style (list, grid or compact) from the Menu style action.
- The menu editor lists your custom keys and flows in the Advanced Menu on request: a key that is not in the Quick Menu starts hidden, and its eye shows it there. A Quick Menu and Advanced Menu switch above the preview shows either menu as it will look.
- The quick bar style (Dock rail, Round buttons, Tap to expand) is also chosen in the Quick-action bars editor, where the preview shows it at once. It is applied on Save, and Cancel leaves it alone. The Settings row stays as a shortcut.

- Special keys: Backspace and Delete are the first two keys, each with its own icon, and Windows Magnifier has three keys (Magnifier In, Magnifier Out, Exit Magnifier) with their own icons.
- The virtual full keyboard has an Fn key at the end of the function row that swaps F1 to F12 for F13 to F24, and a new setting adds a permanent F13 to F24 row under the function row.
- The virtual full keyboard draws above the quick bars while it is open (new setting, on by default), so taps on the keys never reach a bar.

- The order of the Quick Menu sections (Actions, Session, Menu) can be changed by dragging in a new Settings row, Quick Menu section order, and directly inside the in-stream menu editor under a new Section order tab. The menu preview updates in real time as sections are reordered. The default order stays the same.

### Changed

- The in-stream menu editor preview sits in a taller box and draws the menu at 62 percent, so most of the menu is visible at once on a phone.
- The network drop card no longer waits out the whole grace period. While the stream is silent, the stream restarts by itself once the network has been back for 3 seconds, or after 10 seconds of silence when the network never reported a drop, instead of only after the grace period (BUG-070).
- The Quick Menu grid is denser: 80 dp columns and 68 dp tiles instead of 96 dp and 88 dp, so more actions fit on a phone and a tablet without scrolling.
- The Russian translation is complete: 590 strings and the step plural that had no Russian text are translated, and 173 existing Russian strings are corrected for meaning, completeness, outdated text and terminology. Keyboard keys and key combinations stay as printed on the keyboard, and only an explanation in brackets is translated (#3).
- The Default entry of the language list is a string resource (`language_default`), so each language shows it in its own words (#3).
- The status label (poor or slow connection, network drop) sits in the top left corner and fades in and out over half a second.
- The language changes at once: the choice applies to every screen as it is created, and the open Settings screen redraws itself without a restart or a black flash. Language moved from Misc to the User interface section next to App style, and it is an app-wide setting for profiles.
- All 21 languages of the language list now have a complete translation.
- The first entry of the language list reads System default, in each language's own wording, and it follows the per-app language of Android 13 and newer, then the system language. The list opens in the same picker as the other choice lists, and every row, including Hebrew, lines up the same way.
- Settings icons follow one rule: a row has an icon when it opens its own screen, editor, file picker or web page, or runs an action (backup, import, export, share, reset, help, links), and no icon when it is a switch, a single choice, a slider or a typed value. The 14 action and link rows that had none now have one.

### Fixed

- Prevented an Activity task race condition in `ShortcutTrampoline` when launching desktop PC shortcuts on Android 16 and 17 (BUG-059, upstream qiin2333/moonlight-vplus#778).
- Streamlined decoder recovery failure handling in `MediaCodecDecoderRenderer` to release recovery monitors and notify listeners rather than aborting with an unhandled exception (BUG-060, upstream qiin2333/moonlight-vplus#783).
- After a switch from Wi-Fi to mobile data the stream now reconnects by itself. The wait for the PC tries every saved address of the PC (local, manual, remote or VPN, IPv6), not only the one the stream used, so a PC reachable by its VPN address is found over mobile data (BUG-073).
- A stream that started on mobile data keeps the low mobile bitrate, so it is now started again by itself as soon as an unmetered network (Wi-Fi) has been up for 1.5 seconds and the PC answers over it, when the stream is at least 8 seconds old and the mobile and normal bitrates differ. A Wi-Fi with no route to the PC never restarts the stream. It follows the Reconnect after a network drop switch (BUG-074).
- Fixed black bars at the left and right of pinned app shortcut icons. A square of the art now fills the visible icon, placed around the drawn icon and lettering so they sit in the center of the shortcut. Pinning an app shortcut again also updates the icon of a shortcut that is already on the home screen.
- Fixed the quick bar style buttons in the bars editor being uneven and cut off in portrait. The three buttons share one fixed height.
- Fixed the app closing, and returning to the PC list, after a Wi-Fi drop when the stream restarted early. The drop watchdog asked the stream for its silence time after the stream had been stopped (BUG-072).
- Fixed the in-stream menu editor on a phone: in portrait the preview is reachable by scrolling the page, in landscape the key list scrolls on its own with Add key or flow fixed under it, and the line across the top of the preview pane is gone.
- Fixed dialogs and editors that stayed open through a rotation keeping the size of the old orientation. They fit the new width, and the menu and bar editors rebuild for it.
- Fixed the quick bar style buttons touching the bar buttons below them in the bars editor.
- Fixed pinned app shortcuts on the home screen sitting off center and cropped by the launcher mask. The art is fitted into the visible middle, and an app with no box art shows its initials.
- Fixed a symbol key such as the semicolon or bracket showing two characters in the Find panel in press-a-key mode. Each symbol key has one name now (BUG-071).
- Fixed the text of the placeholder app tiles (a desktop or app without box art) being faint grey on grey. It has a fixed dark colour now.
- Fixed the quick bar preview in the editor piling buttons onto each other when a bar has many buttons on a high resolution screen. The preview now draws the real bars, icons and style at full size and shrinks the whole screen, in a frame with the shape of the screen, so a phone in landscape shows its bars at the real corners.
- Fixed menus and editors being drawn oversized and cut off inside the small picture-in-picture window. They close when the stream shrinks.
- Fixed a stray line across the rows of the menu and bar editors, which were the scroll dividers of their inner lists.
- Fixed the keyboard taking over the whole screen when searching on a phone in landscape, in the Settings search and in the new Find panel.
- Fixed the picture-in-picture window showing a cropped, shrunken picture with black borders when a stream zoom and pan are saved. Picture-in-picture now shows the whole stream and your zoom and pan come back when the window expands (BUG-061).
- With Language set to Default, the app follows the language picked for Super Artemis in the Android settings (App languages) on Android 13 and newer, and the system language only when no app language is set. Before, the first system language always won (#6).

## [1.0.0] - 2026-10-30

- Surface lift: while Pure black is on, tapping the text of the Pure black row opens a ten level dot slider with a live sample in the dialog. It moves only the cards, fields and sections from flat to lifted, and level 5 is the look that shipped first. The switch keeps its own tap zone behind a divider.
- The color tiles are ten (Wallpaper, eight schemes including the new Amber, and a plus with a rainbow ring that wears the last custom color once one is picked), in one line on a wide dialog and in two rows of five on a phone.
- The Lock key runs an Apollo server command named Lock when the host has one, and shows a one time hint otherwise.
- The performance presets are now called Quick setups, with scenario names (Competitive play, Everyday desktop, Best picture, Battery saver), so they no longer repeat the names of the frame pacing options. Each choice shows a one line summary before it is opened.
- The Lock key help dialog now shows two separate options with numbered steps and a Copy button each: an Apollo server command named Lock, or AutoHotkey v2 for any host.
- The app style picker previews a tap and applies on the Apply button, shows one line of dots that wraps on a phone, keeps the last custom color as its own dot and puts the chosen name in the section label.
- The Support row has a filled red heart with a soft heartbeat that stops when animations are off. The Support dialog keeps its plain header.
- Settings, Host has "Lock key does nothing?" with the AutoHotkey script, a Copy button and a link to the guide, because Windows ignores a Win+L that a stream injects.
- The release process now follows DualClone: a keystore helper, a protected `release` environment, a verifier with a permission allow-list and certificate check, checksums, and a build and test workflow. See docs/RELEASING.md.
- The Blue scheme seed is `2196F3` and the custom color default is `00BCD4`, matching the shared accent list.
- The stream shrinks into picture-in-picture only through the system auto-enter and the leave hint, so dialogs and overlays of other apps no longer shrink it.
- A stream lost behind the lock screen reconnects after unlock even when the display still reported interactive.
- A ringing phone and a running call keep the stream full screen once the phone state permission is granted (asked once, optional).
- The color scheme picker is one line of dots with the last custom color and a plus, and the Support dialog opens with a red banner.
- The on-screen controller now takes its pressed color from the accent chosen in Settings, and keeps its own colors for the Wallpaper scheme.
- The Gemini share account picker no longer shrinks the stream into picture-in-picture.

### Added

- Performance presets (Lowest latency, Balanced, Best quality, Battery saver) in Settings. A preset shows the settings it changes before you apply it, saves your current values as a "Before" profile, and "Use my own settings" switches it off again. "Save my settings as a profile" keeps a hand tuned combination.
- Pure black switch for OLED screens, in Classic and Material You, and a Custom color scheme with a color picker (drag or type a hex code).
- Check for updates in Settings > About, a manual request to GitHub with a one time explanation, and GitHub Sponsors in the Support dialog.
- A sectioned icon picker with a search field and 235 icons, including the microphone and voice family, window layout, media, capture and more.
- Pencil buttons to edit a key or flow in place from the bar editor, the menu editor and the gesture editor (hold a gesture row), and "Edit this key or flow" and "Change icon" in the long press menu of a bar button, also over a stream.
- A key or flow name is optional. A shortcut reads "Ctrl + M" or "Ctrl + M (Gemini Talk)", and pickers say "Key:" for one shortcut and "Flow:" for a sequence.
- The quick bar editor says how many buttons fit per line on this screen and offers 4, 6, 10 and Fit screen.
- `scripts/native_tests/` with a test for stray audio packets, and a Native crash fix note in docs/BUGS.md.

### Changed

- Bars wrap at what fits on the screen, so no button is cut off on a phone, and the preview shows the bar the way the stream draws it.
- The Add key or flow button of the bar editor moved into the header of the button list, the add step chips are 48dp touch targets and the key editor chips fade at 30 steps.
- Screen off ends a stream under the picture-in-picture and resume policies the way Recents does, and unlocking reconnects it.
- Lock (Windows + L) and other Windows key shortcuts hold their keys longer and mark the Windows key like a keyboard does.
- Local files that are not meant for GitHub (`HANDOFF.md`, `FUTURE_WORK.md`) are ignored by git.

### Fixed

- A native crash in the audio thread of debug builds on a stray network packet (BUG-043).
- Back needing extra presses after Settings search (BUG-044) and the microphone preview row sliding into place (BUG-045).
- Unreadable Done and Set buttons in the key editor (BUG-046), a new key vanishing from a bar after switching sets (BUG-047) and bars running off a phone screen (BUG-048).
- Picture-in-picture starting during a call (BUG-050, the Gemini overlay is still open) and the Lock key doing nothing (BUG-049, device check pending).

## [1.0.0]

The first Super Artemis release, not published yet. The date is set when the `v1.0.0` tag is published, and the release notes are in `.github/release-notes/v1.0.0.md`.

### Added

- Microphone streaming to Apollo hosts that negotiate a mic stream (Steam Streaming Microphone on Windows), ported from [@logabell](https://github.com/logabell)'s work in ClassicOldSong/moonlight-android#537. Requires the [mic-enabled Apollo release](https://github.com/logabell/apollo-microphone/releases/tag/v2026.3.18-mic.1) (v2026.3.18-mic.1) or Apollo with [ClassicOldSong/Apollo#1428](https://github.com/ClassicOldSong/Apollo/pull/1428).
- "Microphone at stream start" setting with Always off (default), Always on and Follow last state, remembered per PC.
- Mute and unmute from the in-stream Quick Menu and Advanced Menu. Muting stops the microphone hardware and the Opus encoder, so the privacy indicator turns off and no battery is spent on audio capture.
- "Network drop grace period" setting (10 to 60 seconds, default 30) so short Wi-Fi drops do not end the session, from [@MikalaiKryvusha](https://github.com/MikalaiKryvusha)'s KAST.
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

- Stylus (S Pen) strokes are no longer taken over by the handwriting keyboard on Android 13+, from [@Yundi339](https://github.com/Yundi339)'s fix in qiin2333/moonlight-vplus#634.
- The decoder requests low latency again right after it starts on Android 11+, from [@derflacco](https://github.com/derflacco).
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
- docs/SETUP.md shows how Apollo's Do and Undo commands make Steam Streaming Microphone the default PC microphone only while a stream runs.
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
- The frame pacing setting is honoured again instead of always being forced to Balanced, a renderer loop that dropped about half the frames after any hiccup is gone, and the drop thresholds follow refresh rate changes during a stream, from [@coroiu](https://github.com/coroiu). The broken "Warp Drive" pacing options, which asked the host for double or quadruple the frame rate, are removed. This addresses the Samsung render rate reports in ClassicOldSong/moonlight-android#416, #415 and #376.
- The forced surface frame rate and fixed surface size, which caused stutter, are removed, from [@derflacco](https://github.com/derflacco).
- Decoder setup can no longer loop forever on NVIDIA and MediaTek decoders, the audio effects setting is no longer tied to "Play audio on PC", Android cloud and device transfer backups no longer copy the pairing private key, and the exported poster provider only serves files from its own cache, from [@Rafael-CN](https://github.com/Rafael-CN).
- The Joy-Con fix setting applies again ([@CoolRobloxDeveloper](https://github.com/CoolRobloxDeveloper), ClassicOldSong/moonlight-android#570), 4K streams no longer switch display modes needlessly ([@Mad-Melon](https://github.com/Mad-Melon), #601), Print Screen sends the right key and the Samsung keyboard cover screenshot key reaches the PC ([@Lian-CrescentMoon](https://github.com/Lian-CrescentMoon), #606), and vendor keyboards with out-of-range key codes no longer crash stream start ([@qiin2333](https://github.com/qiin2333), qiin2333/moonlight-vplus#591).
- Adding a PC from an `art://` pairing link no longer fails on slow networks, from [@Swanand01](https://github.com/Swanand01).
- Controller LED crashes, rumble on newer Android versions, Xbox Series controller IDs, Shift for the plus key and Android 15 and 16 key codes, from Moonlight 12.2 ([@cgutman](https://github.com/cgutman), [@T2Fat2Fly](https://github.com/T2Fat2Fly), [@bladeoner](https://github.com/bladeoner), [@ReenigneArcher](https://github.com/ReenigneArcher)).
- Native core: recovery after multi-block video loss and partially dropped IDR frames, no speculative loss reports with RFI off, RTT queries without the ENet lock, thread and pthread attribute leaks, and RTSP hardening against malformed or oversized responses ([@cgutman](https://github.com/cgutman), [@foxirain](https://github.com/foxirain), picked as in ClassicOldSong/moonlight-common-c#2 by [@JustinZeus](https://github.com/JustinZeus)).
- The local mouse cursor setting lists the Ctrl+Alt+Shift shortcuts.
- Multi-finger gestures work in pan and zoom mode, so the gesture that turned it on can turn it off again.
- The floating menu button no longer needs Back Menu to be on.

### Credits

Everyone credited for this release is listed in the README credits table and in Settings > About > Credits, both generated from `app/src/main/res/raw/credits.json`.
