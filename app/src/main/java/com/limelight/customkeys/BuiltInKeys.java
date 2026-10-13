package com.limelight.customkeys;

import com.limelight.R;
import com.limelight.binding.input.KeyboardTranslator;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

// NOTE: The special keys every install starts with (from Artemis), now with stable IDs and default icons.
// NOTE: The "builtin_" ID prefix lets the quick-action bars and gestures pin them exactly like the user's own keys.
public final class BuiltInKeys {
    public static final String PREFIX = "builtin_";

    public static final class Def {
        public final String id;
        public final int nameRes;
        public final String icon;
        // Windows virtual key codes, pressed in order and released in reverse
        public final List<Integer> keys;

        Def(String name, int nameRes, String icon, Integer... keys) {
            this.id = PREFIX + name;
            this.nameRes = nameRes;
            this.icon = icon;
            this.keys = Collections.unmodifiableList(Arrays.asList(keys));
        }
    }

    public static final List<Def> ALL = Collections.unmodifiableList(Arrays.asList(
            new Def("esc", R.string.game_menu_send_keys_esc, "close", KeyboardTranslator.VK_ESCAPE),
            new Def("f11", R.string.game_menu_send_keys_f11, "fullscreen", KeyboardTranslator.VK_F11),
            new Def("alt_f4", R.string.game_menu_send_keys_alt_f4, "power_settings_new",
                    KeyboardTranslator.VK_LMENU, KeyboardTranslator.VK_F4),
            new Def("alt_enter", R.string.game_menu_send_keys_alt_enter, "open_in_full",
                    KeyboardTranslator.VK_LMENU, KeyboardTranslator.VK_RETURN),
            new Def("ctrl_v", R.string.game_menu_send_keys_ctrl_v, "content_paste",
                    KeyboardTranslator.VK_LCONTROL, KeyboardTranslator.VK_V),
            new Def("win", R.string.game_menu_send_keys_win, "window", KeyboardTranslator.VK_LWIN),
            new Def("win_d", R.string.game_menu_send_keys_win_d, "desktop_windows",
                    KeyboardTranslator.VK_LWIN, KeyboardTranslator.VK_D),
            new Def("win_g", R.string.game_menu_send_keys_win_g, "sports_esports",
                    KeyboardTranslator.VK_LWIN, KeyboardTranslator.VK_G),
            new Def("ctrl_alt_tab", R.string.game_menu_send_keys_ctrl_alt_tab, "swap_horiz",
                    KeyboardTranslator.VK_LCONTROL, KeyboardTranslator.VK_LMENU, KeyboardTranslator.VK_TAB),
            new Def("shift_tab", R.string.game_menu_send_keys_shift_tab, "videogame_asset",
                    KeyboardTranslator.VK_LSHIFT, KeyboardTranslator.VK_TAB),
            new Def("win_shift_left", R.string.game_menu_send_keys_win_shift_left, "keyboard_double_arrow_left",
                    KeyboardTranslator.VK_LWIN, KeyboardTranslator.VK_LSHIFT, KeyboardTranslator.VK_LEFT),
            new Def("ctrl_alt_shift_f1", R.string.game_menu_send_keys_ctrl_alt_shift_f1, "monitor",
                    KeyboardTranslator.VK_LCONTROL, KeyboardTranslator.VK_LMENU, KeyboardTranslator.VK_LSHIFT, KeyboardTranslator.VK_F1),
            new Def("ctrl_alt_shift_f12", R.string.game_menu_send_keys_ctrl_alt_shift_f12, "screenshot_monitor",
                    KeyboardTranslator.VK_LCONTROL, KeyboardTranslator.VK_LMENU, KeyboardTranslator.VK_LSHIFT, KeyboardTranslator.VK_F12),
            // The label says Alt + B, and it has always sent Win + Alt + B (the Windows HDR toggle)
            new Def("win_alt_b", R.string.game_menu_send_keys_alt_b, "hdr_on",
                    KeyboardTranslator.VK_LWIN, KeyboardTranslator.VK_LMENU, KeyboardTranslator.VK_B)
    ));

    private BuiltInKeys() {
    }

    public static boolean isBuiltIn(String id) {
        return id != null && id.startsWith(PREFIX);
    }

    public static Def find(String id) {
        for (Def def : ALL) {
            if (def.id.equals(id)) {
                return def;
            }
        }
        return null;
    }
}
