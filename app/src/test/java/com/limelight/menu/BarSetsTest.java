package com.limelight.menu;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;
import androidx.test.core.app.ApplicationProvider;

import com.limelight.actions.StreamActionRegistry;
import com.limelight.shadows.ShadowMoonBridge;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, shadows = {ShadowMoonBridge.class})
public class BarSetsTest {
    private static final String MIC = StreamActionRegistry.ID_MICROPHONE;
    private static final String ZOOM = StreamActionRegistry.ID_ZOOM;
    private static final String KEYBOARD = StreamActionRegistry.ID_TOGGLE_KEYBOARD;

    private SharedPreferences prefs;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().clear().commit();
    }

    @Test
    public void cornerBarsBecomeTheDefaultSet() {
        QuickBarLayout.setItems(prefs, QuickBarLayout.POSITION_TOP_RIGHT, Arrays.asList(ZOOM, MIC));
        QuickBarLayout.setItems(prefs, QuickBarLayout.POSITION_BOTTOM_LEFT, Arrays.asList(MIC, "key:k1", KEYBOARD));
        QuickBarLayout.setDirection(prefs, QuickBarLayout.POSITION_BOTTOM_LEFT, QuickBarLayout.DIRECTION_COLUMN);

        BarSets.State state = BarSets.load(prefs);

        assertEquals(1, state.sets.size());
        List<BarSets.Bar> bars = state.activeSet().bars;
        assertEquals(2, bars.size());
        assertEquals(BarSets.TOP_RIGHT, bars.get(0).anchor);
        assertEquals(2, bars.get(0).perLine);
        assertFalse(bars.get(0).down);
        assertEquals(BarSets.BOTTOM_LEFT, bars.get(1).anchor);
        assertEquals(3, bars.get(1).perLine);
        assertTrue(bars.get(1).down);
        assertEquals(Arrays.asList(MIC, "key:k1", KEYBOARD), bars.get(1).items);
        // The old corner keys stay for older versions
        assertEquals(Arrays.asList(ZOOM, MIC), QuickBarLayout.getItems(prefs, QuickBarLayout.POSITION_TOP_RIGHT));
    }

    @Test
    public void setsRoundTripAndTheActiveSetIsKept() {
        BarSets.Bar bar = new BarSets.Bar(BarSets.CENTER_RIGHT, 3, true, -40, 16, Arrays.asList(MIC, "key:k1"));
        BarSets.State state = new BarSets.State(Arrays.asList(
                new BarSets.Set("Desktop", Collections.singletonList(bar)),
                new BarSets.Set("Game", new ArrayList<>())), 1);
        BarSets.save(prefs, state);

        BarSets.State loaded = BarSets.load(prefs);
        assertEquals(1, loaded.active);
        assertEquals("Game", loaded.activeSet().name);
        BarSets.Bar back = loaded.sets.get(0).bars.get(0);
        assertEquals(BarSets.CENTER_RIGHT, back.anchor);
        assertEquals(3, back.perLine);
        assertTrue(back.down);
        assertEquals(-40, back.offsetXDp);
        assertEquals(16, back.offsetYDp);
        assertEquals(Arrays.asList(MIC, "key:k1"), back.items);
    }

    @Test
    public void brokenOrOutOfRangeValuesAreRepaired() {
        prefs.edit().putString(BarSets.PREF_SETS, "not json").putInt(BarSets.PREF_ACTIVE, 7).commit();
        BarSets.State state = BarSets.load(prefs);
        assertEquals(1, state.sets.size());
        assertEquals(0, state.active);

        BarSets.Bar bar = new BarSets.Bar("middle", 99, false, 9999, -9999, Arrays.asList(MIC, MIC, StreamActionRegistry.ID_DISCONNECT, "removed"));
        assertEquals(BarSets.TOP_LEFT, bar.anchor);
        assertEquals(BarSets.GRID_MAX, bar.perLine);
        assertEquals(1, new BarSets.Bar(BarSets.TOP_LEFT, 0, false, 0, 0, new ArrayList<>()).perLine);
        assertEquals(BarSets.OFFSET_MAX_DP, bar.offsetXDp);
        assertEquals(-BarSets.OFFSET_MAX_DP, bar.offsetYDp);
        // Duplicates, session actions and unknown actions never reach a bar
        assertEquals(Collections.singletonList(MIC), bar.items);
    }

    @Test
    public void gridFillsFromTheAnchorCorner() {
        List<String> items = Arrays.asList("a", "b", "c", "d", "e");
        BarSets.Bar topLeft = new BarSets.Bar(BarSets.TOP_LEFT, 3, false, 0, 0, new ArrayList<>());
        assertEquals(Arrays.asList(Arrays.asList("a", "b", "c"), Arrays.asList("d", "e", null)), BarSets.grid(topLeft, items));

        BarSets.Bar bottomRight = new BarSets.Bar(BarSets.BOTTOM_RIGHT, 3, false, 0, 0, new ArrayList<>());
        assertEquals(Arrays.asList(Arrays.asList(null, "e", "d"), Arrays.asList("c", "b", "a")), BarSets.grid(bottomRight, items));

        // Down fills columns: three per column, the rest starts the next column to the right
        BarSets.Bar down = new BarSets.Bar(BarSets.TOP_LEFT, 3, true, 0, 0, new ArrayList<>());
        assertEquals(Arrays.asList(Arrays.asList("a", "d"), Arrays.asList("b", "e"), Arrays.asList("c", null)), BarSets.grid(down, items));

        // A single line is only as long as its items
        BarSets.Bar wide = new BarSets.Bar(BarSets.TOP_CENTER, 8, false, 0, 0, new ArrayList<>());
        assertEquals(Collections.singletonList(Arrays.asList("a", "b")), BarSets.grid(wide, Arrays.asList("a", "b")));
        assertTrue(BarSets.grid(wide, new ArrayList<>()).isEmpty());
    }

    // The owner's example: 8 buttons at 5 per line show as 5 and 3, and no button is ever left out
    @Test
    public void everyButtonShowsWhateverTheLineLength() {
        List<String> eight = Arrays.asList("1", "2", "3", "4", "5", "6", "7", "8");
        BarSets.Bar bar = new BarSets.Bar(BarSets.TOP_LEFT, 10, false, 0, 0, new ArrayList<>());
        assertEquals(1, BarSets.grid(bar, eight).size());
        assertEquals(8, BarSets.grid(bar, eight).get(0).size());

        bar.perLine = 5;
        List<List<String>> grid = BarSets.grid(bar, eight);
        assertEquals(Arrays.asList("1", "2", "3", "4", "5"), grid.get(0));
        assertEquals(Arrays.asList("6", "7", "8", null, null), grid.get(1));

        for (int perLine = 1; perLine <= BarSets.GRID_MAX; perLine++) {
            for (boolean down : new boolean[] {false, true}) {
                BarSets.Bar any = new BarSets.Bar(BarSets.BOTTOM_RIGHT, perLine, down, 0, 0, new ArrayList<>());
                int shown = 0;
                for (List<String> row : BarSets.grid(any, eight)) {
                    for (String cell : row) {
                        shown += cell != null ? 1 : 0;
                    }
                }
                assertEquals("per line " + perLine + (down ? " down" : " across"), 8, shown);
            }
        }
    }

    // Bars saved by earlier versions as rows and columns keep showing every button
    @Test
    public void gridBarsFromEarlierVersionsMigrate() throws Exception {
        String saved = "[{\"name\":\"Default\",\"bars\":["
                + "{\"anchor\":\"top_left\",\"rows\":1,\"columns\":6,\"items\":[\"" + MIC + "\"]},"
                + "{\"anchor\":\"center_right\",\"rows\":4,\"columns\":1,\"items\":[\"" + MIC + "\"]},"
                + "{\"anchor\":\"bottom_left\",\"rows\":2,\"columns\":3,\"items\":[\"" + MIC + "\"]}]}]";
        prefs.edit().putString(BarSets.PREF_SETS, saved).commit();
        List<BarSets.Bar> bars = BarSets.load(prefs).activeSet().bars;
        assertEquals(6, bars.get(0).perLine);
        assertFalse(bars.get(0).down);
        assertEquals(4, bars.get(1).perLine);
        assertTrue(bars.get(1).down);
        assertEquals(3, bars.get(2).perLine);
        assertFalse(bars.get(2).down);

        // Saving writes rows and columns that cover every button, for older versions
        BarSets.Bar many = new BarSets.Bar(BarSets.TOP_LEFT, 5, false, 0, 0, Arrays.asList(MIC, ZOOM, KEYBOARD));
        assertEquals(1, many.rows());
        assertEquals(3, many.columns());
        BarSets.save(prefs, new BarSets.State(Collections.singletonList(new BarSets.Set("A", Collections.singletonList(many))), 0));
        org.json.JSONObject json = new org.json.JSONArray(prefs.getString(BarSets.PREF_SETS, "")).getJSONObject(0).getJSONArray("bars").getJSONObject(0);
        assertEquals(5, json.getInt("perLine"));
        assertEquals("across", json.getString("fill"));
        assertTrue(json.getInt("rows") * json.getInt("columns") >= 3);
    }

    @Test
    public void addingAButtonKeepsItOnceAndNeverDropsOne() {
        BarSets.Bar row = new BarSets.Bar(BarSets.TOP_LEFT, 1, false, 0, 0, Collections.singletonList(MIC));
        row.add(ZOOM);
        row.add(ZOOM);
        assertEquals(Arrays.asList(MIC, ZOOM), row.items);
        assertEquals(2, row.lines());
    }

    @Test
    public void duplicateSetCopiesEveryBar() {
        BarSets.Set set = new BarSets.Set("Desktop", Collections.singletonList(new BarSets.Bar(BarSets.TOP_LEFT, 2, false, 0, 0, Arrays.asList(MIC, ZOOM))));
        BarSets.Set copy = set.copy("Desktop copy");
        copy.bars.get(0).items.add(KEYBOARD);
        assertEquals(2, set.bars.get(0).items.size());
        assertTrue(copy.bars.get(0).items.contains(KEYBOARD));
    }
}
