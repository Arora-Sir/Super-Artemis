package com.limelight;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import androidx.fragment.app.FragmentFactory;

import com.limelight.customkeys.CustomKeysTestAccess;
import com.limelight.preferences.StreamSettings;
import com.limelight.shadows.ShadowMoonBridge;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

// Regression tests for crashes found in the code audit of the round 2 features
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, shadows = {ShadowMoonBridge.class})
public class CrashGuardsTest {
    // The system recreates the settings fragment by class name after a style change or process death
    @Test
    public void settingsFragmentCanBeRecreatedBySystem() {
        Object fragment = new FragmentFactory().instantiate(getClass().getClassLoader(), StreamSettings.SettingsFragment.class.getName());
        assertNotNull(fragment);
    }

    // A hand-edited or imported custom keys file with null entries loads the valid keys instead of crashing at stream start
    @Test
    public void nullCustomKeyEntriesAreSkipped() throws Exception {
        String json = "{\"data\":[null,{\"id\":\"a\",\"name\":\"Copy\",\"keys\":[\"0x11\",\"0x43\"]}]}";
        assertEquals(1, CustomKeysTestAccess.parse(json).size());
        boolean refused = false;
        try {
            CustomKeysTestAccess.parse("{\"data\":[{\"id\":\"b\",\"name\":\"Bad\",\"keys\":[null]}]}");
        }
        catch (Exception expected) {
            refused = true;
        }
        assertTrue(refused);
    }
}
