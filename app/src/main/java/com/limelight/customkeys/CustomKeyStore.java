package com.limelight.customkeys;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.limelight.GameMenu;
import com.limelight.utils.KeyConfigHelper;
import com.limelight.utils.KeyMapper;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

// NOTE: Reads and writes custom keys in the same specialPrefs JSON that import, export, backup and the on-screen key layer already use.
// NOTE: Keys without an ID get their list position as ID, matching how the on-screen layer names them, so saved key positions survive.
public final class CustomKeyStore {
    public static final class FormatException extends Exception {
        FormatException(String message) {
            super(message);
        }
    }

    private CustomKeyStore() {
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(GameMenu.PREF_NAME, Context.MODE_PRIVATE);
    }

    public static List<CustomKey> load(Context context) throws FormatException {
        return parse(prefs(context).getString(GameMenu.KEY_NAME, ""));
    }

    // Callers that only display keys treat an unreadable file as no keys, the manager reports it instead
    public static List<CustomKey> loadOrEmpty(Context context) {
        try {
            return load(context);
        }
        catch (FormatException e) {
            return new ArrayList<>();
        }
    }

    public static void save(Context context, List<CustomKey> keys) {
        prefs(context).edit().putString(GameMenu.KEY_NAME, toJson(keys)).apply();
    }

    // Replaces the key with the same ID, or appends it with a fresh ID when it is new (null ID)
    public static CustomKey upsert(Context context, CustomKey key) throws FormatException {
        List<CustomKey> keys = load(context);
        CustomKey saved = upsert(keys, key);
        save(context, keys);
        return saved;
    }

    static CustomKey upsert(List<CustomKey> keys, CustomKey key) {
        if (key.id != null) {
            for (int i = 0; i < keys.size(); i++) {
                if (keys.get(i).id.equals(key.id)) {
                    keys.set(i, key);
                    return key;
                }
            }
        }
        CustomKey added = key.withId(key.id != null ? key.id : newId(keys));
        keys.add(added);
        return added;
    }

    public static CustomKey find(Context context, String id) {
        for (CustomKey key : loadOrEmpty(context)) {
            if (key.id.equals(id)) {
                return key;
            }
        }
        return null;
    }

    static List<CustomKey> parse(String json) throws FormatException {
        List<CustomKey> keys = new ArrayList<>();
        if (TextUtils.isEmpty(json)) {
            return keys;
        }
        KeyConfigHelper.ShortcutFile file;
        try {
            file = KeyConfigHelper.parseShortcutFile(json);
        }
        catch (RuntimeException e) {
            throw new FormatException("Custom keys are not valid JSON");
        }
        if (file == null || file.data == null) {
            return keys;
        }
        for (int i = 0; i < file.data.size(); i++) {
            KeyConfigHelper.Shortcut shortcut = file.data.get(i);
            // A hand-edited or imported file can hold null entries, which are skipped like empty ones
            if (shortcut == null) {
                continue;
            }
            List<Integer> codes = new ArrayList<>();
            if (shortcut.keys != null) {
                for (String code : shortcut.keys) {
                    if (code == null) {
                        throw new FormatException("A key code is missing");
                    }
                    codes.add(parseKeyCode(code));
                }
            }
            String id = TextUtils.isEmpty(shortcut.id) ? Integer.toString(i) : shortcut.id;
            String name = shortcut.name != null ? shortcut.name : "";
            List<KeyStep> steps = parseSteps(shortcut.steps);
            CustomKey key = steps.isEmpty() ? new CustomKey(id, name, shortcut.sticky, shortcut.icon, codes)
                    : CustomKey.withSteps(id, name, shortcut.sticky, shortcut.icon, steps);
            keys.add(key.withHotkey(parseHotkey(shortcut)));
        }
        return keys;
    }

    static String toJson(List<CustomKey> keys) {
        List<KeyConfigHelper.Shortcut> shortcuts = new ArrayList<>();
        for (CustomKey key : keys) {
            List<String> codes = new ArrayList<>();
            for (int code : key.keys) {
                codes.add(String.format(Locale.US, "0x%02X", code));
            }
            KeyConfigHelper.Shortcut shortcut = new KeyConfigHelper.Shortcut(key.id, key.name, key.sticky, codes);
            shortcut.icon = key.icon;
            if (!key.isSingleCombo()) {
                shortcut.steps = stepsJson(key.steps);
            }
            if (key.hotkey != null) {
                shortcut.hotkey = new ArrayList<>();
                for (int vk : key.hotkey.virtualKeys()) {
                    shortcut.hotkey.add(String.format(Locale.US, "0x%02X", vk));
                }
                shortcut.hotkeyToPc = key.hotkey.toPc ? Boolean.TRUE : null;
            }
            shortcuts.add(shortcut);
        }
        return new Gson().toJson(new KeyConfigHelper.ShortcutFile(shortcuts));
    }

