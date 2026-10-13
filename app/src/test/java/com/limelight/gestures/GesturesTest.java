package com.limelight.gestures;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
        // Taps and double taps with 3, 4 and 5 fingers, and swipes in four directions with 3 and 4 fingers
        assertEquals(14, keys.size());
        assertEquals("gesture_tap_3", tap(3).prefKey());
        assertEquals("gesture_double_5", doubleTap(5).prefKey());
    }

    @Test
    public void swipesStartAsNothingSoTheirFingerCountsStayWithThePc() {
        for (GestureBindingStore.Slot slot : GestureBindingStore.SLOTS) {
            if (slot.kind.isSwipe()) {
                assertEquals(GestureBindingStore.NONE, GestureBindingStore.get(prefs, slot));
            }
        }
        assertFalse(GestureBindingStore.hasSwipe(prefs, 3));
        GestureBindingStore.set(prefs, new GestureBindingStore.Slot(3, GestureBindingStore.Kind.SWIPE_UP), GestureBindingStore.QUICK_MENU);
        assertTrue(GestureBindingStore.hasSwipe(prefs, 3));
        assertFalse(GestureBindingStore.hasSwipe(prefs, 4));
    }

    @Test
    public void windowsPresetBindsKeyCombosThatParseBack() {
        GestureBindingStore.applyWindowsPreset(prefs);
        String up = GestureBindingStore.get(prefs, new GestureBindingStore.Slot(3, GestureBindingStore.Kind.SWIPE_UP));
        assertEquals(Arrays.asList(0x5B, 0x09), GestureBindingStore.comboKeys(up));
        String right = GestureBindingStore.get(prefs, new GestureBindingStore.Slot(4, GestureBindingStore.Kind.SWIPE_RIGHT));
        assertEquals(Arrays.asList(0xA2, 0x5B, 0x27), GestureBindingStore.comboKeys(right));
        assertTrue(GestureBindingStore.hasSwipe(prefs, 4));
        // Taps keep their own bindings
        assertEquals(StreamActionRegistry.ID_TOGGLE_KEYBOARD, GestureBindingStore.get(prefs, tap(3)));
    }

    @Test
    public void malformedCombosAreRefused() {
        GestureBindingStore.Slot slot = new GestureBindingStore.Slot(3, GestureBindingStore.Kind.SWIPE_DOWN);
        for (String bad : Arrays.asList("combo:", "combo:ZZ", "combo:00", "combo:1FF", "combo:01+02+03+04+05+06+07+08+09")) {
            GestureBindingStore.set(prefs, slot, bad);
            assertEquals(bad, GestureBindingStore.NONE, GestureBindingStore.get(prefs, slot));
        }
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
