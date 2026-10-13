package com.limelight.menu;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
    public void barIsEmptyAndTopLeftByDefault() {
        assertTrue(QuickBarLayout.getItems(prefs).isEmpty());
        assertEquals(QuickBarLayout.POSITION_TOP_LEFT, QuickBarLayout.getPosition(prefs));
    }

    @Test
    public void itemsKeepOrderAndDropSessionUnknownAndDuplicateEntries() {
        QuickBarLayout.setItems(prefs, Arrays.asList(StreamActionRegistry.ID_MICROPHONE, "key:k1", StreamActionRegistry.ID_QUIT,
                "removed_action", "key:", StreamActionRegistry.ID_MICROPHONE, StreamActionRegistry.ID_ZOOM));
        assertEquals(Arrays.asList(StreamActionRegistry.ID_MICROPHONE, "key:k1", StreamActionRegistry.ID_ZOOM), QuickBarLayout.getItems(prefs));
    }

    @Test
    public void positionRoundTripsAndUnknownFallsBackToTopLeft() {
        for (String position : QuickBarLayout.POSITIONS) {
            QuickBarLayout.setPosition(prefs, position);
            assertEquals(position, QuickBarLayout.getPosition(prefs));
            assertEquals(position, QuickBarEditor.positionFor(QuickBarEditor.radioIdFor(position)));
        }
        prefs.edit().putString(QuickBarLayout.PREF_POSITION, "center").commit();
        assertEquals(QuickBarLayout.POSITION_TOP_LEFT, QuickBarLayout.getPosition(prefs));
    }

    @Test
    public void customKeyItemsRoundTrip() {
        assertEquals("k1", QuickBarLayout.customKeyId(QuickBarLayout.customKeyItem("k1")));
        assertEquals(null, QuickBarLayout.customKeyId(StreamActionRegistry.ID_ZOOM));
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
        for (QuickBarEditor.Row row : rows) {
            assertFalse(row.item.equals(StreamActionRegistry.ID_DISCONNECT) || row.item.equals(StreamActionRegistry.ID_QUIT));
        }
    }
}
