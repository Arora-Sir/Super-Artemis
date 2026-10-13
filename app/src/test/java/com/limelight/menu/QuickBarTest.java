package com.limelight.menu;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;
import androidx.test.core.app.ApplicationProvider;

import com.limelight.actions.StreamActionRegistry;
import com.limelight.customkeys.CustomKey;
import com.limelight.shadows.ShadowMoonBridge;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, shadows = {ShadowMoonBridge.class})
public class QuickBarTest {
    private Context context;
    private SharedPreferences prefs;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().clear().commit();
    }

    @Test
    public void everyCornerIsEmptyAndARowByDefault() {
        for (String corner : QuickBarLayout.POSITIONS) {
            assertTrue(QuickBarLayout.getItems(prefs, corner).isEmpty());
            assertEquals(QuickBarLayout.DIRECTION_ROW, QuickBarLayout.getDirection(prefs, corner));
        }
    }

    @Test
    public void cornersKeepTheirOwnItemsAndDirections() {
        QuickBarLayout.setItems(prefs, QuickBarLayout.POSITION_TOP_LEFT, Arrays.asList(StreamActionRegistry.ID_ZOOM));
        QuickBarLayout.setItems(prefs, QuickBarLayout.POSITION_BOTTOM_RIGHT, Arrays.asList(StreamActionRegistry.ID_MICROPHONE, "key:builtin_esc"));
        QuickBarLayout.setDirection(prefs, QuickBarLayout.POSITION_BOTTOM_RIGHT, QuickBarLayout.DIRECTION_COLUMN);

        assertEquals(Arrays.asList(StreamActionRegistry.ID_ZOOM), QuickBarLayout.getItems(prefs, QuickBarLayout.POSITION_TOP_LEFT));
        assertEquals(Arrays.asList(StreamActionRegistry.ID_MICROPHONE, "key:builtin_esc"),
                QuickBarLayout.getItems(prefs, QuickBarLayout.POSITION_BOTTOM_RIGHT));
        assertEquals(QuickBarLayout.DIRECTION_ROW, QuickBarLayout.getDirection(prefs, QuickBarLayout.POSITION_TOP_LEFT));
        assertEquals(QuickBarLayout.DIRECTION_COLUMN, QuickBarLayout.getDirection(prefs, QuickBarLayout.POSITION_BOTTOM_RIGHT));
    }

    @Test
    public void itemsKeepOrderAndDropSessionUnknownAndDuplicateEntries() {
        QuickBarLayout.setItems(prefs, QuickBarLayout.POSITION_TOP_RIGHT, Arrays.asList(StreamActionRegistry.ID_MICROPHONE, "key:k1",
                StreamActionRegistry.ID_QUIT, "removed_action", "key:", StreamActionRegistry.ID_MICROPHONE, StreamActionRegistry.ID_ZOOM));
        assertEquals(Arrays.asList(StreamActionRegistry.ID_MICROPHONE, "key:k1", StreamActionRegistry.ID_ZOOM),
                QuickBarLayout.getItems(prefs, QuickBarLayout.POSITION_TOP_RIGHT));
    }

    @Test
    public void singleBarMovesToItsOldCornerAndOverlayZoomButtonBecomesAPin() {
        prefs.edit()
                .putString(QuickBarLayout.LEGACY_ITEMS, StreamActionRegistry.ID_MICROPHONE + ",key:k1")
                .putString(QuickBarLayout.LEGACY_POSITION, QuickBarLayout.POSITION_BOTTOM_LEFT)
                .putBoolean(QuickBarLayout.LEGACY_ZOOM_BUTTON, true)
                .commit();

        assertEquals(Arrays.asList(StreamActionRegistry.ID_MICROPHONE, "key:k1"),
                QuickBarLayout.getItems(prefs, QuickBarLayout.POSITION_BOTTOM_LEFT));
        assertEquals(Collections.singletonList(StreamActionRegistry.ID_ZOOM),
                QuickBarLayout.getItems(prefs, QuickBarLayout.POSITION_TOP_RIGHT));
        assertFalse(prefs.contains(QuickBarLayout.LEGACY_ITEMS));
        assertFalse(prefs.contains(QuickBarLayout.LEGACY_POSITION));
        assertFalse(prefs.contains(QuickBarLayout.LEGACY_ZOOM_BUTTON));
    }

    @Test
    public void overlayZoomButtonIsNotPinnedTwice() {
        prefs.edit()
                .putString(QuickBarLayout.LEGACY_ITEMS, StreamActionRegistry.ID_ZOOM)
                .putBoolean(QuickBarLayout.LEGACY_ZOOM_BUTTON, true)
                .commit();
        assertEquals(Collections.singletonList(StreamActionRegistry.ID_ZOOM), QuickBarLayout.getItems(prefs, QuickBarLayout.POSITION_TOP_LEFT));
        assertTrue(QuickBarLayout.getItems(prefs, QuickBarLayout.POSITION_TOP_RIGHT).isEmpty());
    }

    @Test
    public void specialKeyItemsRoundTrip() {
        assertEquals("k1", QuickBarLayout.customKeyId(QuickBarLayout.customKeyItem("k1")));
        assertEquals("builtin_esc", QuickBarLayout.customKeyId(QuickBarLayout.customKeyItem("builtin_esc")));
        assertNull(QuickBarLayout.customKeyId(StreamActionRegistry.ID_ZOOM));
    }

    @Test
    public void editorListsPinnedFirstThenEveryActionAndKeyWithoutSessionActions() {
        List<CustomKey> keys = Collections.singletonList(new CustomKey("k1", "Task view", false, "desktop_windows", Arrays.asList(0x5B, 0x09)));
        List<QuickBarEditor.Row> rows = QuickBarEditor.buildRows(context,
                Arrays.asList("key:k1", StreamActionRegistry.ID_MICROPHONE, "key:gone"), keys);

        assertEquals("key:k1", rows.get(0).item);
        assertTrue(rows.get(0).pinned);
        assertEquals(StreamActionRegistry.ID_MICROPHONE, rows.get(1).item);
        assertFalse(rows.get(2).pinned);
        assertEquals(GameMenuLayout.configurableActions().size() + 1, rows.size());
        assertEquals(Arrays.asList("key:k1", StreamActionRegistry.ID_MICROPHONE), QuickBarEditor.pinnedItems(rows));
        for (QuickBarEditor.Row row : rows) {
            assertFalse(row.item.equals(StreamActionRegistry.ID_DISCONNECT) || row.item.equals(StreamActionRegistry.ID_QUIT));
        }
    }
}
