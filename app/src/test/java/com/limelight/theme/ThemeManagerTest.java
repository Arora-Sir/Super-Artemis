package com.limelight.theme;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Checkable;

import androidx.preference.PreferenceManager;
import androidx.test.core.app.ApplicationProvider;

import com.limelight.R;
import com.limelight.shadows.ShadowMoonBridge;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, shadows = {ShadowMoonBridge.class})
public class ThemeManagerTest {
    private Context context;
    private SharedPreferences prefs;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().clear().commit();
    }

    @Test
    public void materialYouIsTheDefaultAndUnknownValuesFallBackToClassic() {
        assertEquals(ThemeManager.MATERIAL_YOU, ThemeManager.currentStyle(context));
        prefs.edit().putString(ThemeManager.PREF_KEY, "neon").commit();
        assertEquals(ThemeManager.CLASSIC, ThemeManager.currentStyle(context));
    }

    @Test
    public void materialYouIsUsedOnAndroid12AndNoticedAsAChange() {
        prefs.edit().putString(ThemeManager.PREF_KEY, ThemeManager.MATERIAL_YOU).commit();
        assertTrue(ThemeManager.isMaterialYou(context));
        assertTrue(ThemeManager.needsRecreate(context, ThemeManager.CLASSIC));
        // What an activity applied includes the color scheme, so a scheme change also rebuilds it
        assertFalse(ThemeManager.needsRecreate(context, ThemeManager.MATERIAL_YOU + ":" + ThemeManager.SCHEME_WALLPAPER));
    }

    @Test
    @Config(sdk = 30)
    public void materialYouIsIgnoredBeforeAndroid12() {
        prefs.edit().putString(ThemeManager.PREF_KEY, ThemeManager.MATERIAL_YOU).commit();
        assertFalse(ThemeManager.isSupported());
        assertEquals(ThemeManager.CLASSIC, ThemeManager.currentStyle(context));
    }

    @Test
    public void settingsSwitchInflatesAsACheckableUnderMaterialYou() {
        Context themed = new ContextThemeWrapper(context, R.style.AppTheme_MaterialYou);
        View view = LayoutInflater.from(themed).inflate(R.layout.preference_widget_material_switch, null);
        assertTrue(view instanceof Checkable);
        assertEquals(android.R.id.checkbox, view.getId());
    }
}
