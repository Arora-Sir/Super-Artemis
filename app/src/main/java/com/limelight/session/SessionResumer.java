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
        if (session == null || !BackgroundPolicy.resumesWhenReopened(PreferenceManager.getDefaultSharedPreferences(activity))) {
            return false;
        }
        activity.startActivity(intentFor(activity, session));
        return true;
    }

    // Called when Settings opened from a stream closes, so it reconnects whatever the leave policy is
    public static boolean resumeNow(Activity activity) {
        ResumeStore.Session session = ResumeStore.takePending(activity, System.currentTimeMillis());
        if (session == null) {
            return false;
        }
        activity.startActivity(intentFor(activity, session));
        return true;
    }

    // NOTE: Offers to reconnect a stream whose picture-in-picture window was closed with its X, once, as a themed dialog.
    // NOTE: Returns true when the offer is shown, so the caller skips the other resume paths this time.
    public static boolean maybeOfferReconnectAfterPipClose(Activity activity) {
        // A screen turned off also stops a stream in picture-in-picture without ending it, so a running stream gets no offer
        if (Game.instance != null) {
            return false;
        }
        ResumeStore.Session session = ResumeStore.takeClosedFromPip(activity, System.currentTimeMillis());
        if (session == null || activity.isFinishing()) {
            return false;
        }
        String app = session.appName != null ? session.appName : activity.getString(com.limelight.R.string.applist_menu_resume);
        com.limelight.ui.ThemedDialogs.show(com.limelight.ui.ThemedDialogs.builder(activity)
                .setTitle(com.limelight.R.string.pip_closed_title)
                .setMessage(activity.getString(com.limelight.R.string.pip_closed_message, app,
                        session.pcName != null ? session.pcName : ""))
                .setPositiveButton(com.limelight.R.string.conn_terminated_reconnect, (dialog, which) ->
                        activity.startActivity(intentFor(activity, session)))
                .setNegativeButton(com.limelight.R.string.pip_closed_not_now, null));
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

    // NOTE: Called from the "connection terminated" dialog, so the same stream starts again without going back to the PC and app lists.
    // NOTE: It uses the last remembered session, which every stream saves when it connects.
    public static boolean reconnectLast(Activity activity) {
        ResumeStore.Session session = ResumeStore.last(activity);
        if (session == null) {
            return false;
        }
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
