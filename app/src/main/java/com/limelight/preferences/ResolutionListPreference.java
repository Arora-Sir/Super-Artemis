package com.limelight.preferences;

import android.content.Context;
import android.util.AttributeSet;

import com.limelight.R;
import com.limelight.ui.ChoiceList;
import com.limelight.ui.ThemedDialogs;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

// NOTE: Displays video resolution options partitioned into three distinct visual sections.
// NOTE: Section 1 lists standard broadcast resolutions with aspect ratio subtitles.
// NOTE: Section 2 lists native display panel modes matching the active device screen.
// NOTE: Section 3 lists user-configured custom dimensions placed at the end of the picker.
// NOTE: Tapping an option applies the change, alerts the user on native modes, and recalibrates bitrate defaults.
public class ResolutionListPreference extends ValueListPreference {

    private static final class ResolutionItem {
        final String value;
        final String label;
        final String subtitle;
        final int iconRes;

        ResolutionItem(String value, String label, String subtitle, int iconRes) {
            this.value = value;
            this.label = label;
            this.subtitle = subtitle;
            this.iconRes = iconRes;
        }
    }

    public ResolutionListPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public ResolutionListPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public ResolutionListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ResolutionListPreference(Context context) {
        super(context);
    }

    @Override
    protected void onClick() {
        CharSequence[] entries = getEntries();
        CharSequence[] values = getEntryValues();
        if (entries == null || values == null || entries.length == 0) {
            return;
        }

        String current = getValue();
        Context context = getContext();
        CharSequence dialogTitle = rawTitle != null ? rawTitle : getDialogTitle();
        ChoiceList choices = new ChoiceList(ThemedDialogs.builder(context), R.drawable.ic_sa_videocam, dialogTitle, R.string.game_menu_cancel);

        String customPrefix = context.getString(R.string.resolution_prefix_custom);
        String customPrefValue = PreferenceConfiguration.readPreferences(context).customResolution;

        List<ResolutionItem> standardList = new ArrayList<>();
        List<ResolutionItem> nativeList = new ArrayList<>();
        List<ResolutionItem> customList = new ArrayList<>();

        for (int i = 0; i < entries.length && i < values.length; i++) {
            String entry = entries[i].toString();
            String val = values[i].toString();
            int[] dims = parseDimensions(val);
            int w = dims[0];
            int h = dims[1];

            // NOTE: A saved custom size that equals a standard size, such as 1920 x 1080, must not take the standard chip away, so only a size that is not standard counts as the custom one.
            boolean standardSize = w > 0 && h > 0 && !PreferenceConfiguration.isNativeResolution(w, h) && !entry.startsWith(customPrefix);
            if (entry.startsWith(customPrefix) || (!standardSize && customPrefValue != null && val.equals(customPrefValue))) {
                customList.add(new ResolutionItem(val, entry.replace("x", " × "), formatCustomSubtitle(w, h), R.drawable.ic_sa_tune));
            } else if (w > 0 && h > 0 && !PreferenceConfiguration.isNativeResolution(w, h)) {
                standardList.add(new ResolutionItem(val, entry, formatStandardSubtitle(w, h), R.drawable.ic_sa_computer));
            } else {
                nativeList.add(new ResolutionItem(val, entry.replace("x", " × "), formatNativeSubtitle(entry, w, h), R.drawable.ic_sa_smartphone));
            }
        }

        if (!standardList.isEmpty()) {
            // NOTE: The standard resolutions share one aspect ratio, so it is named once in the heading and each one is a short chip.
            choices.addSectionHeader(standardHeading(context.getString(R.string.section_resolution_standard), standardList));
            List<ChoiceList.Chip> chips = new ArrayList<>();
            for (ResolutionItem item : standardList) {
                chips.add(new ChoiceList.Chip(item.label, item.subtitle, item.value.equals(current), () -> selectResolution(item.value)));
            }
            choices.addChips(chips);
        }

        if (!nativeList.isEmpty()) {
            choices.addSectionHeader(context.getString(R.string.section_resolution_native));
            for (ResolutionItem item : nativeList) {
                choices.add(item.iconRes, item.label, item.subtitle, item.value.equals(current), () -> selectResolution(item.value));
            }
        }

        // NOTE: The Custom Resolution section always has the row that opens the custom size dialog, because that setting sits in a collapsed group of the Video section and is hard to find.
        // NOTE: A saved custom size that differs from every standard and native size is listed above it.
        choices.addSectionHeader(context.getString(R.string.section_resolution_custom));
        for (ResolutionItem item : customList) {
            choices.add(item.iconRes, item.label, item.subtitle, item.value.equals(current), () -> selectResolution(item.value));
        }
        choices.add(R.drawable.ic_sa_edit, context.getString(R.string.title_custom_resolution), false, this::openCustomResolution);

        choices.show();
    }

