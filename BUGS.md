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
- **Fix Applied:** The five-finger tap first got its own setting, which configurable gestures later replaced: the `gesture_tap_5` binding decides what it does, and the old `checkbox_five_finger_menu` value migrates into it. The back key, floating button and controller combo still follow Back Menu. Gestures must never check `enableBackMenu`.
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
- **Fix Applied:** The tests click the profiles button as a `View` (it is an extended floating action button), inflate every layout with the app theme in both app styles instead of `Theme.AppCompat`, and run `onCreate()` on the application the test framework builds from the manifest instead of a bare `new ArtemisApplication()` without a base context.
- **Verification:** All 233 unit tests pass.
- **Status:** Fixed.

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

## BUG-014: Gestures stopped working in pan and zoom mode

- **Symptom:** After a three-finger double tap turned on pan and zoom, the same gesture could not turn it off. No multi-finger gesture worked until pan and zoom was turned off from the menu.
- **Root Cause:** `Game.handleMotionEvent` sent every touch to `PanZoomHandler` and returned before reaching the multi-finger tap detection.
- **Fix Applied:** `MultiFingerGestureRecognizer` runs at the top of the finger branch, before on-screen control editing, pan and zoom and every touch mode. A recognized gesture cancels what the touch started (a pan, or the touch on the PC). Keep the recognizer first when adding touch modes.
- **Verification:** `GestureRecognizerTest`, and a device check with injected multi-finger touches.
- **Status:** Fixed.

## BUG-015: Floating menu button greyed out with Back Menu off

- **Symptom:** "Floating Button" could not be turned on while "Back Menu" was off, and the button never showed in a stream in that case.
- **Root Cause:** Upstream made the setting depend on `checkbox_enable_quit_dialog`, and `Game` only showed the button when both were on.
- **Fix Applied:** The dependency is removed in `preferences.xml` and `Game` reads only the floating button setting.
- **Verification:** Settings shows the switch enabled with Back Menu off, device check in a stream.
- **Status:** Fixed.

## BUG-016: Material You dialogs sat at the bottom and were narrow on tablets

- **Symptom:** With Material You on, credits, support and other dialogs opened as Samsung bottom sheets about a quarter of the Tab's width, with large type, while Classic dialogs had sharp corners.
- **Root Cause:** Framework `AlertDialog`s used `Theme.DeviceDefault.Dialog.Alert`, which One UI anchors to the bottom, and Classic used the platform Material dialog with 2 dp corners.
- **Fix Applied:** Both styles set `android:alertDialogTheme` to a Super Artemis dialog theme (centered, 28 dp corners, role colors, sentence-case buttons). Super Artemis dialogs also size themselves with `ui/Dialogs`.
- **Verification:** `StyleLayoutsTest` and screenshots on the Tab in both styles.
- **Status:** Fixed.

## BUG-017: Backup dialog crashed in Material You

- **Symptom:** Tapping "Back up settings" closed the app on the Tab with Material You on.
- **Root Cause:** `Theme.SuperArtemis.AppCompatDialog` set `android:textColorPrimary` to `?attr/saOnSurface`, a nested attr chain that `DialogTitle` cannot resolve, so inflation threw `UnsupportedOperationException: Failed to resolve attribute`.
- **Fix Applied:** The androidx dialog theme no longer overrides framework text colors with app roles, and its buttons use `?attr/colorPrimary`. `StyleDialogsTest` opens a framework and an androidx dialog in both styles.
- **Verification:** `StyleDialogsTest`, and the dialog opening on the Tab and the S24 in both styles.
- **Status:** Fixed.

## BUG-018: "Report an issue" opened the GitHub app home screen

- **Symptom:** The bug report link opened the GitHub app, which showed its home screen and then an "Open with" sheet across work profiles.
- **Root Cause:** The GitHub app claims every github.com link but cannot render `/issues/new/choose`. The fork also had Issues turned off on GitHub, so the page returned 404 in a browser.
- **Fix Applied:** `Links.openInBrowser` resolves the default browser and opens the page there, with a `<queries>` entry for browsable https links. Issues must be enabled on the repository.
- **Verification:** Brave opened the page on the S24.
- **Status:** Fixed in the app. Enabling Issues on GitHub is pending.

## BUG-019: Collapsed settings groups still said "Advanced"

