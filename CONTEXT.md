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

## What works (verified on a Galaxy Tab S10 Ultra, Android 16)

- Side-by-side install next to Artemis, Artemis-mic and Moonlight.
- Settings search, including items inside collapsed categories.
- Backup through the system file picker (settings-only by default, pairing keys as one opt-in card with a red caution) and restore with a preview that keeps existing PCs, brings back settings, then restarts the app. The pairing-keys preview shows its red warning.
- Menu editor in Settings: style picker, side by side list and live preview, Session section in red, Cancel keeps the saved layout.
- Custom keys in Settings: empty state, the keyboard recorder (Ctrl + Shift + ESC recorded, duplicates ignored, name filled in), icon choice, the saved JSON (`0xA2`, `0xA0`, `0x1B` with the icon ID) and delete with confirmation.
- In a stream (2026-10-15): Quick Menu sections, Settings from a stream with reconnect on Back, Send special keys and More keys sections, the mouse mode picker with the current mode checked, Settings only once in the menus, the 3-finger tap keyboard toggle, picture-in-picture on Home and expanding it back, and Disconnect.
- Phone width on the S24 (2026-10-15): bar editor, key editor, gesture editor, App style cards, Profiles and search.
- Streaming at native 2960x1848 with Fit fills the screen, and native costs about 2.5% more battery than 1080p on the Tab (1,022 mA against 996 mA at 55 Mbps).

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
- The fastlane screenshots still show upstream Moonlight until new ones are taken.

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
