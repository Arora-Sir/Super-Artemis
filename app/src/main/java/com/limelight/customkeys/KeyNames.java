package com.limelight.customkeys;

import android.content.Context;
import android.text.TextUtils;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.limelight.R;
import com.limelight.binding.input.KeyboardTranslator;
import com.limelight.preferences.PreferenceConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

// NOTE: Turns Android key codes into the Windows virtual key codes custom keys store, and virtual key codes back into readable names.
// NOTE: Names come from the built-in keyboard's own key labels, so the recorder shows exactly what the user tapped.
public final class KeyNames {
    private final KeyboardTranslator translator;
    private final SparseArray<String> names = new SparseArray<>();

    public KeyNames(Context context) {
        translator = new KeyboardTranslator(PreferenceConfiguration.readPreferences(context));
        View keyboard = LayoutInflater.from(context).inflate(R.layout.layout_axixi_keyboard, null);
        collect(keyboard);
    }

    private void collect(View view) {
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                collect(group.getChildAt(i));
            }
            return;
        }
        int keyCode = keyCodeOf(view);
        int vk = virtualKeyFor(keyCode);
        if (vk == 0 || names.get(vk) != null) {
            return;
        }
        CharSequence text = view instanceof TextView ? ((TextView) view).getText() : null;
        names.put(vk, !TextUtils.isEmpty(text) ? text.toString() : KeyEvent.keyCodeToString(keyCode).replace("KEYCODE_", ""));
    }

    // Keys on the built-in keyboard carry their Android key code as the view tag, other views return 0
    public static int keyCodeOf(View view) {
        Object tag = view.getTag();
        if (tag instanceof String) {
            try {
                return Integer.parseInt((String) tag);
            }
            catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }

    // Windows virtual key codes of F13 to F24, which no Android key code maps to (upstream ClassicOldSong/moonlight-android#574)
    public static final int VK_F13 = 0x7C;
    public static final int VK_F24 = 0x87;

    // 0 means the key has no Windows equivalent and cannot be recorded
    public int virtualKeyFor(int androidKeyCode) {
        if (androidKeyCode <= 0) {
            return 0;
        }
        return translator.translate(androidKeyCode, 0, -1) & 0xFF;
    }

    public String label(int vk) {
        if (vk >= VK_F13 && vk <= VK_F24) {
            return "F" + (13 + vk - VK_F13);
        }
        String name = names.get(vk);
        return name != null ? name : String.format(Locale.US, "0x%02X", vk);
    }

    public String describe(List<Integer> keys) {
        List<String> parts = new ArrayList<>();
        for (int vk : keys) {
            parts.add(label(vk));
        }
        return TextUtils.join(" + ", parts);
    }
}
