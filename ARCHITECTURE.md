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
| In-stream menu | `GameMenu.java` (facade), `menu/GameMenuLayout.java`, `menu/GameMenuRenderer.java`, `menu/MenuSections.java`, `menu/GameMenuEditor.java`, `res/layout/game_menu_*.xml` | The sectioned Quick Menu (pinned actions, Session in red, Menu), the Advanced Menu with "Back to Quick Menu", one RecyclerView renderer for the list, compact and grid styles, and the editor with a style picker and live preview |
| Quick-action bar | `menu/QuickActionBar.java`, `menu/QuickBarLayout.java`, `menu/QuickBarEditor.java`, hooks in `Game.java` (`initQuickActionBar`, `connectionStarted`, picture-in-picture) | Small round buttons over the stream for pinned actions and custom keys, in the corner the user picks. Toggles show green while on, the bar fades to 40% after 3 seconds idle, and it hides in picture-in-picture |
| Custom keys | `customkeys/` package (`CustomKeyStore`, `CustomKeyEditor`, `CustomKeysManager`, `KeyNames`, `IconCatalog`), `res/layout/dialog_custom_key_editor.xml` | Records key combinations on the built-in keyboard (or a physical one), with names, sticky and 28 icons, stored in the existing `specialPrefs` JSON so import, export, backup and the on-screen key layer keep working |
| Settings search | `preferences/StreamSettings.java` (`setupSearch`, `onSearchResultClicked`), `res/xml/preferences.xml` | Indexes `preferences.xml` with SearchPreference and expands collapsed categories before highlighting a result |
| Backup and restore | `backup/` package, `res/layout/dialog_backup_options.xml`, `res/layout/dialog_restore_preview.xml` | Exports and imports one JSON file with typed SharedPreferences, profiles, the PC list and optional pairing keys. `BackupRestoreController` runs the dialogs and file pickers, `BackupRestoreManager` moves bytes, `BackupCodec` owns the format and validation, the `*Snapshot` classes capture and restore one kind of data each, `RestoreStrategy` decides merge or replace, and `RestorePreview` describes the outcome before anything is written |
| Decoder | `binding/video/MediaCodecHelper.java` (`applyFrameworkLowLatencyPostStart`), `MediaCodecDecoderRenderer.java` | Requests framework low latency again after the codec starts (Android 11+) |
| Stylus | `ui/StreamView.java` (`init`) | Disables automatic handwriting on Android 13+ so stylus strokes reach the host |
| App style | `theme/ThemeManager.java`, `AppTheme.MaterialYou` in `res/values/styles.xml`, `res/layout/preference_widget_material_switch.xml`, hooks in the list and settings activities and `GameMenu.themedContext` | Opt-in Material You on Android 12+ with dynamic colors, settings switches and rounded dialogs. Activities apply it before inflating and recreate when it changed while they were in the background |
| Leaving the stream | `session/BackgroundPolicy.java`, `session/ResumeStore.java`, `session/SessionResumer.java`, hooks in `Game.java` (`onUserLeaveHint`, `onStop`, `rememberSession`), `PcView.java` and `AppView.java` (`onResume`) | Disconnect, resume when reopened (pending resume for 30 minutes, relaunched through `ShortcutTrampoline`) or picture-in-picture, plus connecting to the last PC on app start |
| Gestures | `gestures/` package (`GestureBindingStore`, `MultiFingerTapDetector`, `GestureDispatcher`, `GestureEditor`), hooks in `Game.java` (`handleMultiTouchGesture` and the trackpad up handler) | Maps 3, 4 and 5-finger taps and double taps to an action, a custom key, the Quick Menu, a middle click or Nothing. Unbound finger counts are not consumed, so they reach the PC |
| Credits | `res/raw/credits.json`, `scripts/render_credits.py` | One credits source rendered into the README and release notes, and later into the app |
| Packaging and CI | `app/build.gradle`, `.github/workflows/build-unified.yml`, `.github/workflows/docs-check.yml`, `scripts/setup-release-signing.ps1` | Package IDs `com.mohitarora.superartemis` (debug `.debug`), `SuperArtemis-<version>-<abi>-<buildType>.apk` names, environment-driven release signing, signed arm64 release builds and the credits check |

## Data and state

