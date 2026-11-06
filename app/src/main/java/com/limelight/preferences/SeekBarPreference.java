package com.limelight.preferences;

import android.content.Context;
import android.text.InputType;
import android.text.method.DigitsKeyListener;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;

import com.limelight.R;

import java.util.Locale;

// Based on a Stack Overflow example: http://stackoverflow.com/questions/1974193/slider-on-my-preferencescreen
// NOTE: The dialog of a numeric setting: "-", a field with the number and "+" above the bar, so a value can be typed, nudged
// by one step or dragged. The mapping between the value and the bar lives in SliderScale; seekbar:scale="log" makes the bar
// logarithmic (the two bitrates). The list line shows the current value above the description. [NOT-TESTED]
public class SeekBarPreference extends Preference
{
    private static final String ANDROID_SCHEMA_URL = "http://schemas.android.com/apk/res/android";
    private static final String SEEKBAR_SCHEMA_URL = "http://schemas.moonlight-stream.com/apk/res/seekbar";

    // Width of the number field, in dp (the mockup: 84)
    private static final int FIELD_DP = 84;
    // Keyboard and remote arrows move a logarithmic bar by 1 % of its length
    private static final int LOG_KEY_STEP = SliderScale.LOG_POSITIONS / 100;

    private AlertDialog dialog;
    private SeekBar seekBar;
    // Description under the title; its side indent is matched to the title after the dialog is shown
    private TextView splashText;
    private EditText valueField;
    private final Context context;

    private final String dialogMessage;
    private final String suffix;
    private final int defaultValue;
    private final int keyStepSize;
    private final SliderScale scale;
    private int currentValue;
    // Value shown in the open dialog; written to the setting only on OK
    private int dialogValue;
    // True while the code itself moves the bar, so the bar listener leaves the shown value alone
    private boolean settingBar;

    public SeekBarPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;

        // Read the message from XML
        int dialogMessageId = attrs.getAttributeResourceValue(ANDROID_SCHEMA_URL, "dialogMessage", 0);
        if (dialogMessageId == 0) {
            dialogMessage = attrs.getAttributeValue(ANDROID_SCHEMA_URL, "dialogMessage");
        }
        else {
            dialogMessage = context.getString(dialogMessageId);
        }

        // Get the suffix for the number displayed in the dialog
        int suffixId = attrs.getAttributeResourceValue(ANDROID_SCHEMA_URL, "text", 0);
        if (suffixId == 0) {
            suffix = attrs.getAttributeValue(ANDROID_SCHEMA_URL, "text");
        }
        else {
            suffix = context.getString(suffixId);
        }

