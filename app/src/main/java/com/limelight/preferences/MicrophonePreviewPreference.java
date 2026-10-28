package com.limelight.preferences;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.util.AttributeSet;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.limelight.R;

public class MicrophonePreviewPreference extends Preference {
    private static final ColorStateList DETECTED_TINT = ColorStateList.valueOf(Color.parseColor("#45c486"));
    private static final ColorStateList MISSING_TINT = ColorStateList.valueOf(Color.parseColor("#5f6f86"));

    private int levelPercent;
    private boolean signalDetected;
    private ProgressBar boundBar;

    public MicrophonePreviewPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MicrophonePreviewPreference(Context context) {
        super(context);
        init();
    }

    private void init() {
        setSelectable(false);
        setWidgetLayoutResource(R.layout.preference_microphone_preview_widget);
        setSummary(R.string.microphone_preview_inactive);
    }

    // NOTE: The level arrives about 20 times per second. Rebinding the whole row each time laid out the list and animated the row away from its place (BUG-045).
    // NOTE: Only a new status text goes through setSummary, which rebinds the row by itself. The level bar is updated in place.
    public void updatePreviewState(String status, double level, boolean detected) {
        int newLevel = Math.max(0, Math.min(100, (int) Math.round(level * 100.0)));
        boolean detectedChanged = detected != signalDetected;
        levelPercent = newLevel;
        signalDetected = detected;
        setSummary(status + "\n" + getContext().getString(
                detected ? R.string.microphone_preview_signal_detected :
                        R.string.microphone_preview_signal_missing));
        if (boundBar != null) {
            boundBar.setProgress(levelPercent);
            if (detectedChanged) {
                boundBar.setProgressTintList(detected ? DETECTED_TINT : MISSING_TINT);
            }
        }
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        boundBar = (ProgressBar) holder.findViewById(R.id.microphone_preview_bar);
        if (boundBar != null) {
            boundBar.setProgress(levelPercent);
            boundBar.setProgressTintList(signalDetected ? DETECTED_TINT : MISSING_TINT);
        }
    }
}
