package com.limelight.theme;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;
import androidx.test.core.app.ApplicationProvider;

import com.limelight.shadows.ShadowMoonBridge;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, shadows = {ShadowMoonBridge.class})
public class ThemeSchemesTest {
    private Context context;
    private SharedPreferences prefs;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().clear().commit();
    }

    @Test
    public void materialYouIsTheDefault() {
        assertEquals(ThemeManager.MATERIAL_YOU, ThemeManager.currentStyle(context));
        assertEquals(ThemeManager.SCHEME_WALLPAPER, ThemeManager.currentScheme(context));
    }

    @Test
    public void classicWrittenByTheOldDefaultMovesToMaterialYouOnce() {
        prefs.edit().putString(ThemeManager.PREF_KEY, ThemeManager.CLASSIC).commit();
        assertEquals(ThemeManager.MATERIAL_YOU, ThemeManager.currentStyle(context));
        assertEquals(ThemeManager.MATERIAL_YOU, prefs.getString(ThemeManager.PREF_KEY, null));
    }

    @Test
    public void classicPickedOnPurposeStays() {
        ThemeManager.choose(context, ThemeManager.CLASSIC, "teal");
        assertEquals(ThemeManager.CLASSIC, ThemeManager.currentStyle(context));
        assertTrue(prefs.getBoolean(ThemeManager.PREF_CHOSEN, false));
    }

    @Test
    public void aSchemeChangeRebuildsScreens() {
        ThemeManager.choose(context, ThemeManager.MATERIAL_YOU, "blue");
        String applied = ThemeManager.MATERIAL_YOU + ":blue";
        assertFalse(ThemeManager.needsRecreate(context, applied));
        ThemeManager.choose(context, ThemeManager.MATERIAL_YOU, "rose");
        assertTrue(ThemeManager.needsRecreate(context, applied));
    }

    @Test
    public void fixedSchemesHaveASeedAndWallpaperHasNone() {
        assertEquals(null, ThemeManager.colorOptions(ThemeManager.SCHEME_WALLPAPER).getContentBasedSeedColor());
        assertEquals(Integer.valueOf(0xFF00897B), ThemeManager.colorOptions("teal").getContentBasedSeedColor());
        ThemeManager.choose(context, ThemeManager.MATERIAL_YOU, "not_a_scheme");
        assertEquals(ThemeManager.SCHEME_WALLPAPER, ThemeManager.currentScheme(context));
    }
}
