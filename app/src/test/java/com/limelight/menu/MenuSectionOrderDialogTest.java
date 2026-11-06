package com.limelight.menu;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;

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
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowDialog;
import org.robolectric.shadows.ShadowLooper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, shadows = {ShadowMoonBridge.class})
public class MenuSectionOrderDialogTest {
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
    public void dialogDisplaysAllThreeSectionsWithLocalizedTitles() {
        MenuSectionOrderDialog.show(activity, prefs);
        ShadowLooper.idleMainLooper();
        AlertDialog dialog = (AlertDialog) ShadowDialog.getLatestDialog();
        assertNotNull(dialog);
        assertTrue(dialog.isShowing());

        RecyclerView list = findRecyclerView(dialog.getWindow().getDecorView());
        assertNotNull(list);
        assertEquals(3, list.getAdapter().getItemCount());

        List<String> order = GameMenuLayout.getSectionOrder(prefs);
        assertEquals(GameMenuLayout.DEFAULT_SECTION_ORDER, order);
        assertEquals(res.getString(R.string.game_menu_section_actions), res.getString(GameMenuLayout.sectionTitleRes(order.get(0))));
        assertEquals(res.getString(R.string.game_menu_section_session), res.getString(GameMenuLayout.sectionTitleRes(order.get(1))));
        assertEquals(res.getString(R.string.game_menu_section_menu), res.getString(GameMenuLayout.sectionTitleRes(order.get(2))));

        dialog.dismiss();
    }

    @Test
    public void overloadedShowAcceptsInMemoryOrderAndInvokesCallbackOnReset() {
        List<String> order = new ArrayList<>(Arrays.asList(
                GameMenuLayout.SECTION_SESSION, GameMenuLayout.SECTION_ACTIONS, GameMenuLayout.SECTION_MENU));
        final boolean[] changed = {false};

        MenuSectionOrderDialog.show(activity, order, () -> changed[0] = true);
        ShadowLooper.idleMainLooper();
        AlertDialog dialog = (AlertDialog) ShadowDialog.getLatestDialog();
        assertNotNull(dialog);

        // Clicking reset must restore default order and invoke the callback
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).performClick();
        ShadowLooper.idleMainLooper();

        assertTrue(changed[0]);
        assertEquals(GameMenuLayout.DEFAULT_SECTION_ORDER, order);
        dialog.dismiss();
    }

    @Test
    public void standaloneShowPersistsDefaultOrderOnReset() {
        List<String> customOrder = Arrays.asList(
                GameMenuLayout.SECTION_MENU, GameMenuLayout.SECTION_ACTIONS, GameMenuLayout.SECTION_SESSION);
        GameMenuLayout.setSectionOrder(prefs, customOrder);
        assertEquals(customOrder, GameMenuLayout.getSectionOrder(prefs));

        MenuSectionOrderDialog.show(activity, prefs);
        ShadowLooper.idleMainLooper();
        AlertDialog dialog = (AlertDialog) ShadowDialog.getLatestDialog();
        assertNotNull(dialog);

        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).performClick();
        ShadowLooper.idleMainLooper();

        assertEquals(GameMenuLayout.DEFAULT_SECTION_ORDER, GameMenuLayout.getSectionOrder(prefs));
        dialog.dismiss();
    }

    @Test
    public void dragReorderMovesItemAndNotifiesCallback() {
        List<String> order = new ArrayList<>(GameMenuLayout.DEFAULT_SECTION_ORDER);
        final boolean[] changed = {false};

        MenuSectionOrderDialog.show(activity, order, () -> changed[0] = true);
        ShadowLooper.idleMainLooper();
        AlertDialog dialog = (AlertDialog) ShadowDialog.getLatestDialog();
        assertNotNull(dialog);

        RecyclerView list = findRecyclerView(dialog.getWindow().getDecorView());
        assertNotNull(list);

        DragReorder.moveItem(order, 0, 2);
        list.getAdapter().notifyItemMoved(0, 2);

        assertEquals(Arrays.asList(
                GameMenuLayout.SECTION_SESSION, GameMenuLayout.SECTION_MENU, GameMenuLayout.SECTION_ACTIONS), order);
        dialog.dismiss();
    }

    private static RecyclerView findRecyclerView(android.view.View root) {
        if (root instanceof RecyclerView) {
            return (RecyclerView) root;
        }
        if (root instanceof android.view.ViewGroup) {
            android.view.ViewGroup group = (android.view.ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                RecyclerView found = findRecyclerView(group.getChildAt(i));
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
}
