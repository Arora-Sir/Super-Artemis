# Super Artemis architecture

Super Artemis is a fork of [Artemis](https://github.com/ClassicOldSong/moonlight-android) (itself a fork of Moonlight for Android). This document describes how the fork is put together today, which parts are upstream code, which parts are new, and why the non-obvious choices were made. Planned work lives in [CONTEXT.md](CONTEXT.md), not here.

## Layers

```
Android UI (Java, androidx)
  PcView / AppView ............ host and app lists
  StreamSettings .............. settings screen (PreferenceFragmentCompat, search)
  backup/ ..................... backup and restore dialogs, file format, merge and replace strategies
  Game ........................ the stream activity (input, overlays, in-stream menu, mic lifecycle)
        |
Java stream layer
  nvstream/NvConnection ....... connection setup, pairing, launch and resume
  nvstream/StreamConfiguration  per-stream options (resolution, codec, mic, control timeout)
  nvstream/jni/MoonBridge ..... JNI surface into the native core
        |
Native (app/src/main/jni/moonlight-core)
  callbacks.c, simplejni.c .... JNI glue, Opus mic encoder thread
  moonlight-common-c .......... git submodule: Arora-Sir/moonlight-common-c, branch super-artemis
        |
Host: Apollo (RTSP, video, audio, control stream over ENet, mic stream over UDP)
```

## Modules added or changed by the fork

| Area | Files | Responsibility |
| :--- | :--- | :--- |
| Microphone capture | `binding/audio/MicrophoneCaptureManager.java` | Opens AudioRecord (UNPROCESSED, then VOICE_RECOGNITION, then MIC), runs the `MicStreamCapture` thread and feeds PCM to the native encoder |
| Microphone state | `binding/audio/MicrophoneStateStore.java`, `preferences/PreferenceConfiguration.java` | Resolves the "Microphone at stream start" setting (off, on, follow last state) and remembers the last manual state per host |
| Microphone lifecycle | `Game.java` (`applyMicrophoneInitialState`, `toggleMicrophoneCapture`) | Starts capture after the connection is up when the setting asks for it, and pauses or resumes capture from the in-stream menu |
| Native mic path | `jni/moonlight-core/callbacks.c`, submodule `MicrophoneStream.c` | Opus encoding on a native worker and the negotiated mic stream to the host |
| Control stream timeout | `StreamConfiguration`, `MoonBridge.startConnection`, `callbacks.c`, submodule `ControlStream.c` | Passes the "Network drop grace period" to the ENet peer timeout |
| Stream actions | `actions/StreamAction.java`, `actions/StreamActionRegistry.java`, `actions/StreamActionHost.java`, `GameActionHost.java` | One catalog of everything a user can trigger in a stream, with stable IDs, live availability, on/off state and labels. Menus, the quick-action bar and gestures run actions through `StreamActionHost`, which `GameActionHost` implements on top of `Game` |
| In-stream menu | `GameMenu.java` (facade), `menu/GameMenuLayout.java`, `menu/GameMenuRenderer.java`, `menu/MenuSections.java`, `menu/GameMenuEditor.java`, `res/layout/game_menu_*.xml` | The sectioned Quick Menu (pinned actions, Session in red, Menu), the Advanced Menu with "Back to Quick Menu", one RecyclerView renderer for the list, compact and grid styles, and the editor with the only style picker (List, Grid, Compact) next to a live preview |
| Quick-action bars | `menu/QuickActionBar.java`, `menu/QuickBarLayout.java`, `menu/QuickBarEditor.java`, hooks in `Game.java` (`initQuickActionBar`, `connectionStarted`, picture-in-picture) | One bar per screen corner, each with its own items and direction (a row or a column that starts in the corner). Toggles show green while on, bars fade to 40% after 3 seconds idle and hide in picture-in-picture. A long press on a button opens `GameMenu.showQuickBarMenu` for that corner (add a key straight onto the bar, pin a key, edit the bar, manage keys), and the Quick Menu opens the editor directly. `QuickBarLayout` migrates the single-bar settings and the old overlay zoom button once |
| Special keys | `customkeys/` package (`BuiltInKeys`, `SpecialKeys`, `SpecialKeysManager`, `CustomKeyStore`, `CustomKeyEditor`, `IconPicker`, `KeyNames`, `IconCatalog`), `res/layout/dialog_custom_key_editor.xml` | One list of built-in keys (stable `builtin_` IDs, default icons, hide, re-icon, reorder, reset) and recorded custom keys (names, sticky, 61 icons). Custom keys stay in the `specialPrefs` JSON so import, export, backup and the on-screen key layer keep working, and the built-in key state sits next to it |
| Settings screen | `preferences/StreamSettings.java` (`setupSearchBar`, `onSearchResultClicked`), `preferences/SettingsSections.java`, `preferences/SettingsListStyle.java`, `preferences/AppStylePreference.java`, `res/layout/activity_stream_settings.xml`, `res/xml/preferences.xml` | A search bar fixed above the list opens SearchPreference in its own overlay, `search:keywords` let everyday words find settings, collapsed sections expand before a result is highlighted. Wide screens get a section list that jumps to and follows the current section. Material You draws each section as one rounded container |
| Backup and restore | `backup/` package, `res/layout/dialog_backup_options.xml`, `res/layout/dialog_restore_preview.xml` | Exports and imports one `.sabackup` file with typed SharedPreferences, profiles, the PC list and optional pairing keys, saved through the file picker or shared. `BackupCrypto` encrypts it with a password (AES-256-GCM, PBKDF2-HMAC-SHA256, authenticated header), `BackupOpenActivity` opens files from other apps, `RestoreSelection` holds the parts the user ticked and `DeviceCompatibility` lists settings the device cannot use. `BackupRestoreController` runs the dialogs and file pickers, `BackupRestoreManager` moves bytes, `BackupCodec` owns the format and validation (`minReaderVersion` for newer files), the `*Snapshot` classes capture and restore one kind of data each, `RestoreStrategy` decides merge or replace, and `RestorePreview` describes the outcome before anything is written |
| Decoder | `binding/video/MediaCodecHelper.java` (`applyFrameworkLowLatencyPostStart`), `MediaCodecDecoderRenderer.java` | Requests framework low latency again after the codec starts (Android 11+) |
| Stylus | `ui/StreamView.java` (`init`) | Disables automatic handwriting on Android 13+ so stylus strokes reach the host |
| App style | `theme/ThemeManager.java`, `theme/AppStylePicker.java`, `AppTheme.MaterialYou` and `ThemeOverlay.SuperArtemis.Classic` in `res/values/styles.xml`, `res/layout/preference_widget_material_switch.xml`, hooks in the list and settings activities and `GameMenu.themedContext` | Classic adds neutral color roles and the rounded dialog to each activity theme. Opt-in Material You on Android 12+ swaps in dynamic colors, settings switches and rounded sections. The picker shows both styles with their real colors and controls. Activities apply the style before inflating and recreate when it changed while they were in the background |
| Leaving the stream | `session/BackgroundPolicy.java`, `session/ResumeStore.java`, `session/SessionResumer.java`, hooks in `Game.java` (`onUserLeaveHint`, `onStop`, `rememberSession`), `PcView.java` and `AppView.java` (`onResume`) | Disconnect, resume when reopened (pending resume for 30 minutes, relaunched through `ShortcutTrampoline`) or picture-in-picture, plus connecting to the last PC on app start |
| Gestures | `gestures/` package (`GestureBindingStore`, `MultiFingerGestureRecognizer`, `MultiFingerTapDetector`, `GestureDispatcher`, `GestureEditor`), hook at the top of the finger branch in `Game.handleMotionEvent` | Maps 3, 4 and 5-finger taps, double taps and holds (0.5, 1 or 2 s) and 3 and 4-finger swipes to an action, a special key, a key combo, the Quick Menu, a middle click or Nothing. The recognizer runs before every touch mode, so gestures work in pan and zoom too. Fingers must land within 250 ms. Swipes and holds claim their finger count at landing, and a tap is claimed at the first finger lift, before the two-finger touch modes can read the lifts as a right click |
| Diagnostics | `diagnostics/CrashReporter.java`, `ui/SafeRun.java`, hooks in `ArtemisApplication` and `PcView.completeOnCreate` | Saves a report for a Java crash (stack trace and the app's own log through `logcat --pid`), reads native crashes and ANRs from `ApplicationExitInfo` on Android 11+, offers the report once on the next launch, and sends logs to a new or existing GitHub issue, Downloads, any app or the clipboard. SafeRun guards menu entries, bar buttons, gestures and Settings editors so an error is logged instead of ending the stream |
| Credits | `res/raw/credits.json`, `scripts/render_credits.py`, `about/CreditsRepository.java` | One credits source rendered into the README, the release notes and the in-app credits |
| Design foundation | `ui/Dialogs.java`, `ui/FlowRow.java`, `res/values/attrs_superartemis.xml`, `res/values/styles.xml` (dialog themes, type scale, buttons), `res/drawable/bg_*.xml` | Color roles (`saSurfaceContainerHigh`, `saPrimary` and so on) filled with greys in Classic and wallpaper colors in Material You, one rounded centered dialog theme for both styles, one type scale, dialog headers with an icon, and width rules (screen width minus margins on phones, 560 dp or 840 dp on tablets). Switches, rounded text fields and segments share styles, every scrolling dialog area gets Material 3 scroll dividers from `Dialogs.fitWidth`, and `Dialogs.isCompact` stacks side by side content under 600 dp |
| About and support | `about/` package (`Links`, `SupportDialog`, `AboutDialogs`, `AboutSection`), `res/layout/dialog_support.xml`, `res/drawable-nodpi/support_upi_qr.png`, `res/drawable/ic_brand_*.xml` | Settings > About: a Support dialog (UPI card with QR, ID and UPI app button, PayPal), developer links with their icons, credits as cards with link chips, source, issues, privacy and version. Every external link of the fork lives in `Links` |
| Packaging and CI | `app/build.gradle`, `.github/workflows/build-unified.yml`, `.github/workflows/docs-check.yml`, `scripts/setup-release-signing.ps1` | Package IDs `com.mohitarora.superartemis` (debug `.debug`), `SuperArtemis-<version>-<abi>-<buildType>.apk` names, environment-driven release signing, signed arm64 release builds and the credits check |

## Data and state

| Store | Location | Contents |
| :--- | :--- | :--- |
| Default SharedPreferences | `shared_prefs/<applicationId>_preferences.xml` | Every setting on the settings screen, the Quick Menu layout and style, the quick-action bar items and direction per corner, gesture bindings |
| Named SharedPreferences | `HiddenApps`, `specialPrefs`, `OSC`, keyboard layout files, `MicrophoneState` | Hidden apps per host, custom keys JSON (`{data:[{id, name, sticky, icon, keys}]}` with keys as Windows VK hex) with the hidden built-in keys, their icons and the key order, on-screen control layouts, per-host last mic state |
| Device-only SharedPreferences | `DecoderTombstone`, `GlPreferences`, `SessionResume`, `CrashReporter` | Crash counters, GPU fingerprint, the last stream with its pending resume time and the newest exit record already offered, never backed up |
| Diagnostics | `files/diagnostics/last-crash.txt`, `files/diagnostics/handled-errors.txt` | The last crash report until it is offered, and errors SafeRun caught, both included in the diagnostic log |
| Profiles | `files/profiles/profiles.json` | Settings profiles, overlaid on the default prefs by `ProfilesManager` |
| Paired PCs | `databases/computers4.db` via `ComputerDatabaseManager` | UUID, name, addresses, MAC and server certificate per PC |
| Client identity | `files/uniqueid`, `files/client.crt`, `files/client.key` | The pairing identity the host trusts |

## Control flows

### Stream start and microphone negotiation

```
Game.onCreate -> StreamConfiguration (enableMicrophone, controlPeerTimeoutMs)
  -> NvConnection.start -> MoonBridge.startConnection -> LiStartConnection (native)
     RTSP DESCRIBE: host advertises opus mic support
     RTSP SETUP/PLAY streamid=mic (only when the mic setting is on and the host supports it)
  -> connectionStarted -> Game.applyMicrophoneInitialState
     off ............... nothing starts (no AudioRecord, no encoder, no privacy indicator)
     on ................ MicrophoneCaptureManager.startStreaming
     follow last state . MicrophoneStateStore.getLastState(host) decides
```

### Mute and unmute during a stream

```
Quick Menu "Unmute microphone" -> Game.toggleMicrophoneCapture
  -> RECORD_AUDIO granted? (otherwise request, resume in onRequestPermissionsResult)
  -> MicrophoneCaptureManager.startStreaming -> AudioRecord + native Opus encoder start
Quick Menu "Mute microphone" -> MicrophoneCaptureManager.stop
  -> AudioRecord released, encoder cleaned up, mic UDP socket stays open for the session
Manual toggles write the per-host state used by "Follow last state".
```

### In-stream menu

```
Back key / floating button / a gesture bound to the Quick Menu (five-finger tap by default) -> GameMenu.showMenu
  -> StreamActionRegistry actions, filtered by isAvailable(GameActionHost), labels from the live state
  -> Quick Menu   Actions: pinned IDs in user order
                  Session: Disconnect and Quit in red (always present, not configurable)
                  Menu:    Advanced Menu, Customize menu, Cancel
  -> Advanced Menu  Actions: every available action not pinned
                    Controller: input device options
                    Menu: Back to Quick Menu, Cancel
  -> GameMenuRenderer draws list, compact or grid in one RecyclerView (style read on every open)
Customize menu -> GameMenuEditor: style picker, drag to reorder, live preview from the same renderer
```

### Backup and restore

```
Backup: BackupRestoreController dialog (pairing keys switch off by default, password required with keys)
  -> Save to device (SAF CreateDocument) or Share (FileProvider, ACTION_SEND)
  -> BackupCodec.create: typed prefs of every non-excluded file + profiles.json + PC rows
     + SHA-256 of the public client certificate (+ identity only if opted in)
  -> BackupCrypto.encrypt when a password is set (PBKDF2 310,000 rounds, AES-256-GCM, header as AAD)
Restore: SAF OpenDocument or BackupOpenActivity (a .sabackup opened from another app)
  -> password prompt for an encrypted file -> BackupCodec.parse validates the whole file (nothing written yet)
  -> preview: one tick per part (settings, layouts, profiles, PCs, pairing keys) and DeviceCompatibility issues
  -> user confirms -> RestoreStrategy.forBackup with the RestoreSelection
     no keys:   MergeHostsStrategy  prefs replaced, HiddenApps and MicrophoneState merged (local wins),
                                    local PCs and identity kept, backup PCs with new UUIDs added
     with keys: ReplaceAllStrategy  prefs mirrored, PC list replaced, identity replaced
  -> process restart so ComputerManagerService, IdentityManager and ProfilesManager reload
```

## Inter-component communication

- Java to native goes through `MoonBridge` static native methods. Native to Java goes through the `BridgeConnListenerCallbacks` and renderer callbacks in `callbacks.c`.
- Settings reach the stream through `PreferenceConfiguration.readPreferences`, which reads the profile overlay on top of the default SharedPreferences.
- The host is reached over RTSP (setup), ENet (control stream, input), UDP (video, audio, mic) and HTTPS (pairing, app list, server commands).
- Host-side automation on connect and disconnect belongs to Apollo's per-client Do and Undo commands, which Apollo runs on every session start and stop.

## Constraints and trade-offs

- The mic stream can only be negotiated during the RTSP handshake, so "off at start" means negotiated but idle, not absent. Turning the mic setting off entirely skips negotiation.
- The control stream timeout only helps if the host's own ping timeout is at least as long.
- Upstream files mix CRLF and LF line endings. Edits preserve each file's existing style so diffs against upstream stay small.
- Five upstream Robolectric tests fail on untouched upstream code as well (see [BUGS.md](BUGS.md)).

## Key Design Decisions

| Decision | Rationale |
| :--- | :--- |
| Combine the native mic and timeout changes in our own `moonlight-common-c` fork (branch `super-artemis`) by cherry-picking | Two upstream sources (logabell's three mic commits and KAST's timeout commit) apply cleanly on the Artemis core, and cherry-picks keep the original authors on every commit |
| Pause and resume capture instead of renegotiating the mic | The mic stream is set up only at connect, while stopping AudioRecord and the encoder already removes the battery cost and the privacy indicator |
| "Always off" is the default mic start state | Most sessions never use the mic, so the safe and low-power behaviour is the default |
| Do not port derflacco's Sync LFR | It depends on derflacco's rewritten render loop, and Artemis already ships its own latest-frame LFR option |
| Do not port CPU affinity or sustained performance mode | Their author removed or reverted them, and decoding runs in the media codec process the app cannot pin |
| Keep the Qualcomm fence keys as they are | They are already in upstream Artemis with the same gating |
| Menu actions have stable IDs and run by position | Allows reordering and hiding, and two items with the same label can no longer trigger each other |
| Anything left out of the Quick Menu stays in the Advanced Menu | No action can become unreachable through customization |
| Disconnect and Quit sit in a fixed, red Session section of the Quick Menu | They end the stream, so they must always be one tap away and never be mistaken for ordinary actions |
| One action registry behind the menu, the quick-action bar and gestures | An action is defined once, so labels, icons, availability and behaviour cannot drift between the places that trigger it |
| `GameActionHost` adapts `Game` instead of `Game` implementing the interface | Keeps the upstream `Game.java` untouched, which keeps future upstream merges small |
| The UPI QR code is a bundled image generated from the same payment link the app opens | No QR library in the app, and the image was verified by decoding it back to the link |
| Support never prompts on its own | It only opens from Settings > About, so it never interrupts a stream |
| Upstream performance log sharing is hidden, not removed | It sends data to a dataset this fork does not run, while hiding keeps the upstream code intact for merges |
| Stale translations of changed strings are removed instead of kept | The English text is accurate, and an outdated translation would describe upstream behaviour |
| Material You turns checkboxes into switches through the preference theme | No preference XML changes, so upstream merges of `preferences.xml` stay simple and Classic is untouched |
| Framework dialogs get one Super Artemis dialog theme in both styles | Dozens of upstream dialogs use `android.app.AlertDialog`, so one `android:alertDialogTheme` gives all of them rounded corners, centered placement and matching colors. The device default (One UI) anchored dialogs to the bottom and kept them narrow on tablets |
| Resume relaunches through `ShortcutTrampoline` instead of storing the stream intent | The intent holds a certificate byte array and a list that cannot be persisted, and the trampoline already binds the PC service, waits for the PC and resolves the app |
| Only Home, Recents and quick switches mark a stream for resume | `onUserLeaveHint` is not called for Disconnect, Quit, Back or the app's own dialogs, so those never reconnect by surprise |
| The picture-in-picture fallback runs only when auto-enter did not start | Android 12+ auto-enter gives the smoother animation, and the fallback covers launchers that skip it on a quick switch |
| A single tap waits for the double tap window only when that finger count has a double tap bound | Users without double taps keep instant single taps |
| Gestures bound to Nothing are passed to the PC instead of being swallowed | Games that use three-finger touches keep working, which is what ClassicOldSong/moonlight-android#540 asks for |
| The quick-action bar never offers Disconnect or Quit | A stray tap on an always-visible button must not end the stream |
| Quick-action bars are anchored to corners, one per corner, instead of being dragged | Fixed corners never cover the floating menu button and survive rotation, and a bar per corner lets each corner hold the buttons that make sense there |
| Custom keys keep the upstream JSON format and only add an optional `icon` field | Files from the upstream wiki, imports, exports and backups keep working in both directions |
| Built-in special keys get stable `builtin_` IDs and a small state store instead of being copied into the custom keys JSON | Hiding, re-icon and order stay reversible with one reset, and the upstream JSON format and the on-screen layer positions stay untouched |
| A custom key without an ID gets its list position as ID on first save | The on-screen key layer already names such keys `custom_<position>`, so their saved positions survive the switch to the editor |
| One RecyclerView renderer for every menu style and the editor preview | The preview cannot disagree with the real menu, and D-pad focus works the same in every style |
| Colors come from theme roles, never raw values | The same layouts render correctly in Classic and Material You, and a unit test inflates every new layout in both |
| Removed duplicate settings are migrated, not dropped | Users keep what they had: hidden default keys, the zoom overlay button as a Pan/Zoom pin, and the menu style in the editor |
| Held keys are released when the built-in keyboard hides, by default | A key held out of sight stays pressed on the PC with nothing on screen to show it. The option stays for Shift-click style use |
| Multi-finger gestures do not depend on Back Menu | Back Menu only describes the physical back key, so turning it off must not remove gesture bindings |
| Gestures are recognized on the stream view before any touch mode, not on the whole window | Touches on on-screen controls and the keyboard stay with those views, so held virtual buttons can never form a gesture, while pan and zoom, trackpad and direct touch all get the same gestures |
| Swipes claim their finger count when the fingers land, taps only at the end | A bound swipe must not move anything on the PC first, while a tap binding should not take three-finger drags away from apps that use them |
| The floating menu button does not depend on Back Menu either | It opens the Quick Menu by itself, so it is useful exactly when Back Menu is off |
| Settings search uses exact matching and expands collapsed categories first | Fuzzy matching ranked loosely related settings first, and the library cannot scroll to items hidden in a collapsed group |
| Backups are settings-only by default, pairing keys are opt-in | A file with keys can connect to the user's PCs as this device, so including them must be a deliberate choice |
| Restore validates the whole file and shows a preview before writing, then restarts the process | A half-applied or surprising restore is worse than none, and long-lived services cache hosts and identity |
| Restore only writes allowlisted files | A crafted backup cannot write outside the expected paths |
| A backup without pairing keys merges PCs, a backup with keys replaces them | Without keys this device keeps its own identity, so its PCs stay valid and must survive. With keys the device becomes the backed-up client, so its PC list must match that identity |
| Per-PC files keep local values on a merge | Hidden apps and the last mic state belong to PCs this device keeps, so local choices win and only missing entries come from the backup |
| Backups record a fingerprint of the public client certificate | The preview can tell whether added PCs work as is or need pairing again, without storing any secret |
| The restore preview counts saved PCs and never calls them paired | `computers4.db` also holds PCs found by LAN discovery, and pair state is only known after asking each host |
| Credits live in one JSON file | README, release notes and the app always agree, and CI catches drift |
| Upstream changes are tracked from a reviewed commit per source, with a weekly report | Nothing slips between reviews, and the report starts exactly where the last review ended |
| Backups are encrypted with a password, never hashed | A hash cannot be reversed, so it cannot protect a file that must be restored. Encryption with a password-derived key does, and the header is authenticated so a weaker iteration count cannot be swapped in |
| Restore asks per part and keeps settings the device cannot use | A tablet backup restored on a phone must not set a resolution the phone cannot show, so those settings keep the local value unless ticked |
| A gesture tap is claimed at the first finger lift | The touch modes track only two fingers, so lifts that reach them turn every 3-finger tap into a right click |
| Crash reports are offered for sharing and never uploaded automatically | Users without root, Shizuku or a PC can report a crash, and the user picks where each report goes |
| SafeRun guards user actions, tests guard layouts | A guard can only catch errors during the call, so view casts and layouts are checked by `scripts/check_view_casts.py` and `EditorsSmokeTest` instead |
| Upstream contributions are small bug fixes only | The upstream maintainer declined menu reordering and Material 3 pending a UI rewrite |
