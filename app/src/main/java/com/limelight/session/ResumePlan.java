package com.limelight.session;

import com.limelight.nvstream.jni.MoonBridge;

// NOTE: When a network drop outlasts the grace period the stream ends, and this plan decides whether the app waits for the network and starts the stream again by itself.
// NOTE: The rules are plain functions so they can be tested without a device, and NetworkResumer runs them.
public final class ResumePlan {
    // How long the app keeps trying before it gives up and shows the normal dialog
    public static final long MAX_WAIT_MS = 5 * 60 * 1000L;
    // The first check comes quickly because the network is often back within a second or two of the callback
    public static final long FIRST_CHECK_DELAY_MS = 1000L;

    private static final long[] CHECK_DELAYS_MS = {1500L, 2000L, 3000L, 5000L};

    private ResumePlan() {
    }

    // NOTE: A stream that ended by itself is resumed when the host went silent first or the network is gone now.
    // NOTE: A graceful end, protected content and a frame conversion failure repeat forever when resumed, so they always show the dialog.
    // NOTE: An end with the network up and no silence means the PC closed the session, which the user should see.
    public static boolean shouldAutoResume(boolean enabled, boolean wasStreaming, int errorCode, boolean silenceSeen, boolean networkUp) {
        if (!enabled || !wasStreaming) {
            return false;
        }
        if (errorCode == MoonBridge.ML_ERROR_GRACEFUL_TERMINATION
                || errorCode == MoonBridge.ML_ERROR_PROTECTED_CONTENT
                || errorCode == MoonBridge.ML_ERROR_FRAME_CONVERSION) {
            return false;
        }
        return silenceSeen || !networkUp;
    }

    // The network was down and has been back for this long while the host is still silent, so waiting for the old stream to wake up is not worth it
    public static final long RESTART_AFTER_NETWORK_BACK_MS = 3000L;
    // The network never reported a loss, yet the host has been silent this long, so the stream is dead and a fresh start is quicker than the grace period
    public static final long RESTART_AFTER_SILENCE_MS = 10000L;

    // NOTE: While the host is silent and the network is up, decides whether to stop waiting for the old stream and start it again at once.
    // NOTE: A stream that comes back by itself within these limits is left alone, and a drop with the network still down keeps waiting for the network.
    public static boolean shouldRestartEarly(boolean enabled, boolean networkUp, boolean networkWentDown, long networkBackMs, long silenceMs) {
        if (!enabled || !networkUp) {
            return false;
        }
        if (networkWentDown) {
            return networkBackMs >= RESTART_AFTER_NETWORK_BACK_MS;
        }
        return silenceMs >= RESTART_AFTER_SILENCE_MS;
    }

    // NOTE: A stream that started on mobile data uses the lower metered bitrate for its whole life, so it is started again once an unmetered network has been stable for a while.
    public static final long RESTART_AFTER_UNMETERED_MS = 1500L;
    // A stream younger than this is left alone, so two networks that keep trading places cannot restart it in a loop
    public static final long MIN_STREAM_MS_FOR_QUALITY_RESTART = 8000L;

    // NOTE: The 1.5 seconds only hide a network that connects and drops again at once. The real check is that the PC answers over the new network, so a Wi-Fi without a route to the PC never triggers a restart.
    public static boolean shouldRestartForQuality(boolean enabled, boolean startedMetered, boolean bitratesDiffer, boolean networkUp,
                                                  boolean networkMetered, boolean hostReachable, long unmeteredForMs, long streamRunMs) {
        if (!enabled || !startedMetered || !bitratesDiffer || !networkUp || networkMetered || !hostReachable) {
            return false;
        }
        return unmeteredForMs >= RESTART_AFTER_UNMETERED_MS && streamRunMs >= MIN_STREAM_MS_FOR_QUALITY_RESTART;
    }

    // The wait before check number attempt (0 is the first retry after a failed check), growing to a steady five seconds
    public static long delayBeforeCheck(int attempt) {
        int index = Math.max(0, Math.min(attempt, CHECK_DELAYS_MS.length - 1));
        return CHECK_DELAYS_MS[index];
    }

    public static boolean expired(long waitedMs) {
        return waitedMs >= MAX_WAIT_MS;
    }
}
