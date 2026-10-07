# Project context

This is the handoff file for anyone (human or AI) resuming work on Super Artemis. It records what works, what is in progress, what comes next and where to start reading. Reset it once 1.0.0 is released and stable.

## Current state

- Branch `unified-main` is based on Artemis `moonlight-noir` (commit `c5cf27f`). Nothing in this repository is published yet. The native core branch `super-artemis` is pushed to `Arora-Sir/moonlight-common-c`.
- Version `1.0.0` (versionCode 1) is set but not released. The current package IDs are `com.limelight.superartemis` (release) and `com.limelight.superartemisdebug` (debug). They change to `com.mohitarora.superartemis` and `com.mohitarora.superartemis.debug` before release (W13).
- Debug builds and the unit tests pass, apart from the five known upstream failures (BUGS.md, BUG-009).

## What works (verified on a Galaxy Tab S10 Ultra, Android 16)

- Side-by-side install next to Artemis, Artemis-mic and Moonlight.
- Settings search, including items inside collapsed categories.
- Menu editor: drag to reorder, save, reset, and the three menu styles in settings.
- Five-finger tap setting stays independent of Back Menu.
- Backup through the system file picker (settings-only by default) and restore that brings back frame rate, Back Menu and menu style, then restarts the app.

## Not yet verified in a live stream

The debug build needs pairing with Apollo first. Then check the mic off by default and mute/unmute, the menu styles in-stream, S Pen behaviour and decode time against the Artemis-mic baseline at 2560x1600, 120 FPS, HEVC.

## Workstreams to 1.0.0

| ID | Work | Status |
| :--- | :--- | :--- |
| W0 | Credits system and attribution in commit history | Done |
| W18 | Project docs (this file, ARCHITECTURE, SETUP, BUGS, CHANGELOG) and the dash check hook | In progress |
| W13 | Package ID `com.mohitarora.superartemis`, APK naming, release signing | Next |
| W1 | Restore keeps paired PCs when the backup has no keys, plus a restore preview | Planned |
| W2 | Backup and restore dialog visual polish | Planned |
| W3 | Shared action registry, Quick Menu sections (Session in red), Customize in the Quick Menu, Back to Quick Menu in Advanced, style picker and live preview in the editor, one RecyclerView renderer | Planned |
| W5 | In-app custom keys with a keyboard recorder and icons | Planned |
| W4 | Floating quick-action bar next to the menu button | Planned |
| W6 | Configurable 3, 4 and 5-finger tap and double tap gestures | Planned |
| W7 | When you leave the stream: Disconnect, Resume when reopened, or Picture-in-picture, plus auto-connect on start | Planned |
| W9 | Opt-in Material You style on Android 12+ | Planned |
| W10 | Branding and link fixes, fastlane, issue templates, FUNDING.yml | Planned |
| W12 | About: Support development (UPI, UPI QR, PayPal), developer profiles, credits, star on GitHub | Planned |
| W17 | Scan active forks and upstream issues for small, high-demand additions | Planned |
| W11 | README rewrite with the fork's USP, release notes | Planned |
| W14 | In-stream verification on the Tab and the S24 | Waiting for pairing |
| W15 | Push, CI release build, draft release, device install | Planned |
| W16 | Small upstream bug-fix PRs after release | Planned |
| W8 | Document Apollo per-client Do and Undo commands for connect and disconnect actions | Planned |

## Open problems

- Restore deletes local PCs even without pairing keys (BUGS.md, BUG-004). Fixed in W1.
- README.md and fastlane metadata still describe upstream Artemis and Moonlight. Rewritten in W10 and W11.
- `SUPER_ARTEMIS.md` duplicates what will move into README.md and this file. Merged in W11.

## Known unknowns

- Whether quick-switch to the previous app enters Picture-in-picture on One UI with Android 16 (measured in W7 before any change).
- Whether the Android 16 trackpad tap issue (ClassicOldSong/moonlight-android#594) and the Samsung pacing issues (#416, #376, #415) reproduce on the test devices.

## Where to start reading

1. ARCHITECTURE.md for layers, modules and design decisions.
2. `app/src/main/java/com/limelight/Game.java` for the stream activity (search for `applyMicrophoneInitialState` and `handleMultiTouchGesture`).
3. `GameMenu.java`, `GameMenuLayout.java` and `GameMenuEditor.java` for the in-stream menu.
4. `preferences/StreamSettings.java` and `preferences/BackupRestoreManager.java` for settings, search and backup.
5. `app/src/main/res/raw/credits.json` and CONTRIBUTING.md before porting anything from another project.
