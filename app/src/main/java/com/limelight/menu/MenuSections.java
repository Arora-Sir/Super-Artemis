package com.limelight.menu;

import android.content.res.Resources;

import java.util.ArrayList;
import java.util.List;

// Joins menu sections into one entry list, each behind its header, and skips sections that turned out empty
public final class MenuSections {
    private final Resources resources;
    private final List<MenuEntry> entries = new ArrayList<>();

    public MenuSections(Resources resources) {
        this.resources = resources;
    }

    public MenuSections add(int headerRes, List<? extends MenuEntry> items) {
        if (!items.isEmpty()) {
            entries.add(MenuEntry.header(resources.getString(headerRes)));
            entries.addAll(items);
        }
        return this;
    }

    public List<MenuEntry> build() {
        return new ArrayList<>(entries);
    }
}
