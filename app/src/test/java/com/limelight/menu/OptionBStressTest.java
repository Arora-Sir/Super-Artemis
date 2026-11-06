package com.limelight.menu;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ApplicationProvider;

import com.limelight.R;
import com.limelight.shadows.ShadowMoonBridge;
import com.limelight.ui.DragReorder;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowDialog;
import org.robolectric.shadows.ShadowLooper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, shadows = {ShadowMoonBridge.class})
public class OptionBStressTest {
    private final Context context = ApplicationProvider.getApplicationContext();
    private final Resources res = context.getResources();
    private AppCompatActivity activity;
    private SharedPreferences prefs;

    @Before
    public void setUp() {
        activity = Robolectric.buildActivity(AppCompatActivity.class).create().get();
        activity.setTheme(R.style.AppTheme_MaterialYou);
        prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        prefs.edit().clear().commit();
    }

    @Test
    public void rapidTabSwitchingPreservesViewStateAndSnapsPreview() {
        GameMenuEditor.show(activity, prefs, () -> { });
        ShadowLooper.idleMainLooper();
        AlertDialog dialog = (AlertDialog) ShadowDialog.getLatestDialog();
        assertNotNull(dialog);

        View tabActions = dialog.findViewById(R.id.game_menu_editor_tab_actions);
        View tabSections = dialog.findViewById(R.id.game_menu_editor_tab_sections);
        View previewQuick = dialog.findViewById(R.id.game_menu_editor_preview_quick);
        View previewAdvanced = dialog.findViewById(R.id.game_menu_editor_preview_advanced);
        FrameLayout listContainer = dialog.findViewById(R.id.game_menu_editor_list);

        LinearLayout listColumn = (LinearLayout) listContainer.getChildAt(0);
        LinearLayout sectionsColumn = (LinearLayout) listContainer.getChildAt(1);

        for (int i = 0; i < 50; i++) {
            tabSections.performClick();
            assertTrue(tabSections.isSelected());
            assertFalse(tabActions.isSelected());
            assertEquals(View.GONE, listColumn.getVisibility());
            assertEquals(View.VISIBLE, sectionsColumn.getVisibility());

            tabActions.performClick();
            assertTrue(tabActions.isSelected());
            assertFalse(tabSections.isSelected());
            assertEquals(View.VISIBLE, listColumn.getVisibility());
            assertEquals(View.GONE, sectionsColumn.getVisibility());
        }

        previewAdvanced.performClick();
        assertTrue(previewAdvanced.isSelected());
        assertFalse(previewQuick.isSelected());

        tabSections.performClick();
        assertTrue(tabSections.isSelected());
        assertEquals(View.VISIBLE, sectionsColumn.getVisibility());
        assertTrue(previewQuick.isSelected());
        assertFalse(previewAdvanced.isSelected());

        dialog.dismiss();
    }

    @Test
    public void sectionReorderingUpdatesLivePreviewImmediately() {
        List<GameMenuEditor.Row> rows = GameMenuEditor.buildRows(
                context,
                Collections.singletonList(com.limelight.actions.StreamActionRegistry.ID_ZOOM),
                Collections.emptyList());

        List<String> order1 = Arrays.asList(
                GameMenuLayout.SECTION_SESSION,
                GameMenuLayout.SECTION_MENU,
                GameMenuLayout.SECTION_ACTIONS);
        List<MenuEntry> preview1 = GameMenuEditor.buildPreview(res, rows, order1);
        List<String> headers1 = new ArrayList<>();
        for (MenuEntry entry : preview1) {
            if (entry.header) {
                headers1.add(entry.label);
            }
        }
        assertEquals(Arrays.asList(
                res.getString(R.string.game_menu_section_session),
                res.getString(R.string.game_menu_section_menu),
                res.getString(R.string.game_menu_section_actions)), headers1);

        List<String> order2 = Arrays.asList(
                GameMenuLayout.SECTION_MENU,
                GameMenuLayout.SECTION_ACTIONS,
                GameMenuLayout.SECTION_SESSION);
        List<MenuEntry> preview2 = GameMenuEditor.buildPreview(res, rows, order2);
        List<String> headers2 = new ArrayList<>();
        for (MenuEntry entry : preview2) {
            if (entry.header) {
                headers2.add(entry.label);
            }
        }
        assertEquals(Arrays.asList(
                res.getString(R.string.game_menu_section_menu),
                res.getString(R.string.game_menu_section_actions),
                res.getString(R.string.game_menu_section_session)), headers2);
    }

