package com.limelight.menu;

import android.content.SharedPreferences;
import android.text.TextUtils;

import com.limelight.actions.StreamAction;
import com.limelight.actions.StreamActionRegistry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// NOTE: Stores the on-screen quick-action bars: each screen corner has its own items, in order, and its own direction (a row or a column).
// NOTE: Items are stream action IDs or "key:<special key ID>". Session actions are never allowed, so one stray tap cannot end a stream.
public final class QuickBarLayout {
    public static final String POSITION_TOP_LEFT = "top_left";
    public static final String POSITION_TOP_RIGHT = "top_right";
    public static final String POSITION_BOTTOM_LEFT = "bottom_left";
    public static final String POSITION_BOTTOM_RIGHT = "bottom_right";
    public static final List<String> POSITIONS = Collections.unmodifiableList(Arrays.asList(
            POSITION_TOP_LEFT, POSITION_TOP_RIGHT, POSITION_BOTTOM_LEFT, POSITION_BOTTOM_RIGHT));

    // A row runs along the top or bottom edge, a column runs down or up the side, always starting in the corner
    public static final String DIRECTION_ROW = "row";
    public static final String DIRECTION_COLUMN = "column";

    static final String KEY_PREFIX = "key:";

    // Settings from the single-bar version, read only to migrate them
    static final String LEGACY_ITEMS = "quick_bar_items";
    static final String LEGACY_POSITION = "list_quick_bar_position";
    // Artemis' separate pan/zoom overlay button, now a pin on the top right bar
    static final String LEGACY_ZOOM_BUTTON = "checkbox_show_overlay_zoom_toggle_button";

    private QuickBarLayout() {
    }

    static String itemsKey(String corner) {
        return "quick_bar_items_" + corner;
    }

    static String directionKey(String corner) {
        return "quick_bar_direction_" + corner;
    }

    public static String customKeyItem(String customKeyId) {
        return KEY_PREFIX + customKeyId;
    }

    // Returns the special key ID for a "key:" item, or null for an action item
    public static String customKeyId(String item) {
        return item.startsWith(KEY_PREFIX) ? item.substring(KEY_PREFIX.length()) : null;
    }

    // NOTE: The single bar moves to the corner it used, and the old pan/zoom overlay button becomes a pin on the top right bar.
    // NOTE: Both run once, then the old settings are removed.
    static void migrate(SharedPreferences prefs) {
        if (!prefs.contains(LEGACY_ITEMS) && !prefs.contains(LEGACY_POSITION) && !prefs.contains(LEGACY_ZOOM_BUTTON)) {
            return;
        }
        Map<String, List<String>> corners = new LinkedHashMap<>();
        for (String corner : POSITIONS) {
            corners.put(corner, parse(prefs.getString(itemsKey(corner), "")));
        }
        List<String> legacyItems = parse(prefs.getString(LEGACY_ITEMS, ""));
        if (!legacyItems.isEmpty()) {
            String corner = prefs.getString(LEGACY_POSITION, POSITION_TOP_LEFT);
            if (!POSITIONS.contains(corner)) {
                corner = POSITION_TOP_LEFT;
            }
            if (corners.get(corner).isEmpty()) {
                corners.put(corner, legacyItems);
            }
        }
        if (prefs.getBoolean(LEGACY_ZOOM_BUTTON, false)) {
            boolean pinned = false;
            for (List<String> items : corners.values()) {
                pinned |= items.contains(StreamActionRegistry.ID_ZOOM);
            }
            if (!pinned) {
                corners.get(POSITION_TOP_RIGHT).add(StreamActionRegistry.ID_ZOOM);
            }
        }
        SharedPreferences.Editor editor = prefs.edit();
        for (Map.Entry<String, List<String>> corner : corners.entrySet()) {
            editor.putString(itemsKey(corner.getKey()), TextUtils.join(",", corner.getValue()));
        }
        editor.remove(LEGACY_ITEMS).remove(LEGACY_POSITION).remove(LEGACY_ZOOM_BUTTON).apply();
    }

    // Empty by default, a corner only shows a bar once the user pins something there
    public static List<String> getItems(SharedPreferences prefs, String corner) {
        migrate(prefs);
        return parse(prefs.getString(itemsKey(corner), ""));
    }

    public static void setItems(SharedPreferences prefs, String corner, List<String> items) {
        prefs.edit().putString(itemsKey(corner), TextUtils.join(",", parse(TextUtils.join(",", items)))).apply();
    }

    public static String getDirection(SharedPreferences prefs, String corner) {
        return DIRECTION_COLUMN.equals(prefs.getString(directionKey(corner), DIRECTION_ROW)) ? DIRECTION_COLUMN : DIRECTION_ROW;
    }

    public static void setDirection(SharedPreferences prefs, String corner, String direction) {
        prefs.edit().putString(directionKey(corner), DIRECTION_COLUMN.equals(direction) ? DIRECTION_COLUMN : DIRECTION_ROW).apply();
    }

    public static boolean isTop(String corner) {
        return POSITION_TOP_LEFT.equals(corner) || POSITION_TOP_RIGHT.equals(corner);
    }

    public static boolean isLeft(String corner) {
        return POSITION_TOP_LEFT.equals(corner) || POSITION_BOTTOM_LEFT.equals(corner);
    }

    // Unknown and session actions and duplicates are dropped. Special key IDs are checked when the bar is drawn, since keys can change anytime.
    static List<String> parse(String raw) {
        Set<String> items = new LinkedHashSet<>();
        for (String part : raw.split(",")) {
            String item = part.trim();
            if (item.isEmpty()) {
                continue;
            }
            String keyId = customKeyId(item);
            if (keyId != null) {
                if (!keyId.isEmpty()) {
                    items.add(item);
                }
                continue;
            }
            StreamAction action = StreamActionRegistry.find(item);
            if (action != null && !action.destructive) {
                items.add(item);
            }
        }
        return new ArrayList<>(items);
    }
}