- **Symptom:** The expand row of collapsed groups showed "Advanced" on an English (India) device although the app overrides it with "Show more".
- **Root Cause:** androidx Preference ships `expand_button_title` in `values-en-rAU`, `rCA`, `rGB`, `rIN` and `rXC`. A region folder in a library outranks the app's default `values`.
- **Fix Applied:** The override is repeated in those five folders.
- **Verification:** The Tab and the S24 show "Show more".
- **Status:** Fixed.

## BUG-020: Phone layouts clipped labels and keys

- **Symptom:** On the S24 with a large font, "Grid with icons" wrapped and clipped, the UPI ID was cut in half, F10, Home and PgUp wrapped on the recorder keyboard, and the password fields hid behind the keyboard.
- **Root Cause:** Fixed 40 dp segment height, side by side UPI details on a narrow card, selectable text that ignores ellipsize, fixed 12 sp key labels, and dialogs without a resize input mode.
- **Fix Applied:** Segments grow with their text on one line, compact screens stack the UPI card, key labels autosize on one line on a taller keyboard, and input dialogs use `SOFT_INPUT_ADJUST_RESIZE`.
- **Verification:** Screenshots on the S24 and the Tab.
- **Status:** Fixed.

## BUG-021: Multi-finger taps also sent a right click

- **Symptom:** In the mouse and trackpad modes, every 3, 4 or 5-finger tap ran its action and also right clicked on the PC.
- **Root Cause:** The touch contexts only track two fingers. Events of the third finger return before `setPointerCount`, so context 1 believed the gesture had two fingers and its `isTap` sent `BUTTON_RIGHT` when its finger lifted. The recognizer only claimed a tap at the last lift, after that click.
- **Fix Applied:** `MultiFingerGestureRecognizer` claims a qualifying tap at the first lift and cancels the touch modes before any of them sees a lift. Finger counts with a hold or a swipe bound are claimed when the fingers land.
- **Verification:** `GestureRecognizerTest.firstLiftOfATapNeverReachesTheTouchModes`, and injected 3-finger taps on the Tab in trackpad mode with no context menu on the PC.
- **Status:** Fixed.

## BUG-022: Quick-action bar editor closed the app

- **Symptom:** Opening "Quick-action bars" in Settings or from the in-stream menu closed the app on the Tab.
- **Root Cause:** The editor rows reuse the menu editor row, whose checkbox became a switch, while `QuickBarEditor` still cast it to `CheckBox`. The `ClassCastException` happened while the list was laid out, after `show()` returned.
- **Fix Applied:** The holder uses `CompoundButton`. `scripts/check_view_casts.py` compares every `findViewById` type with the layouts, and `EditorsSmokeTest` opens and lays out every dialog in both styles.
- **Verification:** The smoke test fails with the old cast and passes with the fix. The editor opens in Settings and in a stream on the Tab.
- **Status:** Fixed.

## BUG-023: Gesture toggles did not update the quick-action bar

- **Symptom:** After Pan/Zoom was turned on by a gesture, its bar button stayed grey.
- **Root Cause:** Only menu taps refreshed the bar states, and gestures run actions through `GameMenu.runAction`.
- **Fix Applied:** `runAction` refreshes the bars after every action.
- **Verification:** The bar button turns green after a 4-finger hold on the Tab.
- **Status:** Fixed.

## BUG-024: An app switch without a Home gesture ended the stream instead of entering picture-in-picture

- **Symptom:** With picture-in-picture as the leave policy, switching to the previous app with One Hand Operation+ disconnected the stream.
- **Root Cause:** Picture-in-picture started only from auto-enter (Home and Recents gestures) or `onUserLeaveHint`, and an app opened over the stream gives neither. The stream paused, stopped and `onStop` disconnected it.
- **Fix Applied:** `Game.onPause` enters picture-in-picture when the policy asks for it, guarded against screen off, the lock screen, split screen, finishing, USB prompts and the app's own screens.
- **Verification:** Pending on both devices with One Hand Operation+ and an `am start` switch during a stream.
- **Status:** Fixed, device check pending.

## BUG-025: In-stream dialogs ignored the app style

- **Symptom:** The mouse mode picker, the quit confirmation, connection errors and the connecting spinner kept the plain platform look under Material You and Classic.
- **Root Cause:** They were built with `new AlertDialog.Builder(activity)` or `new ProgressDialog(activity)` on the stream activity, whose theme is the stream theme.
- **Fix Applied:** `ui/ThemedDialogs` wraps the context with the app style, and `StreamDialogsTest` fails on any dialog in the stream sources built without it.
- **Verification:** Unit test, in-stream look pending.
- **Status:** Fixed.

