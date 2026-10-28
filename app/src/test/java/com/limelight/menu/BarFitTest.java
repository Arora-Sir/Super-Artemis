package com.limelight.menu;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;

// NOTE: A bar saved with 20 buttons per line used to run off a phone screen and clip the rest. It now wraps at what fits.
public class BarFitTest {
    private static BarSets.Bar bar(int perLine, boolean down) {
        return new BarSets.Bar(BarSets.TOP_LEFT, perLine, down, 0, 0, new ArrayList<>());
    }

    @Test
    public void aPhoneFitsEightButtonsAcrossAndATabletAllTwenty() {
        assertEquals(8, BarFit.maxPerLine(BarFit.spanDp(false, 360, 640, 0)));
        assertEquals(18, BarFit.maxPerLine(BarFit.spanDp(false, 832, 384, 0)));
        assertEquals(20, BarFit.maxPerLine(BarFit.spanDp(false, 1480, 924, 0)));
    }

    @Test
    public void downFillsAreLimitedByTheScreenHeight() {
        assertEquals(7, BarFit.maxPerLine(BarFit.spanDp(true, 832, 384, 0)));
        assertEquals(20, BarFit.maxPerLine(BarFit.spanDp(true, 1480, 924, 0)));
    }

    @Test
    public void theMenuButtonTakesOneSlot() {
        int without = BarFit.maxPerLine(BarFit.spanDp(false, 360, 640, 0));
        int with = BarFit.maxPerLine(BarFit.spanDp(false, 360, 640, BarFit.BUTTON_DP + BarFit.GAP_DP));
        assertEquals(without - 1, with);
    }

    @Test
    public void aTinyScreenStillFitsOneButton() {
        assertEquals(1, BarFit.maxPerLine(0));
        assertEquals(1, BarFit.maxPerLine(10));
    }

    @Test
    public void aBarThatFitsIsLeftAloneAndOneThatDoesNotWrapsEarlier() {
        BarSets.Bar small = bar(4, false);
        assertSame(small, BarFit.fitted(small, 360, 640, 0));

        BarSets.Bar wide = bar(20, false);
        BarSets.Bar fitted = BarFit.fitted(wide, 360, 640, 0);
        assertEquals(8, fitted.perLine);
        // The saved intent is not changed
        assertEquals(20, wide.perLine);
    }

    @Test
    public void everyButtonStaysVisibleAfterFitting() {
        BarSets.Bar wide = bar(20, false);
        for (int i = 0; i < 25; i++) {
            wide.items.add("action_" + i);
        }
        BarSets.Bar fitted = BarFit.fitted(wide, 360, 640, 0);
        int cells = 0;
        for (java.util.List<String> row : BarSets.grid(fitted, wide.items)) {
            for (String item : row) {
                cells += item != null ? 1 : 0;
            }
        }
        assertEquals(25, cells);
        assertTrue(fitted.columns() <= 8);
    }

    @Test
    public void overlappingBarsAreDetected() {
        float[] a = {0, 0, 100, 40};
        float[] b = {90, 20, 200, 60};
        float[] c = {100, 0, 200, 40};
        assertTrue(BarFit.overlaps(a, b));
        assertFalse("Touching edges do not cover each other", BarFit.overlaps(a, c));
    }
}
