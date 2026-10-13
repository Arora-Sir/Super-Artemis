package com.limelight.gestures;

import android.os.Build;
import android.view.MotionEvent;

// NOTE: Recognizes taps and swipes with 3 or more fingers on the stream. Game runs it before any touch mode (direct touch, trackpad, pan and zoom),
// NOTE: so a gesture works the same in every mode, including the one that toggles pan and zoom off again.
// NOTE: Fingers must all land within a short window, which keeps fast play on on-screen buttons from ever counting as a gesture.
// NOTE: A finger count with a swipe bound is claimed as soon as the fingers land, and its touches stop reaching the PC.
// NOTE: Taps are only taken back at the end, so finger counts with just a tap bound still pass drags and holds to the PC.
public final class MultiFingerGestureRecognizer {
    public enum Direction { UP, DOWN, LEFT, RIGHT }

    public interface Callbacks {
        boolean handlesTap(int fingers);

        boolean ownsSwipes(int fingers);

        void onTap(int fingers, long eventTimeMs);

        void onSwipe(int fingers, Direction direction);

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

    private boolean tracking;
    private boolean invalid;
    private boolean claimed;
    private boolean swiped;
    private boolean lifted;
    private int fingers;
    private long firstDownTime;
    private long lastDownTime;
    private long firstLiftTime;
    private float startX;
    private float startY;
    private float maxDistance;

    public MultiFingerGestureRecognizer(Callbacks callbacks, float density) {
        this.callbacks = callbacks;
        this.density = density;
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
                if (tracking && !lifted) {
                    lifted = true;
                    firstLiftTime = event.getEventTime();
                }
                return claimed;

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
        // A finger that lands late (a thumb joining a held on-screen button) or a stylus never starts a gesture
        if (count >= MIN_FINGERS && (lastDownTime - firstDownTime > LANDING_WINDOW_MS || !allFingers(event))) {
            invalid = true;
        }
        if (!claimed && !invalid && count >= MIN_FINGERS && callbacks.ownsSwipes(count)) {
            claimed = true;
            callbacks.cancelPassThrough(event);
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
        if (claimed && !swiped && !invalid && event.getEventTime() - lastDownTime <= SWIPE_MAX_MS) {
            Direction direction = swipeDirection(dx, dy, SWIPE_MIN_DP * density);
            if (direction != null) {
                swiped = true;
                callbacks.onSwipe(fingers, direction);
            }
        }
        return claimed;
    }

    private boolean onLastFingerUp(MotionEvent event) {
        boolean consume = claimed;
        boolean canceled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && (event.getFlags() & MotionEvent.FLAG_CANCELED) != 0;
        if (tracking && !invalid && !swiped && !canceled && fingers >= MIN_FINGERS) {
            long held = (lifted ? firstLiftTime : event.getEventTime()) - lastDownTime;
            if (held <= TAP_MAX_MS && maxDistance <= TAP_SLOP_DP * density && callbacks.handlesTap(fingers)) {
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
        tracking = false;
        invalid = false;
        claimed = false;
        swiped = false;
        lifted = false;
        fingers = 0;
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