        // Get default, min, and max seekbar values
        defaultValue = attrs.getAttributeIntValue(ANDROID_SCHEMA_URL, "defaultValue", PreferenceConfiguration.getDefaultBitrate(context));
        int maxValue = attrs.getAttributeIntValue(ANDROID_SCHEMA_URL, "max", 100);
        int minValue = attrs.getAttributeIntValue(SEEKBAR_SCHEMA_URL, "min", 1);
        int stepSize = attrs.getAttributeIntValue(SEEKBAR_SCHEMA_URL, "step", 1);
        int divisor = attrs.getAttributeIntValue(SEEKBAR_SCHEMA_URL, "divisor", 1);
        keyStepSize = attrs.getAttributeIntValue(SEEKBAR_SCHEMA_URL, "keyStep", 0);
        boolean logarithmic = "log".equals(attrs.getAttributeValue(SEEKBAR_SCHEMA_URL, "scale"));
        scale = new SliderScale(minValue, maxValue, stepSize, divisor, logarithmic);
    }

    private int dp(int value) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, context.getResources().getDisplayMetrics()));
    }

    // Value with its unit, as the list line shows it: "15,0 Mbps", "30 s", "90%"
    private String withSuffix(int value) {
        String t = scale.format(value, Locale.getDefault());
        if (suffix == null) {
            return t;
        }
        // A space before every unit but "%", so one-letter units read "60 s", not "60s"
        return t.concat(suffix.equals("%") ? suffix : " " + suffix);
    }

    // Shows a value in the field and puts the bar on its position
    private void showValue(int value) {
        dialogValue = scale.bound(value);
        valueField.setText(scale.format(dialogValue, Locale.getDefault()));
        valueField.setSelection(valueField.getText().length());
        settingBar = true;
        seekBar.setProgress(scale.positionOf(dialogValue));
        settingBar = false;
    }

    // Takes what the user typed in the field; text that is not a number brings back the last value
    private void takeTypedValue() {
        Integer typed = scale.parse(valueField.getText().toString());
        showValue(typed != null ? typed : dialogValue);
    }

    protected AlertDialog getDialog() {
        if (dialog != null) {
            return dialog;
        }

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(6, 6, 6, 6);

        splashText = new TextView(context);
        StepperViews.alignWithTitle(context, splashText, 6);
        if (dialogMessage != null) {
            splashText.setText(dialogMessage);
        }
        layout.addView(splashText);

        // Row above the bar: "-", the number field with its unit, "+"
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        row.setPadding(0, dp(8), 0, dp(4));

        View minus = StepperViews.roundButton(context, R.drawable.ic_stepper_minus, "-");
        View plus = StepperViews.roundButton(context, R.drawable.ic_stepper_plus, "+");

        valueField = StepperViews.numberField(context, FIELD_DP, 0);
        int inputType = InputType.TYPE_CLASS_NUMBER;
        if (scale.getMin() < 0) {
            inputType |= InputType.TYPE_NUMBER_FLAG_SIGNED;
        }
        boolean fractional = !scale.format(0, Locale.US).equals("0");
        if (fractional) {
            inputType |= InputType.TYPE_NUMBER_FLAG_DECIMAL;
        }
        // The number keyboard offers "." or "," depending on the language, so the field takes both; the raw input type is set
        // after the key listener, because a key listener alone switches the keyboard to letters
        valueField.setKeyListener(DigitsKeyListener.getInstance(fractional ? "0123456789-.," : "0123456789-"));
        valueField.setRawInputType(inputType);
        valueField.setOnEditorActionListener((v, actionId, event) -> {
            takeTypedValue();
            return false;
        });
        valueField.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                takeTypedValue();
            }
        });

        // "-", the box with the number and its unit inside, "+" — the row of the accepted mockup (variant A)
        row.addView(minus, StepperViews.buttonParams(context));
        row.addView(StepperViews.box(context, valueField, suffix), StepperViews.middleParams(context));
        row.addView(plus, StepperViews.buttonParams(context));
        layout.addView(row, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        minus.setOnClickListener(v -> {
            takeTypedValue();
            showValue(scale.minus(dialogValue));
        });
        plus.setOnClickListener(v -> {
            takeTypedValue();
            showValue(scale.plus(dialogValue));
        });

        seekBar = new SeekBar(context);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int position, boolean fromUser) {
                if (settingBar) {
                    return;
                }
                // The bar shows the value of its position; the field follows it
                dialogValue = scale.valueAt(position);
                valueField.setText(scale.format(dialogValue, Locale.getDefault()));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                valueField.clearFocus();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        layout.addView(seekBar, StepperViews.barParams(context));

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);
        dialogBuilder.setTitle(getTitle());
        dialogBuilder.setView(layout);

        dialogBuilder.setPositiveButton("OK", (dialog, which) -> {
            // A number typed without pressing Done still counts
            takeTypedValue();
            if (shouldPersist()) {
                currentValue = dialogValue;
                persistInt(currentValue);
                callChangeListener(currentValue);
                notifyChanged();
            }

            dialog.dismiss();
        });
        dialogBuilder.setNegativeButton(context.getString(R.string.cancel), (dialog, which) -> dialog.dismiss());

        dialog = dialogBuilder.create();
        return dialog;
    }

    protected void updateSeekbar() {
        seekBar.setMax(scale.positions());
        if (scale.isLogarithmic()) {
            seekBar.setKeyProgressIncrement(LOG_KEY_STEP);
        }
        else if (keyStepSize != 0) {
            // keyStep is written in setting units, while a linear bar counts steps
            seekBar.setKeyProgressIncrement(Math.max(1, keyStepSize / scale.buttonStep(0)));
        }
        showValue(currentValue);
    }

    @Override
    protected void onSetInitialValue(boolean restore, Object defaultValue)
    {
        super.onSetInitialValue(restore, defaultValue);
        if (restore) {
            currentValue = shouldPersist() ? getPersistedInt(this.defaultValue) : 0;
        }
        else {
            currentValue = (Integer) defaultValue;
        }
    }

    // List line: the current value first, then the description from preferences.xml
    @Override
    public CharSequence getSummary() {
        CharSequence description = super.getSummary();
        int value = shouldPersist() && getSharedPreferences() != null ? getPersistedInt(defaultValue) : currentValue;
        String shown = withSuffix(value);
        if (description == null || description.length() == 0) {
            return shown;
        }
        return shown + "\n" + description;
    }

    public void setProgress(int progress) {
        this.currentValue = progress;
        if (seekBar != null) {
            showValue(progress);
        }
    }
    public int getProgress() {
        return currentValue + scale.getMin();
    }

    public void showDialog() {
        AlertDialog dialog = getDialog();
        if (shouldPersist()) {
            currentValue = getPersistedInt(defaultValue);
        }
        updateSeekbar();
        dialog.show();
        // The bar takes the colour of the dialog buttons, as in the mockup
        StepperViews.tintBar(seekBar, dialog.getButton(AlertDialog.BUTTON_POSITIVE).getCurrentTextColor());
        StepperViews.pillButtons(dialog);
        StepperViews.matchTitleIndent(dialog, splashText);
    }

    @Override
    protected void onClick() {
        super.onClick();
        showDialog();
    }
}
