package com.limelight.preferences;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class ResolutionHeadingTest {

    @Test
    public void standardSizesShareSixteenByNine() {
        assertEquals("Standard Resolutions (16:9)",
                ResolutionListPreference.sharedRatioHeading("Standard Resolutions",
                        Arrays.asList("640x360", "1280x720", "1920x1080", "3840x2160")));
    }

    @Test
    public void tabletNativeModesShareSixteenByTen() {
        assertEquals("Native Display (16:10)",
                ResolutionListPreference.sharedRatioHeading("Native Display",
                        Arrays.asList("2960x1848", "2960x1850")));
    }

    @Test
    public void phoneNativeModesWithDifferentRatiosKeepThePlainHeading() {
        assertEquals("Native Display",
                ResolutionListPreference.sharedRatioHeading("Native Display",
                        Arrays.asList("2272x1080", "2400x1080")));
    }

    @Test
    public void emptyListKeepsThePlainHeading() {
        assertEquals("Native Display",
                ResolutionListPreference.sharedRatioHeading("Native Display", Collections.<String>emptyList()));
    }
}
