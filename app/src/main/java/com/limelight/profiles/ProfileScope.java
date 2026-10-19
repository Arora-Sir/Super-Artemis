package com.limelight.profiles;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.limelight.R;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

// NOTE: The one place that decides which settings belong to a profile and which apply to the whole app.
// NOTE: The profile editor is the Settings screen in profile mode, so every setting shows there, and app-wide ones are greyed out with a note instead of vanishing.
// NOTE: ProfileScopeTest fails when a key listed here no longer exists in preferences.xml, so a renamed setting cannot silently drop out of this list.
public final class ProfileScope {
    private ProfileScope() {
    }

    // Settings and editors that act on the whole app, whatever profile is active
    public static final Set<String> APP_WIDE = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "list_app_style",
            "list_background_policy",
            "checkbox_auto_connect_last",
            "checkbox_gestures_enabled",
            "pref_customize_game_menu",
            "pref_custom_keys",
            "option_help_custom_keys",
            "import_keyboard_file",
            "export_keyboard_file",
            "import_special_button_file",
            "option_reset_osc_preference")));

    // Saved in each profile, but its editor always edits the active profile, so it is edited from the stream instead
    public static final Set<String> EDIT_WHEN_ACTIVE = Collections.unmodifiableSet(new HashSet<>(Collections.singletonList(
            "pref_quick_bar")));

    // Whole sections that only make sense for the app: backup of every profile and About
    public static final Set<String> APP_SECTIONS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "category_backup_restore",
            "category_about")));

    // Greys out app-wide settings with a note and hides the app sections, so the rest of the screen matches Settings exactly
    public static void applyToProfileEditor(PreferenceFragmentCompat fragment) {
        for (String key : APP_WIDE) {
            mark(fragment.findPreference(key), fragment.getString(R.string.profile_scope_app_wide));
        }
        for (String key : EDIT_WHEN_ACTIVE) {
            mark(fragment.findPreference(key), fragment.getString(R.string.profile_scope_edit_when_active));
        }
        for (String key : APP_SECTIONS) {
            Preference section = fragment.findPreference(key);
            if (section != null) {
                section.setVisible(false);
            }
        }
    }

    private static void mark(Preference preference, String note) {
        if (preference == null) {
            return;
        }
        preference.setEnabled(false);
        // List settings show their value through a summary provider, which refuses a manual summary until it is removed
        preference.setSummaryProvider(null);
        preference.setSummary(note);
    }
}
