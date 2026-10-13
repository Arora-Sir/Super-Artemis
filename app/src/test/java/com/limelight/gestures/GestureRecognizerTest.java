package com.limelight.gestures;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.view.MotionEvent;

import com.limelight.shadows.ShadowMoonBridge;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, shadows = {ShadowMoonBridge.class})
public class GestureRecognizerTest {
    // Density 1, so distances below are in dp
    private static final float DENSITY = 1f;

    private final List<String> events = new ArrayList<>();
    private final List<Integer> tapFingers = new ArrayList<>();
    private final List<Integer> swipeFingers = new ArrayList<>();
    private final List<Integer> holdFingers = new ArrayList<>();
    // The hold timer, fired by hand so the tests do not wait
    private final List<Runnable> scheduled = new ArrayList<>();
    private MultiFingerGestureRecognizer recognizer;

    @Before
    public void setUp() {
        tapFingers.clear();
        swipeFingers.clear();
        holdFingers.clear();
        scheduled.clear();
        events.clear();
        recognizer = new MultiFingerGestureRecognizer(new MultiFingerGestureRecognizer.Callbacks() {
            @Override
            public boolean handlesTap(int fingers) {
                return tapFingers.contains(fingers);
            }

            @Override
            public boolean ownsSwipes(int fingers) {
                return swipeFingers.contains(fingers);
            }

            @Override
            public boolean handlesHold(int fingers) {
                return holdFingers.contains(fingers);
            }

            @Override
            public long holdDurationMs() {
                return 500;
            }

            @Override
            public void onTap(int fingers, long eventTimeMs) {
                events.add("tap" + fingers);
            }

            @Override
            public void onHold(int fingers) {
                events.add("hold" + fingers);
            }

            @Override
            public void onSwipe(int fingers, MultiFingerGestureRecognizer.Direction direction) {
                events.add("swipe" + fingers + direction);
            }

            @Override
            public void cancelPassThrough(MotionEvent event) {
                events.add("cancel");
            }
        }, DENSITY, new MultiFingerTapDetector.Scheduler() {
            @Override
            public void schedule(Runnable runnable, long delayMs) {
                scheduled.add(runnable);
            }

            @Override
            public void cancel(Runnable runnable) {
                scheduled.remove(runnable);
            }
        });
    }

    private void elapseHold() {
        List<Runnable> due = new ArrayList<>(scheduled);
        scheduled.clear();
        for (Runnable runnable : due) {
            runnable.run();
        }
    }

    // Builds a touch event with every finger at (x + 60 * i, y)
    private static MotionEvent touch(int action, int pointerIndex, int pointers, long time, float x, float y, int toolType) {
        MotionEvent.PointerProperties[] props = new MotionEvent.PointerProperties[pointers];
        MotionEvent.PointerCoords[] coords = new MotionEvent.PointerCoords[pointers];
        for (int i = 0; i < pointers; i++) {
            props[i] = new MotionEvent.PointerProperties();
            props[i].id = i;
            props[i].toolType = toolType;
            coords[i] = new MotionEvent.PointerCoords();
            coords[i].x = x + 60 * i;
            coords[i].y = y;
            coords[i].pressure = 1;
            coords[i].size = 1;
        }
        int fullAction = action | (pointerIndex << MotionEvent.ACTION_POINTER_INDEX_SHIFT);
        return MotionEvent.obtain(0, time, fullAction, pointers, props, coords, 0, 0, 1, 1, 0, 0, 0, 0);
    }

    private boolean feed(int action, int pointerIndex, int pointers, long time, float x, float y) {
        return recognizer.onTouchEvent(touch(action, pointerIndex, pointers, time, x, y, MotionEvent.TOOL_TYPE_FINGER));
    }

    // Lands n fingers starting at the given time, 20ms apart
    private long land(int fingers, long start, float x, float y) {
        feed(MotionEvent.ACTION_DOWN, 0, 1, start, x, y);
        long time = start;
        for (int count = 2; count <= fingers; count++) {
            time += 20;
            feed(MotionEvent.ACTION_POINTER_DOWN, count - 1, count, time, x, y);
        }
        return time;
    }

    private boolean liftAll(int fingers, long time, float x, float y) {
        boolean consumed = false;
        for (int count = fingers; count >= 2; count--) {
            consumed |= feed(MotionEvent.ACTION_POINTER_UP, count - 1, count, time, x, y);
        }
        return feed(MotionEvent.ACTION_UP, 0, 1, time, x, y) || consumed;
    }

    @Test
    public void quickThreeFingerTapIsReportedAndTakenBackFromThePc() {
        tapFingers.add(3);
        long time = land(3, 1000, 100, 100);
        assertTrue(liftAll(3, time + 120, 104, 102));
        assertEquals(Arrays.asList("cancel", "tap3"), events);
    }

    // NOTE: Regression test for the right click that came with every 3, 4 and 5-finger tap in the mouse and trackpad modes.
    // NOTE: The touch modes track two fingers, so any lift that reaches them looks like a two-finger tap. The first lift is now consumed.
    @Test
    public void firstLiftOfATapNeverReachesTheTouchModes() {
        tapFingers.add(3);
        long time = land(3, 1000, 100, 100);
        assertTrue(feed(MotionEvent.ACTION_POINTER_UP, 2, 3, time + 100, 100, 100));
        assertEquals(Collections.singletonList("cancel"), events);
        assertTrue(feed(MotionEvent.ACTION_POINTER_UP, 1, 2, time + 110, 100, 100));
        assertTrue(feed(MotionEvent.ACTION_UP, 0, 1, time + 120, 100, 100));
        assertEquals(Arrays.asList("cancel", "tap3"), events);
    }

