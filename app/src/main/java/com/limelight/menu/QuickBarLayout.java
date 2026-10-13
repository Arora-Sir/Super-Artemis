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

// NOTE: Stores what the on-screen quick-action bar shows, in what order, and which screen corner it sits in.
// NOTE: Items are stream action IDs or "key:<custom key ID>". Session actions are never allowed, so one stray tap cannot end a stream.
public final class QuickBarLayout {
    public static final String PREF_ITEMS = "quick_bar_items";
    public static final String PREF_POSITION = "list_quick_bar_position";

    public static final String POSITION_TOP_LEFT = "top_left";
    public static final String POSITION_TOP_RIGHT = "top_right";
    public static final String POSITION_BOTTOM_LEFT = "bottom_left";
    public static final String POSITION_BOTTOM_RIGHT = "bottom_right";
    static final List<String> POSITIONS = Collections.unmodifiableList(Arrays.asList(
            POSITION_TOP_LEFT, POSITION_TOP_RIGHT, POSITION_BOTTOM_LEFT, POSITION_BOTTOM_RIGHT));

    static final String KEY_PREFIX = "key:";

    private QuickBarLayout() {
    }

    public static String customKeyItem(String customKeyId) {
        return KEY_PREFIX + customKeyId;
    }

    // Returns the custom key ID for a "key:" item, or null for an action item
    public static String customKeyId(String item) {
        return item.startsWith(KEY_PREFIX) ? item.substring(KEY_PREFIX.length()) : null;
    }

    // Empty by default, the bar only appears once the user pins something
    public static List<String> getItems(SharedPreferences prefs) {
        return parse(prefs.getString(PREF_ITEMS, ""));
    }

    public static void setItems(SharedPreferences prefs, List<String> items) {
        prefs.edit().putString(PREF_ITEMS, TextUtils.join(",", parse(TextUtils.join(",", items)))).apply();
    }

    public static String getPosition(SharedPreferences prefs) {
        String position = prefs.getString(PREF_POSITION, POSITION_TOP_LEFT);
        return POSITIONS.contains(position) ? position : POSITION_TOP_LEFT;
    }

    public static void setPosition(SharedPreferences prefs, String position) {
        prefs.edit().putString(PREF_POSITION, POSITIONS.contains(position) ? position : POSITION_TOP_LEFT).apply();
    }

    // Unknown and session actions and duplicates are dropped. Custom key IDs are checked when the bar is drawn, since keys can change anytime.
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
