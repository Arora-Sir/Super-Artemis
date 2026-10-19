package com.limelight.customkeys;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import androidx.preference.PreferenceManager;

import com.limelight.GameMenu;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

// NOTE: One list of special keys for Send special keys, the quick-action bars and gestures: the built-in keys and the user's own keys.
// NOTE: Built-in keys can be hidden, get another icon and be reordered. The user's keys stay in the custom keys JSON that import, export and the on-screen layer read.
// NOTE: Hidden keys, icons and order live in specialPrefs next to that JSON, so backup and restore carry them along.
public final class SpecialKeys {
    static final String PREF_HIDDEN = "builtin_hidden";
    static final String PREF_ICONS = "builtin_icons";
    static final String PREF_ORDER = "special_key_order";
    // Setting from before built-in keys could be hidden one by one, read only to migrate it
    static final String LEGACY_HIDE_DEFAULTS = "checkbox_enable_clear_default_special_button";

    private SpecialKeys() {
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(GameMenu.PREF_NAME, Context.MODE_PRIVATE);
    }

    // The old "Disable default special keys" switch becomes "every built-in key hidden", once
    public static void migrate(Context context) {
        SharedPreferences defaults = PreferenceManager.getDefaultSharedPreferences(context);
        if (!defaults.getBoolean(LEGACY_HIDE_DEFAULTS, false)) {
            return;
        }
        Set<String> hidden = new HashSet<>();
        for (BuiltInKeys.Def def : BuiltInKeys.ALL) {
            // The switch covered the keys Artemis had, so More keys stay visible
            if (!def.more) {
                hidden.add(def.id);
            }
        }
        prefs(context).edit().putString(PREF_HIDDEN, TextUtils.join(",", hidden)).apply();
        defaults.edit().remove(LEGACY_HIDE_DEFAULTS).apply();
    }

    // Built-in and custom keys that are shown, in the user's order. Without a saved order the user's keys come right after the Artemis keys and More keys come last.
    public static List<CustomKey> visible(Context context) {
        migrate(context);
        List<CustomKey> keys = new ArrayList<>();
        List<CustomKey> more = new ArrayList<>();
        Set<String> hidden = hiddenIds(context);
        Map<String, String> icons = iconOverrides(context);
        for (BuiltInKeys.Def def : BuiltInKeys.ALL) {
            if (!hidden.contains(def.id)) {
                (def.more ? more : keys).add(builtIn(context, def, icons));
            }
        }
        keys.addAll(CustomKeyStore.loadOrEmpty(context));
        keys.addAll(more);
        return order(keys, savedOrder(context));
    }

    // Keys of the More keys group, which Send special keys lists on their own page
    public static boolean isMoreKey(String id) {
        BuiltInKeys.Def def = BuiltInKeys.isBuiltIn(id) ? BuiltInKeys.find(id) : null;
        return def != null && def.more;
    }

    // Built-in keys the user hid, so the manager can offer to show them again
    public static List<CustomKey> hiddenBuiltIns(Context context) {
        migrate(context);
        List<CustomKey> keys = new ArrayList<>();
        Set<String> hidden = hiddenIds(context);
        Map<String, String> icons = iconOverrides(context);
        for (BuiltInKeys.Def def : BuiltInKeys.ALL) {
            if (hidden.contains(def.id)) {
                keys.add(builtIn(context, def, icons));
            }
        }
        return keys;
    }

    // Hidden built-in keys count as gone, so a pinned button or gesture for one does nothing
    public static CustomKey find(Context context, String id) {
        if (BuiltInKeys.isBuiltIn(id)) {
            BuiltInKeys.Def def = BuiltInKeys.find(id);
            if (def == null || hiddenIds(context).contains(id)) {
                return null;
            }
            return builtIn(context, def, iconOverrides(context));
        }
        return CustomKeyStore.find(context, id);
    }

