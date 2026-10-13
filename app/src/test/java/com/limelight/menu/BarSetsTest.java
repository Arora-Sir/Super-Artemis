package com.limelight.menu;

import static org.junit.Assert.assertEquals;
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
        assertEquals(1, bars.get(0).rows);
        assertEquals(2, bars.get(0).columns);
        assertEquals(BarSets.BOTTOM_LEFT, bars.get(1).anchor);
        assertEquals(3, bars.get(1).rows);
        assertEquals(1, bars.get(1).columns);
        assertEquals(Arrays.asList(MIC, "key:k1", KEYBOARD), bars.get(1).items);
        // The old corner keys stay for older versions
        assertEquals(Arrays.asList(ZOOM, MIC), QuickBarLayout.getItems(prefs, QuickBarLayout.POSITION_TOP_RIGHT));
    }

    @Test
    public void setsRoundTripAndTheActiveSetIsKept() {
        BarSets.Bar bar = new BarSets.Bar(BarSets.CENTER_RIGHT, 2, 3, -40, 16, Arrays.asList(MIC, "key:k1"));
        BarSets.State state = new BarSets.State(Arrays.asList(
                new BarSets.Set("Desktop", Collections.singletonList(bar)),
                new BarSets.Set("Game", new ArrayList<>())), 1);
        BarSets.save(prefs, state);

        BarSets.State loaded = BarSets.load(prefs);
        assertEquals(1, loaded.active);
        assertEquals("Game", loaded.activeSet().name);
        BarSets.Bar back = loaded.sets.get(0).bars.get(0);
        assertEquals(BarSets.CENTER_RIGHT, back.anchor);
        assertEquals(2, back.rows);
        assertEquals(3, back.columns);
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

        BarSets.Bar bar = new BarSets.Bar("middle", 0, 99, 9999, -9999, Arrays.asList(MIC, MIC, StreamActionRegistry.ID_DISCONNECT, "removed"));
        assertEquals(BarSets.TOP_LEFT, bar.anchor);
        assertEquals(1, bar.rows);
        assertEquals(BarSets.GRID_MAX, bar.columns);
        assertEquals(BarSets.OFFSET_MAX_DP, bar.offsetXDp);
        assertEquals(-BarSets.OFFSET_MAX_DP, bar.offsetYDp);
        // Duplicates, session actions and unknown actions never reach a bar
        assertEquals(Collections.singletonList(MIC), bar.items);
    }

    @Test
    public void gridFillsFromTheAnchorCorner() {
        List<String> items = Arrays.asList("a", "b", "c", "d", "e");
        BarSets.Bar topLeft = new BarSets.Bar(BarSets.TOP_LEFT, 2, 3, 0, 0, new ArrayList<>());
        assertEquals(Arrays.asList(Arrays.asList("a", "b", "c"), Arrays.asList("d", "e", null)), BarSets.grid(topLeft, items));

        BarSets.Bar bottomRight = new BarSets.Bar(BarSets.BOTTOM_RIGHT, 2, 3, 0, 0, new ArrayList<>());
        assertEquals(Arrays.asList(Arrays.asList(null, "e", "d"), Arrays.asList("c", "b", "a")), BarSets.grid(bottomRight, items));

        // Items past the grid are kept but not drawn, and a single used row is only as wide as its items
        BarSets.Bar small = new BarSets.Bar(BarSets.CENTER, 1, 2, 0, 0, new ArrayList<>());
        assertEquals(Collections.singletonList(Arrays.asList("a", "b")), BarSets.grid(small, items));
        BarSets.Bar wide = new BarSets.Bar(BarSets.TOP_CENTER, 1, 8, 0, 0, new ArrayList<>());
        assertEquals(Collections.singletonList(Arrays.asList("a", "b")), BarSets.grid(wide, Arrays.asList("a", "b")));
    }

    @Test
    public void addingToAFullBarGrowsItAlongItsSide() {
        BarSets.Bar row = new BarSets.Bar(BarSets.TOP_LEFT, 1, 1, 0, 0, Collections.singletonList(MIC));
        row.add(ZOOM);
        assertEquals(1, row.rows);
        assertEquals(2, row.columns);

        BarSets.Bar column = new BarSets.Bar(BarSets.TOP_LEFT, 2, 1, 0, 0, Arrays.asList(MIC, ZOOM));
        column.add(KEYBOARD);
        assertEquals(3, column.rows);
        assertEquals(1, column.columns);

        column.add(KEYBOARD);
        assertEquals(3, column.items.size());
    }

    @Test
    public void duplicateSetCopiesEveryBar() {
        BarSets.Set set = new BarSets.Set("Desktop", Collections.singletonList(new BarSets.Bar(BarSets.TOP_LEFT, 1, 2, 0, 0, Arrays.asList(MIC, ZOOM))));
        BarSets.Set copy = set.copy("Desktop copy");
        copy.bars.get(0).items.add(KEYBOARD);
        assertEquals(2, set.bars.get(0).items.size());
        assertTrue(copy.bars.get(0).items.contains(KEYBOARD));
    }
}
