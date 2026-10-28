package com.limelight.session;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Build;
import android.telephony.TelephonyCallback;
import android.telephony.TelephonyManager;

// NOTE: Tells the stream when a call starts or ends, so Android 12 auto-enter is turned off while the phone rings or a call runs.
// NOTE: Android 12 and later only. Older versions have no mode listener, and their manual entry paths read the mode directly through PipGuard.
public final class CallModeWatcher {
    private final AudioManager audio;
    private final Runnable onChanged;
    private Object listener;
    private Object phoneListener;
    private final TelephonyManager telephony;
    // NOTE: Samsung keeps the audio mode normal while the phone rings, so the phone state is read when the permission was granted.
    private volatile int callState = TelephonyManager.CALL_STATE_IDLE;

    public CallModeWatcher(Context context, Runnable onChanged) {
        this.audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        this.onChanged = onChanged;
        this.telephony = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
    }

    public static boolean hasPhoneStatePermission(Context context) {
        return context.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED;
    }

    // A ringing phone or a running call counts as the ringtone mode, which PipGuard treats as a call
    public int mode() {
        int mode = audio != null ? audio.getMode() : AudioManager.MODE_NORMAL;
        if (mode == AudioManager.MODE_NORMAL && callState != TelephonyManager.CALL_STATE_IDLE) {
            return AudioManager.MODE_RINGTONE;
        }
        return mode;
    }

    // The permission is optional and may be granted while the stream runs, so a stopped watcher starts again on the next resume
    public void startPhoneState(Context context) {
        if (telephony == null || phoneListener != null || Build.VERSION.SDK_INT < Build.VERSION_CODES.S || !hasPhoneStatePermission(context)) {
            return;
        }
        PhoneListener l = new PhoneListener();
        try {
            telephony.registerTelephonyCallback(context.getMainExecutor(), l);
            phoneListener = l;
        }
        catch (SecurityException e) {
            // Revoked in between, the audio mode alone is used
        }
    }

    private final class PhoneListener extends TelephonyCallback implements TelephonyCallback.CallStateListener {
        @Override
        public void onCallStateChanged(int state) {
            callState = state;
            onChanged.run();
        }
    }

    public void start(Context context) {
        startPhoneState(context);
        if (audio == null || listener != null || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return;
        }
        AudioManager.OnModeChangedListener modeListener = mode -> onChanged.run();
        audio.addOnModeChangedListener(context.getMainExecutor(), modeListener);
        listener = modeListener;
    }

    public void stop() {
        if (phoneListener != null && telephony != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            telephony.unregisterTelephonyCallback((TelephonyCallback) phoneListener);
            phoneListener = null;
            callState = TelephonyManager.CALL_STATE_IDLE;
        }
        if (audio == null || listener == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return;
        }
        audio.removeOnModeChangedListener((AudioManager.OnModeChangedListener) listener);
        listener = null;
    }
}
