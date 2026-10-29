package com.limelight.theme;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.preference.PreferenceFragmentCompat;

import com.limelight.R;
import com.limelight.preferences.AmoledBlackPreference;
import com.limelight.ui.Dialogs;
import com.limelight.ui.SafeRun;

// NOTE: Surface lift is how far cards, fields and sections rise from the black of Pure black, in ten levels from flat to lifted.
// NOTE: The slider only moves the sample inside the dialog. Apply stores the level and rebuilds the screen, and Cancel changes nothing, as in the app style picker.
// NOTE: It opens from the Pure black row (AmoledBlackPreference) while Pure black is on, and its level shows in the summary of that row.
public final class SurfaceLiftDialog {
    private SurfaceLiftDialog() {
    }

    public static void bind(PreferenceFragmentCompat fragment) {
        AmoledBlackPreference row = fragment.findPreference(ThemeManager.PREF_AMOLED);
        if (row == null) {
            return;
        }
        Context context = fragment.requireContext();
        row.setSummaryProvider(p -> row.isChecked()
                ? context.getString(R.string.summary_amoled_lift, ThemeManager.amoledLift(context) + 1, AmoledLadder.LEVELS)
                : context.getString(R.string.summary_amoled_black));
        row.setOnAdjust(() -> SafeRun.run(context, "surface_lift", () -> show(fragment.requireActivity(),
                () -> fragment.requireActivity().getWindow().getDecorView().post(() -> fragment.requireActivity().recreate()))));
    }

    // The position of a level, such as "5 of 10, Recommended"
    static String position(Context context, int level) {
        String position = context.getString(R.string.surface_lift_position, level + 1, AmoledLadder.LEVELS);
        return level == AmoledLadder.RECOMMENDED ? position + ", " + context.getString(R.string.surface_lift_recommended) : position;
    }

    public static void show(Activity activity, Runnable onApplied) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        Context dialogContext = builder.getContext();
        View body = LayoutInflater.from(dialogContext).inflate(R.layout.dialog_surface_lift, null);
        LiftSlider slider = body.findViewById(R.id.surface_lift_slider);
        TextView position = body.findViewById(R.id.surface_lift_position);
        View card = body.findViewById(R.id.surface_lift_card);
        View field = body.findViewById(R.id.surface_lift_field);
        View highest = body.findViewById(R.id.surface_lift_highest);

        Runnable show = () -> {
            int level = slider.getProgress();
            tint(card, AmoledLadder.container(level), dialogContext);
            tint(field, AmoledLadder.high(level), dialogContext);
            tint(highest, AmoledLadder.highest(level), dialogContext);
            position.setText(position(dialogContext, level));
            slider.setContentDescription(activity.getString(R.string.title_surface_lift) + ", " + position(dialogContext, level));
        };
        slider.setInitial(ThemeManager.amoledLift(activity));
        slider.setListener(level -> show.run());
        show.run();

        builder.setCustomTitle(Dialogs.header(dialogContext, R.drawable.ic_sa_palette,
                        activity.getString(R.string.title_surface_lift), activity.getString(R.string.surface_lift_hint)))
                .setView(body)
                .setPositiveButton(R.string.app_style_apply, (d, which) -> {
                    ThemeManager.setAmoledLift(activity, slider.getProgress());
                    onApplied.run();
                })
                .setNeutralButton(R.string.surface_lift_reset, null)
                .setNegativeButton(R.string.game_menu_cancel, null);
        AlertDialog dialog = Dialogs.show(builder, Dialogs.WIDTH_STANDARD_DP);
        // Reset moves the slider to the recommended level and keeps the dialog open
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> slider.setProgress(AmoledLadder.RECOMMENDED));
    }

    private static void tint(View view, int color, Context context) {
        GradientDrawable box = new GradientDrawable();
        box.setColor(color);
        box.setCornerRadius(Dialogs.dp(context, 12));
        view.setBackground(box);
    }
}
