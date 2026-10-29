package com.limelight.preferences;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.CheckBoxPreference;
import androidx.preference.PreferenceViewHolder;

import com.limelight.R;

// NOTE: The Pure black row. While it is on, a tap on the text opens Surface lift and the switch has its own tap zone behind a thin divider, as in DualClone.
// NOTE: While it is off, a tap anywhere on the row turns it on, like every other switch row.
public class AmoledBlackPreference extends CheckBoxPreference {
    private Runnable onAdjust;

    public AmoledBlackPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public AmoledBlackPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public AmoledBlackPreference(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public AmoledBlackPreference(@NonNull Context context) {
        super(context);
    }

    public void setOnAdjust(Runnable onAdjust) {
        this.onAdjust = onAdjust;
    }

    @Override
    protected void onClick() {
        if (isChecked() && onAdjust != null) {
            onAdjust.run();
            return;
        }
        super.onClick();
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        View box = holder.findViewById(android.R.id.checkbox);
        if (!(box instanceof CompoundButton)) {
            return;
        }
        // The control toggles itself, and the divider beside it only shows while there is something to adjust
        box.setClickable(isChecked());
        box.setFocusable(isChecked());
        if (box.getParent() instanceof ViewGroup) {
            ViewGroup frame = (ViewGroup) box.getParent();
            frame.setBackgroundResource(isChecked() ? R.drawable.bg_switch_divider : 0);
            // The frame keeps its own start padding as the gap to the text, and the divider adds a little more
            if (frame.getTag() == null) {
                frame.setTag(frame.getPaddingStart());
            }
            int base = (Integer) frame.getTag();
            int gap = (int) (16 * frame.getResources().getDisplayMetrics().density);
            frame.setPaddingRelative(isChecked() ? base + gap : base, frame.getPaddingTop(), frame.getPaddingEnd(), frame.getPaddingBottom());
        }
    }
}
