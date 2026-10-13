package com.limelight.preferences;

import android.content.Context;
import android.util.AttributeSet;

import androidx.preference.ListPreference;

import com.limelight.theme.AppStylePicker;

// The app style setting, opened as two preview cards instead of a plain list of names
public class AppStylePreference extends ListPreference {
    public AppStylePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onClick() {
        AppStylePicker.show(getContext(), getValue(), picked -> {
            if (!picked.equals(getValue()) && callChangeListener(picked)) {
                setValue(picked);
            }
        });
    }
}