## BUG-026: Dragging to the end of a list stalled and fast drags saved a different order

- **Symptom:** Holding a dragged row at the top or bottom of the special keys, menu editor or bar editor list barely scrolled, and after a fast drag the saved order differed from the screen.
- **Root Cause:** ItemTouchHelper's default edge scroll grows with the fifth power of time over two seconds (about 3% speed after one second), and the lists used `Collections.swap` while the adapter drew `notifyItemMoved`, which only match for neighbouring rows.
- **Fix Applied:** `ui/DragReorder` with a distance and time based edge scroll that starts at a third of full speed, and a real move of the item.
- **Verification:** On the Tab a 1.3 second hold moved a row about 29 places instead of 1.5, and a drag down and back up restored the exact saved order.
- **Status:** Fixed.

## BUG-027: Settings search missed settings for broad words

- **Symptom:** Searching "mouse" did not show Touchscreen Mode, Remember mouse mode or Remote desktop mouse mode.
- **Root Cause:** SearchPreference returns at most 10 results ranked by a greedy fuzzy score that favors titles starting with the query. The limit is a private constant up to version 2.7.3.
- **Fix Applied:** SearchPreference v2.5.1 is vendored in `third_party/searchpreference` and returns every match ranked by title, section, details and synonyms.
- **Verification:** `SettingsSearchTest` indexes the real settings screen. Reproduced on the Tab before the fix (exactly 10 results). After the fix "mouse" lists every mouse setting and "pointer" finds them through synonyms on the Tab.
- **Status:** Fixed.

## BUG-028: Text fields built in code never opened the keyboard

- **Symptom:** Tapping the field in the restore password prompt, a key step value dialog or the set name dialog showed a cursor but no keyboard, so nothing could be typed.
- **Root Cause:** Those fields were built with `new EditText(context, null, 0, R.style.Widget_SuperArtemis_TextField)`. A default style attribute of 0 drops the platform `editTextStyle`, and the text field style had an empty parent, so the field was not focusable in touch mode and a tap never started input. Fields declared in XML kept `editTextStyle` and worked.
- **Fix Applied:** `Widget.SuperArtemis.TextField` has `android:Widget.Material.EditText` as its parent.
- **Verification:** `TextFieldsTest` checks focusable in touch mode, clickable and text editor in both styles. Reproduced on the S24 (cursor, `mInputShown=false`). On the Tab the Type text step and Rename set fields now show the keyboard (`mInputShown=true`).
- **Status:** Fixed.

## BUG-029: A new key landed after all the More keys

- **Symptom:** After the special keys had been reordered once, a newly recorded key appeared at the very end of the list, under the 24 More keys, instead of with the other everyday keys.
- **Root Cause:** `SpecialKeys.order` placed keys named in the saved order first and appended every other key at the end, and the More keys are part of the saved order.
- **Fix Applied:** A key missing from the saved order goes before the first More key.
- **Verification:** `SpecialKeysTest.aKeyAddedAfterASavedOrderGoesBeforeMoreKeys`, and on the Tab a new key showed right above the More keys entry.
- **Status:** Fixed.

## BUG-030: The key editor lost its chip styles when opened from another editor

- **Symptom:** Opening Add key or flow from the bar, menu or gesture editor showed the step chips and segments without their outline and fill.
- **Root Cause:** `KeyPicker` opened the key editor from the picker dialog's own context. A dialog theme does not carry the chip and segment styles, so the nested dialog fell back to plain buttons.
- **Fix Applied:** `KeyPicker.addButton` takes the activity context for the editor and the dialog context only for the button itself.
- **Verification:** On the Tab the editor opened from the bar editor shows the same chips as the one opened from Settings.
- **Status:** Fixed.

## BUG-031: The keyboard accessibility service claimed to be a screen reader

- **Symptom:** With the physical keyboard service enabled, apps that look for a screen reader (YouTube and others) read their screens aloud although TalkBack was off. Reported upstream as ClassicOldSong/moonlight-android#609.
- **Root Cause:** `KeyboardAccessibilityService.onServiceConnected` set `FEEDBACK_SPOKEN`, and `res/xml/keyboard_accessibility_service.xml` declared `feedbackAllMask`, which includes spoken feedback.
- **Fix Applied:** Both declare generic feedback. The service only filters key events and gives no feedback of its own.
- **Verification:** `KeyboardServiceFeedbackTest` reads the service XML. The device check needs the service enabled in Accessibility settings, which neither test device has.
- **Status:** Fixed.

