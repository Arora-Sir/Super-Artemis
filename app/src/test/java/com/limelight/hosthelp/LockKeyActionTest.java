package com.limelight.hosthelp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.limelight.binding.input.KeyboardTranslator;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

// NOTE: The Lock key runs an Apollo server command named Lock when the host has one, and sends Win+L otherwise.
public class LockKeyActionTest {
    private static final short VK_L = 0x4C;
    private static final short VK_RWIN = 0x5C;
    private static final short VK_CTRL = 0x11;

    @Test
    public void onlyWinPlusLCountsAsTheLockKey() {
        assertTrue(LockKeyAction.isLockCombo(new short[] {(short) KeyboardTranslator.VK_LWIN, VK_L}));
        assertTrue(LockKeyAction.isLockCombo(new short[] {VK_RWIN, VK_L}));
        assertFalse(LockKeyAction.isLockCombo(new short[] {VK_CTRL, VK_L}));
        assertFalse(LockKeyAction.isLockCombo(new short[] {(short) KeyboardTranslator.VK_LWIN, VK_L, VK_CTRL}));
        assertFalse(LockKeyAction.isLockCombo(new short[] {(short) KeyboardTranslator.VK_LWIN}));
        assertFalse(LockKeyAction.isLockCombo(null));
    }

    @Test
    public void aCommandNamedLockIsFoundInAnyCaseAndPosition() {
        assertEquals(1, LockKeyAction.findLockCommand(Arrays.asList("Bubbles", "Lock")));
        assertEquals(0, LockKeyAction.findLockCommand(Arrays.asList("lock pc", "Bubbles")));
        assertEquals(2, LockKeyAction.findLockCommand(Arrays.asList("Sleep", "Bubbles", "  LOCK screen ")));
    }

    @Test
    public void unlockAndUnrelatedNamesAreNeverTheLockCommand() {
        assertEquals(-1, LockKeyAction.findLockCommand(Arrays.asList("Unlock", "Bubbles", "Sleep")));
        assertEquals(-1, LockKeyAction.findLockCommand(Collections.<String>emptyList()));
        assertEquals(-1, LockKeyAction.findLockCommand(null));
        assertEquals(-1, LockKeyAction.findLockCommand(Arrays.asList((String) null, "Sleep")));
    }
}
