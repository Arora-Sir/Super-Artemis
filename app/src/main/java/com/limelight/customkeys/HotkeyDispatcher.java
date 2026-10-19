package com.limelight.customkeys;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// NOTE: Matches key presses from a keyboard connected to the phone against the hotkeys of the custom keys during a stream.
// NOTE: A match releases the modifiers already sent to the PC, runs the key or flow, and swallows the key down and its key up, unless the hotkey also sends to the PC.
// NOTE: Keys without a hotkey and every key that matches nothing pass through untouched, so the PC keeps getting normal typing.
public final class HotkeyDispatcher {
    public interface Sink {
        // Sends a key up for one modifier the PC may still hold, so the flow does not run with Ctrl or Alt pressed
        void releaseModifier(int vk);

        void run(CustomKey key);
    }

    private final Sink sink;
    private List<CustomKey> bound = new ArrayList<>();
    // Android key codes whose key down was swallowed, so their key up is swallowed too
    private final Set<Integer> swallowed = new HashSet<>();

    public HotkeyDispatcher(Sink sink) {
        this.sink = sink;
    }

    public void setKeys(List<CustomKey> keys) {
        List<CustomKey> withHotkey = new ArrayList<>();
        if (keys != null) {
            for (CustomKey key : keys) {
                if (key.hotkey != null) {
                    withHotkey.add(key);
                }
            }
        }
        bound = withHotkey;
    }

    public boolean hasHotkeys() {
        return !bound.isEmpty();
    }

    // Returns true when the key down is consumed and must not reach the PC
    public boolean onKeyDown(int androidKeyCode, int vk, int metaState, int repeatCount) {
        if (bound.isEmpty() || vk == 0 || Hotkey.modifierBit(vk) != 0) {
            return false;
        }
        if (repeatCount > 0) {
            return swallowed.contains(androidKeyCode);
        }
        int mods = Hotkey.modifiersOf(metaState);
        for (CustomKey key : bound) {
            if (key.hotkey.matches(mods, vk)) {
                if (key.hotkey.toPc) {
                    sink.run(key);
                    return false;
                }
                releaseHeld(mods);
                swallowed.add(androidKeyCode);
                sink.run(key);
                return true;
            }
        }
        return false;
    }

    public boolean onKeyUp(int androidKeyCode) {
        return swallowed.remove(androidKeyCode);
    }

    private void releaseHeld(int mods) {
        if ((mods & Hotkey.CTRL) != 0) {
            sink.releaseModifier(0xA2);
            sink.releaseModifier(0xA3);
        }
        if ((mods & Hotkey.SHIFT) != 0) {
            sink.releaseModifier(0xA0);
            sink.releaseModifier(0xA1);
        }
        if ((mods & Hotkey.ALT) != 0) {
            sink.releaseModifier(0xA4);
            sink.releaseModifier(0xA5);
        }
        if ((mods & Hotkey.WIN) != 0) {
            sink.releaseModifier(0x5B);
            sink.releaseModifier(0x5C);
        }
    }

    // The ID of another key that already uses the same hotkey, or null
    public static String conflict(List<CustomKey> keys, String ownId, Hotkey hotkey) {
        if (hotkey == null || keys == null) {
            return null;
        }
        for (CustomKey key : keys) {
            if (key.hotkey != null && key.hotkey.sameKeys(hotkey) && (ownId == null || !ownId.equals(key.id))) {
                return key.id;
            }
        }
        return null;
    }
}
