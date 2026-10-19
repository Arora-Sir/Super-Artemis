package com.limelight.customkeys;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.view.KeyEvent;

import com.limelight.shadows.ShadowMoonBridge;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

// NOTE: Phone-side hotkeys: matching, storage in the key JSON, conflicts, and what reaches the PC when one fires.
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, shadows = {ShadowMoonBridge.class})
public class HotkeysTest {
    private static final int VK_1 = 0x31;
    private static final int CTRL_ALT = KeyEvent.META_CTRL_ON | KeyEvent.META_CTRL_LEFT_ON | KeyEvent.META_ALT_ON | KeyEvent.META_ALT_LEFT_ON;

    private static CustomKey key(String id, Hotkey hotkey) {
        return new CustomKey(id, "Key " + id, false, null, Collections.singletonList(0x5B)).withHotkey(hotkey);
    }

    @Test
    public void eitherSideOfAModifierMatchesAndExtraModifiersDoNot() {
        Hotkey hotkey = Hotkey.fromVirtualKeys(Arrays.asList(0xA3, 0xA5, VK_1), false);
        assertEquals(Hotkey.CTRL | Hotkey.ALT, hotkey.modifiers);
        assertTrue(hotkey.matches(Hotkey.modifiersOf(CTRL_ALT), VK_1));
        assertFalse(hotkey.matches(Hotkey.modifiersOf(CTRL_ALT | KeyEvent.META_SHIFT_ON), VK_1));
        assertFalse(hotkey.matches(Hotkey.modifiersOf(KeyEvent.META_CTRL_ON), VK_1));
        assertEquals(Arrays.asList(0xA2, 0xA4, VK_1), hotkey.virtualKeys());
        assertNull("only modifiers is not a hotkey", Hotkey.fromVirtualKeys(Arrays.asList(0xA2, 0xA4), false));
    }

    @Test
    public void commonPcShortcutsAreFlagged() {
        assertTrue(new Hotkey(Hotkey.ALT, 0x09, false).isCommonPcShortcut());
        assertTrue(new Hotkey(Hotkey.CTRL, 'C', false).isCommonPcShortcut());
        assertTrue(new Hotkey(0, 'A', false).isCommonPcShortcut());
        assertFalse(new Hotkey(Hotkey.CTRL | Hotkey.ALT, VK_1, false).isCommonPcShortcut());
        assertFalse(new Hotkey(0, KeyNames.VK_F13, false).isCommonPcShortcut());
    }

    @Test
    public void hotkeySurvivesTheKeyFileAndOlderFilesHaveNone() throws Exception {
        List<CustomKey> keys = new ArrayList<>();
        keys.add(key("a", new Hotkey(Hotkey.CTRL | Hotkey.ALT, VK_1, true)));
        keys.add(key("b", null));
        List<CustomKey> back = CustomKeyStore.parse(CustomKeyStore.toJson(keys));
        assertTrue(back.get(0).hotkey.sameKeys(keys.get(0).hotkey));
        assertTrue(back.get(0).hotkey.toPc);
        assertNull(back.get(1).hotkey);
        assertNull(CustomKeyStore.parse("{\"data\":[{\"id\":\"x\",\"name\":\"Old\",\"keys\":[\"0x5B\"]}]}").get(0).hotkey);
        // A broken hotkey is dropped and the key itself still loads
        CustomKey broken = CustomKeyStore.parse("{\"data\":[{\"id\":\"x\",\"name\":\"Old\",\"keys\":[\"0x5B\"],\"hotkey\":[\"zz\"]}]}").get(0);
        assertNull(broken.hotkey);
        assertEquals("Old", broken.name);
    }

