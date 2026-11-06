package com.limelight.menu;

import android.content.SharedPreferences;
import android.text.TextUtils;

import com.limelight.R;
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
    // NOTE: Custom keys listed in the Advanced Menu. A key is left out until its eye is switched on, because a user can have dozens of keys.
    public static final String PREF_ADVANCED_KEYS = "game_menu_advanced_keys";

    // NOTE: The order of the Quick Menu sections, three IDs joined by commas. Unknown IDs are dropped and missing ones go last in the default order.
    public static final String PREF_SECTION_ORDER = "game_menu_section_order";
    public static final String SECTION_ACTIONS = "actions";
    public static final String SECTION_SESSION = "session";
    public static final String SECTION_MENU = "menu";
    public static final List<String> DEFAULT_SECTION_ORDER = Collections.unmodifiableList(Arrays.asList(SECTION_ACTIONS, SECTION_SESSION, SECTION_MENU));

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

    // NOTE: Actions the menu editor, the Quick Menu actions and the Advanced Menu list.
    // NOTE: Settings is left out because the Quick Menu always shows it in its Menu section, so it would appear twice. Bars, gestures and flows still offer it.
    public static List<StreamAction> menuActions() {
        List<StreamAction> actions = configurableActions();
        for (int i = actions.size() - 1; i >= 0; i--) {
            if (isMenuNavigation(actions.get(i).id)) {
                actions.remove(i);
            }
        }
        return actions;
    }

    public static boolean isMenuNavigation(String id) {
        return StreamActionRegistry.ID_SETTINGS.equals(id);
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
        prefs.edit().remove(PREF_QUICK_ITEMS).remove(PREF_HIDDEN_ITEMS).remove(PREF_ADVANCED_KEYS).remove(PREF_SECTION_ORDER).apply();
    }

    public static List<String> getSectionOrder(SharedPreferences prefs) {
        return parseSectionOrder(prefs.getString(PREF_SECTION_ORDER, null));
    }

    public static void setSectionOrder(SharedPreferences prefs, List<String> order) {
        prefs.edit().putString(PREF_SECTION_ORDER, TextUtils.join(",", parseSectionOrder(TextUtils.join(",", order)))).apply();
    }

    static List<String> parseSectionOrder(String raw) {
        Set<String> order = new LinkedHashSet<>();
        if (raw != null) {
            for (String part : raw.split(",")) {
                String id = part.trim();
                if (DEFAULT_SECTION_ORDER.contains(id)) {
                    order.add(id);
                }
            }
        }
        order.addAll(DEFAULT_SECTION_ORDER);
        return new ArrayList<>(order);
    }

    public static int sectionTitleRes(String section) {
        if (SECTION_SESSION.equals(section)) {
            return R.string.game_menu_section_session;
        }
        if (SECTION_MENU.equals(section)) {
            return R.string.game_menu_section_menu;
        }
        return R.string.game_menu_section_actions;
    }

    public static Set<String> getHiddenItems(SharedPreferences prefs) {
        return new LinkedHashSet<>(parse(prefs.getString(PREF_HIDDEN_ITEMS, ""), false));
    }

    public static void setHiddenItems(SharedPreferences prefs, Collection<String> ids) {
        prefs.edit().putString(PREF_HIDDEN_ITEMS, TextUtils.join(",", parse(TextUtils.join(",", ids), false))).apply();
    }

    // The stored items ("key:<id>") of the custom keys the Advanced Menu lists, in saved order
    public static List<String> getAdvancedKeys(SharedPreferences prefs) {
        List<String> keys = new ArrayList<>();
        for (String id : parse(prefs.getString(PREF_ADVANCED_KEYS, ""))) {
            if (keyId(id) != null) {
                keys.add(id);
            }
        }
        return keys;
    }

    public static void setAdvancedKeys(SharedPreferences prefs, Collection<String> items) {
        List<String> keys = new ArrayList<>();
        for (String id : parse(TextUtils.join(",", items))) {
            if (keyId(id) != null) {
                keys.add(id);
            }
        }
        prefs.edit().putString(PREF_ADVANCED_KEYS, TextUtils.join(",", keys)).apply();
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
