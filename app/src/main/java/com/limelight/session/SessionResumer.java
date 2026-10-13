package com.limelight.session;

import android.app.Activity;
import android.content.Intent;

import androidx.preference.PreferenceManager;

import com.limelight.AppView;
import com.limelight.Game;
import com.limelight.ShortcutTrampoline;

// NOTE: Starts a remembered stream through ShortcutTrampoline, the same path as pinned app shortcuts, which binds the PC service,
// NOTE: waits for the PC to come online and launches the app. Used for "Resume when reopened" and "Connect to the last PC on app start".
public final class SessionResumer {
    public static final String PREF_AUTO_CONNECT = "checkbox_auto_connect_last";

    private SessionResumer() {
    }

    // Called when the PC or app list comes back to the front, which is where Recents and the launcher return to after a stream
    public static boolean maybeResume(Activity activity) {
        ResumeStore.Session session = ResumeStore.takePending(activity, System.currentTimeMillis());
        if (session == null || !BackgroundPolicy.isResume(PreferenceManager.getDefaultSharedPreferences(activity))) {
            return false;
        }
        activity.startActivity(intentFor(activity, session));
        return true;
    }

    // Called once when the app is opened fresh, never when returning from a stream
    public static boolean maybeAutoConnect(Activity activity) {
        if (!PreferenceManager.getDefaultSharedPreferences(activity).getBoolean(PREF_AUTO_CONNECT, false)) {
            return false;
        }
        ResumeStore.Session session = ResumeStore.last(activity);
        if (session == null) {
            return false;
        }
        // The launch below already reconnects, so a pending resume must not start a second one
        ResumeStore.clearPending(activity);
        activity.startActivity(intentFor(activity, session));
        return true;
    }

    static Intent intentFor(Activity activity, ResumeStore.Session session) {
        Intent intent = new Intent(activity, ShortcutTrampoline.class);
        intent.putExtra(AppView.UUID_EXTRA, session.pcUuid);
        intent.putExtra(AppView.NAME_EXTRA, session.pcName);
        intent.putExtra(Game.EXTRA_APP_NAME, session.appName);
        intent.putExtra(Game.EXTRA_APP_UUID, session.appUuid);
        if (session.appId >= 0) {
            intent.putExtra(Game.EXTRA_APP_ID, String.valueOf(session.appId));
        }
        intent.putExtra(Game.EXTRA_APP_HDR, session.hdr);
        intent.setAction(Intent.ACTION_DEFAULT);
        return intent;
    }
}
