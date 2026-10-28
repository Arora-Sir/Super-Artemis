package com.limelight.customkeys;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.limelight.shadows.ShadowMoonBridge;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Arrays;

// NOTE: A key name is optional. A single shortcut reads "Ctrl + M" or "Ctrl + M (Gemini Talk)", a flow reads as its name, and pickers say Key or Flow.
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, shadows = {ShadowMoonBridge.class})
public class KeyLabelsTest {
    private final Context context = ApplicationProvider.getApplicationContext();

    private static CustomKey combo(String id, String name, Integer... keys) {
        return new CustomKey(id, name, false, null, Arrays.asList(keys));
    }

    private static CustomKey flow(String name) {
        return CustomKey.withSteps("f", name, false, null, Arrays.asList(
                KeyStep.combo(Arrays.asList(0xA2, 0x43)), KeyStep.text("hello")));
    }

    @Test
    public void aShortcutWithoutANameReadsAsItsKeys() {
        assertEquals("Ctrl + M", KeyLabels.displayLabel(context, combo("a", "", 0xA2, 0x4D)));
    }

    @Test
    public void aShortcutWithANameShowsKeysAndName() {
        assertEquals("Ctrl + M (Gemini Talk)", KeyLabels.displayLabel(context, combo("a", "Gemini Talk", 0xA2, 0x4D)));
    }

    @Test
    public void aNameThatIsJustTheKeysIsNotRepeated() {
        assertEquals("Ctrl + M", KeyLabels.displayLabel(context, combo("a", "Ctrl + M", 0xA2, 0x4D)));
    }

    @Test
    public void aFlowReadsAsItsNameOrItsFirstStep() {
        assertEquals("Greeting", KeyLabels.displayLabel(context, flow("Greeting")));
        assertTrue(KeyLabels.displayLabel(context, flow("")).endsWith("(+1)"));
    }

    @Test
    public void builtInKeysKeepTheirOwnName() {
        assertEquals("Lock", KeyLabels.displayLabel(context, combo(BuiltInKeys.PREFIX + "win_l", "Lock", 0x5B, 0x4C)));
    }

    @Test
    public void pickersSayKeyForAShortcutAndFlowForASequence() {
        assertEquals("Key: Ctrl + M (Gemini Talk)", KeyLabels.pickerLabel(context, combo("a", "Gemini Talk", 0xA2, 0x4D)));
        assertEquals("Flow: Greeting", KeyLabels.pickerLabel(context, flow("Greeting")));
    }
}
