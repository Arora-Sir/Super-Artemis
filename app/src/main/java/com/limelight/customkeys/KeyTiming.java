package com.limelight.customkeys;

import com.limelight.binding.input.KeyboardTranslator;
import com.limelight.nvstream.input.KeyboardPacket;

// NOTE: How a shortcut from a button or a flow is sent: how long the keys stay down and which modifier flag the Windows key carries.
// NOTE: A person holds Win+L for 80 to 150 ms and Android marks the Windows key down with its own modifier, while a button used to hold 25 ms and send it without the flag, and Windows ignored the lock.
// NOTE: Every other shortcut keeps its 25 ms hold, so existing keys behave exactly as before.
public final class KeyTiming {
    public static final long DEFAULT_HOLD_MS = 25;
    public static final long WINDOWS_KEY_HOLD_MS = 120;
    // Windows virtual key code of the right Windows key, next to KeyboardTranslator.VK_LWIN
    private static final short VK_RWIN = 0x5C;

    private KeyTiming() {
    }

    public static boolean isWindowsKey(short key) {
        return key == KeyboardTranslator.VK_LWIN || key == VK_RWIN;
    }

    // How long a shortcut keeps its keys down before releasing them
    public static long holdMs(short[] keys) {
        for (short key : keys) {
            if (isWindowsKey(key)) {
                return WINDOWS_KEY_HOLD_MS;
            }
        }
        return DEFAULT_HOLD_MS;
    }

    // The modifier flags of a key down. The Windows key carries its own flag, as it does from a hardware keyboard.
    public static byte downModifier(short key, byte modifiers) {
        return isWindowsKey(key) ? (byte) (modifiers | KeyboardPacket.MODIFIER_META) : modifiers;
    }
}
