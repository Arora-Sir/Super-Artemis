package com.limelight.theme;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

// NOTE: Every end of the Surface lift slider must differ by an amount a person can see on an OLED screen, and the recommended level is the look that shipped first.
public class AmoledLadderTest {
    private static int grey(int color) {
        return color & 0xFF;
    }

    @Test
    public void theRecommendedLevelIsTheLookThatShippedFirst() {
        assertEquals(AmoledLadder.RECOMMENDED, 4);
        assertEquals(0xFF0D0D0D, AmoledLadder.container(AmoledLadder.RECOMMENDED));
        assertEquals(0xFF161616, AmoledLadder.high(AmoledLadder.RECOMMENDED));
        assertEquals(0xFF1F1F1F, AmoledLadder.highest(AmoledLadder.RECOMMENDED));
    }

    @Test
    public void everyLevelRisesAndEveryStepStaysApart() {
        for (int level = 0; level < AmoledLadder.LEVELS; level++) {
            int container = grey(AmoledLadder.container(level));
            int high = grey(AmoledLadder.high(level));
            int highest = grey(AmoledLadder.highest(level));
            assertTrue("a card is told apart from the black page at level " + level, container >= 4);
            assertTrue("a field is told apart from a card at level " + level, high - container >= 4);
            assertTrue("the highest step is told apart at level " + level, highest - high >= 4);
            if (level > 0) {
                assertTrue("container rises at level " + level, container - grey(AmoledLadder.container(level - 1)) >= 2);
                assertTrue("high rises at level " + level, high - grey(AmoledLadder.high(level - 1)) >= 3);
                assertTrue("highest rises at level " + level, highest - grey(AmoledLadder.highest(level - 1)) >= 3);
            }
        }
    }

    @Test
    public void aLevelOutsideTheRangeIsClamped() {
        assertEquals(AmoledLadder.container(0), AmoledLadder.container(-3));
        assertEquals(AmoledLadder.container(AmoledLadder.LEVELS - 1), AmoledLadder.container(99));
    }
}