| Store | Location | Contents |
| :--- | :--- | :--- |
| Default SharedPreferences | `shared_prefs/<applicationId>_preferences.xml` | Every setting on the settings screen, the Quick Menu layout and style |
| Named SharedPreferences | `HiddenApps`, `specialPrefs`, `OSC`, keyboard layout files, `MicrophoneState` | Hidden apps per host, custom keys JSON (`{data:[{id, name, sticky, icon, keys}]}` with keys as Windows VK hex), on-screen control layouts, per-host last mic state |
| Device-only SharedPreferences | `DecoderTombstone`, `GlPreferences`, `SessionResume` | Crash counters, GPU fingerprint and the last stream with its pending resume time, never backed up |
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
Back key / five-finger tap / floating button -> GameMenu.showMenu
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
Backup: BackupRestoreController dialog (pairing keys card, off by default) -> SAF CreateDocument
  -> BackupCodec.create: typed prefs of every non-excluded file + profiles.json + PC rows
     + SHA-256 of the public client certificate (+ identity only if opted in)
Restore: SAF OpenDocument -> BackupCodec.parse validates the whole file (nothing written yet)
  -> RestorePreview: date, app version, counts, PCs kept and added, whether pairing keys are replaced
  -> user confirms -> RestoreStrategy.forBackup
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
| Upstream performance log sharing is hidden, not removed | It sends data to a dataset this fork does not run, while hiding keeps the upstream code intact for merges |
| Stale translations of changed strings are removed instead of kept | The English text is accurate, and an outdated translation would describe upstream behaviour |
| Material You turns checkboxes into switches through the preference theme | No preference XML changes, so upstream merges of `preferences.xml` stay simple and Classic is untouched |
| Framework dialogs use the device default dialog theme under Material You | Dozens of upstream dialogs use `android.app.AlertDialog`, and the device default is the rounded system style on Android 12+ without touching each dialog |
| Resume relaunches through `ShortcutTrampoline` instead of storing the stream intent | The intent holds a certificate byte array and a list that cannot be persisted, and the trampoline already binds the PC service, waits for the PC and resolves the app |
| Only Home, Recents and quick switches mark a stream for resume | `onUserLeaveHint` is not called for Disconnect, Quit, Back or the app's own dialogs, so those never reconnect by surprise |
| The picture-in-picture fallback runs only when auto-enter did not start | Android 12+ auto-enter gives the smoother animation, and the fallback covers launchers that skip it on a quick switch |
| A single tap waits for the double tap window only when that finger count has a double tap bound | Users without double taps keep instant single taps |
| Gestures bound to Nothing are passed to the PC instead of being swallowed | Games that use three-finger touches keep working, which is what ClassicOldSong/moonlight-android#540 asks for |
| The quick-action bar never offers Disconnect or Quit | A stray tap on an always-visible button must not end the stream |
| The quick-action bar is anchored to a chosen corner instead of being dragged | Four fixed corners never cover the floating menu button, survive rotation, and need no saved coordinates |
| Custom keys keep the upstream JSON format and only add an optional `icon` field | Files from the upstream wiki, imports, exports and backups keep working in both directions |
| A custom key without an ID gets its list position as ID on first save | The on-screen key layer already names such keys `custom_<position>`, so their saved positions survive the switch to the editor |
| One RecyclerView renderer for every menu style and the editor preview | The preview cannot disagree with the real menu, and D-pad focus works the same in every style |
| Five-finger tap has its own setting | Back Menu only describes the physical back key |
| Settings search uses exact matching and expands collapsed categories first | Fuzzy matching ranked loosely related settings first, and the library cannot scroll to items hidden in a collapsed group |
| Backups are settings-only by default, pairing keys are opt-in | A file with keys can connect to the user's PCs as this device, so including them must be a deliberate choice |
| Restore validates the whole file and shows a preview before writing, then restarts the process | A half-applied or surprising restore is worse than none, and long-lived services cache hosts and identity |
| Restore only writes allowlisted files | A crafted backup cannot write outside the expected paths |
| A backup without pairing keys merges PCs, a backup with keys replaces them | Without keys this device keeps its own identity, so its PCs stay valid and must survive. With keys the device becomes the backed-up client, so its PC list must match that identity |
| Per-PC files keep local values on a merge | Hidden apps and the last mic state belong to PCs this device keeps, so local choices win and only missing entries come from the backup |
| Backups record a fingerprint of the public client certificate | The preview can tell whether added PCs work as is or need pairing again, without storing any secret |
| The restore preview counts saved PCs and never calls them paired | `computers4.db` also holds PCs found by LAN discovery, and pair state is only known after asking each host |
| Credits live in one JSON file | README, release notes and the app always agree, and CI catches drift |
| Upstream contributions are small bug fixes only | The upstream maintainer declined menu reordering and Material 3 pending a UI rewrite |
