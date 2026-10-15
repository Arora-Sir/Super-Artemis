package com.bytehamster.lib.preferencesearch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.limelight.R;
import com.limelight.shadows.ShadowMoonBridge;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

// NOTE: Indexes the real settings screen the way the search bar does and checks what everyday queries find.
// NOTE: "mouse" once missed Touchscreen Mode, Remember mouse mode and Remote desktop mouse mode, because the library kept only its 10 best fuzzy scores.
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, shadows = {ShadowMoonBridge.class})
public class SettingsSearchTest {
    private PreferenceParser parser;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        SearchConfiguration.setSynonyms(com.limelight.preferences.StreamSettingsAccess.synonyms());
        SearchConfiguration config = new SearchConfiguration();
        parser = new PreferenceParser(context);
        parser.addResourceFile(config.index(R.xml.preferences));
    }

    private List<String> keys(String query) {
        List<String> keys = new ArrayList<>();
        for (PreferenceItem item : parser.searchFor(query, false)) {
            keys.add(item.key);
        }
        return keys;
    }

    @Test
    public void mouseFindsEveryMouseAndTouchSetting() {
        List<String> keys = keys("mouse");
        assertTrue(keys.size() > 10);
        assertTrue(keys.contains("mouse_mode_list"));
        assertTrue(keys.contains("checkbox_remember_mouse_mode"));
        assertTrue(keys.contains("checkbox_absolute_mouse_mode"));
    }

    @Test
    public void titleMatchesComeBeforeSectionMatches() {
        List<PreferenceItem> results = parser.searchFor("mouse", false);
        boolean seenOther = false;
        for (PreferenceItem item : results) {
            boolean inTitle = item.title != null && item.title.toLowerCase().contains("mouse");
            if (!inTitle) {
                seenOther = true;
            }
            else {
                assertTrue("Title match after a weaker match: " + item.title, !seenOther);
            }
        }
    }

    @Test
    public void everydayWordsFindTheirSettings() {
        assertTrue(keys("cursor").contains("checkbox_mouse_local_cursor"));
        // "pointer" appears in no setting text, so it only finds settings through its synonyms
        assertTrue(keys("pointer").contains("mouse_mode_list"));
        assertTrue(keys("touchpad").size() >= keys("trackpad").size() - 1);
        assertEquals(new ArrayList<String>(), keys("   "));
    }

    @Test
    public void directMatchesRankBeforeSynonymMatches() {
        List<String> keys = keys("cursor");
        int localCursor = keys.indexOf("checkbox_mouse_local_cursor");
        int touchscreenMode = keys.indexOf("mouse_mode_list");
        assertTrue(localCursor >= 0 && touchscreenMode >= 0);
        assertTrue(localCursor < touchscreenMode);
    }
}
