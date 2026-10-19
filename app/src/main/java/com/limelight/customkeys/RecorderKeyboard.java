package com.limelight.customkeys;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.RippleDrawable;
import android.content.res.ColorStateList;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.limelight.R;
import com.limelight.ui.Dialogs;

import java.util.function.IntConsumer;

// NOTE: Key recorder keyboard for a phone in portrait (layout D3): Function keys with an F1 to F12 and F13 to F24 switch, Typing laid out like a phone keyboard, Symbols and Navigation.
// NOTE: Every row shares one width unit and every label one size, and modifier keys get a tinted cap, so the keyboard reads evenly at phone width.
// NOTE: Keys carry Android key codes that the recorder turns into Windows key codes, except F13 to F24, which carry their Windows codes directly because Android has none.
// NOTE: Only the recorder uses this. The keyboard shown during a stream keeps its own full PC layout.
public final class RecorderKeyboard {
    static final int TYPING_KEY_DP = 44;
    static final int SMALL_KEY_DP = 40;
    static final int LABEL_SP = 15;
    static final int MOD_LABEL_SP = 14;

    // A key of the layout: its label, Android key code (or a Windows code for F13 to F24), relative width and whether it is a modifier style key
    static final class Key {
        final String label;
        final int code;
        final boolean windowsCode;
        final float weight;
        final boolean mod;

        Key(String label, int code, boolean windowsCode, float weight, boolean mod) {
            this.label = label;
            this.code = code;
            this.windowsCode = windowsCode;
            this.weight = weight;
            this.mod = mod;
        }
    }

    private static Key key(String label, int code) {
        return new Key(label, code, false, 1f, false);
    }

    private static Key mod(String label, int code, float weight) {
        return new Key(label, code, false, weight, true);
    }

    private static Key gap(float weight) {
        return new Key(null, 0, false, weight, false);
    }

    static Key[][] functionRows(boolean high) {
        Key[][] rows = new Key[2][6];
        for (int i = 0; i < 12; i++) {
            int n = (high ? 13 : 1) + i;
            rows[i / 6][i % 6] = high ? new Key("F" + n, KeyNames.VK_F13 + i, true, 1f, false) : key("F" + n, KeyEvent.KEYCODE_F1 + i);
        }
        return rows;
    }

    static Key[][] typingRows() {
        Key[] digits = new Key[10];
        for (int i = 0; i < 10; i++) {
            int digit = (i + 1) % 10;
            digits[i] = key(Integer.toString(digit), KeyEvent.KEYCODE_0 + digit);
        }
        return new Key[][] {
                digits,
                letters("QWERTYUIOP", 0f),
                letters("ASDFGHJKL", 0.5f),
                concat(new Key[] {mod("Shift", KeyEvent.KEYCODE_SHIFT_LEFT, 1.5f)}, letters("ZXCVBNM", 0f), new Key[] {mod("Back", KeyEvent.KEYCODE_DEL, 1.5f)}),
                {mod("Ctrl", KeyEvent.KEYCODE_CTRL_LEFT, 1.2f), mod("Win", KeyEvent.KEYCODE_META_LEFT, 1.2f), mod("Alt", KeyEvent.KEYCODE_ALT_LEFT, 1.2f),
                        mod("Space", KeyEvent.KEYCODE_SPACE, 3f), mod("Tab", KeyEvent.KEYCODE_TAB, 1.2f), mod("Enter", KeyEvent.KEYCODE_ENTER, 1.6f)}
        };
    }

    static Key[][] symbolRows() {
        return new Key[][] {{
                key("`", KeyEvent.KEYCODE_GRAVE), key("-", KeyEvent.KEYCODE_MINUS), key("=", KeyEvent.KEYCODE_EQUALS),
                key("[", KeyEvent.KEYCODE_LEFT_BRACKET), key("]", KeyEvent.KEYCODE_RIGHT_BRACKET), key("\\", KeyEvent.KEYCODE_BACKSLASH),
                key(";", KeyEvent.KEYCODE_SEMICOLON), key("'", KeyEvent.KEYCODE_APOSTROPHE), key(",", KeyEvent.KEYCODE_COMMA),
                key(".", KeyEvent.KEYCODE_PERIOD), key("/", KeyEvent.KEYCODE_SLASH)}};
    }

