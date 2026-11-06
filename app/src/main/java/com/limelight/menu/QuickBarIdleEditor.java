package com.limelight.menu;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;


import com.limelight.R;
import com.limelight.ui.Dialogs;

// NOTE: One dialog for what the quick bars do when idle: dim after X seconds to an opacity, hide after Y seconds, and how visible the edge tab is.
// NOTE: Every value has a slider for quick changes and a number field for exact ones, and both stay in step.
public final class QuickBarIdleEditor {
    // The slider covers the useful range, a typed number can go up to IdlePlan.MAX_SECONDS
    private static final int SLIDER_MAX_SECONDS = 120;

    private QuickBarIdleEditor() {
    }

    // One labelled slider with a number field. The value is read back from get().
    private static final class Field {
        final LinearLayout view;
        final SeekBar slider;
        final EditText input;
        final TextView unit;
        final int max;
        final int typedMax;
        final boolean seconds;
        private int value;
        private boolean syncing;

        Field(Context context, int labelRes, boolean seconds, int initial) {
            this.seconds = seconds;
            this.max = seconds ? SLIDER_MAX_SECONDS : 100;
            this.typedMax = seconds ? IdlePlan.MAX_SECONDS : 100;
            float density = context.getResources().getDisplayMetrics().density;

            view = new LinearLayout(context);
            view.setOrientation(LinearLayout.VERTICAL);
            view.setPadding(0, (int) (8 * density), 0, (int) (4 * density));

            LinearLayout header = new LinearLayout(context);
            header.setOrientation(LinearLayout.HORIZONTAL);
            header.setGravity(Gravity.CENTER_VERTICAL);
            TextView label = new TextView(context);
            label.setText(labelRes);
            androidx.core.widget.TextViewCompat.setTextAppearance(label, R.style.TextAppearance_SuperArtemis_SectionLabel);
            header.addView(label, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            input = new EditText(context);
            input.setInputType(InputType.TYPE_CLASS_NUMBER);
            input.setFilters(new InputFilter[] {new InputFilter.LengthFilter(3)});
            input.setImeOptions(EditorInfo.IME_ACTION_DONE | EditorInfo.IME_FLAG_NO_EXTRACT_UI | EditorInfo.IME_FLAG_NO_FULLSCREEN);
            input.setSelectAllOnFocus(true);
            input.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
            input.setMinWidth((int) (56 * density));
            input.setContentDescription(context.getString(labelRes));
            header.addView(input, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, (int) (48 * density)));

            unit = new TextView(context);
            unit.setPadding((int) (6 * density), 0, 0, 0);
            unit.setMinWidth((int) (44 * density));
            header.addView(unit, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            view.addView(header);

            slider = new SeekBar(context);
            slider.setMax(max);
            slider.setContentDescription(context.getString(labelRes));
            view.addView(slider, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) (48 * density)));

            slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                    if (fromUser) {
                        set(progress, true);
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar bar) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar bar) {
                }
            });
            input.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    if (syncing || s.length() == 0) {
                        return;
                    }
                    try {
                        set(Math.min(typedMax, Integer.parseInt(s.toString())), false);
                    }
                    catch (NumberFormatException ignored) {
                        // The field only accepts digits, so this is an empty or overlong edit
                    }
                }
            });
            set(initial, true);
        }

        private void set(int newValue, boolean updateInput) {
            value = Math.max(0, Math.min(typedMax, newValue));
            syncing = true;
            slider.setProgress(Math.min(value, max));
            if (updateInput && !String.valueOf(value).equals(input.getText().toString())) {
                input.setText(String.valueOf(value));
            }
            Context context = view.getContext();
            // A time of 0 reads as Never, because that stage is then skipped
            unit.setText(seconds ? (value == 0 ? context.getString(R.string.quick_bar_auto_hide_off) : context.getString(R.string.quick_bar_idle_seconds)) : "%");
            syncing = false;
        }

        int get() {
            return value;
        }

        void reset(int newValue) {
            set(newValue, true);
        }
    }

    // Values the four fields hold, in the order of IdlePlan's constructor
    static IdlePlan planOf(Field dimAfter, Field dimLevel, Field hideAfter, Field tabLevel) {
        return new IdlePlan(dimAfter.get(), dimLevel.get(), hideAfter.get(), tabLevel.get());
    }

    public static void show(Context context, SharedPreferences prefs, Runnable onSaved) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        Context dialogContext = builder.getContext();
        float density = context.getResources().getDisplayMetrics().density;
        IdlePlan current = IdlePlan.read(prefs);

        LinearLayout body = new LinearLayout(dialogContext);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding((int) (24 * density), 0, (int) (24 * density), 0);

        Field dimAfter = new Field(dialogContext, R.string.quick_bar_idle_dim_after, true, current.dimSeconds);
        Field dimLevel = new Field(dialogContext, R.string.quick_bar_idle_dim_level, false, current.dimPercent);
        Field hideAfter = new Field(dialogContext, R.string.quick_bar_idle_hide_after, true, current.hideSeconds);
        Field tabLevel = new Field(dialogContext, R.string.quick_bar_idle_tab_level, false, current.tabPercent);
        body.addView(dimAfter.view);
        body.addView(dimLevel.view);
        body.addView(hideAfter.view);
        body.addView(tabLevel.view);

        TextView hint = new TextView(dialogContext);
        hint.setText(R.string.quick_bar_idle_dialog_hint);
        androidx.core.widget.TextViewCompat.setTextAppearance(hint, R.style.TextAppearance_SuperArtemis_BodySmall);
        hint.setPadding(0, (int) (12 * density), 0, (int) (8 * density));
        body.addView(hint);

        // The panel scrolls, so a phone in landscape reaches every field above the keyboard
        ScrollView scroll = new ScrollView(dialogContext);
        scroll.addView(body);
        scroll.setTag(Dialogs.NO_LIST_EDGES);

        builder.setCustomTitle(Dialogs.compactTitle(dialogContext, R.drawable.ic_sa_tune, context.getString(R.string.title_quick_bar_auto_hide)))
                .setView(scroll)
                .setPositiveButton(R.string.game_menu_editor_save, (dialog, which) -> {
                    planOf(dimAfter, dimLevel, hideAfter, tabLevel).save(prefs);
                    if (onSaved != null) {
                        onSaved.run();
                    }
                })
                .setNeutralButton(R.string.game_menu_editor_reset, null)
                .setNegativeButton(R.string.game_menu_cancel, null);
        AlertDialog dialog = Dialogs.show(builder, Dialogs.WIDTH_STANDARD_DP);
        // Reset fills the fields with the defaults and waits for Save, so it never closes the dialog
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener((View v) -> {
            dimAfter.reset(IdlePlan.DEFAULT_DIM_SECONDS);
            dimLevel.reset(IdlePlan.DEFAULT_DIM_PERCENT);
            hideAfter.reset(IdlePlan.DEFAULT_HIDE_SECONDS);
            tabLevel.reset(IdlePlan.DEFAULT_TAB_PERCENT);
        });
    }
}
