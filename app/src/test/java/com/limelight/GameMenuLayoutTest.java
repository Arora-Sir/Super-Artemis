package com.limelight;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, shadows = {ShadowMoonBridge.class})
public class GameMenuLayoutTest {
    private SharedPreferences prefs;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().clear().commit();
    }

    @Test
    public void defaultsToUpstreamQuickMenuOrder() {
        assertEquals(GameMenuLayout.DEFAULT_QUICK_ITEMS, GameMenuLayout.getQuickItems(prefs));
        assertEquals(GameMenuLayout.STYLE_LIST, GameMenuLayout.getStyle(prefs));
    }

    @Test
    public void savedOrderRoundTrips() {
        List<String> custom = Arrays.asList(GameMenuLayout.ID_MICROPHONE, GameMenuLayout.ID_DISCONNECT, GameMenuLayout.ID_TOGGLE_HUD);
        GameMenuLayout.setQuickItems(prefs, custom);
        assertEquals(custom, GameMenuLayout.getQuickItems(prefs));
    }

    @Test
    public void emptySelectionIsKept() {
        GameMenuLayout.setQuickItems(prefs, Collections.emptyList());
        assertTrue(GameMenuLayout.getQuickItems(prefs).isEmpty());
    }

    @Test
    public void unknownAndDuplicateIdsAreDropped() {
        prefs.edit().putString(GameMenuLayout.PREF_QUICK_ITEMS, "quit, removed_action,quit,zoom").commit();
        assertEquals(Arrays.asList(GameMenuLayout.ID_QUIT, GameMenuLayout.ID_ZOOM), GameMenuLayout.getQuickItems(prefs));
    }

    @Test
    public void resetRestoresDefaults() {
        GameMenuLayout.setQuickItems(prefs, Collections.singletonList(GameMenuLayout.ID_QUIT));
        GameMenuLayout.reset(prefs);
        assertEquals(GameMenuLayout.DEFAULT_QUICK_ITEMS, GameMenuLayout.getQuickItems(prefs));
    }

    @Test
    public void unknownStyleFallsBackToList() {
        prefs.edit().putString(GameMenuLayout.PREF_STYLE, "carousel").commit();
        assertEquals(GameMenuLayout.STYLE_LIST, GameMenuLayout.getStyle(prefs));
        prefs.edit().putString(GameMenuLayout.PREF_STYLE, GameMenuLayout.STYLE_GRID).commit();
        assertEquals(GameMenuLayout.STYLE_GRID, GameMenuLayout.getStyle(prefs));
    }

    @Test
    public void catalogIdsAreUniqueAndDefaultsAreKnown() {
        Set<String> ids = new HashSet<>();
        for (GameMenuLayout.Action action : GameMenuLayout.ACTIONS) {
            assertTrue("duplicate id " + action.id, ids.add(action.id));
        }
        for (String id : GameMenuLayout.DEFAULT_QUICK_ITEMS) {
            assertNotNull(GameMenuLayout.find(id));
        }
    }
}
