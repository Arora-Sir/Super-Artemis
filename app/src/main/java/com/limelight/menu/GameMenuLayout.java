package com.limelight.menu;

import android.content.SharedPreferences;
import android.text.TextUtils;

import com.limelight.actions.StreamAction;
import com.limelight.actions.StreamActionRegistry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

// NOTE: Stores which actions and special keys the user pinned to the Quick Menu, in what order, and the menu style.
// NOTE: A pinned special key is stored as "key:<special key ID>", the same item format the quick-action bars use.
// NOTE: Disconnect and Quit are not configurable, the Quick Menu always shows them in its Session section, so nobody can lose them.
public final class GameMenuLayout {
    public static final String PREF_QUICK_ITEMS = "game_menu_quick_items";
    public static final String PREF_STYLE = "list_game_menu_style";
    // Actions left out of the Advanced Menu. Gestures and quick-action bars can still run them.
    public static final String PREF_HIDDEN_ITEMS = "game_menu_hidden_items";

    public static final String STYLE_LIST = "list";
    public static final String STYLE_GRID = "grid";
    public static final String STYLE_COMPACT = "compact";

    // Upstream Artemis Quick Menu order without the session actions, plus the microphone toggle
    public static final List<String> DEFAULT_QUICK_ITEMS = Collections.unmodifiableList(Arrays.asList(
            StreamActionRegistry.ID_UPLOAD_CLIPBOARD, StreamActionRegistry.ID_FETCH_CLIPBOARD, StreamActionRegistry.ID_SERVER_CMD,
            StreamActionRegistry.ID_TOGGLE_KEYBOARD, StreamActionRegistry.ID_ZOOM, StreamActionRegistry.ID_ROTATE_SCREEN,
            StreamActionRegistry.ID_MICROPHONE
    ));

    private GameMenuLayout() {
    }

    // Every action the user can pin or unpin, in catalog order
    public static List<StreamAction> configurableActions() {
        List<StreamAction> actions = new ArrayList<>();
        for (StreamAction action : StreamActionRegistry.ALL) {
            if (!action.destructive) {
                actions.add(action);
            }
        }
        return actions;
    }

    public static boolean isConfigurable(String id) {
        StreamAction action = StreamActionRegistry.find(id);
        return action != null && !action.destructive;
    }

    public static String keyItem(String keyId) {
        return QuickBarLayout.customKeyItem(keyId);
    }

    // The special key ID of a pinned key, or null for an action
    public static String keyId(String item) {
        return QuickBarLayout.customKeyId(item);
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
        prefs.edit().remove(PREF_QUICK_ITEMS).remove(PREF_HIDDEN_ITEMS).apply();
    }

    public static Set<String> getHiddenItems(SharedPreferences prefs) {
        return new LinkedHashSet<>(parse(prefs.getString(PREF_HIDDEN_ITEMS, ""), false));
    }

    public static void setHiddenItems(SharedPreferences prefs, Collection<String> ids) {
        prefs.edit().putString(PREF_HIDDEN_ITEMS, TextUtils.join(",", parse(TextUtils.join(",", ids), false))).apply();
    }

    public static String getStyle(SharedPreferences prefs) {
        return normalizeStyle(prefs.getString(PREF_STYLE, STYLE_LIST));
    }

    public static void setStyle(SharedPreferences prefs, String style) {
        prefs.edit().putString(PREF_STYLE, normalizeStyle(style)).apply();
    }

    static String normalizeStyle(String style) {
        if (STYLE_GRID.equals(style) || STYLE_COMPACT.equals(style)) {
            return style;
        }
        return STYLE_LIST;
    }

    // NOTE: Unknown IDs (from older or newer builds), duplicates and session actions are dropped, order is preserved.
    // NOTE: Layouts saved before 1.0.0 listed Disconnect and Quit, which now live in the Session section instead.
    static List<String> parse(String raw) {
        return parse(raw, true);
    }

    // Hidden items are actions only, because special keys never appear in the Advanced Menu
    private static List<String> parse(String raw, boolean allowKeys) {
        Set<String> ids = new LinkedHashSet<>();
        for (String part : raw.split(",")) {
            String id = part.trim();
            String keyId = allowKeys ? keyId(id) : null;
            // A key deleted later simply does not show, so its ID is kept like the bars keep theirs
            if (isConfigurable(id) || (keyId != null && !keyId.isEmpty())) {
                ids.add(id);
            }
        }
        return new ArrayList<>(ids);
    }
}