    static Key[][] navigationRows() {
        return new Key[][] {
                {mod("Esc", KeyEvent.KEYCODE_ESCAPE, 1f), mod("PrtSc", KeyEvent.KEYCODE_SYSRQ, 1f), mod("Ins", KeyEvent.KEYCODE_INSERT, 1f),
                        gap(0.3f), gap(1f), key("↑", KeyEvent.KEYCODE_DPAD_UP), gap(1f)},
                {mod("Home", KeyEvent.KEYCODE_MOVE_HOME, 1f), mod("End", KeyEvent.KEYCODE_MOVE_END, 1f), mod("Del", KeyEvent.KEYCODE_FORWARD_DEL, 1f),
                        gap(0.3f), key("←", KeyEvent.KEYCODE_DPAD_LEFT), key("↓", KeyEvent.KEYCODE_DPAD_DOWN), key("→", KeyEvent.KEYCODE_DPAD_RIGHT)},
                {mod("PgUp", KeyEvent.KEYCODE_PAGE_UP, 1f), mod("PgDn", KeyEvent.KEYCODE_PAGE_DOWN, 1f), mod("Caps", KeyEvent.KEYCODE_CAPS_LOCK, 1f),
                        gap(0.3f), gap(3f)}
        };
    }

    private static Key[] letters(String chars, float sidePadding) {
        int count = chars.length() + (sidePadding > 0 ? 2 : 0);
        Key[] row = new Key[count];
        int i = 0;
        if (sidePadding > 0) {
            row[i++] = gap(sidePadding);
        }
        for (char c : chars.toCharArray()) {
            row[i++] = key(String.valueOf(c), KeyEvent.KEYCODE_A + (c - 'A'));
        }
        if (sidePadding > 0) {
            row[i] = gap(sidePadding);
        }
        return row;
    }

    private static Key[] concat(Key[]... parts) {
        int size = 0;
        for (Key[] part : parts) {
            size += part.length;
        }
        Key[] all = new Key[size];
        int i = 0;
        for (Key[] part : parts) {
            for (Key k : part) {
                all[i++] = k;
            }
        }
        return all;
    }

    // Builds the keyboard. record gets a Windows key code for every tapped key.
    public static View build(Context context, KeyNames names, IntConsumer record) {
        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);

        root.addView(heading(context, R.string.recorder_section_function));
        LinearLayout switchRow = new LinearLayout(context);
        switchRow.setOrientation(LinearLayout.HORIZONTAL);
        TextView low = chip(context, "F1 - F12");
        TextView high = chip(context, "F13 - F24");
        switchRow.addView(low, chipParams(context, true));
        switchRow.addView(high, chipParams(context, false));
        root.addView(switchRow);
        LinearLayout functionBlock = block(context);
        root.addView(functionBlock);
        TextView hint = new TextView(context);
        hint.setText(R.string.recorder_high_function_hint);
        androidx.core.widget.TextViewCompat.setTextAppearance(hint, R.style.TextAppearance_SuperArtemis_BodySmall);
        int pad = (int) Dialogs.dp(context, 4);
        hint.setPadding(pad, 0, pad, pad);
        root.addView(hint);
        Runnable[] showRows = new Runnable[2];
        showRows[0] = () -> {
            fill(context, functionBlock, functionRows(false), SMALL_KEY_DP, names, record);
            low.setSelected(true);
            high.setSelected(false);
            hint.setVisibility(View.GONE);
        };
        showRows[1] = () -> {
            fill(context, functionBlock, functionRows(true), SMALL_KEY_DP, names, record);
            low.setSelected(false);
            high.setSelected(true);
            hint.setVisibility(View.VISIBLE);
        };
        low.setOnClickListener(v -> showRows[0].run());
        high.setOnClickListener(v -> showRows[1].run());
        showRows[0].run();

