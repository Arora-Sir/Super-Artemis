package com.limelight.menu;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;
import androidx.test.core.app.ApplicationProvider;

import com.limelight.actions.StreamAction;
import com.limelight.actions.StreamActionRegistry;
import com.limelight.shadows.ShadowMoonBridge;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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
    public void defaultsToUpstreamQuickMenuOrderWithoutSessionActions() {
        assertEquals(GameMenuLayout.DEFAULT_QUICK_ITEMS, GameMenuLayout.getQuickItems(prefs));
        assertFalse(GameMenuLayout.DEFAULT_QUICK_ITEMS.contains(StreamActionRegistry.ID_DISCONNECT));
        assertFalse(GameMenuLayout.DEFAULT_QUICK_ITEMS.contains(StreamActionRegistry.ID_QUIT));
        assertEquals(GameMenuLayout.STYLE_LIST, GameMenuLayout.getStyle(prefs));
    }

    @Test
    public void savedOrderRoundTrips() {
        List<String> custom = Arrays.asList(StreamActionRegistry.ID_MICROPHONE, StreamActionRegistry.ID_ZOOM, StreamActionRegistry.ID_TOGGLE_HUD);
        GameMenuLayout.setQuickItems(prefs, custom);
        assertEquals(custom, GameMenuLayout.getQuickItems(prefs));
    }

    @Test
    public void emptySelectionIsKept() {
        GameMenuLayout.setQuickItems(prefs, Collections.emptyList());
        assertTrue(GameMenuLayout.getQuickItems(prefs).isEmpty());
    }

    @Test
    public void unknownDuplicateAndSessionIdsAreDropped() {
        // A layout saved before 1.0.0 still lists Disconnect and Quit, which now always live in the Session section
        prefs.edit().putString(GameMenuLayout.PREF_QUICK_ITEMS, "disconnect,quit, removed_action,zoom,zoom,microphone").commit();
        assertEquals(Arrays.asList(StreamActionRegistry.ID_ZOOM, StreamActionRegistry.ID_MICROPHONE), GameMenuLayout.getQuickItems(prefs));
    }

    @Test
    public void pinnedSpecialKeysAreKeptInOrder() {
        prefs.edit().putString(GameMenuLayout.PREF_QUICK_ITEMS, "key:k1,zoom,key:,key:k1,key:builtin_ctrl_c").commit();
        assertEquals(Arrays.asList("key:k1", StreamActionRegistry.ID_ZOOM, "key:builtin_ctrl_c"), GameMenuLayout.getQuickItems(prefs));
        assertEquals("k1", GameMenuLayout.keyId("key:k1"));
        assertEquals(null, GameMenuLayout.keyId(StreamActionRegistry.ID_ZOOM));
    }

    @Test
    public void hiddenItemsKeepActionsOnly() {
        GameMenuLayout.setHiddenItems(prefs, Arrays.asList(StreamActionRegistry.ID_ROTATE_SCREEN, "key:k1", StreamActionRegistry.ID_DISCONNECT, "removed_action"));
        assertEquals(Collections.singleton(StreamActionRegistry.ID_ROTATE_SCREEN), GameMenuLayout.getHiddenItems(prefs));
        GameMenuLayout.reset(prefs);
        assertTrue(GameMenuLayout.getHiddenItems(prefs).isEmpty());
    }

    @Test
    public void resetRestoresDefaults() {
        GameMenuLayout.setQuickItems(prefs, Collections.singletonList(StreamActionRegistry.ID_ZOOM));
        GameMenuLayout.reset(prefs);
        assertEquals(GameMenuLayout.DEFAULT_QUICK_ITEMS, GameMenuLayout.getQuickItems(prefs));
    }

    @Test
    public void unknownStyleFallsBackToListAndSetStyleNormalizes() {
        prefs.edit().putString(GameMenuLayout.PREF_STYLE, "carousel").commit();
        assertEquals(GameMenuLayout.STYLE_LIST, GameMenuLayout.getStyle(prefs));
        GameMenuLayout.setStyle(prefs, GameMenuLayout.STYLE_GRID);
        assertEquals(GameMenuLayout.STYLE_GRID, GameMenuLayout.getStyle(prefs));
        GameMenuLayout.setStyle(prefs, "carousel");
        assertEquals(GameMenuLayout.STYLE_LIST, GameMenuLayout.getStyle(prefs));
    }

    @Test
    public void configurableActionsExcludeSessionActionsAndDefaultsAreConfigurable() {
        for (StreamAction action : GameMenuLayout.configurableActions()) {
            assertFalse(action.id, action.destructive);
        }
        for (String id : GameMenuLayout.DEFAULT_QUICK_ITEMS) {
            assertNotNull(id, StreamActionRegistry.find(id));
            assertTrue(id, GameMenuLayout.isConfigurable(id));
        }
    }
}
