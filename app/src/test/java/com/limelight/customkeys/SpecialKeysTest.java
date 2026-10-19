package com.limelight.customkeys;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.preference.PreferenceManager;
import androidx.test.core.app.ApplicationProvider;

import com.limelight.GameMenu;
import com.limelight.shadows.ShadowMoonBridge;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, shadows = {ShadowMoonBridge.class})
public class SpecialKeysTest {
    private Context context;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        context.getSharedPreferences(GameMenu.PREF_NAME, Context.MODE_PRIVATE).edit().clear().commit();
        PreferenceManager.getDefaultSharedPreferences(context).edit().clear().commit();
    }

    private static List<String> ids(List<CustomKey> keys) {
        List<String> ids = new ArrayList<>();
        for (CustomKey key : keys) {
            ids.add(key.id);
        }
        return ids;
    }

    @Test
    public void everyBuiltInKeyShowsWithAnIconByDefault() {
        List<CustomKey> keys = SpecialKeys.visible(context);
        assertEquals(BuiltInKeys.ALL.size(), keys.size());
        for (CustomKey key : keys) {
            assertTrue(BuiltInKeys.isBuiltIn(key.id));
            assertTrue(key.icon != null && !key.icon.isEmpty());
            assertFalse(key.keys.isEmpty());
        }
        assertEquals("builtin_esc", keys.get(0).id);
    }

    @Test
    public void hiddenKeysLeaveTheListAndCanComeBack() {
        SpecialKeys.setHidden(context, "builtin_alt_f4", true);
        assertFalse(ids(SpecialKeys.visible(context)).contains("builtin_alt_f4"));
        assertEquals(Collections.singletonList("builtin_alt_f4"), ids(SpecialKeys.hiddenBuiltIns(context)));
        assertNull(SpecialKeys.find(context, "builtin_alt_f4"));

        SpecialKeys.setHidden(context, "builtin_alt_f4", false);
        assertTrue(ids(SpecialKeys.visible(context)).contains("builtin_alt_f4"));
    }

    @Test
    public void builtInIconsCanChangeAndGoBackToTheDefault() {
        SpecialKeys.setBuiltInIcon(context, "builtin_esc", "star");
        assertEquals("star", SpecialKeys.find(context, "builtin_esc").icon);
        SpecialKeys.setBuiltInIcon(context, "builtin_esc", null);
        assertEquals(BuiltInKeys.find("builtin_esc").icon, SpecialKeys.find(context, "builtin_esc").icon);
    }

    @Test
    public void customKeysJoinTheListAndTheSavedOrderWins() {
        CustomKeyStore.save(context, Collections.singletonList(
                new CustomKey("k1", "Task view", false, "desktop_windows", Arrays.asList(0x5B, 0x09))));
        List<CustomKey> keys = SpecialKeys.visible(context);
        // The user's keys follow the Artemis keys, and More keys come after them
        int k1 = ids(keys).indexOf("k1");
        assertFalse(SpecialKeys.isMoreKey(keys.get(k1 - 1).id));
        assertTrue(SpecialKeys.isMoreKey(keys.get(k1 + 1).id));
        assertTrue(SpecialKeys.isMoreKey(keys.get(keys.size() - 1).id));

        List<CustomKey> reordered = new ArrayList<>(keys);
        Collections.reverse(reordered);
        SpecialKeys.saveOrder(context, reordered);
        assertEquals(ids(reordered), ids(SpecialKeys.visible(context)));
        assertEquals("Task view", SpecialKeys.find(context, "k1").name);
    }

    @Test
    public void resetShowsEveryBuiltInKeyAgainAndKeepsCustomKeys() {
        CustomKeyStore.save(context, Collections.singletonList(
                new CustomKey("k1", "Task view", false, null, Arrays.asList(0x5B, 0x09))));
        SpecialKeys.setHidden(context, "builtin_win", true);
        SpecialKeys.setBuiltInIcon(context, "builtin_f11", "bolt");
        SpecialKeys.resetBuiltIns(context);

        List<String> ids = ids(SpecialKeys.visible(context));
        assertEquals(BuiltInKeys.ALL.size() + 1, ids.size());
        assertTrue(ids.contains("builtin_win"));
        assertEquals("fullscreen", SpecialKeys.find(context, "builtin_f11").icon);
        assertTrue(ids.contains("k1"));
    }

    @Test
    public void oldHideDefaultKeysSwitchHidesEveryBuiltInKeyOnce() {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putBoolean(SpecialKeys.LEGACY_HIDE_DEFAULTS, true).commit();
        // The old switch covered the Artemis keys only, so More keys stay visible
        for (CustomKey key : SpecialKeys.visible(context)) {
            assertTrue(SpecialKeys.isMoreKey(key.id));
        }
        int artemisKeys = 0;
        for (BuiltInKeys.Def def : BuiltInKeys.ALL) {
            artemisKeys += def.more ? 0 : 1;
        }
        assertEquals(artemisKeys, SpecialKeys.hiddenBuiltIns(context).size());
        assertEquals(BuiltInKeys.ALL.size() - artemisKeys, SpecialKeys.visible(context).size());
        assertFalse(PreferenceManager.getDefaultSharedPreferences(context).contains(SpecialKeys.LEGACY_HIDE_DEFAULTS));
    }

    @Test
    public void aKeyAddedAfterASavedOrderGoesBeforeMoreKeys() {
        SpecialKeys.saveOrder(context, SpecialKeys.visible(context));
        CustomKeyStore.save(context, Collections.singletonList(
                new CustomKey("k9", "Later key", false, null, Arrays.asList(0x5B, 0x09))));
        List<String> ids = ids(SpecialKeys.visible(context));
        int later = ids.indexOf("k9");
        assertTrue(later > 0);
        assertFalse(SpecialKeys.isMoreKey(ids.get(later - 1)));
        assertTrue(SpecialKeys.isMoreKey(ids.get(later + 1)));
    }

    @Test
    public void moreKeysCoverEverydayShortcutsWithTheirOwnIcons() {
        BuiltInKeys.Def copy = BuiltInKeys.find("builtin_ctrl_c");
        assertTrue(copy.more);
        assertEquals(Arrays.asList(0xA2, 0x43), copy.keys);
        assertEquals("content_copy", copy.icon);
        assertEquals(Arrays.asList(0xA2, 0xA0, 0x1B), BuiltInKeys.find("builtin_ctrl_shift_esc").keys);
        assertFalse(SpecialKeys.isMoreKey("builtin_esc"));
        assertFalse(SpecialKeys.isMoreKey("k1"));
        for (BuiltInKeys.Def def : BuiltInKeys.ALL) {
            if (def.more) {
                assertTrue(def.icon, IconCatalog.drawableFor(def.icon) != IconCatalog.drawableFor("no_such_icon"));
            }
        }
    }

    @Test
    public void builtInIdsAreUniqueAndUseThePrefix() {
        List<String> ids = new ArrayList<>();
        for (BuiltInKeys.Def def : BuiltInKeys.ALL) {
            assertTrue(def.id.startsWith(BuiltInKeys.PREFIX));
            assertFalse(ids.contains(def.id));
            ids.add(def.id);
            assertTrue(IconCatalog.drawableFor(def.icon) != IconCatalog.drawableFor("no_such_icon") || "keyboard".equals(def.icon));
        }
    }
}
