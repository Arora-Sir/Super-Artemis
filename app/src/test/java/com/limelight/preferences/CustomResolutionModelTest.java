package com.limelight.preferences;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.List;

// NOTE: Checks the stops, the nearest stop of a typed size, the minus and plus steps and the validation of the Custom
// NOTE: Resolution dialog on a 19.5:9 phone screen (2340x1080) and a 16:10 tablet screen (2560x1600).
public class CustomResolutionModelTest {
    private final CustomResolutionModel phone = new CustomResolutionModel(1080, 2340);
    private final CustomResolutionModel tablet = new CustomResolutionModel(2560, 1600);

    @Test
    public void nativeStopsAreSharesOfTheScreenRoundedToEight() {
        List<CustomResolutionModel.Stop> s = phone.stops(CustomResolutionModel.Ratio.NATIVE);
        assertEquals(4, s.size());
        assertEquals("25%", s.get(0).label);
        assertEquals(584, s.get(0).width);
        assertEquals(272, s.get(0).height);
        assertEquals(1752, s.get(2).width);
        assertEquals(808, s.get(2).height);
        assertEquals(2340, s.get(3).width);
        assertEquals(1080, s.get(3).height);
        for (CustomResolutionModel.Stop stop : s.subList(0, 3)) {
            assertEquals(0, stop.width % 8);
            assertEquals(0, stop.height % 8);
        }
    }

    @Test
    public void screenIsTakenInLandscapeOrder() {
        assertEquals(2340, phone.nativeWidth());
        assertEquals(1080, phone.nativeHeight());
    }

    @Test
    public void savedSizeOpensOnItsRatio() {
        assertEquals(CustomResolutionModel.Ratio.R16_9, phone.ratioFor(1920, 1080));
        assertEquals(CustomResolutionModel.Ratio.R16_10, phone.ratioFor(2560, 1600));
        assertEquals(CustomResolutionModel.Ratio.NATIVE, phone.ratioFor(1800, 1000));
        // The full tablet screen is both a Native stop and a 16:10 stop; Native comes first
        assertEquals(CustomResolutionModel.Ratio.NATIVE, tablet.ratioFor(2560, 1600));
    }

    @Test
    public void typedSizeSitsBetweenTwoStops() {
        float p = tablet.position(CustomResolutionModel.Ratio.R16_9, 1800, 1000);
        assertTrue("1800x1000 between 900p and 1080p: " + p, p > 1 && p < 2);
        assertEquals(2f, tablet.position(CustomResolutionModel.Ratio.R16_9, 1920, 1080), 0f);
        assertEquals(0f, tablet.position(CustomResolutionModel.Ratio.R16_9, 640, 360), 0f);
        assertEquals(4f, tablet.position(CustomResolutionModel.Ratio.R16_9, 7680, 4320), 0f);
    }

    @Test
    public void minusAndPlusStepThroughStops() {
        CustomResolutionModel.Ratio r = CustomResolutionModel.Ratio.R16_9;
        assertEquals(1, tablet.minus(r, 1920, 1080));
        assertEquals(3, tablet.plus(r, 1920, 1080));
        assertEquals(0, tablet.minus(r, 1280, 720));
        assertEquals(4, tablet.plus(r, 3840, 2160));
        // A typed size goes to the stop below or above it
        assertEquals(1, tablet.minus(r, 1800, 1000));
        assertEquals(2, tablet.plus(r, 1800, 1000));
    }

    @Test
    public void switchingRatioKeepsTheHeight() {
        assertEquals(2, tablet.closestByHeight(CustomResolutionModel.Ratio.R16_9, 1080));
        assertEquals(1, tablet.closestByHeight(CustomResolutionModel.Ratio.R21_9, 1080));
    }

    @Test
    public void validation() {
        assertEquals(CustomResolutionModel.Problem.EMPTY, CustomResolutionModel.check(null, 1080));
        assertEquals(CustomResolutionModel.Problem.EMPTY, CustomResolutionModel.check(1920, 0));
        assertEquals(CustomResolutionModel.Problem.OUT_OF_RANGE, CustomResolutionModel.check(200, 1080));
        assertEquals(CustomResolutionModel.Problem.OUT_OF_RANGE, CustomResolutionModel.check(9000, 1080));
        assertEquals(CustomResolutionModel.Problem.ODD, CustomResolutionModel.check(1921, 1080));
        assertEquals(CustomResolutionModel.Problem.NONE, CustomResolutionModel.check(1920, 1080));
    }

    @Test
    public void parseAcceptsAnySeparator() {
        assertArrayEquals(new int[]{1920, 1080}, CustomResolutionModel.parse("1920x1080"));
        assertArrayEquals(new int[]{1920, 1080}, CustomResolutionModel.parse("1920 × 1080"));
        // Cyrillic "х", the letter a Russian keyboard types on the Latin "x" key
        assertArrayEquals(new int[]{1920, 1080}, CustomResolutionModel.parse("1920х1080"));
        assertNull(CustomResolutionModel.parse("1920"));
        assertNull(CustomResolutionModel.parse("abc"));
    }
}
