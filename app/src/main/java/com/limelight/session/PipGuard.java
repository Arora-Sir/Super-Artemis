package com.limelight.session;

import android.media.AudioManager;

// NOTE: Decides when a stream may shrink into picture-in-picture and when a stream that stopped must reconnect later.
// NOTE: It holds no Android state, so Game only reads its inputs and every rule here is covered by unit tests.
public final class PipGuard {
    private PipGuard() {
    }

    // Everything Game knows at the moment it considers picture-in-picture
    public static final class State {
        public boolean pipSupported = true;
        public boolean pipEnabled = true;
        public boolean connected = true;
        public boolean finishing;
        public boolean changingConfigurations;
        public boolean inPip;
        public boolean multiWindow;
        public boolean externalDisplay;
        public boolean pipSuppressed;
        public boolean openingOwnScreen;
        public boolean interactive = true;
        public boolean keyguardLocked;
        public int audioMode = AudioManager.MODE_NORMAL;
        // The assistant (Gemini, Google Assistant) was invoked a moment ago and shows over the stream
        public boolean assistantRecently;
    }

    // How long after an assistant invocation the stream stays full screen
    public static final long ASSISTANT_WINDOW_MS = 5000;
    // NOTE: While the assistant overlay or a dialog it opened (an account picker after the share button) still covers the stream, the stream stays full screen up to this long.
    public static final long ASSISTANT_HOLD_MS = 120000;

    public static boolean assistantRecently(long nowMs, long invokedAtMs) {
        return assistantRecently(nowMs, invokedAtMs, ASSISTANT_WINDOW_MS);
    }

    public static boolean assistantRecently(long nowMs, long invokedAtMs, long windowMs) {
        return invokedAtMs > 0 && nowMs >= invokedAtMs && nowMs - invokedAtMs < windowMs;
    }

    // NOTE: Android rejects a picture-in-picture aspect ratio outside 0.418 to 2.39 with an exception, which ended the app when it was asked before the first layout.
    // NOTE: Returns numerator and denominator inside that range, or null while the view has no size yet, and then no ratio is set.
    public static int[] aspectRatioFor(int width, int height) {
        if (width <= 0 || height <= 0) {
            return null;
        }
        float ratio = width / (float) height;
        if (ratio > 2.38f) {
            return new int[] {238, 100};
        }
        if (ratio < 0.43f) {
            return new int[] {43, 100};
        }
        return new int[] {width, height};
    }

    // A ringing phone, a call and a voice chat all put the audio system in one of these modes, and none of them is the user leaving the stream
    public static boolean isCallMode(int audioMode) {
        return audioMode == AudioManager.MODE_RINGTONE
                || audioMode == AudioManager.MODE_IN_CALL
                || audioMode == AudioManager.MODE_IN_COMMUNICATION;
    }

    // NOTE: Screen off, the lock screen, split screen, this app's own screens, a USB prompt and calls also pause the stream.
    // NOTE: None of them is the user switching to another app, so the stream stays full screen for them.
    public static boolean mayEnterPip(State s) {
        if (!s.pipSupported || !s.pipEnabled || !s.connected) {
            return false;
        }
        if (s.finishing || s.changingConfigurations || s.inPip || s.multiWindow) {
            return false;
        }
        if (s.externalDisplay || s.pipSuppressed || s.openingOwnScreen) {
            return false;
        }
        if (isCallMode(s.audioMode) || s.assistantRecently) {
            return false;
        }
        return s.interactive && !s.keyguardLocked;
    }

    // Android 12 auto-enter starts the small window by itself, so it is armed only when a manual entry would be allowed
    public static boolean autoEnterArmed(boolean connected, boolean pipSuppressed, int audioMode, boolean assistantRecently) {
        return connected && !pipSuppressed && !isCallMode(audioMode) && !assistantRecently;
    }

    // NOTE: One Hand Operation+ "previous app" hides the stream window and destroys its surface about a second before the app hears about it, so the stream cannot shrink and ends.
    // NOTE: A stream that ended that way, while the screen is on and nothing else explains it, reconnects when the user comes back.
    public static boolean shouldResumeAfterSurfaceLoss(boolean connected, boolean finishing, boolean inPip, boolean changingConfigurations,
                                                       boolean openingOwnScreen, boolean interactive, boolean resumePolicy) {
        return connected && !finishing && !inPip && !changingConfigurations && !openingOwnScreen && interactive && resumePolicy;
    }

    // NOTE: A screen that turned off ends the stream, but it is not the user leaving it.
    // NOTE: Unlocking should bring the stream back, except under "End the stream" and after the user ended it.
    public static boolean shouldResumeAfterScreenOff(boolean connected, boolean interactive, boolean finishing, boolean resumePolicy) {
        return connected && !interactive && !finishing && resumePolicy;
    }
}