    @Test
    public void aHotkeyAnotherKeyUsesIsAConflictButItsOwnIsNot() {
        Hotkey hotkey = new Hotkey(Hotkey.CTRL | Hotkey.ALT, VK_1, false);
        List<CustomKey> keys = Arrays.asList(key("a", hotkey), key("b", null));
        assertEquals("a", HotkeyDispatcher.conflict(keys, "b", hotkey));
        assertEquals("a", HotkeyDispatcher.conflict(keys, null, hotkey.withToPc(true)));
        assertNull(HotkeyDispatcher.conflict(keys, "a", hotkey));
        assertNull(HotkeyDispatcher.conflict(keys, "b", new Hotkey(Hotkey.CTRL | Hotkey.ALT, 0x32, false)));
    }

    private static final class FakeSink implements HotkeyDispatcher.Sink {
        final List<Integer> released = new ArrayList<>();
        final List<String> ran = new ArrayList<>();

        @Override
        public void releaseModifier(int vk) {
            released.add(vk);
        }

        @Override
        public void run(CustomKey key) {
            ran.add(key.id);
        }
    }

    @Test
    public void aMatchRunsTheKeyReleasesHeldModifiersAndSwallowsTheKey() {
        FakeSink sink = new FakeSink();
        HotkeyDispatcher dispatcher = new HotkeyDispatcher(sink);
        dispatcher.setKeys(Arrays.asList(key("a", new Hotkey(Hotkey.CTRL | Hotkey.ALT, VK_1, false)), key("b", null)));

        assertFalse("modifier presses always reach the PC", dispatcher.onKeyDown(KeyEvent.KEYCODE_CTRL_LEFT, 0xA2, KeyEvent.META_CTRL_ON, 0));
        assertTrue(dispatcher.onKeyDown(KeyEvent.KEYCODE_1, VK_1, CTRL_ALT, 0));
        assertEquals(Collections.singletonList("a"), sink.ran);
        assertTrue(sink.released.containsAll(Arrays.asList(0xA2, 0xA3, 0xA4, 0xA5)));
        assertTrue("auto repeat of the swallowed key is swallowed", dispatcher.onKeyDown(KeyEvent.KEYCODE_1, VK_1, CTRL_ALT, 1));
        assertEquals(1, sink.ran.size());
        assertTrue(dispatcher.onKeyUp(KeyEvent.KEYCODE_1));
        assertFalse("the next key up is the PC's again", dispatcher.onKeyUp(KeyEvent.KEYCODE_1));

        assertFalse("other keys pass through", dispatcher.onKeyDown(KeyEvent.KEYCODE_2, 0x32, CTRL_ALT, 0));
        assertFalse("the same key without the modifiers passes through", dispatcher.onKeyDown(KeyEvent.KEYCODE_1, VK_1, 0, 0));
        assertEquals(1, sink.ran.size());
    }

    @Test
    public void alsoSendToPcRunsTheKeyAndLetsThePressThrough() {
        FakeSink sink = new FakeSink();
        HotkeyDispatcher dispatcher = new HotkeyDispatcher(sink);
        dispatcher.setKeys(Collections.singletonList(key("a", new Hotkey(Hotkey.CTRL | Hotkey.ALT, VK_1, true))));
        assertFalse(dispatcher.onKeyDown(KeyEvent.KEYCODE_1, VK_1, CTRL_ALT, 0));
        assertEquals(Collections.singletonList("a"), sink.ran);
        assertTrue(sink.released.isEmpty());
        assertFalse(dispatcher.onKeyUp(KeyEvent.KEYCODE_1));
    }

    @Test
    public void noHotkeysMeansNothingIsTouched() {
        FakeSink sink = new FakeSink();
        HotkeyDispatcher dispatcher = new HotkeyDispatcher(sink);
        dispatcher.setKeys(Collections.singletonList(key("b", null)));
        assertFalse(dispatcher.hasHotkeys());
        assertFalse(dispatcher.onKeyDown(KeyEvent.KEYCODE_1, VK_1, CTRL_ALT, 0));
        assertTrue(sink.ran.isEmpty());
    }
}
