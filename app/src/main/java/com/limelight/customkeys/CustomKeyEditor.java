package com.limelight.customkeys;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.core.widget.TextViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.limelight.R;
import com.limelight.actions.StreamAction;
import com.limelight.menu.GameMenuLayout;
import com.limelight.ui.Dialogs;
import com.limelight.ui.FlowRow;

import java.util.ArrayList;
import java.util.List;

// NOTE: Builds a special key without JSON: a name, an icon and the steps it runs one after another.
// NOTE: Keys, Hold and Release steps are recorded on the built-in keyboard (or a physical one), Text, Wait and Repeat take a value,
// NOTE: and App action picks a stream action. A new key opens with the recorder ready, so a plain shortcut takes the same taps as before.
public final class CustomKeyEditor {
    public interface OnSaved {
        void onSaved(CustomKey key);
    }

    // More keys than this is never a shortcut, and hosts can drop very long chords
    static final int MAX_KEYS = 8;
    static final int MAX_STEPS = 30;

    private CustomKeyEditor() {
    }

    // Adds a key once, in tap order, and refuses keys that have no Windows equivalent or would make the chord too long
    static boolean addKey(List<Integer> keys, int vk) {
        if (vk <= 0 || keys.contains(vk) || keys.size() >= MAX_KEYS) {
            return false;
        }
        keys.add(vk);
        return true;
    }

    // How a step reads in the list and in the automatic name
    static String describe(Context context, KeyNames names, KeyStep step) {
        switch (step.type) {
            case TEXT:
                return context.getString(R.string.step_desc_text, step.text);
            case WAIT:
                return context.getString(R.string.step_desc_wait, step.amount);
            case HOLD:
                return context.getString(R.string.step_desc_hold, names.describe(step.keys));
            case RELEASE:
                return context.getString(R.string.step_desc_release, names.describe(step.keys));
            case REPEAT:
                return context.getString(R.string.step_desc_repeat, step.amount);
            case ACTION: {
                StreamAction action = com.limelight.actions.StreamActionRegistry.find(step.text);
                return context.getString(R.string.step_desc_action, action != null ? context.getString(action.nameRes) : step.text);
            }
            default:
                return names.describe(step.keys);
        }
    }

    static int iconFor(KeyStep.Type type) {
        switch (type) {
            case TEXT:
                return R.drawable.ic_sa_keyboard;
            case WAIT:
                return R.drawable.ic_ck_timer;
            case HOLD:
                return R.drawable.ic_ck_arrow_downward;
            case RELEASE:
                return R.drawable.ic_ck_arrow_upward;
            case REPEAT:
                return R.drawable.ic_ck_refresh;
            case ACTION:
                return R.drawable.ic_ck_bolt;
            default:
                return R.drawable.ic_sa_keyboard_command_key;
        }
    }

    // The automatic name: a shortcut keeps its key names, and a flow lists its first steps
    static String autoName(Context context, KeyNames names, List<KeyStep> steps) {
        if (steps.isEmpty()) {
            return "";
        }
        if (steps.size() == 1) {
            return describe(context, names, steps.get(0));
        }
        String first = describe(context, names, steps.get(0));
        String second = describe(context, names, steps.get(1));
        String name = first + ", " + second + (steps.size() > 2 ? ", ..." : "");
        return name.length() > 24 ? name.substring(0, 23) + "..." : name;
    }

