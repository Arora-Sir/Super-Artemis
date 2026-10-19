package com.limelight.customkeys;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// NOTE: A special key: a name, an icon and the steps it runs one after another (a shortcut, typed text, waits, held keys, actions).
// NOTE: keys holds the first shortcut, which is what older app versions and the on-screen key layer read.
public final class CustomKey {
    public final String id;
    public final String name;
    public final boolean sticky;
    // IconCatalog ID, or null for no icon
    public final String icon;
    public final List<Integer> keys;
    public final List<KeyStep> steps;
    // Phone-side hotkey that runs this key during a stream, or null
    public final Hotkey hotkey;

    // A plain shortcut: one step that presses the keys in order and releases them in reverse
    public CustomKey(String id, String name, boolean sticky, String icon, List<Integer> keys) {
        this(id, name, sticky, icon, Collections.singletonList(KeyStep.combo(keys)), null);
    }

    public static CustomKey withSteps(String id, String name, boolean sticky, String icon, List<KeyStep> steps) {
        return new CustomKey(id, name, sticky, icon, steps, null);
    }

    private CustomKey(String id, String name, boolean sticky, String icon, List<KeyStep> steps, Hotkey hotkey) {
        this.id = id;
        this.name = name;
        this.icon = icon;
        this.hotkey = hotkey;
        this.steps = Collections.unmodifiableList(new ArrayList<>(steps));
        List<Integer> first = new ArrayList<>();
        for (KeyStep step : steps) {
            if (step.type == KeyStep.Type.COMBO) {
                first.addAll(step.keys);
                break;
            }
        }
        this.keys = Collections.unmodifiableList(first);
        // Sticky only means something for a single shortcut on the on-screen key layer
        this.sticky = sticky && isSingleCombo();
    }

    // True for a key that is one shortcut, which runs exactly like keys always did
    public boolean isSingleCombo() {
        return steps.size() == 1 && steps.get(0).type == KeyStep.Type.COMBO;
    }

    public CustomKey withId(String newId) {
        return new CustomKey(newId, name, sticky, icon, steps, hotkey);
    }

    public CustomKey withHotkey(Hotkey value) {
        return new CustomKey(id, name, sticky, icon, steps, value);
    }

    public short[] keyCodes() {
        short[] codes = new short[keys.size()];
        for (int i = 0; i < codes.length; i++) {
            codes[i] = (short) (int) keys.get(i);
        }
        return codes;
    }
}
