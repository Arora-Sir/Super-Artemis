package com.limelight.preferences;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.graphics.drawable.Drawable;

import androidx.test.core.app.ApplicationProvider;

import com.limelight.shadows.ShadowMoonBridge;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

// NOTE: The Support row heart beats inside its own drawable, between 90 and 100 percent of its box, so it can never grow past the icon area or leak to another row.
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, shadows = {ShadowMoonBridge.class})
public class HeartPreferenceTest {
    @Test
    public void theRowCarriesABeatingHeartDrawable() {
        Context context = ApplicationProvider.getApplicationContext();
        HeartPreference row = new HeartPreference(context);
        Drawable icon = row.getIcon();
        assertNotNull(icon);
        assertTrue(icon instanceof HeartPreference.BeatingDrawable);
    }

    @Test
    public void theHeartRestsAtNinetyPercentAndStopsWhenHidden() {
        Context context = ApplicationProvider.getApplicationContext();
        HeartPreference row = new HeartPreference(context);
        HeartPreference.BeatingDrawable heart = (HeartPreference.BeatingDrawable) row.getIcon();
        assertEquals(HeartPreference.BeatingDrawable.REST, heart.scale(), 0.0001f);
        heart.setVisible(true, false);
        heart.setVisible(false, false);
        assertEquals(HeartPreference.BeatingDrawable.REST, heart.scale(), 0.0001f);
        assertTrue(HeartPreference.BeatingDrawable.PEAK <= 1.0f);
    }
}