## BUG-032: The microphone permission prompt shrank the stream into picture-in-picture

- **Symptom:** Unmuting the microphone for the first time showed the permission prompt and the stream went into picture-in-picture behind it.
- **Root Cause:** `Activity.requestPermissions` starts the prompt through a hidden `startActivityForResult` overload, so `Game`'s override never marked it as the app's own screen, and `onPause` entered picture-in-picture.
- **Fix Applied:** `Game` marks the prompt as its own screen right before `requestPermissions`. `onResume` clears the mark as before.
- **Verification:** Found in the release code review. The device check needs the microphone permission revoked first, since both devices already granted it.
- **Status:** Fixed.

## BUG-033: A key flow kept running after the stream ended

- **Symptom:** A flow in a long Wait step kept its thread and the stream activity alive for up to a minute after the stream ended, then sent its key releases to a closed connection.
- **Root Cause:** `FlowRunner.cancel` only set a flag that is read between steps, and `shutdown` let the running task finish. A run after shutdown threw and left the runner marked as busy.
- **Fix Applied:** The runner keeps the running task, cancels it with an interrupt that ends a wait at once, stops its thread with `shutdownNow`, and refuses new keys after shutdown. Flow actions check that the stream is still connected, and the stream closes its open menus when it is destroyed.
- **Verification:** `FlowStepsTest.cancelEndsALongWaitAtOnceAndShutdownRefusesNewKeys` runs a 60 second wait on a real thread and stops it within 2 seconds with the held key released.
- **Status:** Fixed.

## BUG-034: The profile editor crashed when Android rebuilt it

- **Symptom:** "Unable to start activity EditProfileActivity: could not find Fragment constructor", reported from the S24 after the editor was in the background.
- **Root Cause:** `EditProfileActivity.ProfilePreferenceFragment` only had a constructor with arguments. Android rebuilds fragments by class name after a theme change, a rotation or process death, which needs one without arguments. The rebuilt fragment also asked for the profile during `super.onCreate`, before the activity had loaded it.
- **Fix Applied:** A public constructor without arguments, the profile settings read in `onCreatePreferences`, the profile loaded on first use, and the rebuilt fragment kept instead of replaced.
- **Verification:** `EditProfileRecreateTest` recreates the editor and checks the fragment and its settings.
- **Status:** Fixed.

## BUG-035: Rotating at stream start crashed in onConfigurationChanged

- **Symptom:** NullPointerException on `ControllerHandler.enableSensors()` in `Game.onConfigurationChanged`, seen on the S24 when a stream started from portrait.
- **Root Cause:** The branch meant for leaving picture-in-picture ran on every configuration change that was not picture-in-picture, including the rotation to landscape before the controller handler existed.
- **Fix Applied:** Overlays are restored only when leaving picture-in-picture, and both sensor calls check the handler.
- **Verification:** Code path review. Device check on the S24 starting a stream from portrait.
- **Status:** Fixed.

## BUG-036: One Hand Operation+ ended the stream instead of shrinking it into picture-in-picture

- **Symptom:** With picture-in-picture on, "previous app" through One Hand Operation+ switched apps and ended the stream, while YouTube went into picture-in-picture with the same gesture.
- **Root Cause:** One Hand Operation+ uses `startActivityFromRecents`. With Android 12 auto-enter set, the system hid the stream window before pausing it, so `surfaceDestroyed` stopped the connection and `onPause` found nothing to shrink. A screen without picture-in-picture (the PC list) was paused normally on the same gesture.
- **Fix Applied:** First turned auto-enter off and entered picture-in-picture by hand. That broke the Home swipe (BUG-041), so the final fix keeps auto-enter on and enters by hand in `onTopResumedActivityChanged(false)` for switches auto-enter does not cover, while the stream window is still visible.
- **Verification:** On the Tab the One Hand Operation+ swipe and hold now leaves the stream in picture-in-picture over YouTube, expanding returns to the same stream, and Home still enters picture-in-picture.
- **Status:** Fixed.

## BUG-037: Dialog buttons and menu tiles pressed as a sharp box