    @Test
    public void parseSectionOrderSanitizesCorruptedInputs() {
        assertEquals(GameMenuLayout.DEFAULT_SECTION_ORDER, GameMenuLayout.parseSectionOrder(null));
        assertEquals(GameMenuLayout.DEFAULT_SECTION_ORDER, GameMenuLayout.parseSectionOrder(""));
        assertEquals(GameMenuLayout.DEFAULT_SECTION_ORDER, GameMenuLayout.parseSectionOrder("   ,  , "));
        assertEquals(GameMenuLayout.DEFAULT_SECTION_ORDER, GameMenuLayout.parseSectionOrder("bogus,corrupt,unknown"));

        List<String> duplicates = GameMenuLayout.parseSectionOrder("menu,menu,actions,menu,session");
        assertEquals(Arrays.asList(
                GameMenuLayout.SECTION_MENU,
                GameMenuLayout.SECTION_ACTIONS,
                GameMenuLayout.SECTION_SESSION), duplicates);

        List<String> partial = GameMenuLayout.parseSectionOrder("session");
        assertEquals(Arrays.asList(
                GameMenuLayout.SECTION_SESSION,
                GameMenuLayout.SECTION_ACTIONS,
                GameMenuLayout.SECTION_MENU), partial);

        List<String> noisy = GameMenuLayout.parseSectionOrder("  session  , unknown_id , actions , 12345 ");
        assertEquals(Arrays.asList(
                GameMenuLayout.SECTION_SESSION,
                GameMenuLayout.SECTION_ACTIONS,
                GameMenuLayout.SECTION_MENU), noisy);
    }

    @Test
    public void transactionalIntegrityDiscardsOnCancelAndAppliesOnSave() {
        List<String> initial = Arrays.asList(
                GameMenuLayout.SECTION_SESSION,
                GameMenuLayout.SECTION_ACTIONS,
                GameMenuLayout.SECTION_MENU);
        GameMenuLayout.setSectionOrder(prefs, initial);

        final boolean[] changed = {false};
        GameMenuEditor.show(activity, prefs, () -> changed[0] = true);
        ShadowLooper.idleMainLooper();
        AlertDialog dialog = (AlertDialog) ShadowDialog.getLatestDialog();

        dialog.findViewById(R.id.game_menu_editor_tab_sections).performClick();

        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();
        ShadowLooper.idleMainLooper();

        assertFalse(changed[0]);
        assertEquals(initial, GameMenuLayout.getSectionOrder(prefs));

        changed[0] = false;
        GameMenuEditor.show(activity, prefs, () -> changed[0] = true);
        ShadowLooper.idleMainLooper();
        AlertDialog dialog2 = (AlertDialog) ShadowDialog.getLatestDialog();

        dialog2.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        ShadowLooper.idleMainLooper();

        assertTrue(changed[0]);
        assertEquals(initial, GameMenuLayout.getSectionOrder(prefs));
    }

    @Test
    public void sizingBehaviorAcrossDensitiesAndShortScreenHeights() {
        String[] qualifiers = {
                "w411dp-h891dp-xxhdpi",
                "w832dp-h384dp-land-xxhdpi",
                "w1480dp-h924dp-xhdpi",
                "w320dp-h240dp-ldpi"
        };

        for (String q : qualifiers) {
            RuntimeEnvironment.setQualifiers(q);
            AppCompatActivity testActivity = Robolectric.buildActivity(AppCompatActivity.class).create().get();
            testActivity.setTheme(R.style.AppTheme_MaterialYou);
            SharedPreferences testPrefs = PreferenceManager.getDefaultSharedPreferences(testActivity);

            GameMenuEditor.show(testActivity, testPrefs, () -> { });
            ShadowLooper.idleMainLooper();
            AlertDialog dialog = (AlertDialog) ShadowDialog.getLatestDialog();
            assertNotNull(dialog);
            assertTrue(dialog.isShowing());

            View tabActions = dialog.findViewById(R.id.game_menu_editor_tab_actions);
            View tabSections = dialog.findViewById(R.id.game_menu_editor_tab_sections);
            assertNotNull(tabActions);
            assertNotNull(tabSections);

            tabSections.performClick();
            assertTrue(tabSections.isSelected());

            dialog.dismiss();
        }
    }
}
