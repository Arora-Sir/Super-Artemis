package com.limelight.customkeys;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// NOTE: One step of a special key. A key runs its steps one after another, never at the same time.
// NOTE: A plain shortcut is a key with a single COMBO step, so every older key keeps working unchanged.
public final class KeyStep {
    public enum Type {
        // Presses the keys in order and releases them in reverse, like a shortcut
        COMBO,
        // Types the text on the PC as Unicode, so any language and emoji work
        TEXT,
        // Pauses before the next step, in milliseconds
        WAIT,
        // Presses one key and keeps it down until a RELEASE step or the end of the key
        HOLD,
        RELEASE,
        // Runs the previous step again, a number of times
        REPEAT,
        // Runs a stream action such as the microphone toggle, by its registry ID
        ACTION
    }

    public static final int WAIT_MIN_MS = 10;
    public static final int WAIT_MAX_MS = 60_000;
    public static final int REPEAT_MAX = 50;
    public static final int TEXT_MAX = 1000;

    public final Type type;
    // COMBO: the keys of the shortcut. HOLD and RELEASE: exactly one key.
    public final List<Integer> keys;
    // TEXT: the text to type. ACTION: the stream action ID.
    public final String text;
    // WAIT: milliseconds. REPEAT: how many more times the previous step runs.
    public final int amount;

    private KeyStep(Type type, List<Integer> keys, String text, int amount) {
        this.type = type;
        this.keys = Collections.unmodifiableList(new ArrayList<>(keys));
        this.text = text;
        this.amount = amount;
    }

    public static KeyStep combo(List<Integer> keys) {
        return new KeyStep(Type.COMBO, keys, null, 0);
    }

    public static KeyStep text(String text) {
        String value = text != null ? text : "";
        return new KeyStep(Type.TEXT, Collections.emptyList(), value.length() > TEXT_MAX ? value.substring(0, TEXT_MAX) : value, 0);
    }

    public static KeyStep waitMs(int ms) {
        return new KeyStep(Type.WAIT, Collections.emptyList(), null, Math.max(WAIT_MIN_MS, Math.min(WAIT_MAX_MS, ms)));
    }

    public static KeyStep hold(int key) {
        return new KeyStep(Type.HOLD, Collections.singletonList(key), null, 0);
    }

    public static KeyStep release(int key) {
        return new KeyStep(Type.RELEASE, Collections.singletonList(key), null, 0);
    }

    public static KeyStep repeat(int times) {
        return new KeyStep(Type.REPEAT, Collections.emptyList(), null, Math.max(1, Math.min(REPEAT_MAX, times)));
    }

    public static KeyStep action(String actionId) {
        return new KeyStep(Type.ACTION, Collections.emptyList(), actionId, 0);
    }

    // A step that would do nothing (no keys, no text) is never saved
    public boolean isValid() {
        switch (type) {
            case COMBO:
                return !keys.isEmpty();
            case HOLD:
            case RELEASE:
                return keys.size() == 1;
            case TEXT:
            case ACTION:
                return text != null && !text.isEmpty();
            default:
                return true;
        }
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof KeyStep)) {
            return false;
        }
        KeyStep step = (KeyStep) other;
        return type == step.type && keys.equals(step.keys) && amount == step.amount
                && (text == null ? step.text == null : text.equals(step.text));
    }

    @Override
    public int hashCode() {
        return (type.hashCode() * 31 + keys.hashCode()) * 31 + amount + (text != null ? text.hashCode() : 0);
    }
}
