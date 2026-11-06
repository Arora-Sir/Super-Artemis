package com.limelight.preferences;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Locale;

// NOTE: Checks the bar and the minus and plus buttons of SliderScale on the real ranges of preferences.xml.
// NOTE: Tests the bitrate (500 to 300000 kbps, step 500, non-linear), metered bitrate (0 to 300000) and grace time (10 to 60 s).
public class SliderScaleTest {
    private final SliderScale bitrate = new SliderScale(500, 300000, 500, 1000, true);
    private final SliderScale metered = new SliderScale(0, 300000, 500, 1000, true);
    private final SliderScale grace = new SliderScale(10, 60, 5, 1, false);

    @Test
    public void bitrateBarEndsAreTheRangeEnds() {
        assertEquals(500, bitrate.valueAt(0));
        assertEquals(300000, bitrate.valueAt(SliderScale.LOG_POSITIONS));
    }

    @Test
    public void lowBitratesGetARealShareOfTheBar() {
        int pos = bitrate.positionOf(3000);
        assertTrue("3 Mbps at position " + pos, pos > SliderScale.LOG_POSITIONS / 6);
        assertTrue("2 Mbps and 3 Mbps are far apart", bitrate.positionOf(3000) - bitrate.positionOf(2000) > 30);
    }

    @Test
    public void sweetSpotIsNearCenter() {
        int mid = bitrate.valueAt(SliderScale.LOG_POSITIONS / 2);
        assertTrue("Midpoint near 35 to 40 Mbps: " + mid, mid >= 35000 && mid <= 42000);
    }

    @Test
    public void everyButtonValueSurvivesTheBar() {
        for (int v = 500; v < 300000; v = bitrate.plus(v)) {
            assertEquals("value " + v, v, bitrate.valueAt(bitrate.positionOf(v)));
        }
    }

    @Test
    public void bitrateButtonStepGrowsWithTheValue() {
        assertEquals(500, bitrate.buttonStep(3000));
        assertEquals(500, bitrate.buttonStep(15000));
        assertEquals(500, bitrate.buttonStep(30000));
        assertEquals(500, bitrate.buttonStep(45000));
        assertEquals(1000, bitrate.buttonStep(60000));
        assertEquals(5000, bitrate.buttonStep(150000));
    }

    @Test
    public void bitrateButtonsCrossTheLadderCleanly() {
        assertEquals(49500, bitrate.minus(50000));
        assertEquals(51000, bitrate.plus(50000));
        assertEquals(99000, bitrate.minus(100000));
        assertEquals(105000, bitrate.plus(100000));
        assertEquals(500, bitrate.minus(500));
        assertEquals(300000, bitrate.plus(300000));
        assertEquals(15500, bitrate.plus(15300));
        assertEquals(15000, bitrate.minus(15300));
    }

    @Test
    public void meteredBitrateKeepsZeroAtTheStart() {
        assertEquals(0, metered.valueAt(0));
        assertEquals(500, metered.valueAt(1));
        assertEquals(0, metered.positionOf(0));
        assertEquals(0, metered.minus(500));
        assertEquals(500, metered.plus(0));
        assertEquals(300000, metered.valueAt(SliderScale.LOG_POSITIONS));
    }

    @Test
    public void graceTimeIsLinearWithItsOwnStep() {
        assertEquals(10, grace.positions());
        assertEquals(10, grace.valueAt(0));
        assertEquals(30, grace.valueAt(grace.positionOf(30)));
        assertEquals(35, grace.plus(30));
        assertEquals(25, grace.minus(30));
        assertEquals(60, grace.plus(60));
        assertEquals(10, grace.minus(10));
        assertEquals(30, grace.clamp(31));
    }

    @Test
    public void fieldTextFollowsTheLocale() {
        assertEquals("15,0", bitrate.format(15000, new Locale("ru")));
        assertEquals("15.0", bitrate.format(15000, Locale.US));
        assertEquals("30", grace.format(30, Locale.US));
    }

    @Test
    public void typedTextAcceptsBothDecimalMarks() {
        assertEquals(Integer.valueOf(2500), bitrate.parse("2,5"));
        assertEquals(Integer.valueOf(2500), bitrate.parse(" 2.5 "));
        assertEquals(Integer.valueOf(300000), bitrate.parse("999"));
        assertEquals(Integer.valueOf(500), bitrate.parse("0"));
        assertEquals(Integer.valueOf(45), grace.parse("44"));
        assertEquals(Integer.valueOf(15500), bitrate.parse("15,3"));
        assertEquals(Integer.valueOf(0), metered.parse("0,2"));
        assertNull(bitrate.parse("abc"));
        assertNull(bitrate.parse(""));
    }
}
