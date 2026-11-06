package com.limelight.session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.limelight.nvstream.jni.MoonBridge;

import org.junit.Test;

// NOTE: The rules for resuming a stream by itself after a network drop, without any device.
public class ResumePlanTest {
    private static final int SOME_NETWORK_ERROR = -1;

    @Test
    public void resumesAfterASilenceThatEndedTheStream() {
        assertTrue(ResumePlan.shouldAutoResume(true, true, SOME_NETWORK_ERROR, true, true));
    }

    @Test
    public void resumesWhenTheNetworkIsGoneEvenWithoutAnEarlierSilence() {
        assertTrue(ResumePlan.shouldAutoResume(true, true, MoonBridge.ML_ERROR_NO_VIDEO_TRAFFIC, false, false));
    }

    @Test
    public void doesNotResumeWhenTheSettingIsOff() {
        assertFalse(ResumePlan.shouldAutoResume(false, true, SOME_NETWORK_ERROR, true, false));
    }

    @Test
    public void doesNotResumeAStreamThatNeverStarted() {
        assertFalse(ResumePlan.shouldAutoResume(true, false, SOME_NETWORK_ERROR, true, false));
    }

    @Test
    public void doesNotResumeAnEndThatRepeatsForever() {
        assertFalse(ResumePlan.shouldAutoResume(true, true, MoonBridge.ML_ERROR_GRACEFUL_TERMINATION, true, false));
        assertFalse(ResumePlan.shouldAutoResume(true, true, MoonBridge.ML_ERROR_PROTECTED_CONTENT, true, false));
        assertFalse(ResumePlan.shouldAutoResume(true, true, MoonBridge.ML_ERROR_FRAME_CONVERSION, true, false));
    }

    @Test
    public void showsTheDialogWhenThePcClosedTheSessionWithTheNetworkUp() {
        assertFalse(ResumePlan.shouldAutoResume(true, true, SOME_NETWORK_ERROR, false, true));
    }

    @Test
    public void checksSlowDownToAFiveSecondGap() {
        assertEquals(1500L, ResumePlan.delayBeforeCheck(0));
        assertEquals(2000L, ResumePlan.delayBeforeCheck(1));
        assertEquals(3000L, ResumePlan.delayBeforeCheck(2));
        assertEquals(5000L, ResumePlan.delayBeforeCheck(3));
        assertEquals(5000L, ResumePlan.delayBeforeCheck(50));
        assertEquals(1500L, ResumePlan.delayBeforeCheck(-4));
    }

    @Test
    public void givesUpAfterFiveMinutes() {
        assertFalse(ResumePlan.expired(ResumePlan.MAX_WAIT_MS - 1));
        assertTrue(ResumePlan.expired(ResumePlan.MAX_WAIT_MS));
    }

    @Test
    public void aReturnedNetworkWithASilentHostRestartsAfterThreeSeconds() {
        assertFalse(ResumePlan.shouldRestartEarly(true, true, true, 2999, 20000));
        assertTrue(ResumePlan.shouldRestartEarly(true, true, true, 3000, 5000));
    }

    @Test
    public void aStillMissingNetworkKeepsWaitingForTheNetwork() {
        assertFalse(ResumePlan.shouldRestartEarly(true, false, true, 0, 60000));
    }

    @Test
    public void aSilentHostWithoutANetworkLossRestartsOnlyAfterTenSeconds() {
        assertFalse(ResumePlan.shouldRestartEarly(true, true, false, 0, 9999));
        assertTrue(ResumePlan.shouldRestartEarly(true, true, false, 0, 10000));
    }

    @Test
    public void theSettingThatTurnsAutomaticReconnectOffAlsoTurnsTheEarlyRestartOff() {
        assertFalse(ResumePlan.shouldRestartEarly(false, true, true, 60000, 60000));
    }

    // A stream that started on mobile data restarts quickly once an unmetered network is stable and the PC answers over it, and never in a loop.
    @Test
    public void aStreamStartedOnMobileDataRestartsOnceWifiIsStableAndThePcAnswers() {
        long ok = ResumePlan.RESTART_AFTER_UNMETERED_MS;
        long old = ResumePlan.MIN_STREAM_MS_FOR_QUALITY_RESTART;
        assertTrue(ResumePlan.shouldRestartForQuality(true, true, true, true, false, true, ok, old));
        // Wi-Fi only just came back
        assertFalse(ResumePlan.shouldRestartForQuality(true, true, true, true, false, true, ok - 1, old));
        // A Wi-Fi with no route to the PC never restarts the stream
        assertFalse(ResumePlan.shouldRestartForQuality(true, true, true, true, false, false, ok, old));
        // A young stream is left alone
        assertFalse(ResumePlan.shouldRestartForQuality(true, true, true, true, false, true, ok, old - 1));
        // Still on a metered network, or the network is down
        assertFalse(ResumePlan.shouldRestartForQuality(true, true, true, true, true, true, ok, old));
        assertFalse(ResumePlan.shouldRestartForQuality(true, true, true, false, false, true, ok, old));
        // A stream that started on Wi-Fi, a setting that turns it off, and equal bitrates all mean nothing to gain
        assertFalse(ResumePlan.shouldRestartForQuality(true, false, true, true, false, true, ok, old));
        assertFalse(ResumePlan.shouldRestartForQuality(false, true, true, true, false, true, ok, old));
        assertFalse(ResumePlan.shouldRestartForQuality(true, true, false, true, false, true, ok, old));
        // The wait is short, as the owner asked
        assertTrue(ok <= 2000);
    }
}
