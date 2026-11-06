package com.limelight.preferences;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

// NOTE: Checks the resolution bar on the list Titan 1 shows in Super Artemis 1.1.0: the six standard entries and the native
// 2560x1600, which the list puts last and the bar puts between 1440p and 4K by pixel count.
public class ResolutionStopsTest {
    private final ResolutionStops titan = new ResolutionStops(
            new CharSequence[]{"360p", "480p", "720p", "1080p", "1440p", "4K", "Native (2560x1600)"},
            new CharSequence[]{"640x360", "854x480", "1280x720", "1920x1080", "2560x1440", "3840x2160", "2560x1600"});

    @Test
    public void stopsGoFromTheSmallestPictureToTheLargest() {
        assertEquals(7, titan.size());
        assertEquals("360p", titan.get(0).name);
        assertEquals("1440p", titan.get(4).name);
        assertEquals("Native (2560x1600)", titan.get(5).name);
        assertEquals("4K", titan.get(6).name);
    }

    @Test
    public void plusAndMinusWalkTheStops() {
        assertEquals(4, titan.next(1920, 1080));
        assertEquals(5, titan.next(2560, 1440));
        assertEquals(6, titan.next(3840, 2160));
        assertEquals(2, titan.previous(1920, 1080));
        assertEquals(0, titan.previous(640, 360));
        // From a custom size the buttons go to the neighbours around it
        assertEquals(3, titan.next(1680, 1050));
        assertEquals(2, titan.previous(1680, 1050));
    }

    @Test
    public void aCustomSizeIsNotOnTheBarButHasANearestStop() {
        assertEquals(-1, titan.indexOf(1680, 1050));
        assertEquals(3, titan.nearest(1680, 1050));
        assertEquals(3, titan.indexOf(1920, 1080));
    }

    @Test
    public void sizesAreReadWithAnyCross() {
        assertArrayEquals(new int[]{1920, 1080}, ResolutionStops.parse("1920x1080"));
        assertArrayEquals(new int[]{1920, 1080}, ResolutionStops.parse("1920X1080"));
        assertArrayEquals(new int[]{1920, 1080}, ResolutionStops.parse("1920×1080"));
        assertNull(ResolutionStops.parse("1920x"));
        assertNull(ResolutionStops.parse("0x1080"));
        assertNull(ResolutionStops.parse("abc"));
        assertEquals("1680x1050", ResolutionStops.value(1680, 1050));
    }
}
