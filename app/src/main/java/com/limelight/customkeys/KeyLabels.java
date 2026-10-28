package com.limelight.customkeys;

import android.content.Context;

import com.limelight.R;

import java.util.Locale;

// NOTE: The one place that decides how a custom key reads in lists, menus, bars and pickers.
// NOTE: A name is optional. A single shortcut without a name reads as its keys ("Ctrl + M"), and with a name as "Ctrl + M (Gemini Talk)". A flow reads as its name or its first step.
public final class KeyLabels {
    private static KeyNames cachedNames;
    private static String cachedLanguage;

    private KeyLabels() {
    }

    // KeyNames inflates the whole built-in keyboard to read its labels, so one instance serves every list until the language changes
    public static synchronized KeyNames names(Context context) {
        String language = Locale.getDefault().toLanguageTag();
        if (cachedNames == null || !language.equals(cachedLanguage)) {
            cachedNames = new KeyNames(context);
            cachedLanguage = language;
        }
        return cachedNames;
    }

    public static String displayLabel(Context context, CustomKey key) {
        return displayLabel(context, names(context), key);
    }

    public static String displayLabel(Context context, KeyNames names, CustomKey key) {
        String name = key.name == null ? "" : key.name.trim();
        if (!key.isSingleCombo()) {
            return name.isEmpty() ? CustomKeyEditor.autoName(context, names, key.steps) : name;
        }
        String combo = names.describe(key.keys);
        if (name.isEmpty() || name.equals(combo)) {
            return combo;
        }
        // Built-in keys keep their own name, such as Lock or Task Manager
        if (BuiltInKeys.isBuiltIn(key.id)) {
            return name;
        }
        return combo + " (" + name + ")";
    }

    // What the picker lists show, so a single shortcut reads "Key: ..." and a sequence reads "Flow: ..."
    public static String pickerLabel(Context context, CustomKey key) {
        return context.getString(key.isSingleCombo() ? R.string.quick_bar_custom_key : R.string.quick_bar_custom_flow,
                displayLabel(context, key));
    }

    // The name field of the editor shows this as its hint while the name is empty
    public static String suggestedName(Context context, KeyNames names, java.util.List<KeyStep> steps) {
        return CustomKeyEditor.autoName(context, names, steps);
    }
}
