package com.limelight.computers;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class PollSelectionTest {
    private static final long G = PollSelection.GRACE_MS;

    private static int choose(boolean[] complete, boolean[] answered, long[] at, long now) {
        return PollSelection.choose(complete, answered, at, now);
    }

    @Test
    public void fastLocalAddressWinsAtOnce() {
        assertEquals(0, choose(new boolean[]{true, true, false, true}, new boolean[]{true, true, false, false}, new long[]{50, 60, 0, 0}, 70));
    }

    @Test
    public void localStillPendingIsWaitedForInsideTheGraceWindow() {
        // Manual answered at 400 ms, local has not finished, grace has not passed
        assertEquals(PollSelection.KEEP_WAITING, choose(new boolean[]{false, true, true, true}, new boolean[]{false, true, false, false}, new long[]{0, 400, 0, 0}, 400 + G - 1));
    }

    @Test
    public void deadLocalAddressNoLongerBlocksAWorkingManualAddress() {
        assertEquals(1, choose(new boolean[]{false, true, true, true}, new boolean[]{false, true, false, false}, new long[]{0, 400, 0, 0}, 400 + G));
    }

    @Test
    public void localAnsweringInsideTheGraceWindowStillBeatsManual() {
        // Both answered, local finished late but before the grace expired
        assertEquals(0, choose(new boolean[]{true, true, true, true}, new boolean[]{true, true, false, false}, new long[]{900, 400, 0, 0}, 1000));
    }

    @Test
    public void failedHigherAddressesFallThroughToTheFirstAnswer() {
        assertEquals(2, choose(new boolean[]{true, true, true, true}, new boolean[]{false, false, true, true}, new long[]{0, 0, 300, 200}, 350));
    }

    @Test
    public void nothingAnsweredWhenEverythingFailed() {
        assertEquals(PollSelection.NONE_ANSWERED, choose(new boolean[]{true, true, true, true}, new boolean[4], new long[4], 5000));
    }

    @Test
    public void everythingPendingKeepsWaiting() {
        assertEquals(PollSelection.KEEP_WAITING, choose(new boolean[]{false, false, false, false}, new boolean[4], new long[4], 5000));
    }

    @Test
    public void lowerAddressesPendingAfterAHigherOneAnsweredDoNotMatter() {
        assertEquals(1, choose(new boolean[]{true, true, false, false}, new boolean[]{false, true, false, false}, new long[]{0, 100, 0, 0}, 150));
    }
}
