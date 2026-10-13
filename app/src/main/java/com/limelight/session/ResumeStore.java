package com.limelight.session;

import android.content.Context;
import android.content.SharedPreferences;

// NOTE: Remembers the last stream (PC and app) and, when the user leaves under the Resume policy, when they left.
// NOTE: It is device state, so backups leave it out (see PrefsSnapshot), and a pending resume expires after RESUME_WINDOW_MS.
public final class ResumeStore {
    public static final String PREF_NAME = "SessionResume";
    // Long enough for a call or a quick errand, short enough that an old session does not start out of nowhere
    public static final long RESUME_WINDOW_MS = 30 * 60 * 1000L;

    private static final String KEY_PC_UUID = "pc_uuid";
    private static final String KEY_PC_NAME = "pc_name";
    private static final String KEY_APP_NAME = "app_name";
    private static final String KEY_APP_UUID = "app_uuid";
    private static final String KEY_APP_ID = "app_id";
    private static final String KEY_APP_HDR = "app_hdr";
    private static final String KEY_LEFT_AT = "left_at";

    public static final class Session {
        public final String pcUuid;
        public final String pcName;
        public final String appName;
        public final String appUuid;
        public final int appId;
        public final boolean hdr;

        public Session(String pcUuid, String pcName, String appName, String appUuid, int appId, boolean hdr) {
            this.pcUuid = pcUuid;
            this.pcName = pcName;
            this.appName = appName;
            this.appUuid = appUuid;
            this.appId = appId;
            this.hdr = hdr;
        }
    }

    private ResumeStore() {
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // Called when a stream connects. A new stream also cancels any older pending resume.
    public static void remember(Context context, Session session) {
        if (session.pcUuid == null) {
            return;
        }
        prefs(context).edit()
                .putString(KEY_PC_UUID, session.pcUuid)
                .putString(KEY_PC_NAME, session.pcName)
                .putString(KEY_APP_NAME, session.appName)
                .putString(KEY_APP_UUID, session.appUuid)
                .putInt(KEY_APP_ID, session.appId)
                .putBoolean(KEY_APP_HDR, session.hdr)
                .remove(KEY_LEFT_AT)
                .apply();
    }

    public static Session last(Context context) {
        SharedPreferences prefs = prefs(context);
        String pcUuid = prefs.getString(KEY_PC_UUID, null);
        if (pcUuid == null) {
            return null;
        }
        return new Session(pcUuid, prefs.getString(KEY_PC_NAME, null), prefs.getString(KEY_APP_NAME, null),
                prefs.getString(KEY_APP_UUID, null), prefs.getInt(KEY_APP_ID, -1), prefs.getBoolean(KEY_APP_HDR, false));
    }

    public static void markLeft(Context context, long nowMs) {
        prefs(context).edit().putLong(KEY_LEFT_AT, nowMs).commit();
    }

    public static void clearPending(Context context) {
        prefs(context).edit().remove(KEY_LEFT_AT).apply();
    }

    // Returns the session to resume at most once, and nothing when the user left too long ago
    public static Session takePending(Context context, long nowMs) {
        SharedPreferences prefs = prefs(context);
        long leftAt = prefs.getLong(KEY_LEFT_AT, 0);
        if (leftAt <= 0) {
            return null;
        }
        prefs.edit().remove(KEY_LEFT_AT).commit();
        if (nowMs < leftAt || nowMs - leftAt > RESUME_WINDOW_MS) {
            return null;
        }
        return last(context);
    }
}
