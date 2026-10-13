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
- **Fix Applied:** `PrefsSnapshot.isExcluded` skips names starting with `android.` or `_`, plus `DecoderTombstone`, `GlPreferences` and `preferenceSearch`. Restore skips those names when an older backup contains them.
- **Verification:** `BackupRestoreTest` and an on-device backup that lists only app settings files.
- **Status:** Fixed.

## BUG-004: Restore deletes every saved PC

- **Symptom:** Restoring a backup without pairing keys removes PCs that are paired on this device, even though the device's pairing identity is untouched.
- **Root Cause:** `applyBackup` deleted all rows from `computers4.db` before inserting the backup's PCs, whether or not the backup carried pairing keys.
- **Fix Applied:** `RestoreStrategy.forBackup` picks `MergeHostsStrategy` for backups without keys. It keeps every local PC row, the identity and local entries in `HiddenApps` and `MicrophoneState`, and only adds backup PCs whose UUID is new. Backups with keys use `ReplaceAllStrategy`, which still mirrors the backup because the identity changes too. Do not route keyless backups through the replace path.
- **Verification:** `BackupRestoreTest` merge cases, and on the Tab a settings-only restore kept the existing PC and brought back the five-finger setting.
- **Status:** Fixed.

## BUG-005: Line breaks in string resources collapsed into one paragraph

- **Symptom:** A dialog message written with real line breaks in `strings.xml` rendered as a single dense block.
- **Root Cause:** Android collapses raw whitespace in string resources. Only escaped `\n` sequences produce line breaks.
- **Fix Applied:** Use `\n` escapes. The backup dialog was later rebuilt with a dedicated layout instead of a long message.
- **Verification:** On-device screenshot of the backup dialog.
- **Status:** Fixed.

## BUG-006: androidx default preferences name is private

- **Symptom:** `PreferenceManager.getDefaultSharedPreferencesName(Context)` does not compile against androidx.
- **Root Cause:** The method is private in androidx.preference (it is public only in the deprecated framework class).
- **Fix Applied:** `PrefsSnapshot.defaultPreferencesName` uses `context.getPackageName() + "_preferences"`, which is the same name androidx uses.
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

## BUG-010: Restore preview called unpaired PCs paired

- **Symptom:** On a fresh install, the restore preview said "Your 1 paired PC stays paired" although the PC list showed that PC locked (never paired).
- **Root Cause:** `computers4.db` also stores PCs found by LAN discovery. Pair state is not stored there, it is only known after the app asks each host.
- **Fix Applied:** The preview counts saved PCs ("Your 1 PC is kept as it is") and only mentions pairing for PCs added from a backup from another identity, which it detects with the certificate fingerprint.
- **Verification:** `BackupRestoreTest.previewWithoutKeysCountsKeptAndNewPcs` and the Tab screenshot of the preview.
- **Status:** Fixed.

## BUG-011: Three-finger trackpad tap fired two actions

- **Symptom:** In trackpad mode a three-finger tap sent a middle mouse click to the PC and also toggled the soft keyboard.
- **Root Cause:** Upstream commit `16d21834` (ClassicOldSong/moonlight-android) made `TrackpadContext` send a middle click for three fingers, while `Game` still mapped the same tap to the keyboard. Its author reports the follow-up fix in ClassicOldSong/moonlight-android#409 as still unreliable.
- **Fix Applied:** The middle click change is reverted in `TrackpadContext`, and "Middle mouse click" is a gesture action that any tap or double tap can run. Do not reintroduce finger-count handling inside the touch contexts, gestures belong to `GestureDispatcher`.
- **Verification:** `GesturesTest`. A device check of trackpad mode is pending.
- **Status:** Fixed.

## BUG-012: Frame pacing forced to Balanced and half the frames dropped

- **Symptom:** Render FPS stayed below the incoming FPS on Samsung and other 120 Hz devices (ClassicOldSong/moonlight-android#416, #415, #376), and the frame pacing setting had no effect.
- **Root Cause:** The 20.3.0 experimental renderer work in the base overwrote `prefConfig.framePacing` with Balanced on both branches of the low-delay block, a "latest-only" drain loop discarded all but the newest output buffer once two were ready, the drop thresholds were sampled once at stream start, and a reflective `setFrameRate` with a fixed surface size added stutter.
- **Fix Applied:** Ports of coroiu/twilight-android `a3d872503`, `e34cf77c5` and `98298aa6e`, and derflacco/moonlight-android `e3eaf4939` and `5c0d3981a`. `Game.java` must not assign `framePacing` outside the frame rate cap fallbacks, and the renderer must not drain and discard ready buffers.
- **Verification:** Unit suite and build. Render and discard FPS at 120 FPS on the Tab and the S24 are part of the device checks.
- **Status:** Fixed, device check pending.

## BUG-013: Decoder setup could hang the app at connection time

- **Symptom:** On some NVIDIA and MediaTek decoders, connecting never finished and showed no error.
- **Root Cause:** `initializeDecoder()` retries while the low-latency helper reports new options. The NVIDIA block always reported new options and the MediaTek block reported them outside its retry guard.
- **Fix Applied:** Port of Rafael-CN/VibeArtemis `a502acb23`: both guards fixed and the retry loop has an absolute bound. New vendor blocks must keep their `tryNumber` guard.
- **Verification:** Unit suite and build.
- **Status:** Fixed.
