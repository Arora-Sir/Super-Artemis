package com.limelight.preferences;

import android.content.Context;
import android.util.AttributeSet;

import androidx.preference.ListPreference;

import com.limelight.theme.AppStylePicker;
import com.limelight.theme.ThemeManager;

// The app style setting, opened as two preview cards and the color scheme swatches instead of a plain list of names
public class AppStylePreference extends ListPreference {
    public AppStylePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onClick() {
        String scheme = ThemeManager.currentScheme(getContext());
        String style = ThemeManager.currentStyle(getContext());
        AppStylePicker.show(getContext(), style, (picked, pickedScheme) -> {
            boolean changed = !picked.equals(style) || (ThemeManager.MATERIAL_YOU.equals(picked) && !pickedScheme.equals(scheme));
            // The listener rebuilds the screen, so a scheme change alone also runs it
            if (changed && callChangeListener(picked)) {
                ThemeManager.choose(getContext(), picked, pickedScheme);
                setValue(picked);
            }
            else if (!changed) {
                ThemeManager.choose(getContext(), picked, pickedScheme);
            }
        });
    }
}
