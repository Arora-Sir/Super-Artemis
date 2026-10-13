package com.limelight.gestures;

// NOTE: Turns multi-finger taps into single or double tap gestures. A single tap fires at once unless that finger count has a double tap bound,
// NOTE: in which case it waits out the double tap window first. Timing is injected, so the logic is tested without a real clock.
final class MultiFingerTapDetector {
    interface Scheduler {
        void schedule(Runnable runnable, long delayMs);

        void cancel(Runnable runnable);
    }

    interface Listener {
        void onGesture(int fingers, boolean doubleTap);
    }

    interface DoubleTapBindings {
        boolean hasDoubleTap(int fingers);
    }

    static final long DOUBLE_TAP_WINDOW_MS = 300;

    private final Scheduler scheduler;
    private final Listener listener;
    private final DoubleTapBindings bindings;
    private Runnable pendingSingle;
    private int pendingFingers;
    private long pendingTime;

    MultiFingerTapDetector(Scheduler scheduler, Listener listener, DoubleTapBindings bindings) {
        this.scheduler = scheduler;
        this.listener = listener;
        this.bindings = bindings;
    }

    void onTap(int fingers, long eventTimeMs) {
        if (pendingSingle != null) {
            Runnable pending = pendingSingle;
            scheduler.cancel(pending);
            pendingSingle = null;
            if (fingers == pendingFingers && eventTimeMs - pendingTime <= DOUBLE_TAP_WINDOW_MS) {
                listener.onGesture(fingers, true);
                return;
            }
            // A tap with another finger count ends the wait, so the earlier single tap still counts
            pending.run();
        }

        if (!bindings.hasDoubleTap(fingers)) {
            listener.onGesture(fingers, false);
            return;
        }
        pendingFingers = fingers;
        pendingTime = eventTimeMs;
        pendingSingle = () -> {
            pendingSingle = null;
            listener.onGesture(fingers, false);
        };
        scheduler.schedule(pendingSingle, DOUBLE_TAP_WINDOW_MS);
    }
}
