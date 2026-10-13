package com.limelight.menu;

// One row of an in-stream menu: a section header or a tappable item
public class MenuEntry {
    public final String label;
    // Icon for the compact and grid styles, 0 when the item has none
    public final int iconRes;
    public final boolean destructive;
    public final boolean withGameFocus;
    // Null for headers and for items that only close the menu, such as Cancel
    public final Runnable runnable;
    public final boolean header;

    public MenuEntry(String label, int iconRes, boolean destructive, boolean withGameFocus, Runnable runnable) {
        this(label, iconRes, destructive, withGameFocus, runnable, false);
    }

    private MenuEntry(String label, int iconRes, boolean destructive, boolean withGameFocus, Runnable runnable, boolean header) {
        this.label = label;
        this.iconRes = iconRes;
        this.destructive = destructive;
        this.withGameFocus = withGameFocus;
        this.runnable = runnable;
        this.header = header;
    }

    public static MenuEntry header(String label) {
        return new MenuEntry(label, 0, false, false, null, true);
    }
}
