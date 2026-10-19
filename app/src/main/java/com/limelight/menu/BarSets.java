package com.limelight.menu;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import com.limelight.profiles.ProfilesManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// NOTE: Quick-action bars as named sets. A set holds any number of bars, and each bar has an anchor, a line length, a fill direction, an offset and its items.
// NOTE: A bar never hides a button: it shows "per line" buttons in each line and starts a new line for the rest, across in rows or down in columns.
// NOTE: One set is active at a time and the Quick Menu switches between sets, so a game layout and a desktop layout can each keep their own bars.
// NOTE: The first load turns the four corner bars of earlier versions into a "Default" set. Their old keys stay untouched, so an older version still reads them.
// NOTE: Sets belong to the active settings profile. Reads go through the profile overlay and saves go into the profile, so each profile keeps its own sets.
// NOTE: Without an active profile, or for a profile made before sets existed, the default settings hold the sets.
public final class BarSets {
    public static final String PREF_SETS = "quick_bar_sets";
    public static final String PREF_ACTIVE = "quick_bar_active_set";

    public static final String TOP_LEFT = "top_left";
    public static final String TOP_CENTER = "top_center";
    public static final String TOP_RIGHT = "top_right";
    public static final String CENTER_LEFT = "center_left";
    public static final String CENTER = "center";
    public static final String CENTER_RIGHT = "center_right";
    public static final String BOTTOM_LEFT = "bottom_left";
    public static final String BOTTOM_CENTER = "bottom_center";
    public static final String BOTTOM_RIGHT = "bottom_right";
    // Reading order, which is also the order of the 3 by 3 anchor picker
    public static final List<String> ANCHORS = Collections.unmodifiableList(Arrays.asList(
            TOP_LEFT, TOP_CENTER, TOP_RIGHT, CENTER_LEFT, CENTER, CENTER_RIGHT, BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT));

    // Most buttons in one line of a bar
    public static final int GRID_MAX = 20;
    // Far enough to cross a phone screen from any anchor, near enough that a bar never gets lost off screen
    public static final int OFFSET_MAX_DP = 400;

    public static final class Bar {
        public String anchor;
        // Buttons in one line before the next line starts
        public int perLine;
        // False fills rows across the screen, true fills columns down the screen
        public boolean down;
        public int offsetXDp;
        public int offsetYDp;
        public final List<String> items;

        public Bar(String anchor, int perLine, boolean down, int offsetXDp, int offsetYDp, List<String> items) {
            this.anchor = ANCHORS.contains(anchor) ? anchor : TOP_LEFT;
            this.perLine = clamp(perLine, 1, GRID_MAX);
            this.down = down;
            this.offsetXDp = clamp(offsetXDp, -OFFSET_MAX_DP, OFFSET_MAX_DP);
            this.offsetYDp = clamp(offsetYDp, -OFFSET_MAX_DP, OFFSET_MAX_DP);
            this.items = new ArrayList<>(QuickBarLayout.parse(String.join(",", items)));
        }

        // NOTE: Reads a bar saved by earlier versions as a grid of rows and columns.
        // NOTE: A single row fills across and a single column fills down, keeping its length. A larger grid fills across at its column count, so every saved button shows.
        static Bar fromGrid(String anchor, int rows, int columns, int offsetXDp, int offsetYDp, List<String> items) {
            rows = clamp(rows, 1, GRID_MAX);
            columns = clamp(columns, 1, GRID_MAX);
            boolean down = columns == 1 && rows > 1;
            return new Bar(anchor, down ? rows : columns, down, offsetXDp, offsetYDp, items);
        }

        // Adds an item once. A new line starts by itself when the current one is full.
        public void add(String item) {
            if (!items.contains(item)) {
                items.add(item);
            }
        }

        public Bar copy() {
            return new Bar(anchor, perLine, down, offsetXDp, offsetYDp, items);
        }

        // Lines the bar needs for its buttons, at least one
        public int lines() {
            return Math.max(1, (items.size() + perLine - 1) / perLine);
        }

        // Rows and columns of the drawn grid, also saved so older versions still show every button
        public int rows() {
            return down ? Math.min(Math.max(1, items.size()), perLine) : lines();
        }

        public int columns() {
            return down ? lines() : Math.min(Math.max(1, items.size()), perLine);
        }

        public boolean isTop() {
            return anchor.startsWith("top_");
        }

        public boolean isBottom() {
            return anchor.startsWith("bottom_");
        }

        public boolean isLeft() {
            return anchor.endsWith("_left");
        }

        public boolean isRight() {
            return anchor.endsWith("_right");
        }
    }

    public static final class Set {
        public String name;
        public final List<Bar> bars;

        public Set(String name, List<Bar> bars) {
            this.name = name;
            this.bars = new ArrayList<>(bars);
        }

        public Set copy(String newName) {
            List<Bar> copies = new ArrayList<>();
            for (Bar bar : bars) {
                copies.add(bar.copy());
            }
            return new Set(newName, copies);
        }
    }

    public static final class State {
        public final List<Set> sets;
        public int active;

        public State(List<Set> sets, int active) {
            this.sets = new ArrayList<>(sets);
            if (this.sets.isEmpty()) {
                this.sets.add(new Set(DEFAULT_NAME, new ArrayList<>()));
            }
            this.active = clamp(active, 0, this.sets.size() - 1);
        }

        public Set activeSet() {
            return sets.get(active);
        }
    }

    static final String DEFAULT_NAME = "Default";

    private BarSets() {
    }

    static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public static State load(Context context) {
        return load(ProfilesManager.getInstance().getOverlayingSharedPreferences(context));
    }

