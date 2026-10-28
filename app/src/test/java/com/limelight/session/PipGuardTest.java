package com.limelight.session;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.media.AudioManager;

import org.junit.Test;

// NOTE: Covers when a stream may shrink into picture-in-picture, and when a screen that turned off should bring the stream back.
public class PipGuardTest {
    private static PipGuard.State ready() {
        return new PipGuard.State();
    }

    @Test
    public void aPlainAppSwitchMayEnter() {
        assertTrue(PipGuard.mayEnterPip(ready()));
    }

    @Test
    public void callsAndVoiceChatsNeverEnter() {
        for (int mode : new int[]{AudioManager.MODE_RINGTONE, AudioManager.MODE_IN_CALL, AudioManager.MODE_IN_COMMUNICATION}) {
            PipGuard.State s = ready();
            s.audioMode = mode;
            assertFalse("mode " + mode, PipGuard.mayEnterPip(s));
        }
        PipGuard.State normal = ready();
        normal.audioMode = AudioManager.MODE_NORMAL;
        assertTrue(PipGuard.mayEnterPip(normal));
    }

    @Test
    public void screenOffAndTheLockScreenNeverEnter() {
        PipGuard.State off = ready();
        off.interactive = false;
        assertFalse(PipGuard.mayEnterPip(off));

        PipGuard.State locked = ready();
        locked.keyguardLocked = true;
        assertFalse(PipGuard.mayEnterPip(locked));
    }

    @Test
    public void ownScreensSplitScreenAndSuppressionNeverEnter() {
        PipGuard.State own = ready();
        own.openingOwnScreen = true;
        assertFalse(PipGuard.mayEnterPip(own));

        PipGuard.State split = ready();
        split.multiWindow = true;
        assertFalse(PipGuard.mayEnterPip(split));

        PipGuard.State suppressed = ready();
        suppressed.pipSuppressed = true;
        assertFalse(PipGuard.mayEnterPip(suppressed));

        PipGuard.State external = ready();
        external.externalDisplay = true;
        assertFalse(PipGuard.mayEnterPip(external));
    }

    @Test
    public void nothingEntersWhenTheStreamIsNotRunningOrPipIsOff() {
        PipGuard.State idle = ready();
        idle.connected = false;
        assertFalse(PipGuard.mayEnterPip(idle));

        PipGuard.State off = ready();
        off.pipEnabled = false;
        assertFalse(PipGuard.mayEnterPip(off));

        PipGuard.State old = ready();
        old.pipSupported = false;
        assertFalse(PipGuard.mayEnterPip(old));

        PipGuard.State closing = ready();
        closing.finishing = true;
        assertFalse(PipGuard.mayEnterPip(closing));

        PipGuard.State already = ready();
        already.inPip = true;
        assertFalse(PipGuard.mayEnterPip(already));

        PipGuard.State rotating = ready();
        rotating.changingConfigurations = true;
        assertFalse(PipGuard.mayEnterPip(rotating));
    }

    // NOTE: onResume asked for picture-in-picture params before the first layout, the ratio was 0 by 0, and Android ended the app (seen on the Tab, 2026-10-24).
    @Test
    public void aspectRatioStaysInsideWhatAndroidAccepts() {
        assertNull(PipGuard.aspectRatioFor(0, 0));
        assertNull(PipGuard.aspectRatioFor(1920, 0));
        assertNull(PipGuard.aspectRatioFor(0, 1080));
        assertArrayEquals(new int[] {1920, 1080}, PipGuard.aspectRatioFor(1920, 1080));
        assertArrayEquals(new int[] {238, 100}, PipGuard.aspectRatioFor(5000, 1000));
        assertArrayEquals(new int[] {43, 100}, PipGuard.aspectRatioFor(1000, 5000));
        int[] edge = PipGuard.aspectRatioFor(2390, 1000);
        assertTrue(edge[0] / (float) edge[1] <= 2.39f);
    }

    // NOTE: On the Tab, One Hand Operation+ destroyed the stream surface 1.3 seconds before onPause, and the stream ended without a way back (log of 2026-10-24).
    @Test
    public void aStreamLostToAnotherAppReconnectsOnReturn() {
        assertTrue(PipGuard.shouldResumeAfterSurfaceLoss(true, false, false, false, false, true, true));
        assertFalse("The user ended it", PipGuard.shouldResumeAfterSurfaceLoss(true, true, false, false, false, true, true));
        assertFalse("Rotation", PipGuard.shouldResumeAfterSurfaceLoss(true, false, false, true, false, true, true));
        assertFalse("Settings or a prompt of the app", PipGuard.shouldResumeAfterSurfaceLoss(true, false, false, false, true, true, true));
        assertFalse("Already ended", PipGuard.shouldResumeAfterSurfaceLoss(false, false, false, false, false, true, true));
        assertFalse("End the stream policy", PipGuard.shouldResumeAfterSurfaceLoss(true, false, false, false, false, true, false));
        assertFalse("In the small window", PipGuard.shouldResumeAfterSurfaceLoss(true, false, true, false, false, true, true));
    }

    @Test
    public void autoEnterIsOffWhileACallRuns() {
        assertTrue(PipGuard.autoEnterArmed(true, false, AudioManager.MODE_NORMAL, false));
        assertFalse(PipGuard.autoEnterArmed(true, false, AudioManager.MODE_RINGTONE, false));
        assertFalse(PipGuard.autoEnterArmed(true, false, AudioManager.MODE_IN_CALL, false));
        assertFalse(PipGuard.autoEnterArmed(false, false, AudioManager.MODE_NORMAL, false));
        assertFalse(PipGuard.autoEnterArmed(true, true, AudioManager.MODE_NORMAL, false));
    }

    // NOTE: Gemini from the power button opens an assistant activity over the stream, and auto-enter shrank the stream into the small window (Tab log, 2026-10-24).
    @Test
    public void theAssistantOverlayKeepsTheStreamFullScreenForAFewSeconds() {
        assertTrue(PipGuard.assistantRecently(10_000, 8_000));
        assertFalse(PipGuard.assistantRecently(10_000, 4_000));
        assertFalse("Never invoked", PipGuard.assistantRecently(10_000, 0));
        assertFalse("Clock went back", PipGuard.assistantRecently(5_000, 8_000));

        PipGuard.State s = ready();
        s.assistantRecently = true;
        assertFalse(PipGuard.mayEnterPip(s));
        assertFalse(PipGuard.autoEnterArmed(true, false, AudioManager.MODE_NORMAL, true));
    }

    @Test
    public void aScreenThatTurnedOffResumesOnlyUnderAResumePolicy() {
        assertTrue(PipGuard.shouldResumeAfterScreenOff(true, false, false, true));
        // The screen is on, so this is a normal pause
        assertFalse(PipGuard.shouldResumeAfterScreenOff(true, true, false, true));
        // The stream already ended, or the user ended it
        assertFalse(PipGuard.shouldResumeAfterScreenOff(false, false, false, true));
        assertFalse(PipGuard.shouldResumeAfterScreenOff(true, false, true, true));
        // End the stream never reconnects
        assertFalse(PipGuard.shouldResumeAfterScreenOff(true, false, false, false));
    }

    @Test
    public void theAssistantHoldLastsLongerThanTheFirstWindow() {
        assertFalse(PipGuard.assistantRecently(20_000, 10_000));
        assertTrue(PipGuard.assistantRecently(20_000, 10_000, PipGuard.ASSISTANT_HOLD_MS));
        assertFalse(PipGuard.assistantRecently(200_000, 10_000, PipGuard.ASSISTANT_HOLD_MS));
    }
}
