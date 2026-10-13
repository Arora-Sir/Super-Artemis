package com.limelight.gestures;

import android.content.SharedPreferences;

import com.limelight.actions.StreamAction;
import com.limelight.actions.StreamActionRegistry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

// NOTE: Which action each multi-finger gesture runs: taps and double taps with 3, 4 or 5 fingers, and swipes with 3 or 4 fingers in four directions.
// NOTE: Values are "none", "quick_menu", "middle_click", a stream action ID, "key:<special key ID>" or "combo:<hex key codes joined by +>".
// NOTE: Defaults match the gestures Artemis always had, swipes start as Nothing, and the old "Five-finger tap opens menu" switch decides the five-finger default.
public final class GestureBindingStore {
    public static final String NONE = "none";
    public static final String QUICK_MENU = "quick_menu";
    public static final String MIDDLE_CLICK = "middle_click";
    static final String KEY_PREFIX = "key:";
    static final String COMBO_PREFIX = "combo:";
    // Setting from before configurable gestures, read only to migrate it
    static final String LEGACY_FIVE_FINGER_MENU = "checkbox_five_finger_menu";

    public static final int[] FINGER_COUNTS = {3, 4, 5};
    // Five-finger swipes look like a palm on the screen, so only 3 and 4 fingers can swipe
    public static final int[] SWIPE_FINGER_COUNTS = {3, 4};

    public enum Kind {
        TAP("tap_"), DOUBLE_TAP("double_"),
        SWIPE_UP("swipe_up_"), SWIPE_DOWN("swipe_down_"), SWIPE_LEFT("swipe_left_"), SWIPE_RIGHT("swipe_right_");

        final String prefix;

        Kind(String prefix) {
            this.prefix = prefix;
        }

        public boolean isSwipe() {
            return this != TAP && this != DOUBLE_TAP;
        }
    }

    public static final class Slot {
        public final int fingers;
        public final Kind kind;
        public final boolean doubleTap;

        public Slot(int fingers, boolean doubleTap) {
            this(fingers, doubleTap ? Kind.DOUBLE_TAP : Kind.TAP);
        }

        public Slot(int fingers, Kind kind) {
            this.fingers = fingers;
            this.kind = kind;
            this.doubleTap = kind == Kind.DOUBLE_TAP;
        }

        public String prefKey() {
            return "gesture_" + kind.prefix + fingers;
        }
    }

    public static final List<Slot> SLOTS;

    static {
        List<Slot> slots = new ArrayList<>();
        for (int fingers : FINGER_COUNTS) {
            slots.add(new Slot(fingers, Kind.TAP));
        }
        for (int fingers : FINGER_COUNTS) {
            slots.add(new Slot(fingers, Kind.DOUBLE_TAP));
        }
        for (int fingers : SWIPE_FINGER_COUNTS) {
            for (Kind kind : Arrays.asList(Kind.SWIPE_UP, Kind.SWIPE_DOWN, Kind.SWIPE_LEFT, Kind.SWIPE_RIGHT)) {
                slots.add(new Slot(fingers, kind));
            }
        }
        SLOTS = Collections.unmodifiableList(slots);
    }

    private GestureBindingStore() {
    }

    public static String defaultFor(SharedPreferences prefs, Slot slot) {
        if (slot.kind != Kind.TAP) {
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

    // True when any swipe with this many fingers runs something, so those touches belong to the app from the moment the fingers land
    public static boolean hasSwipe(SharedPreferences prefs, int fingers) {
        for (Slot slot : SLOTS) {
            if (slot.fingers == fingers && slot.kind.isSwipe() && !NONE.equals(get(prefs, slot))) {
                return true;
            }
        }
        return false;
    }

    // NOTE: The Windows touchpad gestures on 3 and 4-finger swipes: Task View, show desktop, switch apps and switch virtual desktops.
    // NOTE: Useful in mouse and trackpad modes. In direct touch mode Windows 11 already reads these swipes itself when they are left as Nothing.
    public static void applyWindowsPreset(SharedPreferences prefs) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(new Slot(3, Kind.SWIPE_UP).prefKey(), combo(0x5B, 0x09));
        editor.putString(new Slot(3, Kind.SWIPE_DOWN).prefKey(), combo(0x5B, 0x44));
        editor.putString(new Slot(3, Kind.SWIPE_LEFT).prefKey(), combo(0xA4, 0xA0, 0x09));
        editor.putString(new Slot(3, Kind.SWIPE_RIGHT).prefKey(), combo(0xA4, 0x09));
        editor.putString(new Slot(4, Kind.SWIPE_LEFT).prefKey(), combo(0xA2, 0x5B, 0x25));
        editor.putString(new Slot(4, Kind.SWIPE_RIGHT).prefKey(), combo(0xA2, 0x5B, 0x27));
        editor.apply();
    }

    public static String customKeyBinding(String customKeyId) {
        return KEY_PREFIX + customKeyId;
    }

    public static String customKeyId(String binding) {
        return binding.startsWith(KEY_PREFIX) ? binding.substring(KEY_PREFIX.length()) : null;
    }

    static String combo(int... keys) {
        StringBuilder value = new StringBuilder(COMBO_PREFIX);
        for (int i = 0; i < keys.length; i++) {
            if (i > 0) {
                value.append('+');
            }
            value.append(String.format(Locale.US, "%02X", keys[i]));
        }
        return value.toString();
    }

    // Windows virtual key codes of a "combo:" binding, or null for any other binding or a malformed one
    public static List<Integer> comboKeys(String binding) {
        if (!binding.startsWith(COMBO_PREFIX)) {
            return null;
        }
        List<Integer> keys = new ArrayList<>();
        try {
            for (String part : binding.substring(COMBO_PREFIX.length()).split("\\+")) {
                int code = Integer.parseInt(part, 16);
                if (code <= 0 || code > 0xFF) {
                    return null;
                }
                keys.add(code);
            }
        }
        catch (NumberFormatException e) {
            return null;
        }
        return keys.isEmpty() || keys.size() > 8 ? null : keys;
    }

    // Session actions are refused, so a stray multi-finger gesture can never end the stream
    static boolean isValid(String binding) {
        if (NONE.equals(binding) || QUICK_MENU.equals(binding) || MIDDLE_CLICK.equals(binding)) {
            return true;
        }
        if (binding.startsWith(COMBO_PREFIX)) {
            return comboKeys(binding) != null;
        }
        String keyId = customKeyId(binding);
        if (keyId != null) {
            return !keyId.isEmpty();
        }
        StreamAction action = StreamActionRegistry.find(binding);
        return action != null && !action.destructive;
    }
}