- **Symptom:** Tapping Reconnect or OK on the "Connection terminated" dialog, and tiles in the Quick Menu, showed a square ripple that did not match the rounded Material 3 look.
- **Root Cause:** Dialog buttons used the platform borderless button background, and the tiles and grids used `selectableItemBackground`. Both draw an unmasked rectangular ripple.
- **Fix Applied:** Dialog buttons use the rounded `bg_button_text` ripple with a pill shape. Tiles and grid items use `bg_pressable_rounded`, a ripple masked to 16dp corners.
- **Verification:** `DialogButtonShapeTest` builds the stream dialogs in both app styles and checks every button ripple has a rounded mask. Seen on the Tab in the backup dialog, the PC and app grids and the Quick Menu.
- **Status:** Fixed.

## BUG-038: The F13 to F24 row filled the whole key recorder

- **Symptom:** In the key recorder on the Tab, the new F13 to F24 row took the full keyboard height and pushed every other key out of view.
- **Root Cause:** `CustomKeyEditor.addHighFunctionKeys` copied the first row's layout params through the `LinearLayout.LayoutParams(ViewGroup.LayoutParams)` constructor, which drops `layout_weight`. The row kept `match_parent` height without the weight that shares the height between rows.
- **Fix Applied:** The params are copied through the `LinearLayout.LayoutParams` overload, which keeps the weight.
- **Verification:** `CustomKeysTest.highFunctionRowIsOneRowTall` measures the keyboard and fails without the fix. On the Tab the recorder shows all rows, and Ctrl + F13 recorded there reached the PC as Left Ctrl (0xA2) and F13 (0x7C).
- **Status:** Fixed.

## BUG-039: The menu editor showed two rows on a phone in landscape

- **Symptom:** On the S24 in landscape the menu editor showed about two action rows under the style buttons and the hint, and dragging the list scrolled only the list.
- **Root Cause:** The short-screen body is a `NestedScrollView`, but the action list handles the drag first, so the page with the header never scrolled out of the way.
- **Fix Applied:** `ui/PageFirstScrollView` scrolls the page first when the list is dragged up, then the list. A drag down returns the list to its top first and then the page.
- **Verification:** `PageFirstScrollViewTest` and `EditorsSmokeTest.menuEditorScrollsOnAPhoneInLandscape`. On the S24 one drag on the list moves the header away and the list and preview fill the dialog.
- **Status:** Fixed.

## BUG-040: Disabled dialog buttons looked the same as enabled ones

- **Symptom:** In the key editor, Save looked tappable before any key was confirmed, and Clear on the hotkey row looked tappable with no hotkey set.
- **Root Cause:** The dialog and text button styles set one fixed accent color as the text color, with no disabled state, so `setEnabled(false)` changed nothing visible.
- **Fix Applied:** `Dialogs.fitWidth` gives every dialog button a color list with the Material 3 disabled look (38 percent of its color), and the same helper fades in-layout text buttons such as Clear. The list is built in code, since Android 5 and 6 cannot read theme colors from a color list file.
- **Verification:** `DialogButtonShapeTest.disabledDialogButtonsLookDisabled`. On the S24, Save and Clear fade until they can be used.
- **Status:** Fixed.

## BUG-041: The Home swipe paused the stream and the move into picture-in-picture was abrupt

