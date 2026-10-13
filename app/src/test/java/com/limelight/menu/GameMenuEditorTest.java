package com.limelight.menu;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.res.Resources;

import androidx.test.core.app.ApplicationProvider;

import com.limelight.R;
import com.limelight.actions.StreamActionRegistry;
import com.limelight.customkeys.CustomKey;
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
    private static final List<CustomKey> NO_KEYS = new ArrayList<>();
    private final Context context = ApplicationProvider.getApplicationContext();
    private final Resources res = context.getResources();

    @Test
    public void specialKeysCanBePinnedNextToActions() {
        CustomKey copy = new CustomKey("k1", "Copy", false, "content_copy", Arrays.asList(0xA2, 0x43));
        CustomKey paste = new CustomKey("k2", "Paste", false, null, Arrays.asList(0xA2, 0x56));
        List<GameMenuEditor.Row> rows = GameMenuEditor.buildRows(context, Arrays.asList(
                GameMenuLayout.keyItem("k1"), StreamActionRegistry.ID_ZOOM, GameMenuLayout.keyItem("gone")), Arrays.asList(copy, paste));

        assertEquals("key:k1", rows.get(0).item);
        assertTrue(rows.get(0).inQuickMenu);
        assertEquals(null, rows.get(0).action);
        assertEquals(StreamActionRegistry.ID_ZOOM, rows.get(1).item);
        // A pinned key that no longer exists is left out, and unpinned keys come last
        assertEquals("key:k2", rows.get(rows.size() - 1).item);
        assertFalse(rows.get(rows.size() - 1).inQuickMenu);
        assertEquals(GameMenuLayout.configurableActions().size() + 2, rows.size());

        List<MenuEntry> preview = GameMenuEditor.buildPreview(res, rows);
        assertEquals(res.getString(R.string.quick_bar_custom_key, "Copy"), preview.get(1).label);
    }

    @Test
    public void rowsListPinnedActionsFirstAndNeverSessionActions() {
        List<GameMenuEditor.Row> rows = GameMenuEditor.buildRows(context, Arrays.asList(
                StreamActionRegistry.ID_MICROPHONE, StreamActionRegistry.ID_DISCONNECT, StreamActionRegistry.ID_ZOOM), NO_KEYS);

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
        List<GameMenuEditor.Row> rows = GameMenuEditor.buildRows(context, Arrays.asList(StreamActionRegistry.ID_ZOOM), NO_KEYS);
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
        List<GameMenuEditor.Row> rows = GameMenuEditor.buildRows(context, new ArrayList<>(), NO_KEYS);
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
