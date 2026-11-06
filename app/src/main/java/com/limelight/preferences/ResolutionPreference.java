package com.limelight.preferences;

import android.content.Context;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.limelight.R;

// NOTE: The Resolution setting as two number fields with a fixed "×" between them, "-" and "+" on the sides and a bar under them.
// NOTE: The bar and the buttons walk the entries the list holds (ResolutionStops); typing two numbers that are not in the list
// makes a custom resolution, saved through the same path as the "Custom resolution" setting (CustomResolutionListener).
// NOTE: The stored value stays "WxH" in list_resolution, so the stream, the profiles and the existing change listener
// (bitrate reset, native resolution warning) see nothing new. [NOT-TESTED]
public class ResolutionPreference extends ValueListPreference {
    // Called on OK with a size that is not in the list, so the settings screen can add it as the custom entry
    public interface CustomResolutionListener {
        void onCustomResolution(String value);
    }

    private static final int FIELD_DP = 70;
    // Four digits are enough for 8K; longer input is cut
    private static final int MAX_DIGITS = 4;

    private CustomResolutionListener customListener;
    private ResolutionStops stops;
    private EditText widthField;
    private EditText heightField;
    private TextView nameText;
    private SeekBar seekBar;
    // Description under the title; its side indent is matched to the title after the dialog is shown
    private TextView splashText;
    // True while the code itself moves the bar
    private boolean settingBar;

    public ResolutionPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setCustomResolutionListener(CustomResolutionListener listener) {
        customListener = listener;
    }

    private int dp(int value) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, getContext().getResources().getDisplayMetrics()));
    }

    // Two numbers from the fields, or null while one of them is empty or zero
    private int[] typedSize() {
        return ResolutionStops.parse(widthField.getText() + "x" + heightField.getText());
    }

    // Shows a size in the fields, names it under them and puts the bar on its stop (or the nearest one for a custom size)
    private void showSize(int width, int height) {
        widthField.setText(String.valueOf(width));
        heightField.setText(String.valueOf(height));
        nameSize(width, height);
    }

    private void nameSize(int width, int height) {
        int i = stops.indexOf(width, height);
        nameText.setText(i >= 0 ? stops.get(i).name : getContext().getString(R.string.resolution_prefix_custom));
        settingBar = true;
        seekBar.setProgress(i >= 0 ? i : stops.nearest(width, height));
        settingBar = false;
    }

    // Puts the bar and the name under the fields in line with what was typed
    private void takeTypedSize() {
        int[] wh = typedSize();
        if (wh != null) {
            nameSize(wh[0], wh[1]);
        }
    }

    private void showStop(int index) {
        ResolutionStops.Stop s = stops.get(index);
        showSize(s.width, s.height);
    }

    @Override
    protected void onClick() {
        Context context = getContext();
        stops = new ResolutionStops(getEntries(), getEntryValues());
        if (stops.size() == 0) {
            // Nothing to put on the bar: the plain list dialog still works
            super.onClick();
            return;
        }

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(6, 6, 6, 6);

        CharSequence description = getDescription();
        if (description != null) {
            splashText = new TextView(context);
            StepperViews.alignWithTitle(context, splashText, 6);
            splashText.setText(description);
            layout.addView(splashText);
        }

        // Row: "-", width, the fixed "x", height, "+"
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        row.setPadding(0, dp(8), 0, 0);

        View minus = StepperViews.roundButton(context, R.drawable.ic_stepper_minus, "-");
        View plus = StepperViews.roundButton(context, R.drawable.ic_stepper_plus, "+");
        widthField = StepperViews.numberField(context, FIELD_DP, MAX_DIGITS);
        heightField = StepperViews.numberField(context, FIELD_DP, MAX_DIGITS);
        // The "x" between the boxes is plain text, so it can never be typed wrong or erased
        TextView times = new TextView(context);
        times.setText("×");
        times.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
        times.setTextColor(StepperViews.secondaryTextColor(context));
        times.setPadding(dp(6), 0, dp(6), 0);

        // "-", the width box, "x", the height box, "+" — the row of the accepted mockup (variant R2)
        LinearLayout sizes = new LinearLayout(context);
        sizes.setOrientation(LinearLayout.HORIZONTAL);
        sizes.setGravity(Gravity.CENTER_VERTICAL);
        sizes.addView(StepperViews.box(context, widthField, null));
        sizes.addView(times);
        sizes.addView(StepperViews.box(context, heightField, null));
        row.addView(minus, StepperViews.buttonParams(context));
        row.addView(sizes, StepperViews.middleParams(context));
        row.addView(plus, StepperViews.buttonParams(context));
        layout.addView(row, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        // Name of the size under the fields: "1080p", "Native (2560x1600)" or "Custom"
        nameText = new TextView(context);
        nameText.setGravity(Gravity.CENTER_HORIZONTAL);
        nameText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        // Clear space between the fields and the name under them (owner 2026-11-06: more than 10 dp)
        nameText.setPadding(0, dp(20), 0, 0);
        layout.addView(nameText, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        seekBar = new SeekBar(context);
        seekBar.setMax(stops.size() - 1);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                if (!settingBar) {
                    showStop(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar bar) {
                widthField.clearFocus();
                heightField.clearFocus();
            }

            @Override
            public void onStopTrackingTouch(SeekBar bar) {}
        });
        layout.addView(seekBar, StepperViews.barParams(context));

        minus.setOnClickListener(v -> {
            int[] wh = typedSize();
            showStop(wh != null ? stops.previous(wh[0], wh[1]) : 0);
        });
        plus.setOnClickListener(v -> {
            int[] wh = typedSize();
            showStop(wh != null ? stops.next(wh[0], wh[1]) : stops.size() - 1);
        });
        widthField.setOnEditorActionListener((v, actionId, event) -> {
            takeTypedSize();
            return false;
        });
        heightField.setOnEditorActionListener((v, actionId, event) -> {
            takeTypedSize();
            return false;
        });
        widthField.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                takeTypedSize();
            }
        });
        heightField.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                takeTypedSize();
            }
        });

        int[] current = ResolutionStops.parse(getValue());
        if (current != null) {
            showSize(current[0], current[1]);
        }
        else {
            showStop(0);
        }

        AlertDialog shown = new AlertDialog.Builder(context)
                .setTitle(getTitle())
                .setView(layout)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    int[] wh = typedSize();
                    if (wh == null) {
                        return;
                    }
                    String value = ResolutionStops.value(wh[0], wh[1]);
                    if (value.equals(getValue())) {
                        return;
                    }
                    boolean inList = stops.indexOf(wh[0], wh[1]) >= 0;
                    if (callChangeListener(value)) {
                        setValue(value);
                        if (!inList && customListener != null) {
                            customListener.onCustomResolution(value);
                        }
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
        // The name under the fields takes the colour of the dialog buttons, as the accent label of the mockup
        int accent = shown.getButton(AlertDialog.BUTTON_POSITIVE).getCurrentTextColor();
        nameText.setTextColor(accent);
        StepperViews.tintBar(seekBar, accent);
        StepperViews.pillButtons(shown);
        StepperViews.matchTitleIndent(shown, splashText);
    }
}
