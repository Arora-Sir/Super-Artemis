package com.limelight.menu;

import android.content.SharedPreferences;

// NOTE: What the quick bars do while the user does not touch the stream, as two stages: dim to a chosen opacity, then hide completely and leave an edge tab.
// NOTE: Both times count from the last touch. A dim time of 0 skips the dimming stage and a hide time of 0 never hides.
public final class IdlePlan {
    public static final String PREF_DIM_SECONDS = "quick_bar_dim_seconds";
    public static final String PREF_DIM_PERCENT = "quick_bar_dim_percent";
    public static final String PREF_HIDE_SECONDS = "quick_bar_idle_hide_seconds";
    public static final String PREF_TAB_PERCENT = "quick_bar_tab_percent";

    public static final int DEFAULT_DIM_SECONDS = 3;
    public static final int DEFAULT_DIM_PERCENT = 40;
    public static final int DEFAULT_HIDE_SECONDS = 0;
    public static final int DEFAULT_TAB_PERCENT = 100;
    public static final int MAX_SECONDS = 600;

    public final int dimSeconds;
    public final int dimPercent;
    public final int hideSeconds;
    public final int tabPercent;

    public IdlePlan(int dimSeconds, int dimPercent, int hideSeconds, int tabPercent) {
        this.dimSeconds = clamp(dimSeconds, 0, MAX_SECONDS);
        this.dimPercent = clamp(dimPercent, 0, 100);
        this.hideSeconds = clamp(hideSeconds, 0, MAX_SECONDS);
        this.tabPercent = clamp(tabPercent, 0, 100);
    }

    public static IdlePlan read(SharedPreferences prefs) {
        return new IdlePlan(
                readInt(prefs, PREF_DIM_SECONDS, DEFAULT_DIM_SECONDS),
                readInt(prefs, PREF_DIM_PERCENT, DEFAULT_DIM_PERCENT),
                readInt(prefs, PREF_HIDE_SECONDS, DEFAULT_HIDE_SECONDS),
                readInt(prefs, PREF_TAB_PERCENT, DEFAULT_TAB_PERCENT));
    }

    public void save(SharedPreferences prefs) {
        prefs.edit()
                .putInt(PREF_DIM_SECONDS, dimSeconds)
                .putInt(PREF_DIM_PERCENT, dimPercent)
                .putInt(PREF_HIDE_SECONDS, hideSeconds)
                .putInt(PREF_TAB_PERCENT, tabPercent)
                .apply();
    }

    // Milliseconds after the last touch until the bars dim, or -1 when they never dim. A dim time at or after the hide time would never be seen.
    public long dimDelayMs() {
        boolean before = hideSeconds == 0 || dimSeconds < hideSeconds;
        return dimSeconds > 0 && before ? dimSeconds * 1000L : -1;
    }

    // Milliseconds after the last touch until the bars hide completely, or -1 when they never hide
    public long hideDelayMs() {
        return hideSeconds > 0 ? hideSeconds * 1000L : -1;
    }

    public float dimAlpha() {
        return dimPercent / 100f;
    }

    public float tabAlpha() {
        return tabPercent / 100f;
    }

    // An opacity of 0 means no edge tab at all, so a hidden bar comes back with a touch on the stream only
    public boolean showsTab() {
        return tabPercent > 0;
    }

    private static int readInt(SharedPreferences prefs, String key, int fallback) {
        try {
            return prefs.getInt(key, fallback);
        }
        catch (ClassCastException e) {
            return fallback;
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
