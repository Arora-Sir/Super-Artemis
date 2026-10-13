package com.limelight.menu;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.res.Resources;

import androidx.test.core.app.ApplicationProvider;

import com.limelight.R;
import com.limelight.actions.StreamActionRegistry;
import com.limelight.shadows.ShadowMoonBridge;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, shadows = {ShadowMoonBridge.class})
public class GameMenuEditorTest {
    private final Resources res = ApplicationProvider.getApplicationContext().getResources();

    @Test
    public void rowsListPinnedActionsFirstAndNeverSessionActions() {
        List<GameMenuEditor.Row> rows = GameMenuEditor.buildRows(Arrays.asList(
                StreamActionRegistry.ID_MICROPHONE, StreamActionRegistry.ID_DISCONNECT, StreamActionRegistry.ID_ZOOM));

        assertEquals(StreamActionRegistry.ID_MICROPHONE, rows.get(0).action.id);
        assertEquals(StreamActionRegistry.ID_ZOOM, rows.get(1).action.id);
        assertTrue(rows.get(1).inQuickMenu);
        assertFalse(rows.get(2).inQuickMenu);
        assertEquals(GameMenuLayout.configurableActions().size(), rows.size());
        for (GameMenuEditor.Row row : rows) {
            assertFalse(row.action.destructive);
        }
    }

    @Test
    public void previewShowsActionsThenSessionInRedThenNavigation() {
        List<GameMenuEditor.Row> rows = GameMenuEditor.buildRows(Arrays.asList(StreamActionRegistry.ID_ZOOM));
        List<MenuEntry> preview = GameMenuEditor.buildPreview(res, rows);

        List<String> labels = new ArrayList<>();
        for (MenuEntry entry : preview) {
            labels.add((entry.header ? "#" : "") + (entry.destructive ? "!" : "") + entry.label);
        }
        assertEquals(Arrays.asList(
                "#" + res.getString(R.string.game_menu_section_actions),
                res.getString(R.string.game_menu_item_zoom),
                "#" + res.getString(R.string.game_menu_section_session),
                "!" + res.getString(R.string.game_menu_disconnect),
                "!" + res.getString(R.string.game_menu_quit_session),
                "#" + res.getString(R.string.game_menu_section_menu),
                res.getString(R.string.game_menu_advanced),
                res.getString(R.string.game_menu_customize),
                res.getString(R.string.game_menu_customize_quick_bar),
                res.getString(R.string.game_menu_cancel)), labels);
    }

    @Test
    public void emptyActionsSectionIsLeftOut() {
        List<GameMenuEditor.Row> rows = GameMenuEditor.buildRows(new ArrayList<>());
        List<MenuEntry> preview = GameMenuEditor.buildPreview(res, rows);
        assertTrue(preview.get(0).header);
        assertEquals(res.getString(R.string.game_menu_section_session), preview.get(0).label);
    }

    @Test
    public void styleRadioMappingRoundTrips() {
        for (String style : Arrays.asList(GameMenuLayout.STYLE_LIST, GameMenuLayout.STYLE_GRID, GameMenuLayout.STYLE_COMPACT)) {
            assertEquals(style, GameMenuEditor.styleFor(GameMenuEditor.radioIdFor(style)));
        }
    }
}
