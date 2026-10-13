package com.limelight.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DragReorderTest {
    @Test
    public void aJumpOverSeveralRowsMovesTheItemLikeTheScreenShowsIt() {
        List<String> keys = new ArrayList<>(Arrays.asList("esc", "f11", "alt_f4", "alt_enter", "win"));
        DragReorder.moveItem(keys, 0, 3);
        assertEquals(Arrays.asList("f11", "alt_f4", "alt_enter", "esc", "win"), keys);
        DragReorder.moveItem(keys, 3, 0);
        assertEquals(Arrays.asList("esc", "f11", "alt_f4", "alt_enter", "win"), keys);
    }

    @Test
    public void edgeScrollStartsUsefulAndGrowsWithDistanceAndTime() {
        int max = 60;
        // The default curve scrolls at about 3% after one second, which looked stuck
        int start = DragReorder.edgeScroll(max, 200, 10, 0);
        assertTrue("starts at " + start, start >= max / 8);
        assertTrue(DragReorder.edgeScroll(max, 200, 150, 0) > start);
        assertEquals(max, DragReorder.edgeScroll(max, 200, 400, DragReorder.RAMP_MS));
        assertTrue(DragReorder.edgeScroll(max, 200, -10, 0) < 0);
        assertEquals(0, DragReorder.edgeScroll(max, 200, 0, 1000));
    }
}
