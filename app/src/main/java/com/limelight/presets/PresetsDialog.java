package com.limelight.presets;

import android.app.AlertDialog;
import android.content.Context;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.widget.TextViewCompat;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;

import com.limelight.R;
import com.limelight.profiles.SettingsProfile;
import com.limelight.ui.ChoiceList;
import com.limelight.ui.Dialogs;

import java.util.List;

// NOTE: Settings > Performance presets: pick a preset, read what it trades and exactly which settings change, then apply.
// NOTE: Nothing is overwritten. The values in use are saved as a "Before" profile first, and "Use my own settings" switches the preset off again.
public final class PresetsDialog {
    private static final String KEY = "pref_performance_presets";

    private PresetsDialog() {
    }

    // Shows the active preset in the summary of the Settings row and opens the chooser from it
    public static void bind(Fragment fragment) {
        Preference row = ((androidx.preference.PreferenceFragmentCompat) fragment).findPreference(KEY);
        if (row == null) {
            return;
        }
        Context context = fragment.requireContext();
        row.setSummary(summary(context));
        row.setOnPreferenceClickListener(p -> {
            com.limelight.ui.SafeRun.run(context, KEY, () -> show(context, () -> row.setSummary(summary(context))));
            return true;
        });
    }

    static String summary(Context context) {
        Preset active = PresetActions.activePreset(context);
        if (active == null) {
            return context.getString(R.string.summary_presets_off);
        }
        String name = context.getString(active.nameRes);
        return PresetActions.isCustomized(active) ? context.getString(R.string.summary_presets_edited, name) : context.getString(R.string.summary_presets_on, name);
    }