    // Pass null to create a new key, which gets a fresh ID when saved
    @SuppressLint("ClickableViewAccessibility")
    public static void show(Context context, CustomKey existing, OnSaved onSaved) {
        final KeyNames names = new KeyNames(context);
        final List<KeyStep> steps = new ArrayList<>(existing != null ? existing.steps : new ArrayList<>());
        final String[] icon = {existing != null ? existing.icon : null};
        // The name follows the steps until the user types a name of their own
        final boolean[] nameEdited = {existing != null};
        final boolean[] settingName = {false};
        // Recorder state: the keys being recorded, which step they replace (-1 adds a new one) and what kind of step it is
        final List<Integer> recording = new ArrayList<>();
        final int[] recordingIndex = {-1};
        final KeyStep.Type[] recordingType = {null};

        View body = LayoutInflater.from(context).inflate(R.layout.dialog_custom_key_editor, null);
        EditText nameField = body.findViewById(R.id.custom_key_name);
        TextView combo = body.findViewById(R.id.custom_key_combo);
        TextView recorderTitle = body.findViewById(R.id.custom_key_recorder_title);
        View recorder = body.findViewById(R.id.custom_key_recorder);
        LinearLayout stepList = body.findViewById(R.id.custom_key_steps);
        TextView stepsEmpty = body.findViewById(R.id.custom_key_steps_empty);
        FlowRow addStep = body.findViewById(R.id.custom_key_add_step);
        CompoundButton sticky = body.findViewById(R.id.custom_key_sticky);
        RecyclerView iconGrid = body.findViewById(R.id.custom_key_icons);
        FrameLayout keyboardContainer = body.findViewById(R.id.custom_key_keyboard);

        if (existing != null) {
            nameField.setText(existing.name);
            sticky.setChecked(existing.sticky);
        }

        final AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle(existing != null ? R.string.custom_key_edit_title : R.string.custom_key_add_title)
                .setView(body)
                .setPositiveButton(R.string.game_menu_editor_save, (d, which) -> {
                    String id = existing != null ? existing.id : null;
                    String name = nameField.getText().toString().trim();
                    onSaved.onSaved(CustomKey.withSteps(id, name, sticky.isChecked(), icon[0], steps));
                })
                .setNegativeButton(R.string.game_menu_cancel, null)
                .create();

        final Runnable[] refresh = new Runnable[1];
        refresh[0] = () -> {
            stepList.removeAllViews();
            for (int i = 0; i < steps.size(); i++) {
                stepList.addView(stepRow(context, names, steps, i, refresh[0], index -> {
                    KeyStep step = steps.get(index);
                    if (step.type == KeyStep.Type.COMBO || step.type == KeyStep.Type.HOLD || step.type == KeyStep.Type.RELEASE) {
                        recording.clear();
                        recording.addAll(step.keys);
                        recordingIndex[0] = index;
                        recordingType[0] = step.type;
                        recorder.setVisibility(View.VISIBLE);
                        recorderTitle.setText(step.type == KeyStep.Type.COMBO
                                ? R.string.custom_key_record_title_combo : R.string.custom_key_record_title_single);
                        combo.setText(names.describe(recording));
                    }
                    else {
                        askValue(context, step.type, step, value -> {
                            steps.set(index, value);
                            refresh[0].run();
                        });
                    }
                }));
            }
            stepList.setVisibility(steps.isEmpty() ? View.GONE : View.VISIBLE);
            stepsEmpty.setVisibility(steps.isEmpty() ? View.VISIBLE : View.GONE);
            boolean single = steps.size() == 1 && steps.get(0).type == KeyStep.Type.COMBO;
            sticky.setEnabled(single);
            if (!single) {
                sticky.setChecked(false);
            }
            if (!nameEdited[0]) {
                settingName[0] = true;
                nameField.setText(autoName(context, names, steps));
                settingName[0] = false;
            }
            Button save = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            if (save != null) {
                save.setEnabled(!steps.isEmpty() && !TextUtils.isEmpty(nameField.getText().toString().trim()));
            }
        };

        nameField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!settingName[0]) {
                    nameEdited[0] = true;
                }
                Button save = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                if (save != null) {
                    save.setEnabled(!steps.isEmpty() && !TextUtils.isEmpty(s.toString().trim()));
                }
            }
        });

        // Opens the recorder for a new Keys, Hold or Release step
        final java.util.function.Consumer<KeyStep.Type> startRecording = type -> {
            recording.clear();
            recordingIndex[0] = -1;
            recordingType[0] = type;
            recorder.setVisibility(View.VISIBLE);
            recorderTitle.setText(type == KeyStep.Type.COMBO ? R.string.custom_key_record_title_combo : R.string.custom_key_record_title_single);
            combo.setText("");
            combo.requestFocus();
        };

        body.findViewById(R.id.custom_key_record_done).setOnClickListener(v -> {
            if (!recording.isEmpty()) {
                KeyStep step = recordingType[0] == KeyStep.Type.COMBO ? KeyStep.combo(recording)
                        : recordingType[0] == KeyStep.Type.HOLD ? KeyStep.hold(recording.get(0)) : KeyStep.release(recording.get(0));
                if (recordingIndex[0] >= 0) {
                    steps.set(recordingIndex[0], step);
                }
                else if (steps.size() < MAX_STEPS) {
                    steps.add(step);
                }
            }
            recorder.setVisibility(View.GONE);
            refresh[0].run();
        });
        body.findViewById(R.id.custom_key_record_cancel).setOnClickListener(v -> {
            recorder.setVisibility(View.GONE);
            refresh[0].run();
        });
        body.findViewById(R.id.custom_key_backspace).setOnClickListener(v -> {
            if (!recording.isEmpty()) {
                recording.remove(recording.size() - 1);
                combo.setText(names.describe(recording));
            }
        });

        // Hold and Release take exactly one key, so a new tap replaces the recorded one
        final java.util.function.IntConsumer record = vk -> {
            if (recordingType[0] != KeyStep.Type.COMBO && vk > 0) {
                recording.clear();
                recording.add(vk);
                combo.setText(names.describe(recording));
            }
            else if (addKey(recording, vk)) {
                combo.setText(names.describe(recording));
            }
        };

        // The built-in keyboard records on tap instead of sending input, and its hide key has no use here
        View keyboard = LayoutInflater.from(context).inflate(R.layout.layout_axixi_keyboard, keyboardContainer, false);
        wireKeyboard(keyboard, view -> record.accept(names.virtualKeyFor(KeyNames.keyCodeOf(view))));
        fitKeyLabels(keyboard);
        // Taller keys on a phone, where the keyboard is as wide as the screen and every key is narrow
        keyboard.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                (int) Dialogs.dp(context, Dialogs.isCompact(context) ? 280 : 224)));
        keyboardContainer.addView(keyboard);

        // One chip per step type
        KeyStep.Type[] types = KeyStep.Type.values();
        int[] labels = {R.string.step_type_combo, R.string.step_type_text, R.string.step_type_wait, R.string.step_type_hold,
                R.string.step_type_release, R.string.step_type_repeat, R.string.step_type_action};
        for (int i = 0; i < types.length; i++) {
            KeyStep.Type type = types[i];
            TextView chip = (TextView) LayoutInflater.from(context).inflate(R.layout.sa_chip, addStep, false);
            chip.setText(labels[i]);
            android.graphics.drawable.Drawable chipIcon = context.getDrawable(iconFor(type));
            if (chipIcon != null) {
                int size = (int) Dialogs.dp(context, 16);
                chipIcon.setBounds(0, 0, size, size);
                chip.setCompoundDrawablesRelative(chipIcon, null, null, null);
            }
            chip.setOnClickListener(v -> {
                if (steps.size() >= MAX_STEPS) {
                    return;
                }
                if (type == KeyStep.Type.COMBO || type == KeyStep.Type.HOLD || type == KeyStep.Type.RELEASE) {
                    startRecording.accept(type);
                }
                else {
                    askValue(context, type, null, value -> {
                        steps.add(value);
                        refresh[0].run();
                    });
                }
            });
            addStep.addView(chip);
        }

        IconPicker.attach(iconGrid, icon);

        // Physical keyboards record while the key box has focus, so typing the name still works
        dialog.setOnKeyListener((d, keyCode, event) -> {
            if (recorder.getVisibility() != View.VISIBLE || !combo.hasFocus() || keyCode == KeyEvent.KEYCODE_BACK) {
                return false;
            }
            if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
                record.accept(names.virtualKeyFor(keyCode));
            }
            return true;
        });

        dialog.show();
        // The recorder keyboard needs room, so the editor uses the wide dialog size
        Dialogs.fitWidth(dialog, Dialogs.WIDTH_WIDE_DP);
        // A new key starts with the recorder open, so a plain shortcut needs no extra tap
        if (steps.isEmpty()) {
            startRecording.accept(KeyStep.Type.COMBO);
        }
        refresh[0].run();
    }

    private interface IndexAction {
        void run(int index);
    }

    private static View stepRow(Context context, KeyNames names, List<KeyStep> steps, int index, Runnable refresh, IndexAction edit) {
        View row = LayoutInflater.from(context).inflate(R.layout.custom_key_step_row, null, false);
        KeyStep step = steps.get(index);
        ((TextView) row.findViewById(R.id.step_number)).setText(String.valueOf(index + 1));
        ((ImageView) row.findViewById(R.id.step_icon)).setImageResource(iconFor(step.type));
        ((TextView) row.findViewById(R.id.step_label)).setText(describe(context, names, step));
        row.setOnClickListener(v -> edit.run(index));
        View up = row.findViewById(R.id.step_up);
        View down = row.findViewById(R.id.step_down);
        up.setEnabled(index > 0);
        up.setAlpha(index > 0 ? 1f : 0.3f);
        down.setEnabled(index < steps.size() - 1);
        down.setAlpha(index < steps.size() - 1 ? 1f : 0.3f);
        up.setOnClickListener(v -> {
            java.util.Collections.swap(steps, index, index - 1);
            refresh.run();
        });
        down.setOnClickListener(v -> {
            java.util.Collections.swap(steps, index, index + 1);
            refresh.run();
        });
        row.findViewById(R.id.step_delete).setOnClickListener(v -> {
            steps.remove(index);
            refresh.run();
        });
        return row;
    }

    private interface StepValue {
        void accept(KeyStep step);
    }

    // Text, Wait and Repeat ask for one value, App action picks from the stream actions
    private static void askValue(Context context, KeyStep.Type type, KeyStep current, StepValue result) {
        if (type == KeyStep.Type.ACTION) {
            List<StreamAction> actions = GameMenuLayout.configurableActions();
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            Context dialogContext = builder.getContext();
            LinearLayout list = new LinearLayout(dialogContext);
            list.setOrientation(LinearLayout.VERTICAL);
            int side = (int) Dialogs.dp(dialogContext, 8);
            list.setPadding(side, 0, side, side);
            ScrollView scroll = new ScrollView(dialogContext);
            scroll.addView(list);
            builder.setCustomTitle(Dialogs.header(dialogContext, R.drawable.ic_ck_bolt, context.getString(R.string.step_action_title), null))
                    .setView(scroll)
                    .setNegativeButton(R.string.game_menu_cancel, null);
            AlertDialog[] dialog = new AlertDialog[1];
            for (StreamAction action : actions) {
                View row = LayoutInflater.from(dialogContext).inflate(R.layout.sa_choice_row, list, false);
                ((ImageView) row.findViewById(R.id.sa_choice_icon)).setImageResource(action.iconRes);
                ((TextView) row.findViewById(R.id.sa_choice_label)).setText(action.nameRes);
                row.findViewById(R.id.sa_choice_check).setVisibility(current != null && action.id.equals(current.text) ? View.VISIBLE : View.INVISIBLE);
                row.setOnClickListener(v -> {
                    result.accept(KeyStep.action(action.id));
                    if (dialog[0] != null) {
                        dialog[0].dismiss();
                    }
                });
                list.addView(row);
            }
            dialog[0] = Dialogs.show(builder, Dialogs.WIDTH_STANDARD_DP);
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        Context dialogContext = builder.getContext();
        EditText field = new EditText(dialogContext, null, 0, R.style.Widget_SuperArtemis_TextField);
        int titleRes;
        int iconRes = iconFor(type);
        if (type == KeyStep.Type.TEXT) {
            titleRes = R.string.step_type_text;
            field.setHint(R.string.step_text_hint);
            field.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
            field.setFilters(new android.text.InputFilter[] {new android.text.InputFilter.LengthFilter(KeyStep.TEXT_MAX)});
            if (current != null) {
                field.setText(current.text);
            }
        }
        else {
            titleRes = type == KeyStep.Type.WAIT ? R.string.step_type_wait : R.string.step_type_repeat;
            field.setHint(type == KeyStep.Type.WAIT ? R.string.step_wait_hint : R.string.step_repeat_hint);
            field.setInputType(InputType.TYPE_CLASS_NUMBER);
            field.setText(String.valueOf(current != null ? current.amount : (type == KeyStep.Type.WAIT ? 200 : 1)));
        }
        LinearLayout box = new LinearLayout(dialogContext);
        int side = (int) Dialogs.dp(dialogContext, 24);
        box.setPadding(side, 0, side, 0);
        box.addView(field, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        builder.setCustomTitle(Dialogs.header(dialogContext, iconRes, context.getString(titleRes), null))
                .setView(box)
                .setPositiveButton(R.string.custom_key_record_done, (d, w) -> {
                    String value = field.getText().toString();
                    if (type == KeyStep.Type.TEXT) {
                        if (!value.isEmpty()) {
                            result.accept(KeyStep.text(value));
                        }
                        return;
                    }
                    int number;
                    try {
                        number = Integer.parseInt(value.trim());
                    }
                    catch (NumberFormatException e) {
                        return;
                    }
                    result.accept(type == KeyStep.Type.WAIT ? KeyStep.waitMs(number) : KeyStep.repeat(number));
                })
                .setNegativeButton(R.string.game_menu_cancel, null);
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                    | android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }
        dialog.show();
        Dialogs.fitWidth(dialog, Dialogs.WIDTH_STANDARD_DP);
        field.requestFocus();
    }

    private interface KeyTap {
        void onTap(View key);
    }

    // NOTE: Key labels stay on one line and shrink to fit their key, so F10, Home or PgUp never wrap into two lines.
    // NOTE: This matters most with a large system font size, which grows the fixed 12sp labels past the width of a phone key.
    private static void fitKeyLabels(View view) {
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                fitKeyLabels(group.getChildAt(i));
            }
            return;
        }
        if (view instanceof TextView && !(view instanceof android.widget.EditText)) {
            TextView label = (TextView) view;
            label.setMaxLines(1);
            label.setIncludeFontPadding(false);
            TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(label, 7, 14, 1, android.util.TypedValue.COMPLEX_UNIT_SP);
        }
    }

    private static void wireKeyboard(View view, KeyTap tap) {
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                wireKeyboard(group.getChildAt(i), tap);
            }
            return;
        }
        if ("hide".equals(view.getTag())) {
            view.setVisibility(View.INVISIBLE);
            return;
        }
        if (KeyNames.keyCodeOf(view) > 0) {
            view.setClickable(true);
            view.setOnClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                tap.onTap(v);
            });
        }
    }
}
