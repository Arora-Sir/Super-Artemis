package com.limelight.customkeys;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.limelight.GameMenu;
import com.limelight.binding.input.KeyboardTranslator;
import com.limelight.shadows.ShadowMoonBridge;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, shadows = {ShadowMoonBridge.class})
public class CustomKeysTest {
    private Context context;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        context.getSharedPreferences(GameMenu.PREF_NAME, Context.MODE_PRIVATE).edit().clear().commit();
    }

    @Test
    public void legacyFileWithoutIdsUsesListPositionsAndVkNames() throws Exception {
        // Format documented on the upstream wiki, written by hand before the in-app editor existed
        List<CustomKey> keys = CustomKeyStore.parse("{\"data\":[{\"name\":\"Copy\",\"keys\":[\"0xA2\",\"0x43\"]},"
                + "{\"name\":\"Start\",\"sticky\":true,\"keys\":[\"VK_LWIN\"]}]}");

        assertEquals(2, keys.size());
        assertEquals("0", keys.get(0).id);
        assertEquals("1", keys.get(1).id);
        assertEquals(Arrays.asList(0xA2, 0x43), keys.get(0).keys);
        assertEquals(Arrays.asList(KeyboardTranslator.VK_LWIN), keys.get(1).keys);
        assertTrue(keys.get(1).sticky);
        assertNull(keys.get(0).icon);
    }

    @Test
    public void saveAndLoadRoundTripKeepsIdsIconsAndOrder() throws Exception {
        List<CustomKey> keys = new ArrayList<>();
        keys.add(new CustomKey("0", "Task view", false, "desktop_windows", Arrays.asList(0x5B, 0x09)));
        keys.add(new CustomKey("k1", "Escape", true, null, Arrays.asList(0x1B)));
        CustomKeyStore.save(context, keys);

        List<CustomKey> loaded = CustomKeyStore.load(context);
        assertEquals(2, loaded.size());
        assertEquals("0", loaded.get(0).id);
        assertEquals("desktop_windows", loaded.get(0).icon);
        assertArrayEquals(new short[]{0x5B, 0x09}, loaded.get(0).keyCodes());
        assertEquals("k1", loaded.get(1).id);
        assertTrue(loaded.get(1).sticky);
        assertTrue(CustomKeyStore.toJson(keys).contains("\"0x5B\""));
    }

    @Test
    public void upsertReplacesByIdAndAppendsNewKeysWithFreshIds() throws Exception {
        List<CustomKey> keys = new ArrayList<>();
        keys.add(new CustomKey("0", "Old", false, null, Arrays.asList(0x1B)));

        CustomKeyStore.upsert(keys, new CustomKey("0", "Renamed", false, "bolt", Arrays.asList(0x1B)));
        CustomKey added = CustomKeyStore.upsert(keys, new CustomKey(null, "New", false, null, Arrays.asList(0x70)));
        CustomKey second = CustomKeyStore.upsert(keys, new CustomKey(null, "Newer", false, null, Arrays.asList(0x71)));

        assertEquals(3, keys.size());
        assertEquals("Renamed", keys.get(0).name);
        assertFalse(added.id.isEmpty());
        assertNotEquals(added.id, second.id);
        assertNotEquals("0", added.id);
    }

    @Test
    public void unreadableFileIsReportedNotSilentlyOverwritten() {
        context.getSharedPreferences(GameMenu.PREF_NAME, Context.MODE_PRIVATE).edit()
                .putString(GameMenu.KEY_NAME, "{\"data\":[{\"name\":\"Bad\",\"keys\":[\"F13\"]}]}").commit();
        try {
            CustomKeyStore.load(context);
            fail("Expected FormatException");
        }
        catch (CustomKeyStore.FormatException expected) {
            // expected
        }
        assertTrue(CustomKeyStore.loadOrEmpty(context).isEmpty());
    }

    @Test
    public void recorderAddsEachKeyOnceUpToTheLimit() {
        List<Integer> keys = new ArrayList<>();
        assertTrue(CustomKeyEditor.addKey(keys, 0xA2));
        assertFalse("duplicate", CustomKeyEditor.addKey(keys, 0xA2));
        assertFalse("no Windows equivalent", CustomKeyEditor.addKey(keys, 0));
        for (int vk = 0x41; keys.size() < CustomKeyEditor.MAX_KEYS; vk++) {
            assertTrue(CustomKeyEditor.addKey(keys, vk));
        }
        assertFalse("too many keys", CustomKeyEditor.addKey(keys, 0x70));
    }

    @Test
    public void keyNamesComeFromTheBuiltInKeyboard() {
        KeyNames names = new KeyNames(context);
        assertEquals(0x1B, names.virtualKeyFor(android.view.KeyEvent.KEYCODE_ESCAPE));
        assertEquals("ESC", names.label(0x1B));
        assertEquals("0xFF", names.label(0xFF));
    }

    @Test
    public void unknownIconFallsBackToKeyboard() {
        assertEquals(IconCatalog.drawableFor("keyboard"), IconCatalog.drawableFor("removed_icon"));
        // 28 original icons and 33 added in round 2, with unique IDs
        assertEquals(75, IconCatalog.ICONS.size());
        java.util.Set<String> ids = new java.util.HashSet<>();
        for (IconCatalog.Icon icon : IconCatalog.ICONS) {
            assertEquals(true, ids.add(icon.id));
        }
    }
}
