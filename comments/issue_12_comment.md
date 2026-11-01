# Upstream Review: qiin2333/moonlight-vplus (v12.14.2 through v12.14.3)

Evaluated all 10 upstream commits and 3 releases reported in this watch cycle. Two stability bug fixes have been ported surgically to the Super Artemis codebase. Architectural enhancements have been audited and cataloged for the post-1.0.0 roadmap.

---

### Ported Bug Fixes

1. **ShortcutTrampoline Activity Stack Race on Android 16 and 17**
   - Upstream commit: `167e6ea67` (PR #778 by qiin2333).
   - Root cause: `ShortcutTrampoline` is declared with `android:noHistory="true"`. When launching a PC shortcut without a specific application target, calling `finish()` immediately before `startActivities()` caused a system NPE in `Task.mLaunchNextToBubble` on Android 16 and 17 devices (notably HONOR and Xiaomi). The early destruction of the trampoline stripped the active task while the system resolved the multi-activity stack.
   - Resolution: Removed the premature `finish()` call. The `FLAG_ACTIVITY_CLEAR_TASK` intent flag naturally clears the task, and the `noHistory` attribute ensures clean system disposal once the destination stack starts.
   - Target file: `app/src/main/java/com/limelight/ShortcutTrampoline.java`.

2. **Clean Stream Teardown on MediaCodec Decoder Recreation Failure**
   - Upstream commit: `b7d606129` (PR #783 by qiin2333).
   - Root cause: In `MediaCodecDecoderRenderer.doCodecRecoveryIfRequired`, when decoder recreation threw `IllegalStateException`, the method threw `RendererException` directly inside the monitor block. Because the exception escaped before `codecRecoveryMonitor.notifyAll()` and `codecRecoveryThreadQuiescedFlags = 0` ran, all other quiesced decoder threads remained deadlocked, and the uncaught runtime exception killed the entire application process.
   - Resolution: Caught `IllegalStateException` during decoder reset, flagged `stopping = true`, and stored `recoveryFailure`. All waiting threads are woken and the recovery monitor is safely released before dispatching `crashListener.notifyCrash(recoveryFailure)`. In `tryConfigureDecoder`, `IOException` now rethrows as `IllegalStateException` under `throwOnCodecError` so all decoder initialization errors flow through the same graceful teardown pipeline.
   - Target file: `app/src/main/java/com/limelight/binding/video/MediaCodecDecoderRenderer.java`.

---

### Architectural Audit and Deferred Features

3. **In-Session Resolution Hot-Switching (`0ef771f6b` / PR #780 by qiin2333)**
   - Requires updating the `moonlight-common-c` native submodule to include client-driven resolution requests via encrypted control packet `0x5506` (`LiRequestResolutionChange`), coupled with host-side announcements in Foundation Sunshine (#729).
   - Slated for evaluation in the post-1.0.0 and 1.1.0 roadmap alongside coordinate unscaling and display mode management.

4. **Native Resolution Orientation Preservation on Resume (`629e6415f` / PR #781 by qiin2333)**
   - Upstream fix is coupled to the `moonlight-vplus` in-stream GameMenu card and `DisplayChoiceLists` architecture. Super Artemis handles native resolution dimensions directly via `PreferenceConfiguration.isNativeResolution`. Not applicable to the current Super Artemis UI pipeline.

5. **Layered Haptics Coordination and Continuous Gain (`373e71adb` / PR #775 and `646bc95ae` / PR #785 by qiin2333)**
   - Overhauls haptics via `moonlight-audio-haptics` SDK, RichTap HE2 envelopes, and 153 pre-generated `.ogg` assets across 205 files.
   - Deferred to the dedicated post-1.0.0 controller and audio haptics milestone.

6. **Virtual Crown Touch Switch (`1f444a020` / PR #784 by Yundi339)**
   - Specific to the `moonlight-vplus` proprietary virtual crown on-screen control interface. Not applicable.

7. **Release Synchronizations (`91eb9cf1b`, `d5a32c2c6`, `c1ca79cf2`)**
   - Tag versions v12.14.2, v12.14.2-beta.1, and v12.14.3. Informational only.
