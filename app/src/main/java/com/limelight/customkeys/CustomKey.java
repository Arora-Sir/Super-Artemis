package com.limelight.customkeys;

import java.util.ArrayList;
import java.util.List;

// A user-defined key combination, sent to the host as Windows virtual key codes pressed in order and released in reverse
public final class CustomKey {
    public final String id;
    public final String name;
    public final boolean sticky;
    // IconCatalog ID, or null for no icon
    public final String icon;
    public final List<Integer> keys;

    public CustomKey(String id, String name, boolean sticky, String icon, List<Integer> keys) {
        this.id = id;
        this.name = name;
        this.sticky = sticky;
        this.icon = icon;
        this.keys = new ArrayList<>(keys);
    }

    public short[] keyCodes() {
        short[] codes = new short[keys.size()];
        for (int i = 0; i < codes.length; i++) {
            codes[i] = (short) (int) keys.get(i);
        }
        return codes;
    }
}
