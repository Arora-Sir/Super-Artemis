package com.limelight;

import static org.junit.Assert.assertEquals;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.limelight.shadows.ShadowMoonBridge;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

// The mouse mode picker shows one icon per mode by position, so a mode added to the names needs its icon too
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, shadows = {ShadowMoonBridge.class})
public class MouseModeIconsTest {
    @Test
    public void everyMouseModeHasAnIcon() {
        Context context = ApplicationProvider.getApplicationContext();
        assertEquals(context.getResources().getStringArray(R.array.mouse_mode_names).length, Game.MOUSE_MODE_ICONS.length);
    }
}
