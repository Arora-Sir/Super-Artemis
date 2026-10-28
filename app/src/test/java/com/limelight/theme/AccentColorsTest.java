package com.limelight.theme;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;
import androidx.test.core.app.ApplicationProvider;

import com.limelight.shadows.ShadowMoonBridge;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

// NOTE: The custom accent color reads hex codes the way people type them and never accepts anything that is not a color.
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, shadows = {ShadowMoonBridge.class})
public class AccentColorsTest {
    @Test
    public void hexCodesAreNormalizedFromEveryCommonForm() {
        assertEquals("#3D6FD8", AccentColors.normalize("3d6fd8"));
        assertEquals("#3D6FD8", AccentColors.normalize("#3D6FD8"));
        assertEquals("#3D6FD8", AccentColors.normalize("  #3d6fd8 "));
        assertEquals("#AABBCC", AccentColors.normalize("#abc"));
    }

    @Test
    public void anythingThatIsNotAColorIsRejected() {
        assertNull(AccentColors.normalize(null));
        assertNull(AccentColors.normalize(""));
        assertNull(AccentColors.normalize("#12345"));
        assertNull(AccentColors.normalize("#1234567"));
        assertNull(AccentColors.normalize("#GGGGGG"));
        assertNull(AccentColors.normalize("red"));
        assertNull(AccentColors.normalize("#12 456"));
    }

    @Test
    public void typedTextKeepsOnlyUpToSixHexDigits() {
        assertEquals("3D6FD8", AccentColors.sanitize("#3d6fd8"));
        assertEquals("3D6FD8", AccentColors.sanitize("3d-6f g d8zz99"));
        assertEquals("", AccentColors.sanitize("xyz!"));
        assertEquals("ABCDEF", AccentColors.sanitize("abcdefabcdef"));
    }

    @Test
    public void hsvConversionRoundTripsAndClampsOutOfRangeValues() {
        int color = AccentColors.parseOrDefault("#3D6FD8");
        float[] hsv = AccentColors.toHsv(color);
        assertEquals(color, AccentColors.fromHsv(hsv[0], hsv[1], hsv[2]));
        // A touch past the edge of the picker still gives a valid color
        assertEquals(AccentColors.fromHsv(360f, 1f, 1f), AccentColors.fromHsv(900f, 5f, 9f));
        assertEquals(0xFF000000, AccentColors.fromHsv(-20f, -1f, -1f));
    }

    @Test
    public void invalidCodesFallBackToTheDefaultSeed() {
        assertEquals(AccentColors.DEFAULT_SEED, AccentColors.parseOrDefault("nope"));
        assertEquals(0xFFFF0000, AccentColors.parseOrDefault("#ff0000"));
        assertEquals("#FF0000", AccentColors.toHex(0xFFFF0000));
    }

    @Test
    public void veryDarkSeedsAreFlaggedAndBrightOnesAreNot() {
        assertTrue(AccentColors.lowContrastOnDark(0xFF101010));
        assertFalse(AccentColors.lowContrastOnDark(0xFF3D6FD8));
        assertFalse(AccentColors.lowContrastOnDark(0xFFFFFFFF));
        assertEquals(0xFFFFFFFF, AccentColors.onColor(0xFF101010));
        assertEquals(0xFF000000, AccentColors.onColor(0xFFF0F0F0));
    }

    @Test
    public void theCustomSchemeStoresItsColorAndRebuildsScreensWhenItChanges() {
        Context context = ApplicationProvider.getApplicationContext();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().clear().commit();
        ThemeManager.choose(context, ThemeManager.MATERIAL_YOU, ThemeManager.SCHEME_CUSTOM);
        ThemeManager.chooseCustomColor(context, 0xFF336699);

        assertEquals("#336699", prefs.getString(ThemeManager.PREF_CUSTOM_COLOR, null));
        assertEquals(0xFF336699, ThemeManager.customSeed(context));
        assertEquals(Integer.valueOf(0xFF336699), ThemeManager.colorOptions(context, ThemeManager.SCHEME_CUSTOM).getContentBasedSeedColor());

        String applied = ThemeManager.MATERIAL_YOU + ":custom:#336699";
        assertFalse(ThemeManager.needsRecreate(context, applied));
        ThemeManager.chooseCustomColor(context, 0xFF993366);
        assertTrue(ThemeManager.needsRecreate(context, applied));
    }
}
