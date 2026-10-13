package com.limelight.gestures;

import android.os.Build;
import android.view.MotionEvent;

// NOTE: Recognizes taps, holds and swipes with 3 or more fingers on the stream. Game runs it before any touch mode (direct touch, trackpad, pan and zoom),
// NOTE: so a gesture works the same in every mode, including the one that toggles pan and zoom off again.
// NOTE: Fingers must all land within a short window, which keeps fast play on on-screen buttons from ever counting as a gesture.
// NOTE: A finger count with a swipe or a hold bound is claimed as soon as the fingers land, and its touches stop reaching the PC.
// NOTE: A finger count with only a tap bound is claimed when the first finger lifts, so drags and holds still reach the PC,
// NOTE: and the touch modes never see the lifts that they would read as a two-finger tap (a right click).
public final class MultiFingerGestureRecognizer {
    public enum Direction { UP, DOWN, LEFT, RIGHT }

    public interface Callbacks {
        boolean handlesTap(int fingers);

        boolean ownsSwipes(int fingers);

        boolean handlesHold(int fingers);

        // How long the fingers stay still before a hold runs
        long holdDurationMs();

        void onTap(int fingers, long eventTimeMs);

        void onSwipe(int fingers, Direction direction);

        void onHold(int fingers);

        // Ends whatever this touch already started on the PC or in pan and zoom, because the gesture takes it over
        void cancelPassThrough(MotionEvent event);
    }

    // All fingers land within this time of the first one (Mapbox uses 150ms, RustDesk 200ms)
    static final long LANDING_WINDOW_MS = 250;
    // From the last finger down to the first finger up, as Moonlight and Artemis have always used
    static final long TAP_MAX_MS = 300;
    static final float TAP_SLOP_DP = 24;
    static final float SWIPE_MIN_DP = 48;
    static final long SWIPE_MAX_MS = 700;
    // The main axis must be at least twice the other one, so diagonal drags do nothing
    static final float SWIPE_AXIS_RATIO = 2f;
    private static final int MIN_FINGERS = 3;

    private final Callbacks callbacks;
    private final float density;
    private final MultiFingerTapDetector.Scheduler scheduler;
    private final Runnable holdRunnable = this::onHoldElapsed;

    private boolean tracking;
    private boolean invalid;
    private boolean claimed;
    private boolean swiped;
    private boolean lifted;
    private boolean tapPending;
    private boolean held;
    private boolean holdScheduled;
    private boolean swipesBound;
    private int fingers;
    private int holdFingers;
    private long firstDownTime;
    private long lastDownTime;
    private float startX;
    private float startY;
    private float maxDistance;

    public MultiFingerGestureRecognizer(Callbacks callbacks, float density, MultiFingerTapDetector.Scheduler scheduler) {
        this.callbacks = callbacks;
        this.density = density;
        this.scheduler = scheduler;
    }

    // Returns true when the event belongs to a gesture and must not reach the PC or any touch mode
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                reset();
                tracking = true;
                firstDownTime = event.getEventTime();
                fingers = 1;
                invalid = !allFingers(event);
                return false;

            case MotionEvent.ACTION_POINTER_DOWN:
                return onFingerDown(event);

            case MotionEvent.ACTION_MOVE:
                return onMove(event);

            case MotionEvent.ACTION_POINTER_UP:
                return onFirstLifts(event);

            case MotionEvent.ACTION_UP:
                return onLastFingerUp(event);

            case MotionEvent.ACTION_CANCEL:
                boolean wasClaimed = claimed;
                reset();
                return wasClaimed;

