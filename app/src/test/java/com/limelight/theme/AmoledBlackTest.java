package com.limelight.theme;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.ColorUtils;
import androidx.preference.PreferenceManager;
import androidx.test.core.app.ApplicationProvider;

import com.limelight.R;
import com.limelight.shadows.ShadowMoonBridge;
import com.limelight.ui.Dialogs;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;

// NOTE: Pure black is a switch on top of either style. Windows and dialogs are #000000, and text, cards and accents keep readable contrast on it.
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, shadows = {ShadowMoonBridge.class})
public class AmoledBlackTest {
    private Context context;
    private SharedPreferences prefs;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().clear().commit();
    }

    // NOTE: Robolectric cannot load the dynamic color resources overlay, so Material You gets its theme and the pure black overlay the way ThemeManager.apply adds them.
    private AppCompatActivity themed(String style, boolean amoled) {
        ThemeManager.choose(context, style, ThemeManager.SCHEME_WALLPAPER);
        prefs.edit().putBoolean(ThemeManager.PREF_AMOLED, amoled).commit();
        ActivityController<AppCompatActivity> controller = Robolectric.buildActivity(AppCompatActivity.class);
        AppCompatActivity activity = controller.get();
        if (ThemeManager.MATERIAL_YOU.equals(style)) {
            activity.setTheme(R.style.AppTheme_MaterialYou);
            if (amoled) {
                activity.getTheme().applyStyle(R.style.ThemeOverlay_SuperArtemis_Amoled, true);
            }
        }
        else {
            ThemeManager.apply(activity);
        }
        return controller.setup().get();
    }

    private static double contrast(int foreground, int background) {
        return ColorUtils.calculateContrast(foreground, background);
    }

    @Test
    public void offByDefaultAndDialogsKeepTheirContainerColor() {
        AppCompatActivity activity = themed(ThemeManager.MATERIAL_YOU, false);
        assertFalse(ThemeManager.isAmoled(activity));
        assertNotEquals(Color.BLACK, Dialogs.color(activity, R.attr.saDialogSurface));
    }

    @Test
    public void windowsAndDialogsAreBlackInBothStyles() {
        AppCompatActivity my = themed(ThemeManager.MATERIAL_YOU, true);
        assertEquals(Color.BLACK, Dialogs.color(my, R.attr.saDialogSurface));
        assertEquals(Color.BLACK, Dialogs.color(my, R.attr.saBackground));
        AppCompatActivity classic = themed(ThemeManager.CLASSIC, true);
        assertEquals(Color.BLACK, Dialogs.color(classic, R.attr.saDialogSurface));
        assertEquals(Color.BLACK, Dialogs.color(classic, R.attr.saBackground));
    }

    @Test
    public void textCardsAndAccentsStayReadableOnBlack() {
        for (String style : new String[] {ThemeManager.CLASSIC, ThemeManager.MATERIAL_YOU}) {
            {
                AppCompatActivity activity = themed(style, true);
                String where = style;
                int black = Dialogs.color(activity, R.attr.saDialogSurface);
                assertTrue("text on black " + where, contrast(Dialogs.color(activity, R.attr.saOnSurface), black) >= 7);
                assertTrue("secondary text on black " + where, contrast(Dialogs.color(activity, R.attr.saOnSurfaceVariant), black) >= 4.5);
                assertTrue("accent on black " + where, contrast(Dialogs.color(activity, R.attr.saPrimary), black) >= 3);
                int card = Dialogs.color(activity, R.attr.saSurfaceContainerHighest);
                assertTrue("text on a card " + where, contrast(Dialogs.color(activity, R.attr.saOnSurface), card) >= 7);
                assertNotEquals("A card is told apart from the dialog " + where, black, card);
                assertTrue("tonal button label " + where, contrast(Dialogs.color(activity, R.attr.saOnSecondaryContainer),
                        Dialogs.color(activity, R.attr.saSecondaryContainer)) >= 4.5);
                assertTrue("dialog edge is visible " + where, Color.alpha(Dialogs.color(activity, R.attr.saDialogStroke)) > 0);
            }
        }
    }

    @Test
    public void switchingItRebuildsOpenScreens() {
        ThemeManager.choose(context, ThemeManager.MATERIAL_YOU, "blue");
        String applied = ThemeManager.MATERIAL_YOU + ":blue";
        assertFalse(ThemeManager.needsRecreate(context, applied));
        prefs.edit().putBoolean(ThemeManager.PREF_AMOLED, true).commit();
        assertTrue(ThemeManager.needsRecreate(context, applied));
    }

    @Test
    public void dialogsOverTheStreamFollowIt() {
        ThemeManager.choose(context, ThemeManager.CLASSIC, ThemeManager.SCHEME_WALLPAPER);
        prefs.edit().putBoolean(ThemeManager.PREF_AMOLED, true).commit();
        Context dialog = ThemeManager.dialogContext(context);
        assertEquals(Color.BLACK, Dialogs.color(dialog, R.attr.saDialogSurface));
    }
}