    // A hotkey that cannot be read is dropped instead of refusing the whole file, since the key itself still works from the menu and bars
    static Hotkey parseHotkey(KeyConfigHelper.Shortcut shortcut) {
        if (shortcut.hotkey == null || shortcut.hotkey.isEmpty()) {
            return null;
        }
        List<Integer> vks = new ArrayList<>();
        for (String code : shortcut.hotkey) {
            try {
                vks.add(parseKeyCode(code));
            }
            catch (FormatException | RuntimeException e) {
                return null;
            }
        }
        return Hotkey.fromVirtualKeys(vks, Boolean.TRUE.equals(shortcut.hotkeyToPc));
    }

    // NOTE: Steps of a newer version that this one does not know are skipped, so the rest of the key still runs.
    // NOTE: Steps with missing keys or text are refused, because a hand-edited file must not crash the stream.
    static List<KeyStep> parseSteps(List<KeyConfigHelper.Step> raw) throws FormatException {
        List<KeyStep> steps = new ArrayList<>();
        if (raw == null) {
            return steps;
        }
        for (KeyConfigHelper.Step step : raw) {
            if (step == null || step.type == null) {
                continue;
            }
            List<Integer> codes = new ArrayList<>();
            if (step.keys != null) {
                for (String code : step.keys) {
                    if (code == null) {
                        throw new FormatException("A key code is missing");
                    }
                    codes.add(parseKeyCode(code));
                }
            }
            int amount = step.amount != null ? step.amount : 0;
            KeyStep parsed;
            switch (step.type) {
                case "combo":
                    parsed = KeyStep.combo(codes);
                    break;
                case "text":
                    parsed = KeyStep.text(step.text);
                    break;
                case "wait":
                    parsed = step.amount != null ? KeyStep.waitMs(amount) : null;
                    break;
                case "hold":
                    parsed = codes.size() == 1 ? KeyStep.hold(codes.get(0)) : null;
                    break;
                case "release":
                    parsed = codes.size() == 1 ? KeyStep.release(codes.get(0)) : null;
                    break;
                case "repeat":
                    parsed = step.amount != null ? KeyStep.repeat(amount) : null;
                    break;
                case "action":
                    parsed = KeyStep.action(step.text);
                    break;
                default:
                    continue;
            }
            if (parsed == null || !parsed.isValid()) {
                throw new FormatException("A step of type " + step.type + " is incomplete");
            }
            steps.add(parsed);
        }
        return steps;
    }

    static List<KeyConfigHelper.Step> stepsJson(List<KeyStep> steps) {
        List<KeyConfigHelper.Step> out = new ArrayList<>();
        for (KeyStep step : steps) {
            KeyConfigHelper.Step json = new KeyConfigHelper.Step();
            json.type = step.type.name().toLowerCase(Locale.US);
            if (!step.keys.isEmpty()) {
                json.keys = new ArrayList<>();
                for (int code : step.keys) {
                    json.keys.add(String.format(Locale.US, "0x%02X", code));
                }
            }
            json.text = step.text;
            if (step.type == KeyStep.Type.WAIT || step.type == KeyStep.Type.REPEAT) {
                json.amount = step.amount;
            }
            out.add(json);
        }
        return out;
    }

    // Same rules as the import format: literal hex ("0x1B") or a VK_ constant name from KeyMapper
    static int parseKeyCode(String code) throws FormatException {
        try {
            if (code.startsWith("0x") || code.startsWith("0X")) {
                return Integer.parseInt(code.substring(2), 16);
            }
            if (code.startsWith("VK_")) {
                Field field = KeyMapper.class.getDeclaredField(code);
                return field.getInt(null);
            }
        }
        catch (NumberFormatException | ReflectiveOperationException e) {
            // Falls through to the error below
        }
        throw new FormatException("Unknown key code: " + code);
    }

    // Short, unique and never reused, so the on-screen layer keeps each key's saved position
    public static String newId(List<CustomKey> existing) {
        Set<String> used = new HashSet<>();
        for (CustomKey key : existing) {
            used.add(key.id);
        }
        String id;
        long seed = System.currentTimeMillis();
        do {
            id = "k" + Long.toString(seed++, 36);
        } while (used.contains(id));
        return id;
    }
}
