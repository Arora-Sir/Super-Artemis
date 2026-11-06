package com.limelight.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;
import androidx.test.core.app.ApplicationProvider;

import com.limelight.R;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Locale;

// NOTE: The language is applied by the app itself, so a choice must switch the resources of a screen at once and a screen in the wrong language must be recognised.
@Config(sdk = {33}, shadows = {com.limelight.shadows.ShadowMoonBridge.class, com.limelight.shadows.ShadowGameManager.class})
@RunWith(RobolectricTestRunner.class)
public class AppLanguageTest {
    private Context context;
    private SharedPreferences prefs;
    private Locale originalDefault;

    @Before
    public void setUp() {
        originalDefault = Locale.getDefault();
        context = ApplicationProvider.getApplicationContext();
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().clear().commit();
    }

    @After
    public void tearDown() {
        Locale.setDefault(originalDefault);
    }

    private void choose(String tag) {
        prefs.edit().putString(AppLanguage.PREF_KEY, tag).commit();
    }

    @Test
    public void theChoiceDefaultsToDefault() {
        assertEquals(AppLanguage.DEFAULT, AppLanguage.choice(context));
    }

    @Test
    public void aTagWithACountryKeepsBothParts() {
        Locale locale = AppLanguage.fromTag("zh-CN");
        assertEquals("zh", locale.getLanguage());
        assertEquals("CN", locale.getCountry());
        assertEquals("pt", AppLanguage.fromTag("pt-BR").getLanguage());
        assertEquals("BR", AppLanguage.fromTag("pt-BR").getCountry());
    }

    @Test
    public void aTagWithoutACountryIsJustTheLanguage() {
        Locale locale = AppLanguage.fromTag("de");
        assertEquals("de", locale.getLanguage());
        assertEquals("", locale.getCountry());
    }

    @Test
    public void anExplicitChoiceIsTheEffectiveLocale() {
        choose("ru");
        assertEquals("ru", AppLanguage.effectiveLocale(context).getLanguage());
        choose("zh-TW");
        assertEquals("TW", AppLanguage.effectiveLocale(context).getCountry());
    }

    @Test
    public void applyingSwitchesTheResourcesOfAScreenAtOnce() {
        Activity activity = Robolectric.buildActivity(Activity.class).create().get();
        choose("ru");
        assertTrue("a screen created before the choice is out of date", AppLanguage.isOutOfDate(activity));

        AppLanguage.apply(activity);

        assertFalse(AppLanguage.isOutOfDate(activity));
        assertEquals("Отмена", activity.getString(R.string.network_resume_cancel));
    }

    @Test
    public void switchingBackToEnglishRestoresTheEnglishText() {
        Activity activity = Robolectric.buildActivity(Activity.class).create().get();
        choose("ru");
        AppLanguage.apply(activity);
        choose("en");
        AppLanguage.apply(activity);

        assertEquals("Cancel", activity.getString(R.string.network_resume_cancel));
    }

    @Test
    public void aScreenRecreatesOnlyOnceForAChange() {
        Activity activity = Robolectric.buildActivity(Activity.class).create().get();

        assertTrue(AppLanguage.claimRecreate(activity));
        assertFalse(AppLanguage.claimRecreate(activity));
    }
}