    @Test
    public void boundHoldClaimsTheFingersAtOnceAndFiresAfterItsDuration() {
        holdFingers.add(4);
        long time = land(4, 1000, 100, 100);
        assertEquals(Collections.singletonList("cancel"), events);
        assertEquals(1, scheduled.size());
        elapseHold();
        assertTrue(liftAll(4, time + 900, 100, 100));
        assertEquals(Arrays.asList("cancel", "hold4"), events);
    }

    @Test
    public void heldFingersThatMoveOrLiftEarlyRunNoHold() {
        holdFingers.add(3);
        long time = land(3, 1000, 100, 100);
        feed(MotionEvent.ACTION_MOVE, 0, 3, time + 60, 160, 100);
        assertTrue(scheduled.isEmpty());
        liftAll(3, time + 120, 160, 100);

        time = land(3, 5000, 100, 100);
        liftAll(3, time + 100, 100, 100);
        assertTrue(scheduled.isEmpty());
        assertEquals(Arrays.asList("cancel", "cancel"), events);
    }

    @Test
    public void aHoldIsNeverAlsoATap() {
        tapFingers.add(3);
        holdFingers.add(3);
        long time = land(3, 1000, 100, 100);
        elapseHold();
        assertTrue(liftAll(3, time + 100, 100, 100));
        assertEquals(Arrays.asList("cancel", "hold3"), events);
    }

    @Test
    public void quickTapStillWorksWhenAHoldIsBoundToo() {
        tapFingers.add(5);
        holdFingers.add(5);
        long time = land(5, 1000, 100, 100);
        assertTrue(liftAll(5, time + 100, 100, 100));
        assertEquals(Arrays.asList("cancel", "tap5"), events);
        assertTrue(scheduled.isEmpty());
    }

    @Test
    public void unboundFingerCountPassesEverythingThrough() {
        long time = land(3, 1000, 100, 100);
        assertFalse(liftAll(3, time + 120, 100, 100));
        assertTrue(events.isEmpty());
    }

    @Test
    public void slowHoldIsNotATap() {
        tapFingers.add(3);
        long time = land(3, 1000, 100, 100);
        assertFalse(liftAll(3, time + MultiFingerGestureRecognizer.TAP_MAX_MS + 50, 100, 100));
        assertTrue(events.isEmpty());
    }

    @Test
    public void dragIsNotATap() {
        tapFingers.add(3);
        long time = land(3, 1000, 100, 100);
        feed(MotionEvent.ACTION_MOVE, 0, 3, time + 60, 160, 100);
        assertFalse(liftAll(3, time + 120, 160, 100));
        assertTrue(events.isEmpty());
    }

    @Test
    public void lateFingerLikeAThumbJoiningHeldButtonsNeverStartsAGesture() {
        tapFingers.add(3);
        feed(MotionEvent.ACTION_DOWN, 0, 1, 1000, 100, 100);
        feed(MotionEvent.ACTION_POINTER_DOWN, 1, 2, 1020, 100, 100);
        long late = 1000 + MultiFingerGestureRecognizer.LANDING_WINDOW_MS + 100;
        feed(MotionEvent.ACTION_POINTER_DOWN, 2, 3, late, 100, 100);
        assertFalse(liftAll(3, late + 80, 100, 100));
        assertTrue(events.isEmpty());
    }

    @Test
    public void boundSwipeClaimsTheFingersAtOnceAndFiresOnce() {
        swipeFingers.add(3);
        long time = land(3, 1000, 100, 300);
        assertEquals(Collections.singletonList("cancel"), events);
        assertTrue(feed(MotionEvent.ACTION_MOVE, 0, 3, time + 40, 100, 280));
        assertTrue(feed(MotionEvent.ACTION_MOVE, 0, 3, time + 80, 100, 200));
        assertTrue(feed(MotionEvent.ACTION_MOVE, 0, 3, time + 120, 100, 100));
        assertTrue(liftAll(3, time + 160, 100, 100));
        assertEquals(Arrays.asList("cancel", "swipe3UP"), events);
    }

    @Test
    public void diagonalMovementIsNoSwipe() {
        swipeFingers.add(4);
        long time = land(4, 1000, 100, 300);
        feed(MotionEvent.ACTION_MOVE, 0, 4, time + 80, 180, 220);
        assertTrue(liftAll(4, time + 160, 180, 220));
        assertEquals(Collections.singletonList("cancel"), events);
    }

    @Test
    public void stylusNeverStartsAGesture() {
        tapFingers.add(3);
        recognizer.onTouchEvent(touch(MotionEvent.ACTION_DOWN, 0, 1, 1000, 100, 100, MotionEvent.TOOL_TYPE_STYLUS));
        feed(MotionEvent.ACTION_POINTER_DOWN, 1, 2, 1020, 100, 100);
        feed(MotionEvent.ACTION_POINTER_DOWN, 2, 3, 1040, 100, 100);
        assertFalse(liftAll(3, 1100, 100, 100));
        assertTrue(events.isEmpty());
    }

    @Test
    public void swipeDirectionNeedsDistanceAndAMainAxis() {
        assertEquals(MultiFingerGestureRecognizer.Direction.RIGHT, MultiFingerGestureRecognizer.swipeDirection(60, 10, 48));
        assertEquals(MultiFingerGestureRecognizer.Direction.DOWN, MultiFingerGestureRecognizer.swipeDirection(-5, 50, 48));
        assertEquals(null, MultiFingerGestureRecognizer.swipeDirection(30, 5, 48));
        assertEquals(null, MultiFingerGestureRecognizer.swipeDirection(60, 40, 48));
    }
}
