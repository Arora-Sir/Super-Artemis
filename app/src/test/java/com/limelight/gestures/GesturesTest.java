package com.limelight.gestures;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;
import androidx.test.core.app.ApplicationProvider;

import com.limelight.actions.StreamActionRegistry;
import com.limelight.shadows.ShadowMoonBridge;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, shadows = {ShadowMoonBridge.class})
public class GesturesTest {
    private SharedPreferences prefs;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().clear().commit();
    }

    private static GestureBindingStore.Slot tap(int fingers) {
        return new GestureBindingStore.Slot(fingers, false);
    }

    private static GestureBindingStore.Slot doubleTap(int fingers) {
        return new GestureBindingStore.Slot(fingers, true);
    }

    @Test
    public void defaultsMatchTheGesturesArtemisAlwaysHad() {
        assertEquals(StreamActionRegistry.ID_TOGGLE_KEYBOARD, GestureBindingStore.get(prefs, tap(3)));
        assertEquals(StreamActionRegistry.ID_TOGGLE_FULL_KEYBOARD, GestureBindingStore.get(prefs, tap(4)));
        assertEquals(GestureBindingStore.QUICK_MENU, GestureBindingStore.get(prefs, tap(5)));
        for (int fingers : GestureBindingStore.FINGER_COUNTS) {
            assertEquals(GestureBindingStore.NONE, GestureBindingStore.get(prefs, doubleTap(fingers)));
        }
    }

    @Test
    public void oldFiveFingerSwitchOffMigratesToNothing() {
        prefs.edit().putBoolean(GestureBindingStore.LEGACY_FIVE_FINGER_MENU, false).commit();
        assertEquals(GestureBindingStore.NONE, GestureBindingStore.get(prefs, tap(5)));
    }

    @Test
    public void bindingsRoundTripAndSessionActionsAreRefused() {
        GestureBindingStore.set(prefs, doubleTap(3), GestureBindingStore.MIDDLE_CLICK);
        GestureBindingStore.set(prefs, tap(4), "key:k1");
        GestureBindingStore.set(prefs, tap(5), StreamActionRegistry.ID_QUIT);
        assertEquals(GestureBindingStore.MIDDLE_CLICK, GestureBindingStore.get(prefs, doubleTap(3)));
        assertEquals("k1", GestureBindingStore.customKeyId(GestureBindingStore.get(prefs, tap(4))));
        assertEquals(GestureBindingStore.NONE, GestureBindingStore.get(prefs, tap(5)));

        GestureBindingStore.reset(prefs);
        assertEquals(StreamActionRegistry.ID_TOGGLE_FULL_KEYBOARD, GestureBindingStore.get(prefs, tap(4)));
    }

    @Test
    public void slotKeysAreUnique() {
        Set<String> keys = new HashSet<>();
        for (GestureBindingStore.Slot slot : GestureBindingStore.SLOTS) {
            assertTrue(keys.add(slot.prefKey()));
        }
        assertEquals(6, keys.size());
    }

    // Records gestures and lets the test fire scheduled callbacks by hand
    private static final class Harness implements MultiFingerTapDetector.Scheduler, MultiFingerTapDetector.Listener {
        final List<String> fired = new ArrayList<>();
        final List<Runnable> scheduled = new ArrayList<>();

        @Override
        public void schedule(Runnable runnable, long delayMs) {
            assertEquals(MultiFingerTapDetector.DOUBLE_TAP_WINDOW_MS, delayMs);
            scheduled.add(runnable);
        }

        @Override
        public void cancel(Runnable runnable) {
            scheduled.remove(runnable);
        }

        @Override
        public void onGesture(int fingers, boolean doubleTap) {
            fired.add(fingers + (doubleTap ? "x2" : "x1"));
        }

        void elapse() {
            for (Runnable runnable : new ArrayList<>(scheduled)) {
                scheduled.remove(runnable);
                runnable.run();
            }
        }
    }

    @Test
    public void singleTapFiresAtOnceWhenNoDoubleTapIsBound() {
        Harness harness = new Harness();
        MultiFingerTapDetector detector = new MultiFingerTapDetector(harness, harness, fingers -> false);
        detector.onTap(3, 1000);
        assertEquals(Collections.singletonList("3x1"), harness.fired);
        assertTrue(harness.scheduled.isEmpty());
    }

    @Test
    public void secondTapInsideTheWindowIsADoubleTap() {
        Harness harness = new Harness();
        MultiFingerTapDetector detector = new MultiFingerTapDetector(harness, harness, fingers -> fingers == 3);
        detector.onTap(3, 1000);
        assertTrue(harness.fired.isEmpty());
        detector.onTap(3, 1250);
        assertEquals(Collections.singletonList("3x2"), harness.fired);
        harness.elapse();
        assertEquals(Collections.singletonList("3x2"), harness.fired);
    }

    @Test
    public void singleTapFiresAfterTheWindowWhenNoSecondTapComes() {
        Harness harness = new Harness();
        MultiFingerTapDetector detector = new MultiFingerTapDetector(harness, harness, fingers -> fingers == 3);
        detector.onTap(3, 1000);
        harness.elapse();
        assertEquals(Collections.singletonList("3x1"), harness.fired);
    }

    @Test
    public void lateSecondTapCountsAsTwoSingleTaps() {
        Harness harness = new Harness();
        MultiFingerTapDetector detector = new MultiFingerTapDetector(harness, harness, fingers -> fingers == 3);
        detector.onTap(3, 1000);
        detector.onTap(3, 1000 + MultiFingerTapDetector.DOUBLE_TAP_WINDOW_MS + 1);
        harness.elapse();
        assertEquals(Arrays.asList("3x1", "3x1"), harness.fired);
    }

    @Test
    public void tapWithOtherFingerCountFlushesThePendingSingleTap() {
        Harness harness = new Harness();
        MultiFingerTapDetector detector = new MultiFingerTapDetector(harness, harness, fingers -> fingers == 3);
        detector.onTap(3, 1000);
        detector.onTap(4, 1100);
        assertEquals(Arrays.asList("3x1", "4x1"), harness.fired);
    }
}
