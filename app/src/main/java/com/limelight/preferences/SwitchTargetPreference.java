package com.limelight.preferences;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.preference.CheckBoxPreference;
import androidx.preference.PreferenceViewHolder;

import com.limelight.R;

// NOTE: A settings row with two targets, like the Wi-Fi row in Android settings. The row opens its editor and the switch after the divider turns the feature on or off.
// NOTE: It extends CheckBoxPreference so it shows the Material You switch or the Classic checkbox exactly like every other setting.
// NOTE: The editor opens through the usual click listener, and the switch toggles through callChangeListener, so change listeners and search still work.
public class SwitchTargetPreference extends CheckBoxPreference {
    public SwitchTargetPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    // A tap on the row opens the editor through the click listener and never flips the value
    @Override
    protected void onClick() {
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        View widgetFrame = holder.findViewById(android.R.id.widget_frame);
        if (widgetFrame == null) {
            return;
        }
        widgetFrame.setBackgroundResource(R.drawable.bg_switch_target);
        int start = (int) (16 * getContext().getResources().getDisplayMetrics().density);
        widgetFrame.setPaddingRelative(start, widgetFrame.getPaddingTop(), widgetFrame.getPaddingEnd(), widgetFrame.getPaddingBottom());
        widgetFrame.setMinimumWidth((int) (64 * getContext().getResources().getDisplayMetrics().density));
        widgetFrame.setClickable(true);
        widgetFrame.setFocusable(true);
        widgetFrame.setContentDescription(getTitle());
        widgetFrame.setOnClickListener(v -> {
            boolean value = !isChecked();
            if (callChangeListener(value)) {
                setChecked(value);
            }
        });
    }
}