        root.addView(heading(context, R.string.recorder_section_typing));
        LinearLayout typing = block(context);
        fill(context, typing, typingRows(), TYPING_KEY_DP, names, record);
        root.addView(typing);

        root.addView(heading(context, R.string.recorder_section_symbols));
        LinearLayout symbols = block(context);
        fill(context, symbols, symbolRows(), SMALL_KEY_DP, names, record);
        root.addView(symbols);

        root.addView(heading(context, R.string.recorder_section_navigation));
        LinearLayout navigation = block(context);
        fill(context, navigation, navigationRows(), SMALL_KEY_DP, names, record);
        root.addView(navigation);
        return root;
    }

    private static TextView heading(Context context, int textRes) {
        TextView heading = new TextView(context);
        heading.setText(textRes);
        androidx.core.widget.TextViewCompat.setTextAppearance(heading, R.style.TextAppearance_SuperArtemis_SectionLabel);
        int pad = (int) Dialogs.dp(context, 4);
        heading.setPadding(pad, (int) Dialogs.dp(context, 10), pad, pad);
        return heading;
    }

    private static TextView chip(Context context, String text) {
        TextView chip = new TextView(context, null, 0, R.style.SuperArtemis_Segment);
        chip.setText(text);
        return chip;
    }

    private static LinearLayout.LayoutParams chipParams(Context context, boolean first) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        if (first) {
            params.rightMargin = (int) Dialogs.dp(context, 8);
        }
        params.bottomMargin = (int) Dialogs.dp(context, 6);
        return params;
    }

    private static LinearLayout block(Context context) {
        LinearLayout block = new LinearLayout(context);
        block.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) Dialogs.dp(context, 3);
        block.setPadding(pad, pad, pad, pad);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFF212121);
        bg.setCornerRadius(Dialogs.dp(context, 12));
        block.setBackground(bg);
        return block;
    }

    private static void fill(Context context, LinearLayout block, Key[][] rows, int keyDp, KeyNames names, IntConsumer record) {
        block.removeAllViews();
        int height = (int) Dialogs.dp(context, keyDp);
        for (Key[] row : rows) {
            LinearLayout line = new LinearLayout(context);
            line.setOrientation(LinearLayout.HORIZONTAL);
            for (Key k : row) {
                View view = k.label == null ? new View(context) : keyView(context, k, names, record);
                line.addView(view, new LinearLayout.LayoutParams(0, height, k.weight));
            }
            block.addView(line, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height));
        }
    }

    private static TextView keyView(Context context, Key k, KeyNames names, IntConsumer record) {
        TextView view = new TextView(context);
        view.setText(k.label);
        view.setGravity(Gravity.CENTER);
        view.setMaxLines(1);
        view.setIncludeFontPadding(false);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, k.mod ? MOD_LABEL_SP : LABEL_SP);
        view.setTextColor(k.mod ? 0xFFAEB4C6 : 0xFFE8E8E8);
        view.setBackground(keyBackground(context, k.mod));
        view.setContentDescription(k.label);
        view.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
            record.accept(windowsCode(k, names));
        });
        return view;
    }

    static int windowsCode(Key k, KeyNames names) {
        return k.windowsCode ? k.code : names.virtualKeyFor(k.code);
    }

    // Rounded key cap with a 2dp gap on every side, like the built-in keyboard, and a rounded press ripple
    private static android.graphics.drawable.Drawable keyBackground(Context context, boolean mod) {
        float radius = Dialogs.dp(context, 6);
        GradientDrawable cap = new GradientDrawable();
        cap.setColor(mod ? 0xFF303440 : 0xFF383838);
        cap.setCornerRadius(radius);
        GradientDrawable mask = new GradientDrawable();
        mask.setColor(0xFFFFFFFF);
        mask.setCornerRadius(radius);
        int inset = (int) Dialogs.dp(context, 2);
        RippleDrawable ripple = new RippleDrawable(ColorStateList.valueOf(0x33FFFFFF), cap, mask);
        return new InsetDrawable(ripple, inset);
    }
}
