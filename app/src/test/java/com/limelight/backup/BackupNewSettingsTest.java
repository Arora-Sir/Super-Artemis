package com.limelight.backup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;
import androidx.test.core.app.ApplicationProvider;

import com.limelight.menu.GameMenuLayout;
import com.limelight.shadows.ShadowMoonBridge;
import com.limelight.utils.AppLanguage;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

// NOTE: A backup copies every settings file, so the language and the newer settings must come back after a restore. The app restarts after a restore and the language is read again at startup.
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, shadows = {ShadowMoonBridge.class})
public class BackupNewSettingsTest {
    private Context context;
    private SharedPreferences defaults;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        defaults = PreferenceManager.getDefaultSharedPreferences(context);
        defaults.edit().clear().commit();
    }

    @Test
    public void theLanguageAndTheNewSettingsComeBackAfterARestore() throws Exception {
        defaults.edit()
                .putString(AppLanguage.PREF_KEY, "de")
                .putString("quick_bar_style", "handle")
                .putInt("quick_bar_idle_hide_seconds", 7)
                .putString(GameMenuLayout.PREF_SECTION_ORDER, "session,actions,menu")
                .putBoolean("keyboard_show_f13_f24_row", true)
                .putBoolean("keyboard_render_on_top", false)
                .putBoolean("checkbox_auto_resume_network", false)
                .commit();
        BackupFile backup = BackupCodec.validate(BackupCodec.create(context, false));

        defaults.edit().clear().commit();
        assertEquals(AppLanguage.DEFAULT, AppLanguage.choice(context));

        BackupRestoreManager.applyBackup(context, backup);

        assertEquals("de", AppLanguage.choice(context));
        assertEquals("handle", defaults.getString("quick_bar_style", null));
        assertEquals(7, defaults.getInt("quick_bar_idle_hide_seconds", 0));
        assertEquals("session,actions,menu", defaults.getString(GameMenuLayout.PREF_SECTION_ORDER, null));
        assertTrue(defaults.getBoolean("keyboard_show_f13_f24_row", false));
        assertEquals(false, defaults.getBoolean("keyboard_render_on_top", true));
        assertEquals(false, defaults.getBoolean("checkbox_auto_resume_network", true));
    }
}
