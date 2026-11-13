# Project context

This is the handoff file for anyone (human or AI) resuming work on Super Artemis. It records what works, what is waiting, what comes next and where to start reading. Reset it once 1.0.0 is released and stable.

## Current state

- Branch `unified-main` is based on Artemis `moonlight-noir` (commit `c5cf27f`). Nothing in this repository is published yet. The native core branch `super-artemis` is pushed to `Arora-Sir/moonlight-common-c`, and the common-c fixes from W17 are committed there as `8c83fe9` but not pushed yet.
- Every planned feature for 1.0.0 is implemented. Workstreams after the backup commit (`6f810d67`) are prepared as one cumulative patch per workstream and wait for the commit review.
- Version `1.0.0` (versionCode 1) is set but not released. The package IDs are `com.mohitarora.superartemis` (release) and `com.mohitarora.superartemis.debug` (debug). Earlier test installs used `com.limelight.superartemisdebug`, which can be uninstalled.
- Debug builds pass, and all 233 unit tests pass. The five upstream failures of BUG-009 were test setup bugs and are fixed.
- Round 2 (the owner's 19 points from testing on the Tab, plus two later requests) is implemented: design foundation, settings, About, special keys, quick-action bars per corner, gestures and keyboard. It waits for device checks and the commit review.
- Round 2 follow-ups are implemented and checked on the Tab: encrypted `.sabackup` backups with share and selective restore (Tab to S24 restore verified), phone layouts, hold gestures and the multi-finger right click fix, crash reports and diagnostic logs, quick-action bar long press menu, and an audit of crash risks.
- Round 3 (the owner's 13 points from 2026-10-13) is committed locally as commits 29 to 41 plus a pre-commit hook fix: step flows, More keys, Add key or flow everywhere, hidden Advanced Menu actions, grid bars with sets, sets per profile and profile copies, themed in-stream dialogs, Material You by default with color schemes, the gestures switch row, the drag fixes, the search fix and picture-in-picture on any app switch. Nothing is pushed.
- Round 4 (Tab test pass and UI audit, 2026-10-15) is implemented and uncommitted: text fields that open the keyboard, Settings from a stream, the two-pane bar editor on tablets, the Profiles restyle, key flow naming and summaries, sections in Send special keys and More keys, the mouse mode picker with icons, and smaller fixes (BUG-028 to BUG-030).
- Round 5 (release pass on both devices, 2026-10-15) is implemented and uncommitted: the keyboard service no longer claims to be a screen reader, the microphone prompt no longer enters picture-in-picture, flows stop at once with the stream, menus close with the stream, Settings appears once in the in-stream menus, layout and wording fixes from the screenshot audit, and the BUG-009 test fixes (BUG-031 to BUG-033). Both rounds wait for the owner's commit approval.
- Round 6 (owner decisions, 2026-10-18) is implemented and uncommitted: the single "per line" bar slider, the profile editor as Settings in profile mode (`ProfileScope`), Reconnect after the connection ends and after closing picture-in-picture, the "Starting video" status, Print Screen, the landscape side setting, the keyboard service row, picture-in-picture on One Hand Operation+ app switches, two crash fixes, the home screen and search card restyle, and phone landscape layouts (BUG-034 to BUG-036). Unit suite 239 tests, all passing.
- Round 7 (2026-10-19) is implemented and uncommitted: rounded press ripples on dialog buttons, menu tiles and grids (BUG-037), F13 to F24 in the key recorder, and the shared `ui/ChoiceList` picker replacing five copies. The device pass on the S24 and the Tab is done, with two fixes from it (BUG-038 and BUG-039) and data restored on both.
- Round 9 (2026-10-19) is implemented and uncommitted: phone hotkeys (`customkeys/Hotkey`, `customkeys/HotkeyDispatcher`, wired into `Game.handleKeyDown`), the sectioned recorder for phones in portrait (`customkeys/RecorderKeyboard`, layout D3 chosen by the owner), Save waiting for Done, and faded disabled buttons (BUG-040). Versions follow MAJOR.MINOR.PATCH: 1.0.0 first, 1.0.x for fixes, 1.1.0 for features. Controller hotkeys are planned for 1.1.0.
- Round 10 (2026-10-23): the Home swipe regression (BUG-041) and Recents ending a picture-in-picture stream (BUG-042) are fixed and checked on both devices. `PIPDBG` lifecycle logs stay in debug builds only.
- Round 11 (2026-10-29): Surface lift (ten levels, opened from the Pure black row), ten color tiles with Amber and a plus with a rainbow ring that wears the custom color, the Lock key as an Apollo server command (needs a command named Lock in Apollo), and Quick setups with new names. Quick setups rows with one line summaries, and a Lock key help dialog with two options. Check on the Tab: the lift dialog and its ten levels in both styles, ten tiles on one line, and on a phone two rows of five.

- Round 11 (2026-10-23) is implemented and uncommitted: the native audio crash in debug builds (BUG-043), Settings Back after search and the microphone preview slide (BUG-044 and BUG-045), readable tonal buttons, 48dp chips, a sectioned icon picker with 235 icons and search, optional key names with "Key:" and "Flow:" labels, in-place key editing from every editor and the bar long press menu (BUG-046 and BUG-047), bars that fit the screen (BUG-048), Lock key timing (BUG-049), call and screen off rules for picture-in-picture (BUG-050 and BUG-051), performance presets, pure black, a custom color, GitHub Sponsors and a manual update check. Device checks are pending on the Tab and the S24, and the unit suite has 331 tests, all passing.

- Upstream watch expansion and Issue #12 triage (2026-11-01, commit `5591464f` on `unified-main`):
  - Changes: Added six active community forks (`MikalaiKryvusha/KAST`, `4had45/moonlit-artemis`, `MrOz59/Hestia-Android`, `kefrens/ariane-android`, `joemossjr16/artemis-android-pyrowave`, `Taveszfito/Artemis-Extended-Native-Wireless-DualSense-Features`) to `scripts/upstream_sources.json` and synchronized `UPSTREAM.md`. All newly added sources are pinned to their 2026-11-01 head commit hashes so future weekly runs track forward deltas only. Retained mature quiet forks (`logabell`, `coroiu`) permanently because zero-delta sources produce no issue noise and cost only 1 API call per run. Verified locally with `python scripts/upstream_watch.py` (exit code 0 across all 17 sources).
  - Issue #12 triage completed: Evaluated all 10 commits and 3 releases from `qiin2333/moonlight-vplus`. Ported two stability bug fixes locally: BUG-059 (`ShortcutTrampoline.java` task race condition on Android 16/17) and BUG-060 (`MediaCodecDecoderRenderer.java` graceful stream stop on decoder recreation failure). Cataloged resolution hot-switching (`0ef771f6b`) and layered haptics (`373e71adb`) for the post-1.0.0 roadmap. All 17 sources marked reviewed in `scripts/upstream_sources.json` via `python scripts/upstream_watch.py --mark-reviewed`.
  - Review comment prepared: `comments/issue_12_comment.md`.
  - Next manual review actions for Mohit:
    1. Inspect unstaged changes across `ShortcutTrampoline.java`, `MediaCodecDecoderRenderer.java`, `BUGS.md`, `CHANGELOG.md`, `scripts/upstream_sources.json`, and `CONTEXT.md`.
    2. Review and post `comments/issue_12_comment.md` to Issue #12, then close Issue #12.
    3. Push commit `5591464f` and the new fixes to `origin/unified-main`.

## What works (verified on a Galaxy Tab S10 Ultra, Android 16)

- Side-by-side install next to Artemis, Artemis-mic and Moonlight.
- Settings search, including items inside collapsed categories.
- Backup through the system file picker (settings-only by default, pairing keys as one opt-in card with a red caution) and restore with a preview that keeps existing PCs, brings back settings, then restarts the app. The pairing-keys preview shows its red warning.
- Menu editor in Settings: style picker, side by side list and live preview, Session section in red, Cancel keeps the saved layout.
- Custom keys in Settings: empty state, the keyboard recorder (Ctrl + Shift + ESC recorded, duplicates ignored, name filled in), icon choice, the saved JSON (`0xA2`, `0xA0`, `0x1B` with the icon ID) and delete with confirmation.
- In a stream (2026-10-15): Quick Menu sections, Settings from a stream with reconnect on Back, Send special keys and More keys sections, the mouse mode picker with the current mode checked, Settings only once in the menus, the 3-finger tap keyboard toggle, picture-in-picture on Home and expanding it back, and Disconnect.
- Phone width on the S24 (2026-10-15): bar editor, key editor, gesture editor, App style cards, Profiles and search.
- Streaming at native 2960x1848 with Fit fills the screen, and native costs about 2.5% more battery than 1080p on the Tab (1,022 mA against 996 mA at 55 Mbps).

- Round 12 (2026-11-04, S24 Ultra): picture-in-picture no longer keeps the fullscreen zoom and pan (`PanZoomHandler.enterPictureInPicture` and `exitPictureInPicture`, BUG-061). Checked on the S24 at a small and a large PiP size and on the way back to fullscreen. The microphone and Alt+F4 reports (BUG-062, BUG-063) were host side: Apollo view-only client permissions, one shared Apollo microphone writer for two streaming devices, and an AutoHotkey Alt+F4 guard. No app change for those two, and the host fixes are written up in BUGS.md.
- Round 13 (2026-11-05): network drop card and automatic reconnect (`session/NetworkResumer`, `ResumePlan`, `ui/NetworkStatusCard`), instant language switch (`utils/AppLanguage`, language callbacks in `ArtemisApplication`), quick bar hide toggle and idle auto-hide, one-finger and two-finger gestures, shared picker search (`ui/SearchField`), translations for all 21 picker languages as `values-xx/strings_extra.xml`, Backspace, Delete and Magnifier special keys, an Fn key and an optional F13 to F24 row on the virtual full keyboard (`KeyBoardLayoutController`), the keyboard drawn above the quick bars, a draggable Quick Menu section order (`menu/MenuSectionOrderDialog`) a denser Quick Menu grid, three quick bar looks with a peek tab (rail, round, handle, `QuickActionBar`), typed idle seconds (`SecondsPreference`), a System default language entry and the one icon rule for Settings rows (a row has an icon when it opens its own screen, editor, file picker or page, or runs an action). Round 14 (2026-11-05) adds the two-stage idle behavior (`menu/IdlePlan`, `menu/QuickBarIdleEditor`), custom keys in the Advanced Menu with a Quick Menu and Advanced Menu preview switch, the quick bar style choice inside the bars editor, an early reconnect once the network is back (`ResumePlan.shouldRestartEarly`), a reconnect over any saved address of the PC so a switch to mobile data works (`session/ResumeAddresses`, `session/HostProbe`, BUG-073), a restart of a stream that started on mobile data once Wi-Fi is up for 1.5 s and the PC answers over it (`ResumePlan.shouldRestartForQuality`, BUG-074), a shortcut icon cropped to the visible square (`utils/ShortcutIconComposer`), one name for each symbol key in the Find panel, and in-stream menu editor section reordering (`menu/GameMenuEditor`, `dialog_game_menu_editor.xml`) with live preview updates and persistent preferences. All covered by unit tests, none of it is checked on a device yet.

## Waiting for device checks

Everything below is implemented with unit tests and needs the Tab and the S24, with the debug build paired to Apollo:

- Microphone off by default, mute and unmute, the green privacy dot and Apollo's "first client microphone packet" log.
- Quick Menu sections and all three styles in a stream, Back to Quick Menu, D-pad navigation in the menu.
- Quick-action bars in several corners at once, rows and columns, green toggles, fading, long press names, hidden in picture-in-picture, and the editor preview.
- Special keys: built-in keys with icons in Send special keys, hide and show again, change an icon, reorder, reset, pin a built-in key to a bar, custom keys on the on-screen key layer.
- Every gesture slot including double taps and swipes, gestures in pan and zoom mode, Nothing passing taps to the PC, the Windows swipes preset, trackpad mode three-finger tap.
- Leaving the stream: Disconnect, Resume when reopened (Home, Recents, quick switch, then Recents card and app icon), picture-in-picture on Home and on a quick switch to the previous app, Connect to the last PC on app start.
- Material You on and off from the preview picker: switches, rounded sections, search bar colors, centered rounded dialogs in both styles, in-stream menus.
- Settings: sticky search bar, search for "three", "pip" and "upi", the section list on the Tab, regrouped sections, floating menu button with Back Menu off.
- Built-in keyboard: held Ctrl or Shift released when the keyboard closes, and kept when the option is off.
- About: Support (UPI chooser and the no-app fallback, QR scanned from another phone, copy, PayPal), developer links with icons, credits cards and chips.
- Frame pacing at 120 FPS: render and discard FPS on both devices, compared with Artemis-mic.
- S Pen strokes reach the PC, Print Screen key, Samsung keyboard cover screenshot key.
- New store screenshots for fastlane, taken from the Tab.
- Round 3 in a stream: a flow with text, a hold and a repeat, More keys, a key pinned to the Quick Menu, hidden Advanced Menu actions, grid bars at every anchor with offsets, set switching from the Quick Menu, the mouse mode picker and quit dialog in both styles and in a color scheme, picture-in-picture through One Hand Operation+ and an `am start` app switch, and the Recents card resuming the stream.
- Round 3 outside a stream: profile Duplicate and Copy settings into (needs a test profile), search for "mouse" and "pointer" on the S24.
- Round 5: the microphone permission prompt during a stream (needs the permission revoked first), the keyboard service feedback type in `dumpsys accessibility` (needs the service enabled), and the taller menu editor list on the S24.
- Round 13: toggle Wi-Fi during a stream and watch the card, the automatic reconnect and Cancel. Switch language in Settings and in the app picker on Android 13 and newer. The quick bar toggle and the idle hide. Two-finger and one-finger gestures in direct touch, trackpad and mouse modes. Search in the gesture picker and the bar editor. Quick Menu section order dialog and the denser grid in all three menu styles. Quick bar style rail, round and handle, the peek tab after the idle seconds, and the typed seconds row. Fn key, F13 to F24 row and keyboard above quick bars on the Tab and the S24.
- Round 10 (2026-10-26): the app style picker previews on a tap and applies on the Apply button, with one line of dots (rows on a phone), the last custom color as its own dot and the chosen name in the section label. The Support row with the beating red heart. Settings, Host, Lock key does nothing? (the script and a Copy button). The call check with the phone state permission (asked once, optional). Pure black, the update check and Sponsors on the Tab, and the stricter picture-in-picture rule on Home, Recents, the assistant overlay with its share picker, a call, and screen off.

## Workstreams to 1.0.0

| ID | Work | Status |
| :--- | :--- | :--- |
| W0 | Credits system and attribution in commit history | Done |
| W18 | Project docs (this file, ARCHITECTURE, SETUP, BUGS, CHANGELOG, UPSTREAM, PRIVACY) and the dash check hook | Done, kept current at every step |
| W13 | Package ID `com.mohitarora.superartemis`, APK naming, release signing | Done |
| W1 | Restore keeps paired PCs when the backup has no keys, plus a restore preview | Done |
| W2 | Backup and restore dialog visual polish | Done |
| W3 | Shared action registry, Quick Menu sections (Session in red), Customize in the Quick Menu, Back to Quick Menu in Advanced, style picker and live preview in the editor, one RecyclerView renderer | Done, in-stream check pending |
| W5 | In-app custom keys with a keyboard recorder and icons | Done, in-stream check pending |
| W4 | Quick-action bar in a chosen screen corner | Done, device check pending |
| W6 | Configurable 3, 4 and 5-finger tap and double tap gestures | Done, device check pending |
| W7 | When you leave the stream: Disconnect, Resume when reopened, or Picture-in-picture, plus auto-connect on start | Done, device matrix pending |
| W9 | Opt-in Material You style on Android 12+ | Done, device look pending |
| W10 | Branding and link fixes, fastlane, issue templates, FUNDING.yml, PR template, PRIVACY.md | Done (new store screenshots come from the device checks) |
| W12 | About: Support development (UPI, UPI QR, PayPal), developer profiles, credits, star on GitHub | Done, device check pending |
| W17 | Fork and upstream scan, then the approved fixes | Done: 13 fixes ported with credit. Deferred: Samsung Book Cover pointer capture (ClassicOldSong/moonlight-android#607, needs a device test with S Pen isolation), HDR indicator in the overlay (#431) and mouse sensitivity (#452, #529) |
| W8 | Document Apollo per-client Do and Undo commands for connect and disconnect actions | Done (SETUP.md, with a mouse speed example) |
| W19 | Stay current with upstream: UPSTREAM.md rule, weekly upstream watch issue, Dependabot | Done |
| W11 | README with the fork's USP, CHANGELOG 1.0.0, release notes | Done (`.github/release-notes/v1.0.0.md`, attached to the draft release by CI) |
| W20 | Design foundation: color roles, one rounded centered dialog theme for both styles, type scale, dialog helpers | Done, device look pending |
| W21 | Settings: sticky search, search keywords, regrouped sections, duplicates removed, floating button fix, Material You sections, tablet section list | Done, device check pending |
| W22 | About redesign: Support card, developer links with icons, credits cards | Done, device check pending |
| W23 | Special keys: built-in keys with icons, hide, re-icon, reorder, reset, 75 icons | Done, in-stream check pending |
| W24 | Quick-action bars per corner, row or column, editor preview, migrations | Done, in-stream check pending |
| W25 | Gestures before every touch mode, 3 and 4-finger swipes, Windows preset, editor redesign | Done, in-stream check pending |
| W26 | App style picker with live previews, menu style only in the editor | Done, device check pending |
| W27 | Attribution comments at every ported code site | Done |
| W28 | Release held keys when the built-in keyboard closes | Done, in-stream check pending |
| W29 | Round 3: flows, More keys, Add key or flow, hidden Advanced Menu actions, grid bars and sets, profile copies, themed dialogs, color schemes, gestures switch row, drag, search, picture-in-picture on any switch | Done, editors checked on the Tab, in-stream checks pending |
| W14 | In-stream verification on the Tab and the S24 | Waiting (list above) |
| W15 | Commit review, push, signing secrets, `v1.0.0` tag, CI draft release, install on both devices | Waiting for the owner |
| W16 | Small upstream bug-fix PRs after release | After release |

## Open problems

- The common-c fixes are committed in the submodule (`8c83fe9` on `super-artemis`) and must be pushed to `Arora-Sir/moonlight-common-c` before the superproject is pushed.
- The fastlane 10-inch screenshots are updated with genuine Galaxy Tab S10 Ultra captures and embedded in README.md.

## Known unknowns

- Whether One Hand Operation+ app switches now enter picture-in-picture through `onPause` on both devices (Android 16 on the Tab, One UI on the S24).
- Whether the Android 16 trackpad tap issue (ClassicOldSong/moonlight-android#594) reproduces on the Tab.
- How much the frame pacing fixes change render FPS on the S24 and the Tab at 120 FPS.

## Where to start reading

1. ARCHITECTURE.md for layers, modules and design decisions.
2. `app/src/main/java/com/limelight/Game.java` for the stream activity (search for `applyMicrophoneInitialState`, `handleMultiTouchGesture`, `initQuickActionBar` and `onUserLeaveHint`).
3. `actions/StreamActionRegistry.java` for every stream action, then `GameMenu.java` and the `menu/` package for the in-stream menu, the editor and the quick-action bars (start at `BarSets`).
4. `ui/Dialogs.java` and `res/values/styles.xml` for the shared dialog, type scale and color roles that every new screen uses.
5. `customkeys/` (start at `SpecialKeys`), `gestures/` (start at `MultiFingerGestureRecognizer`), `session/`, `theme/` and `about/` for the features of the same names.
6. `preferences/StreamSettings.java` for settings and search, and the `backup/` package (start at `BackupRestoreController` and `RestoreStrategy`) for backup and restore.
7. `app/src/main/res/raw/credits.json`, CONTRIBUTING.md and UPSTREAM.md before porting anything from another project.

- Round 15 (2026-11-11): Settings row titles wrap to two lines on a phone (`SettingsListStyle.allowTwoLineTitle`), the standard resolutions are a chip grid (`ChoiceList.addChips`), a profile can be the start for new profiles (`ProfilesManager.defaultTemplateId`, issue 5), and the custom color picker replaces the style dialog instead of stacking on it. Also in round 15: the Quick-action bars editor previews Tap to expand as its handle and opens the idle dim and hide dialog itself (the Settings rows for bar style, idle behavior and Quick Menu order are gone), the tablet panes fade instead of drawing lines, the bars no longer dim or hide while a finger is on the stream (`QuickActionBar.onStreamTouchAction`), the Copy profile menu is a `ChoiceList` with a star on the start profile, and dialogs take their width from the window manager so the Stream closed dialog fits the screen after a landscape stream (BUG-075). Checked on the Android 13 emulator at phone width and by the owner on the S24 Ultra. BUG-076 records a one-off Android swipe-to-PiP failure that closed the stream, which is a platform problem and not an app crash.
