package com.limelight.session;

import android.content.SharedPreferences;

// NOTE: What happens to a stream when the user leaves it with Home, Recents or a quick switch to another app.
// NOTE: It replaces the old picture-in-picture switch, which still decides the value until the user picks one.
public final class BackgroundPolicy {
    public static final String PREF_KEY = "list_background_policy";

    // Ends the stream, nothing keeps running (the Artemis default)
    public static final String DISCONNECT = "disconnect";
    // Ends the stream and reconnects to the same app when the user comes back within ResumeStore.RESUME_WINDOW_MS
    public static final String RESUME = "resume";
    // Keeps streaming in a picture-in-picture window
    public static final String PIP = "pip";

    // Setting from before this policy existed, read only to migrate it
    static final String LEGACY_PIP = "checkbox_enable_pip";

    private BackgroundPolicy() {
    }

    public static String get(SharedPreferences prefs) {
        String value = prefs.getString(PREF_KEY, null);
        if (DISCONNECT.equals(value) || RESUME.equals(value) || PIP.equals(value)) {
            return value;
        }
        return prefs.getBoolean(LEGACY_PIP, false) ? PIP : DISCONNECT;
    }

    public static boolean isPip(SharedPreferences prefs) {
        return PIP.equals(get(prefs));
    }

    public static boolean isResume(SharedPreferences prefs) {
        return RESUME.equals(get(prefs));
    }

    // NOTE: Under picture-in-picture, Android refuses the small window while the Recents overview shows the stream as a card, so the stream ends there.
    // NOTE: Those streams get the resume behavior instead, so coming back to the card or the app reconnects. Only "End the stream" never reconnects.
    public static boolean resumesWhenReopened(SharedPreferences prefs) {
        return !DISCONNECT.equals(get(prefs));
    }
}
