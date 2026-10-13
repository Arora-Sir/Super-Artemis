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
        // Everyday Windows shortcuts listed under More keys, after the keys Artemis always had
        public final boolean more;

        Def(String name, int nameRes, String icon, Integer... keys) {
            this(false, name, nameRes, icon, keys);
        }

        Def(boolean more, String name, int nameRes, String icon, Integer... keys) {
            this.id = PREFIX + name;
            this.nameRes = nameRes;
            this.icon = icon;
            this.keys = Collections.unmodifiableList(Arrays.asList(keys));
            this.more = more;
        }
    }

    private static Def more(String name, int nameRes, String icon, Integer... keys) {
        return new Def(true, name, nameRes, icon, keys);
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
                    KeyboardTranslator.VK_LWIN, KeyboardTranslator.VK_LMENU, KeyboardTranslator.VK_B),
            // More keys
            more("ctrl_a", R.string.more_key_ctrl_a, "select_all", KeyboardTranslator.VK_LCONTROL, 0x41),
            more("ctrl_c", R.string.more_key_ctrl_c, "content_copy", KeyboardTranslator.VK_LCONTROL, 0x43),
            more("ctrl_x", R.string.more_key_ctrl_x, "content_cut", KeyboardTranslator.VK_LCONTROL, 0x58),
            more("ctrl_z", R.string.more_key_ctrl_z, "undo", KeyboardTranslator.VK_LCONTROL, 0x5A),
            more("ctrl_y", R.string.more_key_ctrl_y, "redo", KeyboardTranslator.VK_LCONTROL, 0x59),
            more("ctrl_s", R.string.more_key_ctrl_s, "save", KeyboardTranslator.VK_LCONTROL, 0x53),
            more("ctrl_f", R.string.more_key_ctrl_f, "search", KeyboardTranslator.VK_LCONTROL, 0x46),
            more("win_v", R.string.more_key_win_v, "content_paste_search", KeyboardTranslator.VK_LWIN, 0x56),
            more("win_shift_s", R.string.more_key_win_shift_s, "screenshot_region", KeyboardTranslator.VK_LWIN, KeyboardTranslator.VK_LSHIFT, 0x53),
            more("alt_tab", R.string.more_key_alt_tab, "flip_to_front", KeyboardTranslator.VK_LMENU, KeyboardTranslator.VK_TAB),
            more("ctrl_shift_esc", R.string.more_key_ctrl_shift_esc, "monitoring", KeyboardTranslator.VK_LCONTROL, KeyboardTranslator.VK_LSHIFT, KeyboardTranslator.VK_ESCAPE),
            more("ctrl_w", R.string.more_key_ctrl_w, "tab_close", KeyboardTranslator.VK_LCONTROL, 0x57),
            more("ctrl_t", R.string.more_key_ctrl_t, "tab", KeyboardTranslator.VK_LCONTROL, 0x54),
            more("ctrl_shift_t", R.string.more_key_ctrl_shift_t, "history", KeyboardTranslator.VK_LCONTROL, KeyboardTranslator.VK_LSHIFT, 0x54),
            more("win_e", R.string.more_key_win_e, "folder_open", KeyboardTranslator.VK_LWIN, 0x45),
            more("win_l", R.string.more_key_win_l, "lock", KeyboardTranslator.VK_LWIN, 0x4C),
            more("win_r", R.string.more_key_win_r, "terminal", KeyboardTranslator.VK_LWIN, 0x52),
            more("home", R.string.more_key_home, "first_page", 0x24),
            more("end", R.string.more_key_end, "last_page", 0x23),
            more("page_up", R.string.more_key_page_up, "keyboard_double_arrow_up", 0x21),
            more("page_down", R.string.more_key_page_down, "keyboard_double_arrow_down", 0x22),
            more("delete", R.string.more_key_delete, "backspace", 0x2E),
            more("tab", R.string.more_key_tab, "keyboard_tab", KeyboardTranslator.VK_TAB),
            more("enter", R.string.more_key_enter, "keyboard_return", KeyboardTranslator.VK_RETURN)
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