    public static void setHidden(Context context, String id, boolean hide) {
        Set<String> hidden = hiddenIds(context);
        if (hide) {
            hidden.add(id);
        }
        else {
            hidden.remove(id);
        }
        prefs(context).edit().putString(PREF_HIDDEN, TextUtils.join(",", hidden)).apply();
    }

    // A null icon goes back to the key's default icon
    public static void setBuiltInIcon(Context context, String id, String icon) {
        Map<String, String> icons = iconOverrides(context);
        if (icon == null) {
            icons.remove(id);
        }
        else {
            icons.put(id, icon);
        }
        prefs(context).edit().putString(PREF_ICONS, new JSONObject(icons).toString()).apply();
    }

    // Keeps the custom keys JSON in the same order, so the on-screen layer and exports match the menu
    public static void saveOrder(Context context, List<CustomKey> keys) {
        List<String> ids = new ArrayList<>();
        List<CustomKey> custom = new ArrayList<>();
        for (CustomKey key : keys) {
            ids.add(key.id);
            if (!BuiltInKeys.isBuiltIn(key.id)) {
                custom.add(key);
            }
        }
        prefs(context).edit().putString(PREF_ORDER, TextUtils.join(",", ids)).apply();
        if (!custom.isEmpty()) {
            CustomKeyStore.save(context, custom);
        }
    }

    // Shows every built-in key again with its default icon and puts them back first. The user's own keys are kept.
    public static void resetBuiltIns(Context context) {
        prefs(context).edit().remove(PREF_HIDDEN).remove(PREF_ICONS).remove(PREF_ORDER).apply();
    }

    // NOTE: Keys named in the saved order come first in that order, and any others keep their natural order.
    // NOTE: A key missing from the saved order (a key added later, a built-in key shown again) goes before the first More key, so it is not buried under the More keys group.
    static List<CustomKey> order(List<CustomKey> keys, List<String> order) {
        Map<String, CustomKey> byId = new LinkedHashMap<>();
        for (CustomKey key : keys) {
            byId.put(key.id, key);
        }
        List<CustomKey> ordered = new ArrayList<>();
        for (String id : order) {
            CustomKey key = byId.remove(id);
            if (key != null) {
                ordered.add(key);
            }
        }
        for (CustomKey key : byId.values()) {
            int firstMore = -1;
            for (int i = 0; i < ordered.size() && !isMoreKey(key.id); i++) {
                if (isMoreKey(ordered.get(i).id)) {
                    firstMore = i;
                    break;
                }
            }
            if (firstMore < 0) {
                ordered.add(key);
            }
            else {
                ordered.add(firstMore, key);
            }
        }
        return ordered;
    }

    static CustomKey builtIn(Context context, BuiltInKeys.Def def, Map<String, String> icons) {
        String icon = icons.containsKey(def.id) ? icons.get(def.id) : def.icon;
        return new CustomKey(def.id, context.getString(def.nameRes), false, icon, def.keys);
    }

    private static Set<String> hiddenIds(Context context) {
        String raw = prefs(context).getString(PREF_HIDDEN, "");
        Set<String> ids = new HashSet<>();
        for (String id : raw.split(",")) {
            if (!id.trim().isEmpty()) {
                ids.add(id.trim());
            }
        }
        return ids;
    }

    private static List<String> savedOrder(Context context) {
        String raw = prefs(context).getString(PREF_ORDER, "");
        return raw.isEmpty() ? new ArrayList<>() : new ArrayList<>(Arrays.asList(raw.split(",")));
    }

    private static Map<String, String> iconOverrides(Context context) {
        Map<String, String> icons = new LinkedHashMap<>();
        try {
            JSONObject json = new JSONObject(prefs(context).getString(PREF_ICONS, "{}"));
            for (java.util.Iterator<String> it = json.keys(); it.hasNext(); ) {
                String id = it.next();
                icons.put(id, json.getString(id));
            }
        }
        catch (JSONException e) {
            // An unreadable override list just means default icons
        }
        return icons;
    }
}
