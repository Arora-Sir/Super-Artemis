package com.limelight.actions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.limelight.R;
import com.limelight.shadows.ShadowMoonBridge;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, shadows = {ShadowMoonBridge.class})
public class MouseActionStringsTest {
    private static final String[] LOCALES = {"cs", "de", "el", "es", "fr", "hu", "in", "it", "iw", "ja", "ko", "nb-rNO", "nl",
            "pt-rBR", "ro", "ru", "sv", "uk", "vi", "zh-rCN", "zh-rTW"};

    private static final int[] IDS = {R.string.action_mouse_left_click, R.string.action_mouse_right_click, R.string.action_mouse_middle_click,
            R.string.action_mouse_back, R.string.action_mouse_forward, R.string.action_scroll_up, R.string.action_scroll_down};

    private static Context context(String locale) {
        RuntimeEnvironment.setQualifiers(locale);
        return ApplicationProvider.getApplicationContext();
    }

    @Test
    public void everyLanguageTranslatesEveryMouseAction() {
        // The application context is shared and follows the latest qualifiers, so the English text is copied out first
        String[] english = new String[IDS.length];
        Context base = context("en");
        for (int i = 0; i < IDS.length; i++) {
            english[i] = base.getString(IDS[i]);
        }
        for (String locale : LOCALES) {
            Context localized = context(locale);
            for (int i = 0; i < IDS.length; i++) {
                assertNotEquals(locale + " still shows English for " + english[i], english[i], localized.getString(IDS[i]));
            }
        }
    }

    @Test
    public void italianKeepsItsApostrophe() {
        assertEquals("Scorri verso l'alto", context("it").getString(R.string.action_scroll_up));
    }

    @Test
    public void middleClickReusesTheGestureTranslation() {
        for (String locale : LOCALES) {
            Context localized = context(locale);
            assertEquals(locale, localized.getString(R.string.gesture_action_middle_click), localized.getString(R.string.action_mouse_middle_click));
        }
    }
}
