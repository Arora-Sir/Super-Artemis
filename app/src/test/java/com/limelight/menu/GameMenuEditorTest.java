package com.limelight.menu;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;
import androidx.test.core.app.ApplicationProvider;

import com.limelight.R;
import com.limelight.actions.StreamActionRegistry;
import com.limelight.customkeys.CustomKey;
import com.limelight.shadows.ShadowMoonBridge;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowDialog;
import org.robolectric.shadows.ShadowLooper;

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
    public void hiddenActionsAreMarkedOnlyOutsideTheQuickMenu() {
        java.util.Set<String> hidden = new java.util.HashSet<>(Arrays.asList(StreamActionRegistry.ID_ZOOM, StreamActionRegistry.ID_ROTATE_SCREEN));
        List<GameMenuEditor.Row> rows = GameMenuEditor.buildRows(context, Arrays.asList(StreamActionRegistry.ID_ZOOM), hidden, NO_KEYS);

        assertFalse(rows.get(0).hidden);
        for (GameMenuEditor.Row row : rows) {
            if (StreamActionRegistry.ID_ROTATE_SCREEN.equals(row.item)) {
                assertTrue(row.hidden);
            }
        }
    }

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
        assertEquals(GameMenuLayout.menuActions().size() + 2, rows.size());

        List<MenuEntry> preview = GameMenuEditor.buildPreview(res, rows);
        // The preview shows the key name like the real Quick Menu, without the "Key:" prefix of the editor list
        // A named shortcut reads with its keys, like every other list (KeyLabels)
        assertEquals("Ctrl + C (Copy)", preview.get(1).label);
    }

    @Test
    public void unpinnedKeysAreHiddenFromTheAdvancedMenuUntilTheirEyeIsOn() {
        CustomKey copy = new CustomKey("k1", "Copy", false, "content_copy", Arrays.asList(0xA2, 0x43));
        CustomKey paste = new CustomKey("k2", "Paste", false, null, Arrays.asList(0xA2, 0x56));
        List<GameMenuEditor.Row> rows = GameMenuEditor.buildRows(context, new ArrayList<>(), new java.util.HashSet<>(),
                Arrays.asList(GameMenuLayout.keyItem("k2")), Arrays.asList(copy, paste));
        GameMenuEditor.Row copyRow = rows.get(rows.size() - 2);
        GameMenuEditor.Row pasteRow = rows.get(rows.size() - 1);
        assertEquals("key:k1", copyRow.item);
        assertTrue(copyRow.hidden);
        assertFalse(pasteRow.hidden);

        List<MenuEntry> advanced = GameMenuEditor.buildAdvancedPreview(res, rows);
        boolean copyShown = false;
        boolean pasteShown = false;
        for (MenuEntry entry : advanced) {
            copyShown |= entry.label.contains("Copy");
            pasteShown |= entry.label.contains("Paste");
        }
        assertFalse(copyShown);
        assertTrue(pasteShown);
    }

    @Test
    public void theAdvancedPreviewListsNavigationAndLeavesOutPinnedAndHiddenActions() {
        java.util.Set<String> hidden = new java.util.HashSet<>(Arrays.asList(StreamActionRegistry.ID_ROTATE_SCREEN));
        List<GameMenuEditor.Row> rows = GameMenuEditor.buildRows(context, Arrays.asList(StreamActionRegistry.ID_ZOOM), hidden, NO_KEYS);
        List<MenuEntry> advanced = GameMenuEditor.buildAdvancedPreview(res, rows);
        List<String> labels = new ArrayList<>();
        for (MenuEntry entry : advanced) {
            labels.add(entry.label);
        }
        assertFalse(labels.contains(res.getString(StreamActionRegistry.find(StreamActionRegistry.ID_ZOOM).nameRes)));
        assertFalse(labels.contains(res.getString(StreamActionRegistry.find(StreamActionRegistry.ID_ROTATE_SCREEN).nameRes)));
        assertTrue(labels.contains(res.getString(R.string.game_menu_back_to_quick_menu)));
    }

    @Test
    public void rowsListPinnedActionsFirstAndNeverSessionActions() {
        List<GameMenuEditor.Row> rows = GameMenuEditor.buildRows(context, Arrays.asList(
                StreamActionRegistry.ID_MICROPHONE, StreamActionRegistry.ID_DISCONNECT, StreamActionRegistry.ID_ZOOM), NO_KEYS);

        assertEquals(StreamActionRegistry.ID_MICROPHONE, rows.get(0).action.id);
        assertEquals(StreamActionRegistry.ID_ZOOM, rows.get(1).action.id);
        assertTrue(rows.get(1).inQuickMenu);
        assertFalse(rows.get(2).inQuickMenu);
        assertEquals(GameMenuLayout.menuActions().size(), rows.size());
        for (GameMenuEditor.Row row : rows) {
            assertFalse(row.action.destructive);
        }
    }

    @Test
    public void settingsStaysOutOfTheMenuListsButNotOutOfBarsAndGestures() {
        // The Quick Menu always shows Settings in its Menu section, so the editor and the Advanced Menu never list it again
        List<GameMenuEditor.Row> rows = GameMenuEditor.buildRows(context, Arrays.asList(StreamActionRegistry.ID_SETTINGS), NO_KEYS);
        for (GameMenuEditor.Row row : rows) {
            assertFalse(StreamActionRegistry.ID_SETTINGS.equals(row.item));
        }
        boolean inConfigurable = false;
        for (com.limelight.actions.StreamAction action : GameMenuLayout.configurableActions()) {
            inConfigurable |= StreamActionRegistry.ID_SETTINGS.equals(action.id);
        }
        assertTrue(inConfigurable);
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
                res.getString(R.string.game_menu_settings),
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

    @Test
    public void previewShowsSectionsInCustomOrder() {
        List<GameMenuEditor.Row> rows = GameMenuEditor.buildRows(context, Arrays.asList(StreamActionRegistry.ID_ZOOM), NO_KEYS);
        List<MenuEntry> preview = GameMenuEditor.buildPreview(res, rows, Arrays.asList(
                GameMenuLayout.SECTION_SESSION, GameMenuLayout.SECTION_MENU, GameMenuLayout.SECTION_ACTIONS));

        List<String> headers = new ArrayList<>();
        for (MenuEntry entry : preview) {
            if (entry.header) {
                headers.add(entry.label);
            }
        }
        assertEquals(Arrays.asList(
                res.getString(R.string.game_menu_section_session),
                res.getString(R.string.game_menu_section_menu),
                res.getString(R.string.game_menu_section_actions)), headers);
    }

    @Test
    public void editorPersistsSectionOrderOnSave() {
        AppCompatActivity activity = Robolectric.buildActivity(AppCompatActivity.class).create().get();
        activity.setTheme(R.style.AppTheme_MaterialYou);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        prefs.edit().clear().commit();

        assertEquals(GameMenuLayout.DEFAULT_SECTION_ORDER, GameMenuLayout.getSectionOrder(prefs));

        final boolean[] changed = {false};
        GameMenuEditor.show(activity, prefs, () -> changed[0] = true);
        ShadowLooper.idleMainLooper();
        android.app.AlertDialog dialog = (android.app.AlertDialog) ShadowDialog.getLatestDialog();

        // Switch to the section order tab
        dialog.findViewById(R.id.game_menu_editor_tab_sections).performClick();
        assertTrue(dialog.findViewById(R.id.game_menu_editor_tab_sections).isSelected());
        assertFalse(dialog.findViewById(R.id.game_menu_editor_tab_actions).isSelected());

        // Find the sections RecyclerView
        android.widget.FrameLayout listContainer = dialog.findViewById(R.id.game_menu_editor_list);
        android.widget.LinearLayout sectionsColumn = (android.widget.LinearLayout) listContainer.getChildAt(1);
        assertEquals(android.view.View.VISIBLE, sectionsColumn.getVisibility());
        androidx.recyclerview.widget.RecyclerView sectionsList = (androidx.recyclerview.widget.RecyclerView) sectionsColumn.getChildAt(0);
        assertEquals(3, sectionsList.getAdapter().getItemCount());

        // Save dialog
        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).performClick();
        ShadowLooper.idleMainLooper();
        assertTrue(changed[0]);
        assertEquals(GameMenuLayout.DEFAULT_SECTION_ORDER, GameMenuLayout.getSectionOrder(prefs));
    }

    @Test
    public void editorRevertsSectionOrderOnReset() {
        AppCompatActivity activity = Robolectric.buildActivity(AppCompatActivity.class).create().get();
        activity.setTheme(R.style.AppTheme_MaterialYou);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        prefs.edit().clear().commit();
        GameMenuLayout.setSectionOrder(prefs, Arrays.asList(
                GameMenuLayout.SECTION_SESSION, GameMenuLayout.SECTION_MENU, GameMenuLayout.SECTION_ACTIONS));

        final boolean[] changed = {false};
        GameMenuEditor.show(activity, prefs, () -> changed[0] = true);
        ShadowLooper.idleMainLooper();
        android.app.AlertDialog dialog = (android.app.AlertDialog) ShadowDialog.getLatestDialog();

        dialog.getButton(android.app.AlertDialog.BUTTON_NEUTRAL).performClick();
        ShadowLooper.idleMainLooper();
        assertTrue(changed[0]);
        assertEquals(GameMenuLayout.DEFAULT_SECTION_ORDER, GameMenuLayout.getSectionOrder(prefs));
    }

    @Test
    public void editorDiscardsSectionOrderOnCancel() {
        AppCompatActivity activity = Robolectric.buildActivity(AppCompatActivity.class).create().get();
        activity.setTheme(R.style.AppTheme_MaterialYou);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        prefs.edit().clear().commit();
        List<String> initialOrder = Arrays.asList(
                GameMenuLayout.SECTION_MENU, GameMenuLayout.SECTION_ACTIONS, GameMenuLayout.SECTION_SESSION);
        GameMenuLayout.setSectionOrder(prefs, initialOrder);

        final boolean[] changed = {false};
        GameMenuEditor.show(activity, prefs, () -> changed[0] = true);
        ShadowLooper.idleMainLooper();
        android.app.AlertDialog dialog = (android.app.AlertDialog) ShadowDialog.getLatestDialog();

        dialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE).performClick();
        ShadowLooper.idleMainLooper();
        assertFalse(changed[0]);
        assertEquals(initialOrder, GameMenuLayout.getSectionOrder(prefs));
    }

    @Test
    public void switchingToSectionOrderTabSwapsPreviewBackToQuickMenu() {
        AppCompatActivity activity = Robolectric.buildActivity(AppCompatActivity.class).create().get();
        activity.setTheme(R.style.AppTheme_MaterialYou);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        prefs.edit().clear().commit();

        GameMenuEditor.show(activity, prefs, () -> { });
        ShadowLooper.idleMainLooper();
        android.app.AlertDialog dialog = (android.app.AlertDialog) ShadowDialog.getLatestDialog();

        android.view.View previewQuick = dialog.findViewById(R.id.game_menu_editor_preview_quick);
        android.view.View previewAdvanced = dialog.findViewById(R.id.game_menu_editor_preview_advanced);
        android.view.View tabActions = dialog.findViewById(R.id.game_menu_editor_tab_actions);
        android.view.View tabSections = dialog.findViewById(R.id.game_menu_editor_tab_sections);

        assertTrue(previewQuick.isSelected());
        assertTrue(tabActions.isSelected());

        previewAdvanced.performClick();
        assertTrue(previewAdvanced.isSelected());
        assertFalse(previewQuick.isSelected());

        tabSections.performClick();
        assertTrue(tabSections.isSelected());
        assertFalse(tabActions.isSelected());
        assertTrue(previewQuick.isSelected());
        assertFalse(previewAdvanced.isSelected());

        dialog.dismiss();
    }
}
