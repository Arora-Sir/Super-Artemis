package com.limelight.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import android.app.AlertDialog;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.limelight.R;
import com.limelight.shadows.ShadowMoonBridge;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

import java.util.ArrayList;
import java.util.List;

// NOTE: Every picker (mouse mode, gesture actions, hold time, app action step, profile copy) is built by ChoiceList.
// NOTE: This checks the rows, the check on the current choice, and that a pick closes the dialog before its action runs.
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, shadows = {ShadowMoonBridge.class})
public class ChoiceListTest {
    @Test
    public void rowsShowTheCurrentChoiceAndAPickClosesBeforeItsAction() {
        AppCompatActivity activity = Robolectric.buildActivity(AppCompatActivity.class).setup().get();
        activity.setTheme(R.style.AppTheme);
        List<String> picked = new ArrayList<>();
        boolean[] openWhenPicked = new boolean[1];
        ChoiceList choices = new ChoiceList(ThemedDialogs.builder(activity), R.drawable.ic_gm_mouse_mode, "Mouse mode", R.string.game_menu_cancel);
        choices.add(R.drawable.ic_sa_mouse, "First", false, () -> picked.add("first"));
        AlertDialog[] dialog = new AlertDialog[1];
        choices.add(R.drawable.ic_sa_mouse, "Second", true, () -> {
            openWhenPicked[0] = dialog[0].isShowing();
            picked.add("second");
        });
        dialog[0] = choices.show();
        ShadowLooper.idleMainLooper();

        assertEquals(2, choices.list().getChildCount());
        View first = choices.list().getChildAt(0);
        View second = choices.list().getChildAt(1);
        assertEquals("First", ((TextView) first.findViewById(R.id.sa_choice_label)).getText().toString());
        assertEquals(View.INVISIBLE, first.findViewById(R.id.sa_choice_check).getVisibility());
        assertEquals(View.VISIBLE, second.findViewById(R.id.sa_choice_check).getVisibility());

        second.performClick();
        ShadowLooper.idleMainLooper();
        assertEquals(1, picked.size());
        assertEquals("second", picked.get(0));
        assertFalse(openWhenPicked[0]);
        assertFalse(dialog[0].isShowing());
    }

    // NOTE: A chip grid holds short choices in one wrapping block. The chosen chip is selected, and a tap closes the dialog before its action runs.
    @Test
    public void chipsShareOneGridAndAPickClosesBeforeItsAction() {
        AppCompatActivity activity = Robolectric.buildActivity(AppCompatActivity.class).setup().get();
        activity.setTheme(R.style.AppTheme);
        List<String> picked = new ArrayList<>();
        ChoiceList choices = new ChoiceList(ThemedDialogs.builder(activity), R.drawable.ic_sa_videocam, "Video resolution", R.string.game_menu_cancel);
        List<ChoiceList.Chip> chips = new ArrayList<>();
        chips.add(new ChoiceList.Chip("720p", "1280 x 720", false, () -> picked.add("720")));
        chips.add(new ChoiceList.Chip("1080p", "1920 x 1080", true, () -> picked.add("1080")));
        chips.add(new ChoiceList.Chip("4K", "3840 x 2160", false, () -> picked.add("4k")));
        choices.addChips(chips);
        AlertDialog dialog = choices.show();
        ShadowLooper.idleMainLooper();

        // One child holds every chip, instead of one tall row each
        assertEquals(1, choices.list().getChildCount());
        LinearLayout grid = (LinearLayout) choices.list().getChildAt(0);
        // Three choices fit one line of three equal cells
        assertEquals(1, grid.getChildCount());
        LinearLayout line = (LinearLayout) grid.getChildAt(0);
        assertEquals(3, line.getChildCount());
        assertFalse(line.getChildAt(0).isSelected());
        org.junit.Assert.assertTrue(line.getChildAt(1).isSelected());
        assertEquals("1080p, 1920 x 1080", line.getChildAt(1).getContentDescription().toString());

        line.getChildAt(2).performClick();
        ShadowLooper.idleMainLooper();
        assertEquals(1, picked.size());
        assertEquals("4k", picked.get(0));
        assertFalse(dialog.isShowing());
    }

    // NOTE: The columns divide the choices evenly, so the last line is never a single cell on its own (six choices are 3 by 2, eight are 4 by 2).
    @Test
    public void chipColumnsFillEveryLine() {
        assertEquals(1, ChoiceList.chipColumns(1));
        assertEquals(3, ChoiceList.chipColumns(3));
        assertEquals(4, ChoiceList.chipColumns(4));
        assertEquals(3, ChoiceList.chipColumns(6));
        assertEquals(4, ChoiceList.chipColumns(8));
        assertEquals(3, ChoiceList.chipColumns(9));
        // Five do not divide, so the last line has two cells and the empty cells keep the widths equal
        assertEquals(3, ChoiceList.chipColumns(5));
    }
}
