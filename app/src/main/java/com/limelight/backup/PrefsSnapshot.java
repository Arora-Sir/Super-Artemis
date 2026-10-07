package com.limelight.backup;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

// NOTE: Captures and restores every app SharedPreferences file with typed values, so booleans stay booleans and string sets stay sets.
// NOTE: Files listed as merge files keep their local values and only gain keys that exist in the backup alone.
final class PrefsSnapshot {
    // Logical name for the default SharedPreferences file so backups move between debug and release builds
    static final String DEFAULT_PREFS_KEY = "_default";

    // Device-specific state, framework internals and search history that must not travel to another install
    private static final Set<String> EXCLUDED_PREFS = new HashSet<>(Arrays.asList(
            "DecoderTombstone", "GlPreferences", "preferenceSearch"));

    private PrefsSnapshot() {
    }

    // Same file name androidx PreferenceManager uses for getDefaultSharedPreferences()
    static String defaultPreferencesName(Context context) {
        return context.getPackageName() + "_preferences";
    }

    // Framework files (android.*), androidx markers (_has_set_default_values) and the exclusion list stay out of backups
    static boolean isExcluded(String name) {
        return EXCLUDED_PREFS.contains(name) || name.startsWith("android.") || name.startsWith("_");
    }

    // Every non-excluded SharedPreferences file of this app, including dynamically named keyboard layouts
    static Set<String> listFiles(Context context) {
        Set<String> names = new HashSet<>();
        File dir = new File(context.getApplicationInfo().dataDir, "shared_prefs");
        String[] fileNames = dir.list();
        if (fileNames != null) {
            for (String fileName : fileNames) {
                if (fileName.endsWith(".xml")) {
                    String name = fileName.substring(0, fileName.length() - 4);
                    if (!isExcluded(name)) {
                        names.add(name);
                    }
                }
            }
        }
        return names;
    }

    static JSONObject capture(Context context) throws JSONException {
        JSONObject json = new JSONObject();
        String defaultName = defaultPreferencesName(context);
        for (String name : listFiles(context)) {
            SharedPreferences prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE);
            json.put(name.equals(defaultName) ? DEFAULT_PREFS_KEY : name, encode(prefs.getAll()));
        }
        return json;
    }

    static void validate(JSONObject prefsJson) throws JSONException, InvalidBackupException {
        Iterator<String> files = prefsJson.keys();
        while (files.hasNext()) {
            String name = files.next();
            if (name.isEmpty() || name.contains("/") || name.contains("\\") || name.contains("..")) {
                throw new InvalidBackupException("Invalid preference file name " + name);
            }
            JSONObject entries = prefsJson.getJSONObject(name);
            Iterator<String> keys = entries.keys();
            while (keys.hasNext()) {
                decodeValue(entries.getJSONObject(keys.next()));
            }
        }
    }

    // Mirrors the backup into SharedPreferences, except merge files where local values win
    static void restore(Context context, JSONObject prefsJson, Set<String> mergeFiles)
            throws JSONException, IOException, InvalidBackupException {
        String defaultName = defaultPreferencesName(context);
        Set<String> restored = new HashSet<>();

        Iterator<String> files = prefsJson.keys();
        while (files.hasNext()) {
            String key = files.next();
            if (!DEFAULT_PREFS_KEY.equals(key) && isExcluded(key)) {
                // Written by an older build that did not filter this file yet
                continue;
            }
            String name = DEFAULT_PREFS_KEY.equals(key) ? defaultName : key;
            restored.add(name);
            SharedPreferences prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE);
            write(prefs, prefsJson.getJSONObject(key), mergeFiles.contains(name));
        }

        // Settings files that exist locally but not in the backup are cleared so the result mirrors the backup
        for (String name : listFiles(context)) {
            if (!restored.contains(name) && !mergeFiles.contains(name)) {
                if (!context.getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear().commit()) {
                    throw new IOException("Failed to clear preferences " + name);
                }
            }
        }
    }

    static int countEntries(JSONObject prefsJson) {
        JSONObject defaults = prefsJson.optJSONObject(DEFAULT_PREFS_KEY);
        return defaults != null ? defaults.length() : 0;
    }

    static JSONObject encode(Map<String, ?> all) throws JSONException {
        JSONObject json = new JSONObject();
        for (Map.Entry<String, ?> entry : all.entrySet()) {
            Object value = entry.getValue();
            JSONObject typed = new JSONObject();
            if (value instanceof Boolean) {
                typed.put("type", "boolean");
                typed.put("value", value);
            }
            else if (value instanceof Integer) {
                typed.put("type", "int");
                typed.put("value", value);
            }
            else if (value instanceof Long) {
                typed.put("type", "long");
                typed.put("value", value);
            }
            else if (value instanceof Float) {
                typed.put("type", "float");
                typed.put("value", ((Float) value).doubleValue());
            }
            else if (value instanceof String) {
                typed.put("type", "string");
                typed.put("value", value);
            }
            else if (value instanceof Set) {
                JSONArray array = new JSONArray();
                for (Object item : (Set<?>) value) {
                    array.put(String.valueOf(item));
                }
                typed.put("type", "stringSet");
                typed.put("value", array);
            }
            else {
                continue;
            }
            json.put(entry.getKey(), typed);
        }
        return json;
    }

    static Object decodeValue(JSONObject typed) throws JSONException, InvalidBackupException {
        String type = typed.getString("type");
        switch (type) {
            case "boolean":
                return typed.getBoolean("value");
            case "int":
                return typed.getInt("value");
            case "long":
                return typed.getLong("value");
            case "float":
                return (float) typed.getDouble("value");
            case "string":
                return typed.getString("value");
            case "stringSet":
                JSONArray array = typed.getJSONArray("value");
                Set<String> set = new HashSet<>();
                for (int i = 0; i < array.length(); i++) {
                    set.add(array.getString(i));
                }
                return set;
            default:
                throw new InvalidBackupException("Unknown preference type " + type);
        }
    }

    private static void write(SharedPreferences prefs, JSONObject entries, boolean merge)
            throws JSONException, IOException, InvalidBackupException {
        SharedPreferences.Editor editor = merge ? prefs.edit() : prefs.edit().clear();
        Iterator<String> keys = entries.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            if (merge && prefs.contains(key)) {
                continue;
            }
            Object value = decodeValue(entries.getJSONObject(key));
            if (value instanceof Boolean) {
                editor.putBoolean(key, (Boolean) value);
            }
            else if (value instanceof Integer) {
                editor.putInt(key, (Integer) value);
            }
            else if (value instanceof Long) {
                editor.putLong(key, (Long) value);
            }
            else if (value instanceof Float) {
                editor.putFloat(key, (Float) value);
            }
            else if (value instanceof String) {
                editor.putString(key, (String) value);
            }
            else {
                @SuppressWarnings("unchecked")
                Set<String> set = (Set<String>) value;
                editor.putStringSet(key, set);
            }
        }
        if (!editor.commit()) {
            throw new IOException("Failed to write preferences");
        }
    }
}