- **Symptom:** Reported on the S24 and the Tab: with picture-in-picture on, swiping up to Home stopped the stream, and when it did shrink it jumped into the small window instead of sliding.
- **Root Cause:** The BUG-036 fix turned Android 12 auto-enter off. A gesture Home then had no swipe-to-picture-in-picture animation and relied on a manual request one frame after `onUserLeaveHint`, which came after the launcher animation (abrupt) or after the window was hidden (stream stopped).
- **Fix Applied:** Auto-enter is on while streaming, so the launcher runs its own swipe animation. `onTopResumedActivityChanged(false)` enters by hand for switches without auto-enter, `onUserLeaveHint` keeps a one-frame fallback, and both use the same checks as `onPause` (screen off, lock screen, split screen, this app's own screens).
- **Verification:** Debug lifecycle logs (`PIPDBG`) on both devices: the Home swipe runs `onSwipePipToHomeAnimationStart` and is already in picture-in-picture when the activity pauses, an app switch enters picture-in-picture, Settings from a stream does not, closing the window with X offers Reconnect, and expanding returns to the same stream.
- **Status:** Fixed.

## BUG-042: Opening Recents ended a picture-in-picture stream for good

- **Symptom:** On the Tab, the Recents gesture showed the stream as a card, and tapping the card went back to the PC list instead of the stream.
- **Root Cause:** Android refuses picture-in-picture while the Recents overview shows the task (every request returned false, with auto-enter on or off), so the hidden surface stopped the connection. The resume mark was only written under the "Resume when reopened" policy and only in `onStop`, after the connection was already gone.
- **Fix Applied:** Under the picture-in-picture policy, `surfaceDestroyed` marks a stream the user left and that could not shrink for resume, and `SessionResumer.maybeResume` now reconnects for every policy except "End the stream" (`BackgroundPolicy.resumesWhenReopened`).
- **Verification:** `SessionTest.onlyTheDisconnectPolicyNeverReconnects`. On the Tab, Recents then tapping the card reconnects to the same app.
- **Status:** Fixed.

## BUG-043: A stray network packet aborted debug builds in the audio thread

- **Symptom:** The debug app on the Tab ended with a native crash (signal 6, thread `AudioRecv`) twice in two days, on 2026-10-21 and 2026-10-23. Release builds were not affected.
- **Root Cause:** The audio socket is bound to a random local port and is not connected, so it accepts any datagram sent to that port, including late packets of an earlier session on a reused port. A datagram whose RTP packet type is neither 97 (audio) nor 127 (FEC) reached `LC_ASSERT_VT(false)` in `RtpAudioQueue.c`, which aborts in debug builds and is compiled out in release builds.
- **Fix Applied:** The audio receive loop drops datagrams of any other type before the queue sees them and logs the first one every second with its length and first eight bytes. `RtpAudioQueue` counts and drops too short packets, bad shard indexes and mismatching FEC headers instead of asserting, and a failed decryption no longer asserts. The submodule `moonlight-common-c` carries the change.
- **Verification:** `scripts/native_tests/audio_queue_foreign_packet.c` aborts with the exact message of the crash report on the old code and passes on the new code (run on the Tab with `run_audio_queue_test.sh`). The log line shows which sender and packet type caused it the next time.
- **Status:** Fixed. The sender of the stray packets is still unknown, and the new log line records it.

## BUG-044: Back needed one extra press after every settings search

- **Symptom:** After tapping a search result in Settings, Back first showed the same Settings again, and every further search added another press before Super Artemis closed.
- **Root Cause:** The search page is added with `addToBackStack`, and `closeSearchPage` only removed the fragment, so each search left one empty back stack entry. Android 13 and later run those entries before the activity finishes.
- **Fix Applied:** `closeSearchPage` pops the search page from the back stack, and `StreamSettings.onBackPressed` lets Back close an open search page on Android 12 and older, which used to end Settings instead.
- **Verification:** `SearchBackStackTest` runs five search cycles and expects an empty back stack.
- **Status:** Fixed.

## BUG-045: The microphone input preview row slid from the bottom of Settings

- **Symptom:** After picking a search result such as Stream, the "Microphone input preview" row appeared at the bottom of the list and moved to its place within a second or two. No other row did.
- **Root Cause:** The level meter rebound its row about 20 times per second, and the default item animator slid any row whose position changed during the two scrolls that bring a search result to the top. The row was always the one in a pending change.
- **Fix Applied:** The settings list has no item animator, the level bar updates in place and only a new status text rebinds the row, and the second scroll follows the first in the same frame.
- **Verification:** `MicrophonePreviewListTest` checks the list has no animator and 100 level updates rebind nothing. The device check is a search for "stream" on the Tab.
- **Status:** Fixed, device check pending.

## BUG-046: Done and Set buttons in the key editor were hard to read

- **Symptom:** The round Done and Set buttons showed a light label on a light fill in every style and color scheme.
- **Root Cause:** A layout `Button` inflated through an AppCompat activity becomes a `MaterialButton`, which tints its own background with the primary color unless `backgroundTint` is cleared. The label color stayed the tonal on-container color.
- **Fix Applied:** `Widget.SuperArtemis.Button` sets `backgroundTint` to null, so the tonal and filled fills keep the roles they were designed with.
- **Verification:** `EditorsSmokeTest.tonalEditorButtonsKeepTheirFillAndStayReadable` failed with the tint `#D0BCFF` before the fix and checks a 4.5 to 1 label contrast in Classic and Material You.
- **Status:** Fixed.

## BUG-047: A key made in the quick bar editor vanished after switching sets

- **Symptom:** Adding a key or flow in the bar editor pinned it, but switching to another set and back, or adding a bar, rebuilt the rows without it, and Save then removed it from the bar.
- **Root Cause:** The editor kept a snapshot of the special keys that was never updated when a key was made. The gesture editor had the same snapshot, so a new key showed as "Deleted custom key" until the editor was reopened.
- **Fix Applied:** A new key joins the editor's own list before rows are rebuilt, the list scrolls to it, and the gesture editor adds it to its options.
- **Verification:** `QuickBarEditorKeysTest` pins a key, switches sets twice and expects it pinned.
- **Status:** Fixed.

## BUG-048: A bar with 20 buttons per line ran off a phone screen

- **Symptom:** The per-line slider allowed 20, which needs 712dp, so buttons past the screen edge were clipped and could not be tapped.
- **Root Cause:** Buttons are a fixed 36dp and the line never wrapped to the screen width. Bars at different anchors were not checked against each other either.
- **Fix Applied:** `BarFit` works out how many buttons fit and the bar wraps there, in the live bars and in the editor preview. The saved number stays as the intent, so a wider screen shows the whole line. The editor says what fits on this screen and offers presets.
- **Verification:** `BarFitTest` and `QuickActionBarFitTest` build a 20 per line bar on a 360dp phone, a landscape phone and a tablet.
- **Status:** Fixed.

## BUG-049: The Lock key (Windows + L) did nothing on the PC

- **Symptom:** The built-in Lock key from a bar did not lock Windows, while Win + L typed on a keyboard did.
- **Root Cause:** Not proven on the host yet. A button held Win and L for 25 ms and sent the Windows key without its own modifier flag, while a keyboard holds the keys for a human length of time and marks the Windows key.
- **Fix Applied:** Shortcuts with a Windows key hold their keys 120 ms and send the Windows key with its modifier flag (`KeyTiming`), for buttons and flows alike. All other shortcuts keep the 25 ms hold.
- **Verification:** `FlowStepsTest` checks the order, the flag and the hold. The device check is the Lock key on the Tab with the PC in front of you.
- **Status:** Fixed, device check pending. If Windows still ignores the injected shortcut, use the Apollo server command in SETUP.md.

## BUG-050: Picture-in-picture started for calls and the Gemini overlay

- **Symptom:** With the picture-in-picture policy, a ringing or answered phone call and the Gemini overlay moved the stream into the small window. The resume policy stayed full screen.
- **Root Cause:** Not fully proven. `onTopResumedActivityChanged(false)`, the leave hint and Android 12 auto-enter all start the window, and none of them checked whether a call was running.
- **Fix Applied:** `PipGuard` refuses the window while the audio mode is a ringtone, a call or a voice chat, and auto-enter is switched off while one runs (`CallModeWatcher`). `PIPDBG` now logs window focus, the audio mode and the screen state to settle the Gemini overlay.
- **Verification:** `PipGuardTest`. The device check is the call and the overlay with `adb logcat | grep PIPDBG`.
- **Status:** Calls are fixed pending the device check. The Gemini overlay is guarded through the assist request that announces it (log of 2026-10-24: `FloatyActivity` opens from the power button, then auto-enter shrinks the stream), and needs one more device check.

## BUG-051: Unlocking the screen did not bring the stream back

- **Symptom:** After the screen turned off and was unlocked, the home screen showed and the stream was gone.
- **Root Cause:** Screen off stops the stream, and only a Home, Recents or app switch marked it for resume.
- **Fix Applied:** A stream that stops while the screen is off is marked for resume under the picture-in-picture and resume policies, so unlocking reconnects it. "End the stream" never reconnects.
- **Verification:** `PipGuardTest.aScreenThatTurnedOffResumesOnlyUnderAResumePolicy`. The device check is power off, wait five seconds, power on and unlock.
- **Status:** Fixed, device check pending.

## BUG-052: The stream screen crashed on open with "Aspect ratio is too extreme"

- **Symptom:** On the Tab, opening a stream ended the app twice with an IllegalArgumentException from `setPictureInPictureParams`, so no stream could start (2026-10-24, debug build with the call guard).
- **Root Cause:** The call guard made `onResume` refresh the picture-in-picture params, and before the first layout the stream view is 0 by 0. Android rejects that ratio, and the exception ended the activity.
- **Fix Applied:** `PipGuard.aspectRatioFor` returns no ratio until the view has a size and keeps any ratio inside 0.43 to 2.38, and `updatePipAutoEnter` catches a rejected value instead of ending the stream.
- **Verification:** `PipGuardTest.aspectRatioStaysInsideWhatAndroidAccepts`. The device check is opening a stream on the Tab.
- **Status:** Fixed, device check pending.

## BUG-053: One Hand Operation+ on the Tab ended the stream with no way back

- **Symptom:** The "previous app" edge swipe moved YouTube to the front and the stream ended, and returning to Super Artemis did not reconnect.
- **Root Cause:** Android hides the stream window and destroys its surface about 1.3 seconds before it calls `onTopResumedActivityChanged`, `onPause` or `onWindowFocusChanged` (log of 2026-10-24, after `startActivityFromRecents`). No leave hint comes either. No app callback runs while the window is still visible, so the stream cannot enter picture-in-picture, and only a leave hint marked a stream for resume.
- **Fix Applied:** A stream whose surface is lost while the screen is on, outside a rotation, Settings or the app's own prompts, and under a policy other than "End the stream", is marked for resume in `surfaceDestroyed` (`PipGuard.shouldResumeAfterSurfaceLoss`), so coming back reconnects as it does after Recents.
- **Verification:** `PipGuardTest.aStreamLostToAnotherAppReconnectsOnReturn`. The device check is the edge swipe, then the Super Artemis card.
- **Status:** Fixed for the way back. Picture-in-picture for this gesture is not possible from the app, and the Home swipe, the Gemini overlay and normal app switches are covered.

## BUG-054: A stream that ended with the screen off was not reconnected on unlock

- **Symptom:** After the screen turned off and was unlocked, the PC list showed instead of the stream (Tab, 2026-10-24).
- **Root Cause:** The stream ending brought the PC list to the front while the screen was still off, and its resume check took the saved mark and started the reconnect 35 ms after the screen went off. The reconnect had nobody to show it to, and nothing was left to resume at unlock.
- **Fix Applied:** `SessionResumer.maybeResume` keeps the mark while the screen is off or locked, and the lists resume again at unlock, which reconnects the stream.
- **Verification:** `SessionTest.aMarkWaitsUntilTheScreenIsOnAndUnlocked`. The device check is power off, wait five seconds, power on, unlock.
- **Status:** Fixed, device check pending.

### BUG-055: Gemini share dialogs shrink the stream

The Gemini share button opens an account picker over the stream, 5 seconds after the assistant call, and the manual picture-in-picture entry shrank the stream. The assistant hold now lasts until the stream is the top app again, up to 120 seconds (`PipGuard.ASSISTANT_HOLD_MS`). Dialogs from other apps that appear with no assistant call are not covered yet, because a Recents gesture and a dialog look the same in the log (no leave hint, focus lost). Needs a device check.

Device check, 2026-10-26: the stream stayed full screen on the account picker. An incoming call on the Tab (10:15, ringing for 5 seconds, then a leave hint) shrank the stream with the audio mode still normal, because Samsung does not set a call mode while the phone rings. Detecting that needs the READ_PHONE_STATE permission, so it is left as is until the owner decides.

### BUG-056: Dialogs of other apps and a locked screen

Entering picture-in-picture from onTopResumedActivityChanged and onPause shrank the stream for any overlay, and the screen-off mark was skipped when the display still reported interactive behind the lock screen (Tab log, 2026-10-26). Android 12 and later now shrink only through auto-enter and the leave hint, and the screen-off mark also counts a locked keyguard. Recents without a leave hint is the case to re-check on a device. A debug-only KEYDBG log in NvConnection.sendKeyboardInput traces Win+L from the on-screen keyboard.

### BUG-057: Win+L from a stream does nothing on Windows

The Tab sends Win down, L down, L up and Win up with the Windows modifier (KEYDBG log), and Windows still does not lock, because it ignores an injected Win+L. A local `keybd_event` test on the host gave the same result, and an AutoHotkey `#l` hotkey locks for the injected keys. Options and sources are in docs/lock-key-options.md. The Settings row Lock key does nothing? shows the script.

### BUG-058: Unit tests failed at random and a full run took up to 50 minutes

The test JVM ran with Gradle's default 512 MB heap. Robolectric with this many layouts and the 235 icon catalogue filled it, the JVM thrashed in garbage collection and finally died with an OutOfMemoryError, so single tests such as the quick bar editor, the style dialogs and the stream view tests appeared to fail depending on the run. Setting `unitTests.all { maxHeapSize = "2g" }` in `app/build.gradle` fixed it. The full suite now runs 345 tests in about 5 minutes with no failures.
