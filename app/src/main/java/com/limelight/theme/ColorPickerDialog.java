package com.limelight.theme;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.limelight.R;
import com.limelight.ui.Dialogs;

// NOTE: Picks the custom accent color by dragging or by typing a hex code, and shows it on black and on a card before it is used.
// NOTE: The picker and the hex field follow each other. A code that is not a full valid color keeps the last good one and disables Use color.
// NOTE: The color is a seed for the Material dynamic color engine, which builds every tone the app needs from it (see ThemeManager.colorOptions).
public final class ColorPickerDialog {
    public interface OnPicked {
        void onPicked(int color);
    }

    private ColorPickerDialog() {
    }

    public static void show(Context context, int initialColor, OnPicked onPicked) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        Context dialogContext = builder.getContext();
        View body = LayoutInflater.from(dialogContext).inflate(R.layout.dialog_color_picker, null);
        HsvPickerView area = body.findViewById(R.id.color_picker_area);
        EditText hex = body.findViewById(R.id.color_picker_hex);
        TextView onBlack = body.findViewById(R.id.color_picker_on_black);
        TextView onCard = body.findViewById(R.id.color_picker_on_card);
        TextView note = body.findViewById(R.id.color_picker_note);
        Button reset = body.findViewById(R.id.color_picker_reset);

        final int[] current = {0xFF000000 | initialColor};
        final boolean[] settingText = {false};
        final AlertDialog[] dialog = new AlertDialog[1];

        Runnable showColor = () -> {
            swatch(onBlack, current[0], AccentColors.PREVIEW_BLACK, context);
            swatch(onCard, current[0], AccentColors.PREVIEW_CARD, context);
            note.setText(AccentColors.lowContrastOnDark(current[0])
                    ? context.getString(R.string.color_picker_dark_note)
                    : context.getString(R.string.color_picker_contrast, String.format(java.util.Locale.ROOT, "%.1f",
                            AccentColors.contrast(current[0], AccentColors.PREVIEW_CARD))));
        };

        settingText[0] = true;
        hex.setText(AccentColors.toHex(current[0]));
        settingText[0] = false;
        area.setColor(current[0]);
        showColor.run();

        area.setOnColorChanged((color, fromUser) -> {
            current[0] = color;
            settingText[0] = true;
            hex.setText(AccentColors.toHex(color));
            settingText[0] = false;
            if (dialog[0] != null) {
                dialog[0].getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
            }
            showColor.run();
        });

        hex.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (settingText[0]) {
                    return;
                }
                // Typing and pasting are cleaned up here, so only hex digits stay and the code never grows past six
                String clean = AccentColors.sanitize(s);
                String normalized = clean.length() == 6 ? AccentColors.normalize(clean) : null;
                if (normalized != null) {
                    current[0] = AccentColors.parseOrDefault(normalized);
                    area.setColor(current[0]);
                    showColor.run();
                }
                if (dialog[0] != null) {
                    dialog[0].getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(normalized != null);
                }
                String shown = clean.isEmpty() ? "" : "#" + clean;
                if (!shown.equals(s.toString())) {
                    settingText[0] = true;
                    s.replace(0, s.length(), shown);
                    settingText[0] = false;
                }
            }
        });

        reset.setOnClickListener(v -> {
            current[0] = AccentColors.DEFAULT_SEED;
            area.setColor(current[0]);
            settingText[0] = true;
            hex.setText(AccentColors.toHex(current[0]));
            settingText[0] = false;
            dialog[0].getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
            showColor.run();
        });

        builder.setCustomTitle(Dialogs.header(dialogContext, R.drawable.ic_sa_palette,
                        context.getString(R.string.color_picker_title), context.getString(R.string.color_picker_hint)))
                .setView(body)
                .setPositiveButton(R.string.color_picker_use, (d, which) -> onPicked.onPicked(current[0]))
                .setNegativeButton(R.string.game_menu_cancel, null);
        dialog[0] = builder.create();
        // The keyboard must not hide the hex field or the buttons
        dialog[0].getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        dialog[0].show();
        Dialogs.fitWidth(dialog[0], Dialogs.WIDTH_STANDARD_DP);
    }

    // A rounded block in the color with its own label, on the surface the color is judged against
    private static void swatch(TextView view, int color, int surface, Context context) {
        float density = context.getResources().getDisplayMetrics().density;
        GradientDrawable box = new GradientDrawable();
        box.setColor(surface);
        box.setCornerRadius(14 * density);
        box.setStroke((int) (3 * density), color);
        view.setBackground(box);
        view.setTextColor(color);
    }
}
