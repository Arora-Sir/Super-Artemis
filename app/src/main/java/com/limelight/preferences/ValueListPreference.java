package com.limelight.preferences;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.TextView;

import androidx.preference.ListPreference;
import androidx.preference.PreferenceViewHolder;

// NOTE: Displays a list setting with the active value shown inline in the title.
// NOTE: Keeps the description full-width underneath without multi-line summary prepending or unconstrained widget columns.
// NOTE: Long titles wrap up to two lines on narrow phone screens to avoid ellipsis clipping.
public class ValueListPreference extends ListPreference {
    protected CharSequence rawTitle;

    public ValueListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        rawTitle = super.getTitle();
    }

    public ValueListPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        rawTitle = super.getTitle();
    }

    public ValueListPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        rawTitle = super.getTitle();
    }

    public ValueListPreference(Context context) {
        super(context);
        rawTitle = super.getTitle();
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        TextView titleView = (TextView) holder.findViewById(android.R.id.title);
        if (titleView != null) {
            titleView.setSingleLine(false);
            titleView.setMaxLines(2);
            titleView.setEllipsize(TextUtils.TruncateAt.END);
        }
    }

    @Override
    public void setTitle(CharSequence title) {
        rawTitle = title;
        super.setTitle(title);
    }

    // Formatted text of the active value: "1080p", "Native Full-Screen (1560×720)", "60 FPS".
    public CharSequence getDisplayValue() {
        CharSequence entry = getEntry();
        String value = getValue();
        return entry != null ? entry : (value != null ? value.replace("x", " × ") : "");
    }

    @Override
    public CharSequence getTitle() {
        CharSequence value = getDisplayValue();
        if (value == null || value.length() == 0 || rawTitle == null) {
            return rawTitle != null ? rawTitle : super.getTitle();
        }
        return rawTitle + ": " + value;
    }

    @Override
    public CharSequence getDialogTitle() {
        return rawTitle != null ? rawTitle : super.getDialogTitle();
    }

    // Clean description from preferences.xml alone, retaining full width.
    @Override
    public CharSequence getSummary() {
        return super.getSummary();
    }
}
