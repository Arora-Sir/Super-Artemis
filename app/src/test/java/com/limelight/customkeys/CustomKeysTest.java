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
        // The 75 icons of round 2 plus the sectioned catalogue (IconCatalogTest keeps every shipped ID), all with unique IDs
        assertEquals(true, IconCatalog.ICONS.size() >= 75);
        java.util.Set<String> ids = new java.util.HashSet<>();
        for (IconCatalog.Icon icon : IconCatalog.ICONS) {
            assertEquals(true, ids.add(icon.id));
        }
    }
    // The F13 to F24 row must share the keyboard height with the other rows. Copying the row params without the weight once made it fill the whole keyboard.
    @Test
    public void highFunctionRowIsOneRowTall() {
        Context context = new android.view.ContextThemeWrapper(ApplicationProvider.getApplicationContext(), com.limelight.R.style.AppTheme);
        android.view.ViewGroup keyboard = (android.view.ViewGroup) android.view.LayoutInflater.from(context).inflate(com.limelight.R.layout.layout_axixi_keyboard, null);
        int rowsBefore = keyboard.getChildCount();
        List<Integer> recorded = new ArrayList<>();
        CustomKeyEditor.addHighFunctionKeys(context, keyboard, recorded::add);
        assertEquals(rowsBefore + 1, keyboard.getChildCount());
        keyboard.measure(android.view.View.MeasureSpec.makeMeasureSpec(1200, android.view.View.MeasureSpec.EXACTLY),
                android.view.View.MeasureSpec.makeMeasureSpec(800, android.view.View.MeasureSpec.EXACTLY));
        keyboard.layout(0, 0, 1200, 800);
        int fRow = keyboard.getChildAt(1).getHeight();
        int firstRow = keyboard.getChildAt(0).getHeight();
        assertTrue("F row " + fRow + " vs first row " + firstRow, fRow > 0 && Math.abs(fRow - firstRow) <= 2);
        android.view.ViewGroup row = (android.view.ViewGroup) keyboard.getChildAt(1);
        assertEquals(12, row.getChildCount());
        row.getChildAt(11).performClick();
        assertEquals(Arrays.asList(KeyNames.VK_F24), recorded);
    }
    @Test
    @Config(qualifiers = "w411dp-h891dp-port-xxhdpi")
    public void recorderKeyboardIsTallOnAPhoneInPortrait() {
        Context context = ApplicationProvider.getApplicationContext();
        assertEquals((int) (320 * context.getResources().getDisplayMetrics().density), CustomKeyEditor.recorderKeyboardHeight(context));
    }

    // A phone in landscape has about 384dp of height, so the keyboard keeps its minimum and the dialog still shows the key box and buttons
    @Test
    @Config(qualifiers = "w832dp-h384dp-land-xxhdpi")
    public void recorderKeyboardFitsAPhoneInLandscape() {
        Context context = ApplicationProvider.getApplicationContext();
        assertEquals((int) (184 * context.getResources().getDisplayMetrics().density), CustomKeyEditor.recorderKeyboardHeight(context));
    }

    @Test
    public void recorderKeyLabelsShareOneSize() {
        Context context = new android.view.ContextThemeWrapper(ApplicationProvider.getApplicationContext(), com.limelight.R.style.AppTheme);
        android.view.ViewGroup keyboard = (android.view.ViewGroup) android.view.LayoutInflater.from(context).inflate(com.limelight.R.layout.layout_axixi_keyboard, null);
        CustomKeyEditor.addHighFunctionKeys(context, keyboard, vk -> { });
        keyboard.measure(android.view.View.MeasureSpec.makeMeasureSpec(1080, android.view.View.MeasureSpec.EXACTLY),
                android.view.View.MeasureSpec.makeMeasureSpec(900, android.view.View.MeasureSpec.EXACTLY));
        keyboard.layout(0, 0, 1080, 900);
        CustomKeyEditor.unifyKeyLabels(keyboard);
        java.util.Set<Float> shortSizes = new java.util.HashSet<>();
        collectShortSizes(keyboard, shortSizes);
        assertEquals(shortSizes.toString(), 1, shortSizes.size());
    }

    private static void collectShortSizes(android.view.View view, java.util.Set<Float> sizes) {
        if (view instanceof android.view.ViewGroup) {
            for (int i = 0; i < ((android.view.ViewGroup) view).getChildCount(); i++) {
                collectShortSizes(((android.view.ViewGroup) view).getChildAt(i), sizes);
            }
        }
        else if (view instanceof android.widget.TextView && view.getVisibility() == android.view.View.VISIBLE) {
            CharSequence text = ((android.widget.TextView) view).getText();
            if (text.length() > 0 && text.length() <= 3) {
                sizes.add(((android.widget.TextView) view).getTextSize());
            }
        }
    }
    // Keys tapped in the recorder but not confirmed with Done must not be saveable
    @Test
    public void saveWaitsForDoneAndAStep() {
        assertFalse(CustomKeyEditor.canSave(0, "Ctrl + F13", false));
        assertTrue("The name is optional", CustomKeyEditor.canSave(1, "  ", false));
        assertFalse("keys tapped but not confirmed with Done", CustomKeyEditor.canSave(1, "Ctrl + F13", true));
        assertTrue(CustomKeyEditor.canSave(1, "Ctrl + F13", false));
    }
}
