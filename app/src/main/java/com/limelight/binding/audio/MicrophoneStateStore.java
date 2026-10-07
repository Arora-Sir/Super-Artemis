package com.limelight.binding.audio;

import android.content.Context;
import android.content.SharedPreferences;

import com.limelight.preferences.PreferenceConfiguration;

// Remembers the last manual microphone state per host for the "Follow last state" option
public class MicrophoneStateStore {
    public static final String PREF_NAME = "MicrophoneState";
    private static final String KEY_PREFIX = "host_";

    private final SharedPreferences prefs;

    public MicrophoneStateStore(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public boolean getLastState(String hostUuid) {
        if (hostUuid == null) {
            return false;
        }
        return prefs.getBoolean(KEY_PREFIX + hostUuid, false);
    }

    public void setLastState(String hostUuid, boolean enabled) {
        if (hostUuid == null) {
            return;
        }
        prefs.edit().putBoolean(KEY_PREFIX + hostUuid, enabled).apply();
    }

    // Decides whether capture starts together with the stream
    public static boolean resolveInitialState(String initialState, boolean lastState) {
        if (PreferenceConfiguration.MICROPHONE_INITIAL_STATE_ON.equals(initialState)) {
            return true;
        }
        if (PreferenceConfiguration.MICROPHONE_INITIAL_STATE_FOLLOW_LAST.equals(initialState)) {
            return lastState;
        }
        return false;
    }
}
