package com.limelight.preferences;

import android.content.Context;
import android.util.AttributeSet;

import androidx.preference.ListPreference;

// NOTE: A list setting whose line in the settings list shows the chosen entry above the description ("1080p", "60 FPS"),
// as the slider settings show their value. A value that is not among the entries (a custom resolution before the list
// is rebuilt) is shown as it is stored, with "x" written as "×". [NOT-TESTED]
public class ValueListPreference extends ListPreference {
    public ValueListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    // The description from preferences.xml alone, for a dialog that shows the value in its own fields
    protected CharSequence getDescription() {
        return super.getSummary();
    }

    @Override
    public CharSequence getSummary() {
        CharSequence description = getDescription();
        CharSequence entry = getEntry();
        String value = getValue();
        CharSequence shown = entry != null ? entry : (value != null ? value.replace("x", " × ") : null);
        if (shown == null || shown.length() == 0) {
            return description;
        }
        if (description == null || description.length() == 0) {
            return shown;
        }
        return shown + "\n" + description;
    }
}