    // The heading with the shared ratio, such as "Standard Resolutions (16:9)", or the plain heading when the ratios differ
    private static String standardHeading(String heading, List<ResolutionItem> items) {
        String ratio = null;
        for (ResolutionItem item : items) {
            int[] dims = parseDimensions(item.value);
            String own = formatAspectRatio(dims[0], dims[1]);
            if (ratio == null) {
                ratio = own;
            }
            else if (!ratio.equals(own)) {
                return heading;
            }
        }
        return ratio == null || ratio.isEmpty() ? heading : heading + " (" + ratio + ")";
    }

    // The custom size dialog belongs to the Custom resolution row, so this opens it through that row
    private void openCustomResolution() {
        androidx.preference.Preference custom = getPreferenceManager() != null ? getPreferenceManager().findPreference("edit_diy_w_h") : null;
        if (custom != null) {
            custom.performClick();
        }
    }

    private void selectResolution(String val) {
        if (callChangeListener(val)) {
            setValue(val);
            notifyChanged();
        }
    }

    private static int[] parseDimensions(String val) {
        if (val != null) {
            String[] parts = val.split("x");
            if (parts.length == 2) {
                try {
                    return new int[]{Integer.parseInt(parts[0]), Integer.parseInt(parts[1])};
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return new int[]{0, 0};
    }

    private static String formatAspectRatio(int w, int h) {
        if (w <= 0 || h <= 0) {
            return "";
        }
        float ratio = (float) Math.max(w, h) / Math.min(w, h);
        if (Math.abs(ratio - (16.0f / 9.0f)) < 0.03f) {
            return "16:9";
        }
        if (Math.abs(ratio - (16.0f / 10.0f)) < 0.03f) {
            return "16:10";
        }
        if (Math.abs(ratio - (4.0f / 3.0f)) < 0.03f) {
            return "4:3";
        }
        if (Math.abs(ratio - (19.5f / 9.0f)) < 0.05f) {
            return "19.5:9";
        }
        if (Math.abs(ratio - (20.0f / 9.0f)) < 0.05f) {
            return "20:9";
        }
        if (Math.abs(ratio - (21.0f / 9.0f)) < 0.05f) {
            return "21:9";
        }
        if (Math.abs(ratio - (18.7f / 9.0f)) < 0.05f) {
            return "18.7:9";
        }
        return String.format(Locale.US, "%.2f:1", ratio);
    }

    private static String formatStandardSubtitle(int w, int h) {
        return w + " × " + h + " • " + formatAspectRatio(w, h);
    }

    private static String formatNativeSubtitle(String entry, int w, int h) {
        String ratio = formatAspectRatio(w, h);
        if (entry.contains("Full-Screen")) {
            return "Matches device screen • " + ratio;
        }
        return "Cutout safe area • " + ratio;
    }

    private static String formatCustomSubtitle(int w, int h) {
        return "Custom dimensions • " + formatAspectRatio(w, h);
    }
}