            default:
                return claimed;
        }
    }

    private boolean onFingerDown(MotionEvent event) {
        if (!tracking || lifted) {
            return claimed;
        }
        int count = event.getPointerCount();
        if (count <= fingers) {
            return claimed;
        }
        fingers = count;
        lastDownTime = event.getEventTime();
        startX = centroidX(event);
        startY = centroidY(event);
        maxDistance = 0;
        cancelHold();
        // A finger that lands late (a thumb joining a held on-screen button) or a stylus never starts a gesture
        if (count >= MIN_FINGERS && (lastDownTime - firstDownTime > LANDING_WINDOW_MS || !allFingers(event))) {
            invalid = true;
        }
        if (invalid || count < MIN_FINGERS) {
            return claimed;
        }
        boolean holds = callbacks.handlesHold(count);
        swipesBound = callbacks.ownsSwipes(count);
        // Swipes and holds take the fingers at once, before a trackpad drag or a Windows press and hold can start on the PC
        if (!claimed && (holds || swipesBound)) {
            claim(event);
        }
        if (holds) {
            holdFingers = count;
            holdScheduled = true;
            scheduler.schedule(holdRunnable, callbacks.holdDurationMs());
        }
        return claimed;
    }

    private boolean onMove(MotionEvent event) {
        if (!tracking || lifted || fingers < MIN_FINGERS || event.getPointerCount() != fingers) {
            return claimed;
        }
        float dx = centroidX(event) - startX;
        float dy = centroidY(event) - startY;
        maxDistance = Math.max(maxDistance, (float) Math.hypot(dx, dy));
        if (maxDistance > TAP_SLOP_DP * density) {
            // Moving fingers are a swipe or a drag, never a hold
            cancelHold();
        }
        if (claimed && swipesBound && !swiped && !held && !invalid && event.getEventTime() - lastDownTime <= SWIPE_MAX_MS) {
            Direction direction = swipeDirection(dx, dy, SWIPE_MIN_DP * density);
            if (direction != null) {
                swiped = true;
                cancelHold();
                callbacks.onSwipe(fingers, direction);
            }
        }
        return claimed;
    }

    // NOTE: The first lift decides a tap. When it qualifies, the gesture is claimed here, so the remaining lifts never reach
    // NOTE: the touch modes. Those only track two fingers and would send a right click for the second one.
    private boolean onFirstLifts(MotionEvent event) {
        if (tracking && !lifted) {
            lifted = true;
            cancelHold();
            if (qualifiesAsTap(event.getEventTime())) {
                tapPending = true;
                if (!claimed) {
                    claim(event);
                }
            }
        }
        return claimed;
    }

    private boolean onLastFingerUp(MotionEvent event) {
        boolean consume = claimed;
        boolean canceled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && (event.getFlags() & MotionEvent.FLAG_CANCELED) != 0;
        if (!canceled) {
            // A tap whose fingers all lifted together reaches this point without a first lift
            boolean tap = lifted ? tapPending : qualifiesAsTap(event.getEventTime());
            if (tap) {
                if (!claimed) {
                    callbacks.cancelPassThrough(event);
                }
                callbacks.onTap(fingers, event.getEventTime());
                consume = true;
            }
        }
        reset();
        return consume;
    }

    private boolean qualifiesAsTap(long liftTime) {
        return tracking && !invalid && !swiped && !held && fingers >= MIN_FINGERS
                && liftTime - lastDownTime <= TAP_MAX_MS && maxDistance <= TAP_SLOP_DP * density
                && callbacks.handlesTap(fingers);
    }

    private void onHoldElapsed() {
        holdScheduled = false;
        if (tracking && !lifted && !invalid && !swiped && !held && fingers == holdFingers && maxDistance <= TAP_SLOP_DP * density) {
            held = true;
            callbacks.onHold(fingers);
        }
    }

    private void claim(MotionEvent event) {
        claimed = true;
        callbacks.cancelPassThrough(event);
    }

    private void cancelHold() {
        if (holdScheduled) {
            holdScheduled = false;
            scheduler.cancel(holdRunnable);
        }
    }

    // The direction of a finished swipe, or null when the movement is too short or too diagonal
    static Direction swipeDirection(float dx, float dy, float minDistance) {
        float ax = Math.abs(dx);
        float ay = Math.abs(dy);
        if (ax >= minDistance && ax >= ay * SWIPE_AXIS_RATIO) {
            return dx > 0 ? Direction.RIGHT : Direction.LEFT;
        }
        if (ay >= minDistance && ay >= ax * SWIPE_AXIS_RATIO) {
            return dy > 0 ? Direction.DOWN : Direction.UP;
        }
        return null;
    }

    private void reset() {
        cancelHold();
        tracking = false;
        invalid = false;
        claimed = false;
        swiped = false;
        lifted = false;
        tapPending = false;
        held = false;
        swipesBound = false;
        fingers = 0;
        holdFingers = 0;
        maxDistance = 0;
    }

    private static boolean allFingers(MotionEvent event) {
        for (int i = 0; i < event.getPointerCount(); i++) {
            if (event.getToolType(i) != MotionEvent.TOOL_TYPE_FINGER) {
                return false;
            }
        }
        return true;
    }

    // Screen coordinates, since pan and zoom moves the stream view under the fingers
    private static float centroidX(MotionEvent event) {
        float sum = 0;
        for (int i = 0; i < event.getPointerCount(); i++) {
            sum += event.getX(i);
        }
        return sum / event.getPointerCount() + event.getRawX() - event.getX();
    }

    private static float centroidY(MotionEvent event) {
        float sum = 0;
        for (int i = 0; i < event.getPointerCount(); i++) {
            sum += event.getY(i);
        }
        return sum / event.getPointerCount() + event.getRawY() - event.getY();
    }
}
