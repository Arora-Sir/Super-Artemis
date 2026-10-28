package com.limelight.theme;

import static org.junit.Assert.assertEquals;

import android.content.Context;

import androidx.preference.PreferenceManager;
import androidx.test.core.app.ApplicationProvider;

import com.limelight.shadows.ShadowMoonBridge;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

// NOTE: The on-screen controller takes its pressed color from the scheme chosen in Settings, and keeps its own colors for the Wallpaper scheme.
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, shadows = {ShadowMoonBridge.class})
public class StreamAccentTest {
    private Context context;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        PreferenceManager.getDefaultSharedPreferences(context).edit().clear().commit();
    }

    @Test
    public void theWallpaperSchemeLeavesTheControllerAlone() {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString(ThemeManager.PREF_SCHEME, ThemeManager.SCHEME_WALLPAPER).commit();
        assertEquals(0, ThemeManager.streamAccent(context));
    }

    @Test
    public void aFixedSchemeGivesItsSeed() {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString(ThemeManager.PREF_SCHEME, "blue").commit();
        assertEquals(0xFF2196F3, ThemeManager.streamAccent(context));
    }

    @Test
    public void theCustomSchemeGivesThePickedColor() {
        ThemeManager.chooseCustomColor(context, 0xFFAA3355);
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString(ThemeManager.PREF_SCHEME, ThemeManager.SCHEME_CUSTOM).commit();
        assertEquals(0xFFAA3355, ThemeManager.streamAccent(context));
    }
}
