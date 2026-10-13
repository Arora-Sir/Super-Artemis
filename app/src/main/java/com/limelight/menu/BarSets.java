package com.limelight.menu;

import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

// NOTE: Quick-action bars as named sets. A set holds any number of bars, and each bar has an anchor, a grid of rows and columns, an offset and its items.
// NOTE: One set is active at a time and the Quick Menu switches between sets, so a game layout and a desktop layout can each keep their own bars.
// NOTE: The first load turns the four corner bars of earlier versions into a "Default" set. Their old keys stay untouched, so an older version still reads them.
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

    public static final int GRID_MAX = 20;
    // Far enough to cross a phone screen from any anchor, near enough that a bar never gets lost off screen
    public static final int OFFSET_MAX_DP = 400;

    public static final class Bar {
        public String anchor;
        public int rows;
        public int columns;
        public int offsetXDp;
        public int offsetYDp;
        public final List<String> items;

        public Bar(String anchor, int rows, int columns, int offsetXDp, int offsetYDp, List<String> items) {
            this.anchor = ANCHORS.contains(anchor) ? anchor : TOP_LEFT;
            this.rows = clamp(rows, 1, GRID_MAX);
            this.columns = clamp(columns, 1, GRID_MAX);
            this.offsetXDp = clamp(offsetXDp, -OFFSET_MAX_DP, OFFSET_MAX_DP);
            this.offsetYDp = clamp(offsetYDp, -OFFSET_MAX_DP, OFFSET_MAX_DP);
            this.items = new ArrayList<>(QuickBarLayout.parse(String.join(",", items)));
        }

        // Adds an item once and grows the grid when it is full, along the side the bar already runs
        public void add(String item) {
            if (items.contains(item)) {
                return;
            }
            items.add(item);
            if (items.size() > capacity()) {
                if (rows == 1 || (columns != 1 && columns <= rows)) {
                    columns = Math.min(GRID_MAX, columns + 1);
                }
                else {
                    rows = Math.min(GRID_MAX, rows + 1);
                }
            }
        }

        public Bar copy() {
            return new Bar(anchor, rows, columns, offsetXDp, offsetYDp, items);
        }

        // How many buttons the grid has room for, extra items stay saved but are not drawn
        public int capacity() {
            return rows * columns;
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
            bars.add(new Bar(corner, column ? count : 1, column ? 1 : count, 0, 0, items));
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
                    bars.add(new Bar(bar.optString("anchor", TOP_LEFT), bar.optInt("rows", 1), bar.optInt("columns", 1),
                            bar.optInt("x", 0), bar.optInt("y", 0), items));
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
                            .put("rows", bar.rows)
                            .put("columns", bar.columns)
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

    // NOTE: Items fill the grid row by row starting in the anchor's corner, so the first item always sits nearest the anchor.
    // NOTE: Right anchors fill each row from the right, bottom anchors fill rows from the bottom up, and center anchors fill from the top left.
    // NOTE: Returns the grid as rows of items from the top row down, each row from left to right, with null for an empty cell.
    public static List<List<String>> grid(Bar bar, List<String> items) {
        int shown = Math.min(items.size(), bar.capacity());
        int usedRows = shown == 0 ? 0 : (shown + bar.columns - 1) / bar.columns;
        // A grid with one used row is only as wide as its items, so a short bar does not reserve empty cells
        int usedColumns = usedRows == 1 ? shown : bar.columns;
        List<List<String>> rows = new ArrayList<>();
        for (int r = 0; r < usedRows; r++) {
            List<String> row = new ArrayList<>();
            for (int c = 0; c < usedColumns; c++) {
                int index = r * bar.columns + c;
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
