package com.limelight.menu;

import android.content.SharedPreferences;
import android.text.TextUtils;

import com.limelight.actions.StreamAction;
import com.limelight.actions.StreamActionRegistry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

// NOTE: Stores which actions the user pinned to the Quick Menu, in what order, and the menu style.
// NOTE: Disconnect and Quit are not configurable, the Quick Menu always shows them in its Session section, so nobody can lose them.
public final class GameMenuLayout {
    public static final String PREF_QUICK_ITEMS = "game_menu_quick_items";
    public static final String PREF_STYLE = "list_game_menu_style";

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
        Set<String> ids = new LinkedHashSet<>();
        for (String part : raw.split(",")) {
            String id = part.trim();
            if (isConfigurable(id)) {
                ids.add(id);
            }
        }
        return new ArrayList<>(ids);
    }
}
