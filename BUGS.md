# Known bugs and regression-prone fixes

Each entry records a problem that was fixed in a way a future change could silently undo, or a known issue that is still open. Statuses are Fixed, Open, Known (accepted, not caused by this fork) or Workaround.

## BUG-001: Microphone source rejection crashes capture

- **Symptom:** On devices that reject the UNPROCESSED audio source, starting the mic threw from `AudioRecord.Builder.build()` instead of trying the next source.
- **Root Cause:** The upstream mic PR only checked `getState()` after building, but some devices throw during `build()`. This was the open P1 from the review of ClassicOldSong/moonlight-android#537.
- **Fix Applied:** `MicrophoneCaptureManager.buildAudioRecord` catches `IllegalArgumentException`, `UnsupportedOperationException` and `SecurityException` and returns null, so the loop tries VOICE_RECOGNITION and then MIC.
- **Verification:** Mic tests pass, and capture starts on the Tab S10 Ultra.
- **Status:** Fixed.

## BUG-002: Five-finger tap stopped working when Back Menu was off

- **Symptom:** Turning off the Back Menu setting also disabled the five-finger tap that opens the in-stream menu.
- **Root Cause:** Both five-finger paths in `Game.java` checked `prefConfig.enableBackMenu`, although that setting only describes the physical back key.
- **Fix Applied:** A separate `checkbox_five_finger_menu` setting (`enableFiveFingerMenu`) gates the gesture. The back key, floating button and controller combo still follow Back Menu.
- **Verification:** On the Tab, Floating Button greys out with Back Menu off while "Five-finger tap opens menu" stays independent.
- **Status:** Fixed.

## BUG-003: Backups contained framework and library files

- **Symptom:** A backup included `android.app.ActivityThread.IDS`, `_has_set_default_values` and `preferenceSearch` (search history).
- **Root Cause:** The backup enumerated every file in `shared_prefs/` and only excluded crash and GPU state.
- **Fix Applied:** `BackupRestoreManager.isExcludedPreferenceFile` skips names starting with `android.` or `_`, plus `DecoderTombstone`, `GlPreferences` and `preferenceSearch`. Restore skips those names when an older backup contains them.
- **Verification:** `BackupRestoreManagerTest` (7 tests) and an on-device backup that lists only app settings files.
- **Status:** Fixed.

## BUG-004: Restore deletes every saved PC

- **Symptom:** Restoring a backup without pairing keys removes PCs that are paired on this device, even though the device's pairing identity is untouched.
- **Root Cause:** `applyBackup` deletes all rows from `computers4.db` before inserting the backup's PCs.
- **Fix Applied:** Not yet. A merge strategy that keeps local PCs when the backup has no keys is planned (see CONTEXT.md, W1).
- **Verification:** Pending.
- **Status:** Open.

## BUG-005: Line breaks in string resources collapsed into one paragraph

- **Symptom:** A dialog message written with real line breaks in `strings.xml` rendered as a single dense block.
- **Root Cause:** Android collapses raw whitespace in string resources. Only escaped `\n` sequences produce line breaks.
- **Fix Applied:** Use `\n` escapes. The backup dialog was later rebuilt with a dedicated layout instead of a long message.
- **Verification:** On-device screenshot of the backup dialog.
- **Status:** Fixed.

## BUG-006: androidx default preferences name is private

- **Symptom:** `PreferenceManager.getDefaultSharedPreferencesName(Context)` does not compile against androidx.
- **Root Cause:** The method is private in androidx.preference (it is public only in the deprecated framework class).
- **Fix Applied:** `BackupRestoreManager.defaultPreferencesName` uses `context.getPackageName() + "_preferences"`, which is the same name androidx uses.
- **Verification:** Backup and restore round trip test, and the on-device restore of frame rate, Back Menu and menu style.
- **Status:** Fixed.

## BUG-007: Settings search could not reach collapsed items

- **Symptom:** Tapping a search result for a setting inside a collapsed category scrolled nowhere, and short queries ranked loosely related settings first.
- **Root Cause:** SearchPreference finds a result through its adapter position, which does not exist for children hidden by `initialExpandedChildrenCount`. Fuzzy matching was on by default.
- **Fix Applied:** `StreamSettings.onSearchResultClicked` rebuilds the list with the result's parent groups expanded, then scrolls to the result and marks it. Fuzzy matching is disabled.
- **Verification:** On the Tab, searching "customize" opens UI Settings expanded at the item, and "menu style" lists only relevant settings.
- **Status:** Fixed.

## BUG-008: Mixed line endings in upstream files

- **Symptom:** Cherry-picks and scripted edits produced diffs where unchanged lines appeared as removed and re-added.
- **Root Cause:** Upstream files mix CRLF and LF, and tools that normalize endings rewrite whole files.
- **Fix Applied:** Workaround. Edits preserve each file's existing line ending style, and cherry-picked files are normalized back to the style of their upstream version before committing.
- **Verification:** `git diff --ignore-cr-at-eol --stat` matches `git diff --stat` for every commit.
- **Status:** Workaround.

## BUG-009: Five upstream unit tests fail

- **Symptom:** `LayoutInflationTest.allLayoutsInflateSuccessfully`, both `ProfilesNavigationTest` cases, `SimpleStartupTest.testApplicationOnCreate` and `StartupTest.testApplicationStartup` fail.
- **Root Cause:** They fail identically on untouched upstream commit `c5cf27f` (Robolectric class casts and a null base context), so they are not caused by this fork.
- **Fix Applied:** None. The gate for every change is "no failures beyond these five".
- **Verification:** Running the same test classes on an upstream worktree gives the same 5 failures.
- **Status:** Known.
