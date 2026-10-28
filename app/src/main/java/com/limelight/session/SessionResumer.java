package com.limelight.session;

import android.app.Activity;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import androidx.core.content.ContextCompat;
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
        // NOTE: A screen that turns off pauses the stream and brings the app lists back to the front a moment later, while the screen is still off.
        // NOTE: Reconnecting then would use up the mark before the user is there, so the mark waits until the screen is on and unlocked, when the lists resume again.
        if (!deviceInUse(activity)) {
            armUnlockRetry(activity);
            return false;
        }
        ResumeStore.Session session = ResumeStore.takePending(activity, System.currentTimeMillis());
        if (session == null || !BackgroundPolicy.resumesWhenReopened(PreferenceManager.getDefaultSharedPreferences(activity))) {
            return false;
        }
        activity.startActivity(intentFor(activity, session));
        return true;
    }

    // NOTE: The lists can come back to the front while the lock screen is still up, and unlocking then gives them no new resume, so the PC list stayed (Tab log, 2026-10-26).
    // NOTE: One receiver waits for the unlock and screen on broadcasts, tries again once the device is in use, and goes away with the activity.
    private static void armUnlockRetry(Activity activity) {
        if (activity.isFinishing() || activity.isDestroyed()) {
            return;
        }
        final boolean[] done = {false};
        final BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (!done[0] && deviceInUse(activity)) {
                    done[0] = true;
                    try {
                        activity.unregisterReceiver(this);
                    }
                    catch (IllegalArgumentException e) {
                        // Already gone with the activity
                    }
                    maybeResume(activity);
                }
            }
        };
        IntentFilter filter = new IntentFilter(Intent.ACTION_USER_PRESENT);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        ContextCompat.registerReceiver(activity, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);
        activity.getApplication().registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityDestroyed(Activity destroyed) {
                if (destroyed == activity) {
                    activity.getApplication().unregisterActivityLifecycleCallbacks(this);
                    if (!done[0]) {
                        done[0] = true;
                        try {
                            activity.unregisterReceiver(receiver);
                        }
                        catch (IllegalArgumentException e) {
                            // Already unregistered
                        }
                    }
                }
            }

            @Override
            public void onActivityCreated(Activity a, android.os.Bundle state) {
            }

            @Override
            public void onActivityStarted(Activity a) {
            }

            @Override
            public void onActivityResumed(Activity a) {
            }

            @Override
            public void onActivityPaused(Activity a) {
            }

            @Override
            public void onActivityStopped(Activity a) {
            }

            @Override
            public void onActivitySaveInstanceState(Activity a, android.os.Bundle state) {
            }
        });
    }

    // True while the screen is on and not on the lock screen
    static boolean deviceInUse(Context context) {
        android.os.PowerManager power = (android.os.PowerManager) context.getSystemService(Context.POWER_SERVICE);
        android.app.KeyguardManager keyguard = (android.app.KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        return (power == null || power.isInteractive()) && (keyguard == null || !keyguard.isKeyguardLocked());
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
