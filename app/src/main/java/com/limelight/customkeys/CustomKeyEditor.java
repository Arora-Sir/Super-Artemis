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

    // Same limit as the name field (maxLength in dialog_custom_key_editor.xml)
    static final int NAME_MAX = 24;

    // NOTE: The automatic name is the first step, plus the number of further steps for a flow, such as "Ctrl + C (+5)".
    // NOTE: It always fits the name field, so a long first step is shortened with "..." before the count instead of being cut mid-word by the field.
    static String autoName(Context context, KeyNames names, List<KeyStep> steps) {
        if (steps.isEmpty()) {
            return "";
        }
        String first = describe(context, names, steps.get(0));
        String suffix = steps.size() > 1 ? " (+" + (steps.size() - 1) + ")" : "";
        int room = NAME_MAX - suffix.length();
        if (first.length() > room) {
            first = first.substring(0, Math.max(1, room - 3)).trim() + "...";
        }
        return first + suffix;
    }

    // Subtitle of a flow in lists: the number of steps and the first two of them
    public static String summary(Context context, KeyNames names, CustomKey key) {
        StringBuilder steps = new StringBuilder();
        for (int i = 0; i < Math.min(2, key.steps.size()); i++) {
            if (i > 0) {
                steps.append(", ");
            }
            steps.append(describe(context, names, key.steps.get(i)));
        }
        if (key.steps.size() > 2) {
            steps.append(", ...");
        }
        return context.getResources().getQuantityString(R.plurals.special_key_flow_summary, key.steps.size(), key.steps.size(), steps.toString());
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
        final Hotkey[] hotkey = {existing != null ? existing.hotkey : null};
        CompoundButton hotkeyToPc = body.findViewById(R.id.custom_key_hotkey_to_pc);
        hotkeyToPc.setChecked(hotkey[0] != null && hotkey[0].toPc);
        bindHotkeyRow(context, body, names, existing != null ? existing.id : null, hotkey);

        if (existing != null) {
            nameField.setText(existing.name);
            sticky.setChecked(existing.sticky);
        }

        AlertDialog.Builder editorBuilder = new AlertDialog.Builder(context);
        final AlertDialog dialog = editorBuilder
                .setCustomTitle(Dialogs.compactTitle(editorBuilder.getContext(), R.drawable.ic_sa_keyboard,
                        context.getString(existing != null ? R.string.custom_key_edit_title : R.string.custom_key_add_title)))
                .setView(body)
                .setPositiveButton(R.string.game_menu_editor_save, (d, which) -> {
                    String id = existing != null ? existing.id : null;
                    String name = nameField.getText().toString().trim();
                    Hotkey chosen = hotkey[0] != null ? hotkey[0].withToPc(hotkeyToPc.isChecked()) : null;
                    onSaved.onSaved(CustomKey.withSteps(id, name, sticky.isChecked(), icon[0], steps).withHotkey(chosen));
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
                        updateSave(dialog, steps, nameField.getText(), recorder, recording);
                        revealRecorder(body, recorder);
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
            updateSave(dialog, steps, nameField.getText(), recorder, recording);
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
                updateSave(dialog, steps, s, recorder, recording);
            }
        });

        // Opens the recorder for a new Keys, Hold or Release step
        final java.util.function.Consumer<KeyStep.Type> startRecording = type -> {
            recording.clear();
            recordingIndex[0] = -1;
            recordingType[0] = type;
            recorder.setVisibility(View.VISIBLE);
            updateSave(dialog, steps, nameField.getText(), recorder, recording);
            revealRecorder(body, recorder);
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
            updateSave(dialog, steps, nameField.getText(), recorder, recording);
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
            updateSave(dialog, steps, nameField.getText(), recorder, recording);
        };

        // NOTE: A phone in portrait gets the recorder layout with sections and large keys, since the full PC keyboard is too narrow there.
        // NOTE: Landscape and tablets keep the built-in PC keyboard, which records on tap instead of sending input, and its hide key has no use here.
        if (Dialogs.isCompact(context)) {
            keyboardContainer.addView(RecorderKeyboard.build(context, names, record),
                    new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }
        else {
            addPcKeyboard(context, keyboardContainer, names, record);
        }

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
                        // An empty recorder left open from a new key closes, since this step replaces it
                        if (recording.isEmpty()) {
                            recorder.setVisibility(View.GONE);
                        }
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

    // NOTE: The hotkey row shows the current hotkey, Set listens for one combo from a connected keyboard and Clear removes it.
    // NOTE: A hotkey another key already uses is refused, and a shortcut the PC uses all the time is kept with a warning under the row.
    private static void bindHotkeyRow(Context context, View body, KeyNames names, String ownId, Hotkey[] hotkey) {
        TextView value = body.findViewById(R.id.custom_key_hotkey_value);
        TextView note = body.findViewById(R.id.custom_key_hotkey_note);
        android.widget.Button clear = body.findViewById(R.id.custom_key_hotkey_clear);
        Dialogs.fadeWhenDisabled(clear);
        View toPc = body.findViewById(R.id.custom_key_hotkey_to_pc);
        Runnable show = () -> {
            value.setText(hotkey[0] != null ? names.describe(hotkey[0].virtualKeys()) : context.getString(R.string.custom_key_hotkey_none));
            clear.setEnabled(hotkey[0] != null);
            toPc.setEnabled(hotkey[0] != null);
            note.setText(hotkey[0] != null && hotkey[0].isCommonPcShortcut() ? R.string.custom_key_hotkey_common : R.string.custom_key_hotkey_hint);
        };
        show.run();
        clear.setOnClickListener(v -> {
            hotkey[0] = null;
            show.run();
        });
        body.findViewById(R.id.custom_key_hotkey_set).setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            AlertDialog listen = builder
                    .setCustomTitle(Dialogs.header(builder.getContext(), R.drawable.ic_sa_keyboard, context.getString(R.string.custom_key_hotkey_press_title),
                            context.getString(R.string.custom_key_hotkey_press_message)))
                    .setNegativeButton(R.string.game_menu_cancel, null)
                    .create();
            listen.setOnKeyListener((d, keyCode, event) -> {
                if (keyCode == KeyEvent.KEYCODE_BACK || event.getAction() != KeyEvent.ACTION_DOWN || event.getRepeatCount() > 0) {
                    return keyCode != KeyEvent.KEYCODE_BACK;
                }
                int vk = names.virtualKeyFor(keyCode);
                if (vk == 0 || Hotkey.modifierBit(vk) != 0) {
                    return true;
                }
                Hotkey picked = new Hotkey(Hotkey.modifiersOf(event.getMetaState()), vk, false);
                String takenBy = HotkeyDispatcher.conflict(CustomKeyStore.loadOrEmpty(context), ownId, picked);
                if (takenBy != null) {
                    CustomKey other = CustomKeyStore.find(context, takenBy);
                    android.widget.Toast.makeText(context, context.getString(R.string.custom_key_hotkey_taken, other != null ? other.name : takenBy),
                            android.widget.Toast.LENGTH_LONG).show();
                    return true;
                }
                hotkey[0] = picked;
                show.run();
                listen.dismiss();
                return true;
            });
            listen.show();
            Dialogs.fitWidth(listen, Dialogs.WIDTH_STANDARD_DP);
        });
    }

    // NOTE: Save stays off while the recorder holds keys not yet confirmed with Done, so they are never lost or saved half way.
    // NOTE: It also needs at least one step and a name. Dialogs fade a disabled button, so the state is visible.
    static boolean canSave(int stepCount, CharSequence name, boolean pendingKeys) {
        return stepCount > 0 && !TextUtils.isEmpty(name == null ? null : name.toString().trim()) && !pendingKeys;
    }

    private static void updateSave(AlertDialog dialog, List<KeyStep> steps, CharSequence name, View recorder, List<Integer> recording) {
        Button save = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        if (save != null) {
            save.setEnabled(canSave(steps.size(), name, recorder.getVisibility() == View.VISIBLE && !recording.isEmpty()));
        }
    }

    // A key with several steps pushes the recorder below the visible part of the editor, so opening it scrolls just far enough to show it, and not at all when it is already visible
    private static void revealRecorder(View body, View recorder) {
        recorder.post(() -> recorder.requestRectangleOnScreen(new android.graphics.Rect(0, 0, recorder.getWidth(), recorder.getHeight()), false));
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
            com.limelight.ui.ChoiceList choices = new com.limelight.ui.ChoiceList(new AlertDialog.Builder(context), R.drawable.ic_ck_bolt,
                    context.getString(R.string.step_action_title), R.string.game_menu_cancel);
            for (StreamAction action : actions) {
                choices.add(action.iconRes, context.getString(action.nameRes), current != null && action.id.equals(current.text),
                        () -> result.accept(KeyStep.action(action.id)));
            }
            choices.show();
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
        Dialogs.openKeyboard(field);
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

    private static void addPcKeyboard(Context context, FrameLayout keyboardContainer, KeyNames names, java.util.function.IntConsumer record) {
        View keyboard = LayoutInflater.from(context).inflate(R.layout.layout_axixi_keyboard, keyboardContainer, false);
        wireKeyboard(keyboard, view -> record.accept(names.virtualKeyFor(KeyNames.keyCodeOf(view))));
        addHighFunctionKeys(context, (ViewGroup) keyboard, record);
        fitKeyLabels(keyboard);
        keyboard.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                if (right - left != oldRight - oldLeft) {
                    v.post(() -> unifyKeyLabels(v));
                }
            }
        });
        keyboard.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, recorderKeyboardHeight(context)));
        keyboardContainer.addView(keyboard);
    }

    // NOTE: Taller keys on a phone in portrait, where the keyboard is as wide as the screen and every key is narrow. One more row holds F13 to F24.
    // NOTE: A phone in landscape is wide but short, so the keyboard takes the height left under the dialog title, the key box and the buttons, and never less than 184dp.
    static int recorderKeyboardHeight(Context context) {
        android.util.DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        float heightDp = metrics.heightPixels / metrics.density;
        float keyboardDp;
        if (Dialogs.isCompact(context)) {
            keyboardDp = 320;
        }
        else if (heightDp < 480) {
            keyboardDp = Math.max(184, Math.min(256, heightDp - 200));
        }
        else {
            keyboardDp = 256;
        }
        return (int) Dialogs.dp(context, keyboardDp);
    }

    // NOTE: Each label shrinks on its own to fit its key, which left F1 large, F10 small and PrtSc tiny side by side on a phone.
    // NOTE: After the first layout every short label (up to three letters, such as F10, ESC or Tab) takes the smallest of their fitted sizes, so the keyboard reads as one size.
    // NOTE: Longer labels such as Home or Shift keep shrinking to fit but never grow past that size.
    static void unifyKeyLabels(View keyboard) {
        List<TextView> labels = new ArrayList<>();
        collectKeyLabels(keyboard, labels);
        float uniform = Float.MAX_VALUE;
        for (TextView label : labels) {
            if (label.getText().length() <= 3 && label.getWidth() > 0) {
                uniform = Math.min(uniform, label.getTextSize());
            }
        }
        if (uniform == Float.MAX_VALUE) {
            return;
        }
        int uniformSp = Math.max(7, (int) (uniform / keyboard.getResources().getDisplayMetrics().scaledDensity));
        for (TextView label : labels) {
            if (label.getText().length() <= 3) {
                TextViewCompat.setAutoSizeTextTypeWithDefaults(label, TextViewCompat.AUTO_SIZE_TEXT_TYPE_NONE);
                label.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, uniformSp);
            }
            else {
                TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(label, Math.min(7, uniformSp), uniformSp, 1, android.util.TypedValue.COMPLEX_UNIT_SP);
            }
        }
    }

    private static void collectKeyLabels(View view, List<TextView> labels) {
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                collectKeyLabels(group.getChildAt(i), labels);
            }
        }
        else if (view instanceof TextView && !(view instanceof android.widget.EditText) && view.getVisibility() == View.VISIBLE
                && ((TextView) view).getText().length() > 0) {
            labels.add((TextView) view);
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

    // NOTE: F13 to F24 have no Android key code, so the built-in keyboard cannot offer them. The recorder adds them as one extra row.
    // NOTE: Each key records its Windows virtual key code directly, and the PC receives it like any other key (upstream #574).
    static void addHighFunctionKeys(Context context, ViewGroup keyboard, java.util.function.IntConsumer record) {
        if (keyboard.getChildCount() == 0 || !(keyboard.getChildAt(0) instanceof LinearLayout)) {
            return;
        }
        LinearLayout template = (LinearLayout) keyboard.getChildAt(0);
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setLayoutParams(new LinearLayout.LayoutParams((LinearLayout.LayoutParams) template.getLayoutParams()));
        View sample = template.getChildAt(1);
        for (int vk = KeyNames.VK_F13; vk <= KeyNames.VK_F24; vk++) {
            TextView key = new TextView(context);
            key.setText("F" + (13 + vk - KeyNames.VK_F13));
            key.setGravity(android.view.Gravity.CENTER);
            key.setBackground(sample.getBackground().getConstantState().newDrawable().mutate());
            if (sample instanceof TextView) {
                key.setTextColor(((TextView) sample).getTextColors());
            }
            // The key background already draws a 2dp inset on every side, so these keys get no margin, like the built-in rows
            key.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));
            final int code = vk;
            key.setOnClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                record.accept(code);
            });
            row.addView(key);
        }
        keyboard.addView(row, 1);
    }
}
