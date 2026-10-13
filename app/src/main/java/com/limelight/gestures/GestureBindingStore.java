package com.limelight.gestures;

import android.content.SharedPreferences;

import com.limelight.actions.StreamAction;
import com.limelight.actions.StreamActionRegistry;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

// NOTE: Which action each multi-finger tap and double tap runs. Values are "none", "quick_menu", "middle_click", a stream action ID or "key:<custom key ID>".
// NOTE: Defaults match the gestures Artemis always had, and the old "Five-finger tap opens menu" switch decides the five-finger default.
public final class GestureBindingStore {
    public static final String NONE = "none";
    public static final String QUICK_MENU = "quick_menu";
    public static final String MIDDLE_CLICK = "middle_click";
    static final String KEY_PREFIX = "key:";
    // Setting from before configurable gestures, read only to migrate it
    static final String LEGACY_FIVE_FINGER_MENU = "checkbox_five_finger_menu";

    public static final int[] FINGER_COUNTS = {3, 4, 5};

    public static final class Slot {
        public final int fingers;
        public final boolean doubleTap;

        public Slot(int fingers, boolean doubleTap) {
            this.fingers = fingers;
            this.doubleTap = doubleTap;
        }

        public String prefKey() {
            return "gesture_" + (doubleTap ? "double_" : "tap_") + fingers;
        }
    }

    public static final List<Slot> SLOTS = Collections.unmodifiableList(Arrays.asList(
            new Slot(3, false), new Slot(4, false), new Slot(5, false),
            new Slot(3, true), new Slot(4, true), new Slot(5, true)));

    private GestureBindingStore() {
    }

    public static String defaultFor(SharedPreferences prefs, Slot slot) {
        if (slot.doubleTap) {
            return NONE;
        }
        switch (slot.fingers) {
            case 3:
                return StreamActionRegistry.ID_TOGGLE_KEYBOARD;
            case 4:
                return StreamActionRegistry.ID_TOGGLE_FULL_KEYBOARD;
            default:
                return prefs.getBoolean(LEGACY_FIVE_FINGER_MENU, true) ? QUICK_MENU : NONE;
        }
    }

    public static String get(SharedPreferences prefs, Slot slot) {
        String value = prefs.getString(slot.prefKey(), null);
        return value != null && isValid(value) ? value : defaultFor(prefs, slot);
    }

    public static void set(SharedPreferences prefs, Slot slot, String binding) {
        prefs.edit().putString(slot.prefKey(), isValid(binding) ? binding : NONE).apply();
    }

    public static void reset(SharedPreferences prefs) {
        SharedPreferences.Editor editor = prefs.edit();
        for (Slot slot : SLOTS) {
            editor.remove(slot.prefKey());
        }
        editor.apply();
    }

    public static String customKeyBinding(String customKeyId) {
        return KEY_PREFIX + customKeyId;
    }

    public static String customKeyId(String binding) {
        return binding.startsWith(KEY_PREFIX) ? binding.substring(KEY_PREFIX.length()) : null;
    }

    // Session actions are refused, so a stray multi-finger tap can never end the stream
    static boolean isValid(String binding) {
        if (NONE.equals(binding) || QUICK_MENU.equals(binding) || MIDDLE_CLICK.equals(binding)) {
            return true;
        }
        String keyId = customKeyId(binding);
        if (keyId != null) {
            return !keyId.isEmpty();
        }
        StreamAction action = StreamActionRegistry.find(binding);
        return action != null && !action.destructive;
    }
}
