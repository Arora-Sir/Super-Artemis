package com.limelight.preferences;

import android.content.Context;
import android.text.InputType;
import android.text.TextUtils;
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
import androidx.preference.PreferenceViewHolder;

import com.limelight.R;

import java.util.Locale;

// NOTE: Dialog of a numeric setting: minus button, number field and plus button above the slider.
// NOTE: Bitrate is a continuous scalar quantity, so a slider with stepper buttons is natural.
// NOTE: Uses calibrated power curve (35 to 40 Mbps center) and 0.5 Mbps decimal precision.
// NOTE: Active value displays inline in the title to preserve full summary width.
public class SeekBarPreference extends Preference
{
    private static final String ANDROID_SCHEMA_URL = "http://schemas.android.com/apk/res/android";
    private static final String SEEKBAR_SCHEMA_URL = "http://schemas.moonlight-stream.com/apk/res/seekbar";

    private static final int FIELD_DP = 74;
    private static final int LOG_KEY_STEP = SliderScale.LOG_POSITIONS / 100;

    private AlertDialog dialog;
    private SeekBar seekBar;
    private TextView splashText;
    private EditText valueField;
    private final Context context;

    private final String dialogMessage;
    private final String suffix;
    private final int defaultValue;
    private final int keyStepSize;
    private final SliderScale scale;
    private int currentValue;
    private int dialogValue;
    private boolean settingBar;
    private CharSequence rawTitle;

    public SeekBarPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        rawTitle = super.getTitle();

        int dialogMessageId = attrs.getAttributeResourceValue(ANDROID_SCHEMA_URL, "dialogMessage", 0);
        if (dialogMessageId == 0) {
            dialogMessage = attrs.getAttributeValue(ANDROID_SCHEMA_URL, "dialogMessage");
        }
        else {
            dialogMessage = context.getString(dialogMessageId);
        }

        int suffixId = attrs.getAttributeResourceValue(ANDROID_SCHEMA_URL, "text", 0);
        if (suffixId == 0) {
            suffix = attrs.getAttributeValue(ANDROID_SCHEMA_URL, "text");
        }
        else {
            suffix = context.getString(suffixId);
        }

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

    @Override
    public void setTitle(CharSequence title) {
        rawTitle = title;
        super.setTitle(title);
    }

    @Override
    public CharSequence getTitle() {
        if (rawTitle == null) {
            return super.getTitle();
        }
        int value = shouldPersist() && getSharedPreferences() != null ? getPersistedInt(defaultValue) : currentValue;
        return rawTitle + ": " + withSuffix(value);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        TextView titleView = (TextView) holder.findViewById(android.R.id.title);
        if (titleView != null) {
            titleView.setSingleLine(false);
            titleView.setMaxLines(2);
            titleView.setEllipsize(TextUtils.TruncateAt.END);
        }
    }

    @Override
    public CharSequence getSummary() {
        return dialogMessage != null ? dialogMessage : super.getSummary();
    }

    private String withSuffix(int value) {
        String t = scale.format(value, Locale.getDefault());
        if (suffix == null) {
            return t;
        }
        return t.concat(suffix.equals("%") ? suffix : " " + suffix);
    }

    private void showValue(int value) {
        dialogValue = scale.bound(value);
        valueField.setText(scale.format(dialogValue, Locale.getDefault()));
        valueField.setSelection(valueField.getText().length());
        settingBar = true;
        seekBar.setProgress(scale.positionOf(dialogValue));
        settingBar = false;
    }

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

        row.addView(minus, StepperViews.buttonParams(context));
        row.addView(StepperViews.box(context, valueField, suffix), StepperViews.middleParams(context));
        row.addView(plus, StepperViews.buttonParams(context));
        layout.addView(row, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        StepperViews.attachAutoRepeat(minus, () -> {
            takeTypedValue();
            showValue(scale.minus(dialogValue));
        });
        StepperViews.attachAutoRepeat(plus, () -> {
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
        dialogBuilder.setTitle(rawTitle != null ? rawTitle : getTitle());
        dialogBuilder.setView(layout);

        dialogBuilder.setPositiveButton("OK", (dialog, which) -> {
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
