package com.limelight.menu;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import androidx.preference.PreferenceManager;
import androidx.test.core.app.ApplicationProvider;

import com.limelight.R;
import com.limelight.actions.StreamAction;
import com.limelight.actions.StreamActionRegistry;
import com.limelight.shadows.ShadowMoonBridge;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

// NOTE: The menu and bar settings are offered as actions, so the Advanced Menu, the Quick Menu, a bar or a gesture can hold them, and the bars and the editor preview share one look.
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, shadows = {ShadowMoonBridge.class})
public class MenuActionsAndLookTest {
    @Test
    public void theMenuAndBarSettingsAreActionsThatCanBePinnedAndListedInTheAdvancedMenu() {
        for (String id : new String[] {StreamActionRegistry.ID_MENU_STYLE, StreamActionRegistry.ID_QUICK_BAR_STYLE, StreamActionRegistry.ID_MENU_SECTION_ORDER}) {
            StreamAction action = StreamActionRegistry.find(id);
            assertNotNull(id, action);
            assertFalse(id + " is not a session action", action.destructive);
            boolean listed = false;
            for (StreamAction candidate : GameMenuLayout.menuActions()) {
                listed |= candidate.id.equals(id);
            }
            assertTrue(id + " is in the menu editor and the Advanced Menu", listed);
        }
    }

    @Test
    public void theThreeSettingsActionsHaveTheirOwnIcons() {
        int menuStyle = StreamActionRegistry.find(StreamActionRegistry.ID_MENU_STYLE).iconRes;
        int barStyle = StreamActionRegistry.find(StreamActionRegistry.ID_QUICK_BAR_STYLE).iconRes;
        int order = StreamActionRegistry.find(StreamActionRegistry.ID_MENU_SECTION_ORDER).iconRes;
        assertNotEquals(menuStyle, barStyle);
        assertNotEquals(menuStyle, order);
        assertNotEquals(barStyle, order);
    }

    @Test
    public void theLookReadsTheSavedStyleAndFallsBackToTheRail() {
        Context context = ApplicationProvider.getApplicationContext();
        PreferenceManager.getDefaultSharedPreferences(context).edit().clear().commit();
        assertEquals(QuickBarLook.STYLE_RAIL, QuickBarLook.read(context));
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString(QuickBarLook.PREF_STYLE, "round").commit();
        assertEquals(QuickBarLook.STYLE_ROUND, QuickBarLook.read(context));
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString(QuickBarLook.PREF_STYLE, "bogus").commit();
        assertEquals(QuickBarLook.STYLE_RAIL, QuickBarLook.read(context));
    }

    @Test
    public void railAndHandleShareOneLookAndRoundKeepsACirclePerButton() {
        assertTrue(QuickBarLook.railLook(QuickBarLook.STYLE_RAIL));
        assertTrue(QuickBarLook.railLook(QuickBarLook.STYLE_HANDLE));
        assertFalse(QuickBarLook.railLook(QuickBarLook.STYLE_ROUND));
        assertEquals(QuickBarLook.RAIL_GAP_DP, QuickBarLook.gapDp(QuickBarLook.STYLE_RAIL));
        assertEquals(BarFit.GAP_DP, QuickBarLook.gapDp(QuickBarLook.STYLE_ROUND));
        assertEquals(R.drawable.bg_quick_rail_button_active, QuickBarLook.buttonBackground(QuickBarLook.STYLE_RAIL, true));
        assertEquals(R.drawable.floating_menu_button, QuickBarLook.buttonBackground(QuickBarLook.STYLE_ROUND, false));
    }

    @Test
    public void theRailStylesTheContainerAndTheRoundStyleLeavesItPlain() {
        Context context = ApplicationProvider.getApplicationContext();
        float density = context.getResources().getDisplayMetrics().density;
        LinearLayout rail = new LinearLayout(context);
        QuickBarLook.styleContainer(rail, QuickBarLook.STYLE_RAIL, density);
        assertNotNull(rail.getBackground());
        assertEquals((int) (QuickBarLook.RAIL_PADDING_DP * density), rail.getPaddingLeft());

        LinearLayout round = new LinearLayout(context);
        QuickBarLook.styleContainer(round, QuickBarLook.STYLE_ROUND, density);
        assertEquals(null, round.getBackground());
    }

    @Test
    public void aLookButtonHasTheIconAndPaddingOfTheStream() {
        Context context = ApplicationProvider.getApplicationContext();
        float density = context.getResources().getDisplayMetrics().density;
        ImageButton button = QuickBarLook.newButton(context, R.drawable.ic_sa_keyboard, QuickBarLook.STYLE_RAIL, density);
        assertEquals((int) (QuickBarLook.ICON_PADDING_DP * density), button.getPaddingLeft());
        assertNotNull(button.getDrawable());
        assertNotNull(button.getBackground());
    }
}