    public static void show(Context context, Runnable onChanged) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        ChoiceList choices = new ChoiceList(builder, R.drawable.ic_sa_speed, context.getString(R.string.title_presets), R.string.game_menu_cancel);
        Preset active = PresetActions.activePreset(context);
        for (Preset preset : PresetCatalog.load(context)) {
            choices.add(iconFor(preset), context.getString(preset.nameRes), active != null && active.id.equals(preset.id), () -> showDetails(context, preset, onChanged));
        }
        choices.list().addView(hint(choices.context(), R.string.presets_hint));
        choices.add(R.drawable.ic_sa_save, context.getString(R.string.presets_save_current), false, () -> askProfileName(context, onChanged));
        if (active != null) {
            choices.add(R.drawable.ic_sa_undo, context.getString(R.string.presets_use_own), false, () -> {
                PresetActions.useOwnSettings();
                Toast.makeText(context, R.string.presets_own_on, Toast.LENGTH_LONG).show();
                onChanged.run();
            });
        }
        choices.show();
    }

    private static int iconFor(Preset preset) {
        switch (preset.id) {
            case "lowest_latency":
                return R.drawable.ic_sa_speed;
            case "best_quality":
                return R.drawable.ic_sa_high_quality;
            case "battery_saver":
                return R.drawable.ic_sa_battery_saver;
            default:
                return R.drawable.ic_sa_tune;
        }
    }

    private static TextView hint(Context context, int textRes) {
        TextView hint = new TextView(context);
        hint.setText(textRes);
        TextViewCompat.setTextAppearance(hint, R.style.TextAppearance_SuperArtemis_BodySmall);
        int pad = (int) Dialogs.dp(context, 16);
        hint.setPadding(pad, pad / 2, pad, pad / 2);
        return hint;
    }

    // What the preset trades and which settings change from what to what, before anything is applied
    public static void showDetails(Context context, Preset preset, Runnable onChanged) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        Context dialogContext = builder.getContext();
        LinearLayout body = new LinearLayout(dialogContext);
        body.setOrientation(LinearLayout.VERTICAL);
        int side = (int) Dialogs.dp(dialogContext, 24);
        body.setPadding(side, 0, side, (int) Dialogs.dp(dialogContext, 8));

        body.addView(line(dialogContext, R.string.presets_good_for, R.style.TextAppearance_SuperArtemis_SectionLabel));
        body.addView(line(dialogContext, context.getString(preset.prosRes), R.style.TextAppearance_SuperArtemis_BodyMedium));
        body.addView(line(dialogContext, R.string.presets_costs, R.style.TextAppearance_SuperArtemis_SectionLabel));
        body.addView(line(dialogContext, context.getString(preset.consRes), R.style.TextAppearance_SuperArtemis_BodyMedium));
        body.addView(line(dialogContext, R.string.presets_changes, R.style.TextAppearance_SuperArtemis_SectionLabel));
        List<PresetActions.Change> changes = PresetActions.diff(context, preset);
        for (PresetActions.Change change : changes) {
            String text = change.isSame()
                    ? context.getString(R.string.presets_change_same, change.title, change.after)
                    : context.getString(R.string.presets_change_line, change.title, change.before, change.after);
            body.addView(line(dialogContext, text, R.style.TextAppearance_SuperArtemis_BodyMedium));
        }
        body.addView(line(dialogContext, context.getString(R.string.presets_safe_note, context.getString(R.string.preset_before_name, context.getString(preset.nameRes))),
                R.style.TextAppearance_SuperArtemis_BodySmall));

        ScrollView scroll = new ScrollView(dialogContext);
        scroll.addView(body);
        builder.setCustomTitle(Dialogs.header(dialogContext, iconFor(preset), context.getString(preset.nameRes), context.getString(preset.summaryRes)))
                .setView(scroll)
                .setPositiveButton(R.string.presets_apply, (d, which) -> {
                    PresetActions.apply(context, preset);
                    Toast.makeText(context, context.getString(R.string.presets_applied, context.getString(preset.nameRes)), Toast.LENGTH_LONG).show();
                    onChanged.run();
                })
                .setNegativeButton(R.string.game_menu_cancel, null);
        Dialogs.show(builder, Dialogs.WIDTH_STANDARD_DP);
    }

    private static TextView line(Context context, int textRes, int appearance) {
        return line(context, context.getString(textRes), appearance);
    }

    private static TextView line(Context context, String text, int appearance) {
        TextView view = new TextView(context);
        view.setText(text);
        TextViewCompat.setTextAppearance(view, appearance);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = (int) Dialogs.dp(context, 8);
        view.setLayoutParams(params);
        view.setGravity(Gravity.START);
        return view;
    }

    // The values the presets cover, saved under a name so a hand-tuned combination can be switched back to from Profiles
    private static void askProfileName(Context context, Runnable onChanged) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        Context dialogContext = builder.getContext();
        EditText name = new EditText(dialogContext, null, 0, R.style.Widget_SuperArtemis_TextField);
        name.setHint(R.string.presets_profile_name_hint);
        name.setSingleLine(true);
        name.setText(R.string.presets_profile_default_name);
        name.selectAll();
        FrameWrapper wrapper = new FrameWrapper(dialogContext, name);
        builder.setCustomTitle(Dialogs.header(dialogContext, R.drawable.ic_sa_save, context.getString(R.string.presets_save_current), context.getString(R.string.presets_save_hint)))
                .setView(wrapper)
                .setPositiveButton(R.string.game_menu_editor_save, (d, which) -> {
                    String text = name.getText().toString().trim();
                    SettingsProfile saved = PresetActions.saveCurrentAsProfile(context, text.isEmpty() ? context.getString(R.string.presets_profile_default_name) : text);
                    Toast.makeText(context, context.getString(R.string.presets_profile_saved, saved.getName()), Toast.LENGTH_LONG).show();
                    onChanged.run();
                })
                .setNegativeButton(R.string.game_menu_cancel, null);
        AlertDialog dialog = builder.create();
        dialog.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        dialog.show();
        Dialogs.fitWidth(dialog, Dialogs.WIDTH_STANDARD_DP);
    }

    // A field with the side margins of a dialog body
    private static final class FrameWrapper extends LinearLayout {
        FrameWrapper(Context context, EditText field) {
            super(context);
            setOrientation(VERTICAL);
            int side = (int) Dialogs.dp(context, 24);
            setPadding(side, 0, side, (int) Dialogs.dp(context, 8));
            addView(field, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }
    }
}
