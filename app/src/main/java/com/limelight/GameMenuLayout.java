package com.limelight;

import android.content.SharedPreferences;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

// NOTE: Catalog of in-stream menu actions with stable IDs, so the Quick Menu can be reordered and filtered by the user.
// NOTE: Actions left out of the Quick Menu are still listed under Advanced, so nothing can become unreachable.
public final class GameMenuLayout {
    public static final String PREF_QUICK_ITEMS = "game_menu_quick_items";
    public static final String PREF_STYLE = "list_game_menu_style";

    public static final String STYLE_LIST = "list";
    public static final String STYLE_GRID = "grid";
    public static final String STYLE_COMPACT = "compact";

    public static final String ID_DISCONNECT = "disconnect";
    public static final String ID_QUIT = "quit";
    public static final String ID_UPLOAD_CLIPBOARD = "upload_clipboard";
    public static final String ID_FETCH_CLIPBOARD = "fetch_clipboard";
    public static final String ID_SERVER_CMD = "server_cmd";
    public static final String ID_TOGGLE_KEYBOARD = "toggle_keyboard";
    public static final String ID_ZOOM = "zoom";
    public static final String ID_ROTATE_SCREEN = "rotate_screen";
    public static final String ID_MICROPHONE = "microphone";
    public static final String ID_MOUSE_MODE = "mouse_mode";
    public static final String ID_TOGGLE_HUD = "toggle_hud";
    public static final String ID_TOGGLE_FLOATING_BUTTON = "toggle_floating_button";
    public static final String ID_TOGGLE_KEYBOARD_MODEL = "toggle_keyboard_model";
    public static final String ID_TOGGLE_VIRTUAL_CONTROLLER = "toggle_virtual_controller";
    public static final String ID_TOGGLE_FULL_KEYBOARD = "toggle_full_keyboard";
    public static final String ID_TASK_MANAGER = "task_manager";
    public static final String ID_SEND_KEYS = "send_keys";
    public static final String ID_TOUCH_SENSITIVITY = "touch_sensitivity";

    public static final class Action {
        public final String id;
        public final int editorLabelRes;
        public final int iconRes;

        Action(String id, int editorLabelRes, int iconRes) {
            this.id = id;
            this.editorLabelRes = editorLabelRes;
            this.iconRes = iconRes;
        }
    }

    // Every configurable action, in the order the Advanced menu and the editor list them
    public static final List<Action> ACTIONS = Collections.unmodifiableList(Arrays.asList(
            new Action(ID_DISCONNECT, R.string.game_menu_disconnect, R.drawable.ic_gm_disconnect),
            new Action(ID_QUIT, R.string.game_menu_quit_session, R.drawable.ic_gm_quit),
            new Action(ID_UPLOAD_CLIPBOARD, R.string.game_menu_upload_clipboard, R.drawable.ic_gm_upload_clipboard),
            new Action(ID_FETCH_CLIPBOARD, R.string.game_menu_fetch_clipboard, R.drawable.ic_gm_fetch_clipboard),
            new Action(ID_SERVER_CMD, R.string.game_menu_server_cmd, R.drawable.ic_gm_server_cmd),
            new Action(ID_TOGGLE_KEYBOARD, R.string.game_menu_toggle_keyboard, R.drawable.ic_gm_toggle_keyboard),
            new Action(ID_ZOOM, R.string.game_menu_item_zoom, R.drawable.ic_gm_zoom),
            new Action(ID_ROTATE_SCREEN, R.string.game_menu_rotate_screen, R.drawable.ic_gm_rotate_screen),
            new Action(ID_MICROPHONE, R.string.game_menu_item_microphone, R.drawable.ic_gm_microphone),
            new Action(ID_MOUSE_MODE, R.string.game_menu_select_mouse_mode, R.drawable.ic_gm_mouse_mode),
            new Action(ID_TOGGLE_HUD, R.string.game_menu_toggle_hud, R.drawable.ic_gm_toggle_hud),
            new Action(ID_TOGGLE_FLOATING_BUTTON, R.string.game_menu_toggle_floating_button, R.drawable.ic_gm_toggle_floating_button),
            new Action(ID_TOGGLE_KEYBOARD_MODEL, R.string.game_menu_toggle_keyboard_model, R.drawable.ic_gm_toggle_keyboard_model),
            new Action(ID_TOGGLE_VIRTUAL_CONTROLLER, R.string.game_menu_toggle_virtual_model, R.drawable.ic_gm_toggle_virtual_controller),
            new Action(ID_TOGGLE_FULL_KEYBOARD, R.string.game_menu_toggle_virtual_keyboard_model, R.drawable.ic_gm_toggle_full_keyboard),
            new Action(ID_TASK_MANAGER, R.string.game_menu_task_manager, R.drawable.ic_gm_task_manager),
            new Action(ID_SEND_KEYS, R.string.game_menu_send_keys, R.drawable.ic_gm_send_keys),
            new Action(ID_TOUCH_SENSITIVITY, R.string.game_menu_switch_touch_sensitivity_model, R.drawable.ic_gm_touch_sensitivity)
    ));

    // Upstream Artemis Quick Menu order, plus the microphone toggle
    public static final List<String> DEFAULT_QUICK_ITEMS = Collections.unmodifiableList(Arrays.asList(
            ID_DISCONNECT, ID_QUIT, ID_UPLOAD_CLIPBOARD, ID_FETCH_CLIPBOARD, ID_SERVER_CMD,
            ID_TOGGLE_KEYBOARD, ID_ZOOM, ID_ROTATE_SCREEN, ID_MICROPHONE
    ));

    private GameMenuLayout() {
    }

    public static Action find(String id) {
        for (Action action : ACTIONS) {
            if (action.id.equals(id)) {
                return action;
            }
        }
        return null;
    }

    public static List<String> getQuickItems(SharedPreferences prefs) {
        String raw = prefs.getString(PREF_QUICK_ITEMS, null);
        if (raw == null) {
            return new ArrayList<>(DEFAULT_QUICK_ITEMS);
        }
        return parse(raw);
    }

    public static void setQuickItems(SharedPreferences prefs, List<String> ids) {
        prefs.edit().putString(PREF_QUICK_ITEMS, TextUtils.join(",", parse(TextUtils.join(",", ids)))).apply();
    }

    public static void reset(SharedPreferences prefs) {
        prefs.edit().remove(PREF_QUICK_ITEMS).apply();
    }

    public static String getStyle(SharedPreferences prefs) {
        String style = prefs.getString(PREF_STYLE, STYLE_LIST);
        if (STYLE_GRID.equals(style) || STYLE_COMPACT.equals(style)) {
            return style;
        }
        return STYLE_LIST;
    }

    // Unknown IDs (from older or newer builds) and duplicates are dropped, order is preserved
    static List<String> parse(String raw) {
        Set<String> ids = new LinkedHashSet<>();
        for (String part : raw.split(",")) {
            String id = part.trim();
            if (find(id) != null) {
                ids.add(id);
            }
        }
        return new ArrayList<>(ids);
    }
}
