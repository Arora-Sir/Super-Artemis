package com.limelight.customkeys;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.text.Editable;
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
import android.widget.TextView;

import androidx.core.widget.TextViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.limelight.R;
import com.limelight.ui.Dialogs;

import java.util.ArrayList;
import java.util.List;

// NOTE: Records a custom key without JSON: the user taps keys on the built-in keyboard (or presses them on a physical keyboard),
// NOTE: names the key, picks an icon and saves. Keys are pressed in the recorded order and released in reverse when sent.
public final class CustomKeyEditor {
    public interface OnSaved {
        void onSaved(CustomKey key);
    }

    // More keys than this is never a shortcut, and hosts can drop very long chords
    static final int MAX_KEYS = 8;

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

    // Pass null to create a new key, which gets a fresh ID when saved
    @SuppressLint("ClickableViewAccessibility")
    public static void show(Context context, CustomKey existing, OnSaved onSaved) {
        final KeyNames names = new KeyNames(context);
        final List<Integer> keys = new ArrayList<>(existing != null ? existing.keys : new ArrayList<>());
        final String[] icon = {existing != null ? existing.icon : null};
        // The name follows the recorded keys until the user types a name of their own
        final boolean[] nameEdited = {existing != null};
        final boolean[] settingName = {false};

        View body = LayoutInflater.from(context).inflate(R.layout.dialog_custom_key_editor, null);
        EditText nameField = body.findViewById(R.id.custom_key_name);
        TextView combo = body.findViewById(R.id.custom_key_combo);
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
                    onSaved.onSaved(new CustomKey(id, name, sticky.isChecked(), icon[0], keys));
                })
                .setNegativeButton(R.string.game_menu_cancel, null)
                .create();

        final Runnable refresh = () -> {
            combo.setText(names.describe(keys));
            if (!nameEdited[0]) {
                settingName[0] = true;
                nameField.setText(names.describe(keys));
                settingName[0] = false;
            }
            Button save = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            if (save != null) {
                save.setEnabled(!keys.isEmpty() && !TextUtils.isEmpty(nameField.getText().toString().trim()));
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
                    save.setEnabled(!keys.isEmpty() && !TextUtils.isEmpty(s.toString().trim()));
                }
            }
        });

        body.findViewById(R.id.custom_key_backspace).setOnClickListener(v -> {
            if (!keys.isEmpty()) {
                keys.remove(keys.size() - 1);
                refresh.run();
            }
        });

        // The built-in keyboard records on tap instead of sending input, and its hide key has no use here
        View keyboard = LayoutInflater.from(context).inflate(R.layout.layout_axixi_keyboard, keyboardContainer, false);
        wireKeyboard(keyboard, view -> {
            if (addKey(keys, names.virtualKeyFor(KeyNames.keyCodeOf(view)))) {
                refresh.run();
            }
        });
        fitKeyLabels(keyboard);
        // Taller keys on a phone, where the keyboard is as wide as the screen and every key is narrow
        keyboard.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                (int) Dialogs.dp(context, Dialogs.isCompact(context) ? 280 : 224)));
        keyboardContainer.addView(keyboard);

        IconPicker.attach(iconGrid, icon);

        // Physical keyboards record while the key box has focus, so typing the name still works
        dialog.setOnKeyListener((d, keyCode, event) -> {
            if (!combo.hasFocus() || keyCode == KeyEvent.KEYCODE_BACK) {
                return false;
            }
            if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
                if (addKey(keys, names.virtualKeyFor(keyCode))) {
                    refresh.run();
                }
            }
            return true;
        });

        dialog.show();
        // The recorder keyboard needs room, so the editor uses the wide dialog size
        Dialogs.fitWidth(dialog, Dialogs.WIDTH_WIDE_DP);
        refresh.run();
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
