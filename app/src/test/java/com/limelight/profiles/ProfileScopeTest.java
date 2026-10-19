package com.limelight.profiles;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.res.XmlResourceParser;

import androidx.preference.Preference;
import androidx.test.core.app.ApplicationProvider;

import com.limelight.EditProfileActivity;
import com.limelight.R;
import com.limelight.shadows.ShadowMoonBridge;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;
import org.xmlpull.v1.XmlPullParser;

import java.util.HashSet;
import java.util.Set;

// NOTE: The profile editor shows the same settings as Settings. ProfileScope is the one list of what applies to the whole app.
// NOTE: These tests fail when a listed key no longer exists in preferences.xml, and when a profile setting is hidden in the editor.
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, shadows = {ShadowMoonBridge.class})
public class ProfileScopeTest {
    private static Set<String> settingKeys() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        XmlResourceParser parser = context.getResources().getXml(R.xml.preferences);
        Set<String> keys = new HashSet<>();
        for (int event = parser.getEventType(); event != XmlPullParser.END_DOCUMENT; event = parser.next()) {
            if (event == XmlPullParser.START_TAG) {
                String key = parser.getAttributeValue("http://schemas.android.com/apk/res/android", "key");
                if (key != null) {
                    keys.add(key);
                }
            }
        }
        return keys;
    }

    @Test
    public void everyClassifiedKeyStillExists() throws Exception {
        Set<String> keys = settingKeys();
        Set<String> classified = new HashSet<>(ProfileScope.APP_WIDE);
        classified.addAll(ProfileScope.EDIT_WHEN_ACTIVE);
        classified.addAll(ProfileScope.APP_SECTIONS);
        for (String key : classified) {
            assertTrue("ProfileScope lists a setting that preferences.xml no longer has: " + key, keys.contains(key));
        }
    }

    @Test
    public void profileEditorShowsTheProfileSettingsAndGreysOutAppWideOnes() {
        EditProfileActivity activity = Robolectric.buildActivity(EditProfileActivity.class).setup().get();
        ShadowLooper.idleMainLooper();
        EditProfileActivity.ProfilePreferenceFragment fragment = (EditProfileActivity.ProfilePreferenceFragment)
                activity.getSupportFragmentManager().findFragmentById(R.id.preferences_container);
        assertNotNull(fragment);
        // Settings that belong to a profile stay visible and editable
        for (String key : new String[] {"list_resolution", "list_fps", "seekbar_bitrate_kbps", "list_audio_config"}) {
            Preference preference = fragment.findPreference(key);
            if (preference != null) {
                assertTrue(key, preference.isVisible());
                assertTrue(key, preference.isEnabled());
            }
        }
        // App-wide settings stay on screen, greyed out with a note, instead of vanishing
        for (String key : ProfileScope.APP_WIDE) {
            Preference preference = fragment.findPreference(key);
            if (preference != null) {
                assertTrue(key, preference.isVisible());
                assertFalse(key, preference.isEnabled());
            }
        }
    }
}
