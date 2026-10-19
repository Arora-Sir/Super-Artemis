package com.limelight.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import android.app.AlertDialog;
import android.view.View;
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
}
