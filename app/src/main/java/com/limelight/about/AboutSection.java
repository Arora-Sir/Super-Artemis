package com.limelight.about;

import android.content.Context;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.limelight.BuildConfig;
import com.limelight.R;

// Wires the Settings > About entries, so the settings screen only calls bind() and every link comes from Links
public final class AboutSection {
    private AboutSection() {
    }

    public static void bind(PreferenceFragmentCompat fragment) {
        Context context = fragment.requireContext();
        onClick(fragment, "pref_about_support", () -> SupportDialog.show(context));
        onClick(fragment, "pref_about_profiles", () -> AboutDialogs.showProfiles(context));
        onClick(fragment, "pref_about_credits", () -> AboutDialogs.showCredits(context));
        onClick(fragment, "pref_about_star", () -> Links.open(context, Links.REPOSITORY));
        onClick(fragment, "pref_about_source", () -> Links.open(context, Links.REPOSITORY));
        onClick(fragment, "pref_about_issue", () -> Links.open(context, Links.NEW_ISSUE));
        onClick(fragment, "pref_about_privacy", () -> Links.open(context, Links.PRIVACY));
        onClick(fragment, "pref_about_version", () -> Links.open(context, Links.RELEASES));

        Preference version = fragment.findPreference("pref_about_version");
        if (version != null) {
            version.setSummary(context.getString(R.string.summary_about_version, BuildConfig.VERSION_NAME, BuildConfig.BUILD_TYPE));
        }
    }

    private static void onClick(PreferenceFragmentCompat fragment, String key, Runnable action) {
        Preference preference = fragment.findPreference(key);
        if (preference != null) {
            preference.setOnPreferenceClickListener(p -> {
                action.run();
                return true;
            });
        }
    }
}
