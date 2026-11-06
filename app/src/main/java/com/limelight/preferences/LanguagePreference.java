package com.limelight.preferences;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.ListPreference;

import com.limelight.R;
import com.limelight.ui.ChoiceList;
import com.limelight.ui.ThemedDialogs;
import com.limelight.utils.UiHelper;

public class LanguagePreference extends ListPreference {

    public LanguagePreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public LanguagePreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public LanguagePreference(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public LanguagePreference(@NonNull Context context) {
        super(context);
    }

    // NOTE: The list opens in the same picker as every other Super Artemis choice list, with a check on the current language and every row aligned the same way.
    @Override
    protected void onClick() {
        CharSequence[] names = getEntries();
        CharSequence[] values = getEntryValues();
        if (names == null || values == null) {
            return;
        }
        String current = getValue();
        ChoiceList choices = new ChoiceList(ThemedDialogs.builder(getContext()), R.drawable.ic_sa_language, getTitle(), R.string.game_menu_cancel);
        for (int i = 0; i < names.length && i < values.length; i++) {
            String value = values[i].toString();
            choices.add(R.drawable.ic_sa_language, names[i], value.equals(current), () -> {
                if (callChangeListener(value)) {
                    setValue(value);
                }
            });
        }
        choices.show();
    }

    // NOTE: A new choice is applied right here by recreating the settings screen, so the language changes at once and nothing waits for a restart.
    // NOTE: The first value that is loaded when the screen opens is not a change and does not recreate anything.
    @Override
    public void setValue(String value) {
        String old = getValue();
        super.setValue(value);
        if (old != null && !old.equals(value)) {
            Activity activity = findActivity(getContext());
            if (activity != null) {
                activity.getWindow().getDecorView().post(activity::recreate);
            }
        }
    }

    private static Activity findActivity(Context context) {
        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) {
                return (Activity) context;
            }
            context = ((ContextWrapper) context).getBaseContext();
        }
        return null;
    }
}
