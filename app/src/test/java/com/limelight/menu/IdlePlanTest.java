package com.limelight.menu;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;
import androidx.test.core.app.ApplicationProvider;

import com.limelight.shadows.ShadowMoonBridge;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

// NOTE: The idle plan has two stages that count from the last touch, and a time of 0 switches a stage off.
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, shadows = {ShadowMoonBridge.class})
public class IdlePlanTest {
    @Test
    public void theDefaultsDimAfterThreeSecondsAndNeverHide() {
        IdlePlan plan = new IdlePlan(IdlePlan.DEFAULT_DIM_SECONDS, IdlePlan.DEFAULT_DIM_PERCENT, IdlePlan.DEFAULT_HIDE_SECONDS, IdlePlan.DEFAULT_TAB_PERCENT);
        assertEquals(3000, plan.dimDelayMs());
        assertEquals(-1, plan.hideDelayMs());
        assertEquals(0.4f, plan.dimAlpha(), 0.001f);
    }

    @Test
    public void bothStagesRunInOrderWhenTheDimTimeIsShorter() {
        IdlePlan plan = new IdlePlan(5, 30, 20, 100);
        assertEquals(5000, plan.dimDelayMs());
        assertEquals(20000, plan.hideDelayMs());
    }

    @Test
    public void aDimTimeAtOrAfterTheHideTimeSkipsDimmingBecauseItWouldNeverShow() {
        assertEquals(-1, new IdlePlan(10, 40, 10, 100).dimDelayMs());
        assertEquals(-1, new IdlePlan(30, 40, 10, 100).dimDelayMs());
        assertEquals(10000, new IdlePlan(30, 40, 10, 100).hideDelayMs());
    }

    @Test
    public void aDimTimeOfZeroSkipsDimmingAndAHideTimeOfZeroNeverHides() {
        IdlePlan plan = new IdlePlan(0, 40, 0, 100);
        assertEquals(-1, plan.dimDelayMs());
        assertEquals(-1, plan.hideDelayMs());
    }

    @Test
    public void anEdgeTabOpacityOfZeroMeansNoTab() {
        assertFalse(new IdlePlan(3, 40, 10, 0).showsTab());
        assertTrue(new IdlePlan(3, 40, 10, 1).showsTab());
        assertEquals(0.5f, new IdlePlan(3, 40, 10, 50).tabAlpha(), 0.001f);
    }

    @Test
    public void valuesOutsideTheRangeAreClamped() {
        IdlePlan plan = new IdlePlan(-4, 250, 9999, -1);
        assertEquals(0, plan.dimSeconds);
        assertEquals(100, plan.dimPercent);
        assertEquals(IdlePlan.MAX_SECONDS, plan.hideSeconds);
        assertEquals(0, plan.tabPercent);
    }

    @Test
    public void thePlanSurvivesASaveAndALoadAndToleratesAWrongTypedValue() {
        Context context = ApplicationProvider.getApplicationContext();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().clear().commit();
        new IdlePlan(7, 25, 40, 60).save(prefs);
        IdlePlan loaded = IdlePlan.read(prefs);
        assertEquals(7, loaded.dimSeconds);
        assertEquals(25, loaded.dimPercent);
        assertEquals(40, loaded.hideSeconds);
        assertEquals(60, loaded.tabPercent);

        prefs.edit().putString(IdlePlan.PREF_DIM_SECONDS, "oops").commit();
        assertEquals(IdlePlan.DEFAULT_DIM_SECONDS, IdlePlan.read(prefs).dimSeconds);
    }
}
