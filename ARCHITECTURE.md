# Super Artemis architecture

Super Artemis is a fork of [Artemis](https://github.com/ClassicOldSong/moonlight-android) (itself a fork of Moonlight for Android). This document describes how the fork is put together today, which parts are upstream code, which parts are new, and why the non-obvious choices were made. Planned work lives in [CONTEXT.md](CONTEXT.md), not here.

## Layers

```
Android UI (Java, androidx)
  PcView / AppView ............ host and app lists
  StreamSettings .............. settings screen (PreferenceFragmentCompat, search, backup/restore)
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
| In-stream menu | `GameMenu.java`, `GameMenuLayout.java`, `GameMenuEditor.java`, `res/layout/game_menu_*.xml` | Catalog of menu actions with stable IDs, the user-ordered Quick Menu, list/compact/grid rendering and the drag-to-reorder editor |
| Settings search | `preferences/StreamSettings.java` (`setupSearch`, `onSearchResultClicked`), `res/xml/preferences.xml` | Indexes `preferences.xml` with SearchPreference and expands collapsed categories before highlighting a result |
| Backup and restore | `preferences/BackupRestoreManager.java`, `StreamSettings.java` (SAF launchers), `res/layout/dialog_backup_options.xml` | Exports and imports one JSON file with typed SharedPreferences, profiles, the PC list and optional pairing keys |
| Decoder | `binding/video/MediaCodecHelper.java` (`applyFrameworkLowLatencyPostStart`), `MediaCodecDecoderRenderer.java` | Requests framework low latency again after the codec starts (Android 11+) |
| Stylus | `ui/StreamView.java` (`init`) | Disables automatic handwriting on Android 13+ so stylus strokes reach the host |
| Gestures | `Game.java` (`handleMultiTouchGesture` and the trackpad up handler) | 3-finger tap keyboard, 4-finger tap full keyboard, 5-finger tap menu (now behind its own setting) |
| Credits | `res/raw/credits.json`, `scripts/render_credits.py` | One credits source rendered into the README and release notes, and later into the app |
| Packaging and CI | `app/build.gradle`, `.github/workflows/build-unified.yml`, `.github/workflows/docs-check.yml`, `scripts/setup-release-signing.ps1` | Package IDs `com.mohitarora.superartemis` (debug `.debug`), `SuperArtemis-<version>-<abi>-<buildType>.apk` names, environment-driven release signing, signed arm64 release builds and the credits check |

## Data and state

| Store | Location | Contents |
| :--- | :--- | :--- |
| Default SharedPreferences | `shared_prefs/<applicationId>_preferences.xml` | Every setting on the settings screen, the Quick Menu layout and style |
| Named SharedPreferences | `HiddenApps`, `specialPrefs`, `OSC`, keyboard layout files, `MicrophoneState` | Hidden apps per host, custom special keys JSON, on-screen control layouts, per-host last mic state |
| Device-only SharedPreferences | `DecoderTombstone`, `GlPreferences` | Crash counters and GPU fingerprint, never backed up |
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
  -> buildActions(): available actions keyed by GameMenuLayout ID
  -> Quick Menu = configured IDs in user order + Advanced Menu + Cancel
  -> Advanced Menu = every available action not in the Quick Menu + Customize menu + device options
  -> MenuOptionAdapter renders list, compact or grid (style read on every open)
```

### Backup and restore

```
Backup: dialog (pairing keys opt-in) -> SAF CreateDocument -> BackupRestoreManager.writeBackup
  -> typed prefs of every non-excluded file + profiles.json + PC rows (+ identity if opted in)
Restore: confirm -> SAF OpenDocument -> readBackup + validate (whole file, nothing written yet)
  -> applyBackup: prefs mirrored, allowlisted files written atomically, PC rows replaced, identity if present
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
| Anything left out of the Quick Menu stays in the Advanced Menu | Nobody can lock themselves out of Disconnect or Quit |
| Five-finger tap has its own setting | Back Menu only describes the physical back key |
| Settings search uses exact matching and expands collapsed categories first | Fuzzy matching ranked loosely related settings first, and the library cannot scroll to items hidden in a collapsed group |
| Backups are settings-only by default, pairing keys are opt-in | A file with keys can connect to the user's PCs as this device, so including them must be a deliberate choice |
| Restore validates the whole file before writing and restarts the process | A half-applied restore is worse than none, and long-lived services cache hosts and identity |
| Restore only writes allowlisted files | A crafted backup cannot write outside the expected paths |
| Credits live in one JSON file | README, release notes and the app always agree, and CI catches drift |
| Upstream contributions are small bug fixes only | The upstream maintainer declined menu reordering and Material 3 pending a UI rewrite |