    public static void save(Context context, State state) {
        Map<String, Object> values = new HashMap<>();
        values.put(PREF_SETS, toJson(state.sets));
        values.put(PREF_ACTIVE, state.active);
        if (!ProfilesManager.getInstance().putIntoActive(values)) {
            save(PreferenceManager.getDefaultSharedPreferences(context), state);
        }
    }

    public static void setActive(Context context, int index) {
        State state = load(context);
        state.active = clamp(index, 0, state.sets.size() - 1);
        save(context, state);
    }

    public static State load(SharedPreferences prefs) {
        String raw = prefs.getString(PREF_SETS, null);
        if (raw == null) {
            return new State(Collections.singletonList(fromCorners(prefs)), 0);
        }
        List<Set> sets = parse(raw);
        return new State(sets, prefs.getInt(PREF_ACTIVE, 0));
    }

    public static void save(SharedPreferences prefs, State state) {
        prefs.edit().putString(PREF_SETS, toJson(state.sets)).putInt(PREF_ACTIVE, state.active).apply();
    }

    public static void setActive(SharedPreferences prefs, int index) {
        State state = load(prefs);
        state.active = clamp(index, 0, state.sets.size() - 1);
        save(prefs, state);
    }

    // NOTE: A row of n buttons becomes a 1 by n grid and a column becomes n by 1, at the same corner with no offset.
    // NOTE: Empty corners are left out, so the Default set holds only the bars the user actually had.
    static Set fromCorners(SharedPreferences prefs) {
        List<Bar> bars = new ArrayList<>();
        for (String corner : QuickBarLayout.POSITIONS) {
            List<String> items = QuickBarLayout.getItems(prefs, corner);
            if (items.isEmpty()) {
                continue;
            }
            int count = clamp(items.size(), 1, GRID_MAX);
            boolean column = QuickBarLayout.DIRECTION_COLUMN.equals(QuickBarLayout.getDirection(prefs, corner));
            bars.add(new Bar(corner, count, column, 0, 0, items));
        }
        return new Set(DEFAULT_NAME, bars);
    }

    // A broken value (hand edited or from a damaged backup) reads as no sets, so the bars start empty instead of crashing the stream
    static List<Set> parse(String raw) {
        List<Set> sets = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                JSONObject set = array.getJSONObject(i);
                List<Bar> bars = new ArrayList<>();
                JSONArray barArray = set.optJSONArray("bars");
                for (int b = 0; barArray != null && b < barArray.length(); b++) {
                    JSONObject bar = barArray.getJSONObject(b);
                    List<String> items = new ArrayList<>();
                    JSONArray itemArray = bar.optJSONArray("items");
                    for (int it = 0; itemArray != null && it < itemArray.length(); it++) {
                        items.add(itemArray.optString(it, ""));
                    }
                    // Bars saved before the single "per line" setting only have rows and columns
                    if (bar.has("perLine")) {
                        bars.add(new Bar(bar.optString("anchor", TOP_LEFT), bar.optInt("perLine", GRID_MAX),
                                "down".equals(bar.optString("fill", "across")), bar.optInt("x", 0), bar.optInt("y", 0), items));
                    }
                    else {
                        bars.add(Bar.fromGrid(bar.optString("anchor", TOP_LEFT), bar.optInt("rows", 1), bar.optInt("columns", 1),
                                bar.optInt("x", 0), bar.optInt("y", 0), items));
                    }
                }
                sets.add(new Set(set.optString("name", DEFAULT_NAME), bars));
            }
        }
        catch (JSONException e) {
            sets.clear();
        }
        return sets;
    }

    static String toJson(List<Set> sets) {
        JSONArray array = new JSONArray();
        try {
            for (Set set : sets) {
                JSONArray bars = new JSONArray();
                for (Bar bar : set.bars) {
                    bars.put(new JSONObject()
                            .put("anchor", bar.anchor)
                            .put("perLine", bar.perLine)
                            .put("fill", bar.down ? "down" : "across")
                            // Older versions read these, sized so they draw every button
                            .put("rows", bar.rows())
                            .put("columns", bar.columns())
                            .put("x", bar.offsetXDp)
                            .put("y", bar.offsetYDp)
                            .put("items", new JSONArray(bar.items)));
                }
                array.put(new JSONObject().put("name", set.name).put("bars", bars));
            }
        }
        catch (JSONException e) {
            // Only reachable with a NaN number, which no field here can hold
        }
        return array.toString();
    }

    // NOTE: Items fill line by line starting in the anchor's corner, so the first item always sits nearest the anchor, and every item is shown.
    // NOTE: Across fills rows of "per line" items, down fills columns. Right anchors fill from the right, bottom anchors from the bottom up.
    // NOTE: Returns the grid as rows of items from the top row down, each row from left to right, with null for an empty cell.
    public static List<List<String>> grid(Bar bar, List<String> items) {
        int shown = items.size();
        if (shown == 0) {
            return new ArrayList<>();
        }
        int lines = (shown + bar.perLine - 1) / bar.perLine;
        // A single line is only as long as its items, so a short bar does not reserve empty cells
        int lineLength = Math.min(shown, bar.perLine);
        int usedRows = bar.down ? lineLength : lines;
        int usedColumns = bar.down ? lines : lineLength;
        List<List<String>> rows = new ArrayList<>();
        for (int r = 0; r < usedRows; r++) {
            List<String> row = new ArrayList<>();
            for (int c = 0; c < usedColumns; c++) {
                int index = bar.down ? c * bar.perLine + r : r * bar.perLine + c;
                row.add(index < shown ? items.get(index) : null);
            }
            if (bar.isRight()) {
                Collections.reverse(row);
            }
            rows.add(row);
        }
        if (bar.isBottom()) {
            Collections.reverse(rows);
        }
        return rows;
    }
}
