package com.limelight.customkeys;

import android.view.KeyEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// NOTE: A phone-side hotkey: modifiers plus one key pressed on a keyboard connected to the phone, which runs a key or flow during a stream.
// NOTE: Keys are Windows virtual key codes like the rest of the custom keys, with each modifier stored once (left Ctrl, left Shift, left Alt, left Win) so either side matches.
public final class Hotkey {
    public static final int CTRL = 1;
    public static final int SHIFT = 2;
    public static final int ALT = 4;
    public static final int WIN = 8;

    static final int VK_SHIFT = 0xA0;
    static final int VK_CTRL = 0xA2;
    static final int VK_ALT = 0xA4;
    static final int VK_WIN = 0x5B;

    public final int modifiers;
    public final int key;
    // True when the key also reaches the PC after the flow starts
    public final boolean toPc;

    public Hotkey(int modifiers, int key, boolean toPc) {
        this.modifiers = modifiers & (CTRL | SHIFT | ALT | WIN);
        this.key = key;
        this.toPc = toPc;
    }

    public Hotkey withToPc(boolean value) {
        return new Hotkey(modifiers, key, value);
    }

    public static int modifiersOf(int metaState) {
        int mods = 0;
        if ((metaState & KeyEvent.META_CTRL_ON) != 0) {
            mods |= CTRL;
        }
        if ((metaState & KeyEvent.META_SHIFT_ON) != 0) {
            mods |= SHIFT;
        }
        if ((metaState & KeyEvent.META_ALT_ON) != 0) {
            mods |= ALT;
        }
        if ((metaState & KeyEvent.META_META_ON) != 0) {
            mods |= WIN;
        }
        return mods;
    }

    // Both sides of a modifier turn into its single bit, any other key returns 0
    public static int modifierBit(int vk) {
        switch (vk) {
            case 0xA0:
            case 0xA1:
            case 0x10:
                return SHIFT;
            case 0xA2:
            case 0xA3:
            case 0x11:
                return CTRL;
            case 0xA4:
            case 0xA5:
            case 0x12:
                return ALT;
            case 0x5B:
            case 0x5C:
                return WIN;
            default:
                return 0;
        }
    }

    public boolean matches(int pressedModifiers, int pressedKey) {
        return key != 0 && key == pressedKey && modifiers == pressedModifiers;
    }

    public boolean sameKeys(Hotkey other) {
        return other != null && other.key == key && other.modifiers == modifiers;
    }

    // Modifiers first in a fixed order, then the key, the same order the recorder shows
    public List<Integer> virtualKeys() {
        List<Integer> vks = new ArrayList<>();
        if ((modifiers & CTRL) != 0) {
            vks.add(VK_CTRL);
        }
        if ((modifiers & SHIFT) != 0) {
            vks.add(VK_SHIFT);
        }
        if ((modifiers & ALT) != 0) {
            vks.add(VK_ALT);
        }
        if ((modifiers & WIN) != 0) {
            vks.add(VK_WIN);
        }
        vks.add(key);
        return Collections.unmodifiableList(vks);
    }

    // Rebuilds a hotkey from stored virtual keys, or returns null when there is no key that is not a modifier
    public static Hotkey fromVirtualKeys(List<Integer> vks, boolean toPc) {
        if (vks == null) {
            return null;
        }
        int mods = 0;
        int key = 0;
        for (int vk : vks) {
            int bit = modifierBit(vk);
            if (bit != 0) {
                mods |= bit;
            }
            else if (key == 0) {
                key = vk;
            }
        }
        return key == 0 ? null : new Hotkey(mods, key, toPc);
    }

    // NOTE: Shortcuts Windows or most PC apps use all the time. Binding one is allowed, but the editor warns that the PC will no longer get it.
    public boolean isCommonPcShortcut() {
        switch (modifiers) {
            case ALT:
                return key == 0x09 || key == 0x73;
            case CTRL:
                return key == 'A' || key == 'C' || key == 'V' || key == 'X' || key == 'Z' || key == 'Y' || key == 'S' || key == 'F' || key == 'W' || key == 'T' || key == 0x09;
            case CTRL | SHIFT:
                return key == 0x1B || key == 'T' || key == 'Z' || key == 0x09;
            case CTRL | ALT:
                return key == 0x2E;
            case WIN:
                return key == 'D' || key == 'E' || key == 'L' || key == 'R' || key == 'V' || key == 'I' || key == 0x09;
            case 0:
                // A key on its own is typing or a PC shortcut, except F13 to F24 which no physical keyboard sends
                return key < KeyNames.VK_F13 || key > KeyNames.VK_F24;
            default:
                return false;
        }
    }
}
