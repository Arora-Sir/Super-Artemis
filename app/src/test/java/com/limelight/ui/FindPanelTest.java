package com.limelight.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.app.Dialog;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

// NOTE: The Find panel filters by typed words or by pressed keys, lists matches only, pins with one tap in multi choice mode, and closes after one pick otherwise.
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, shadows = {ShadowMoonBridge.class})
public class FindPanelTest {
    private static FindPanel.Item item(String id, String label, List<Integer> keys, boolean checked) {
        return new FindPanel.Item(id, label, R.drawable.ic_ck_keyboard, "Keys", keys, checked);
    }

    private static List<FindPanel.Item> items() {
        return Arrays.asList(
                item("a", "Alt + F4", Arrays.asList(0xA4, 0x73), false),
                item("b", "Alt + Tab", Arrays.asList(0xA4, 0x09), true),
                item("c", "Ctrl + C", Arrays.asList(0xA2, 0x43), false),
                new FindPanel.Item("d", "Toggle Keyboard", R.drawable.ic_ck_keyboard, "Actions", null, false));
    }

    @Test
    public void typedWordsFilterByLabelInAnyOrder() {
        List<FindPanel.Item> shown = FindPanel.filter(items(), false, "tab alt", Collections.emptyList());
        assertEquals(1, shown.size());
        assertEquals("b", shown.get(0).id);
        assertEquals(4, FindPanel.filter(items(), false, "", Collections.emptyList()).size());
    }

    @Test
    public void aPressedKeyListsEveryItemThatUsesIt() {
        List<FindPanel.Item> shown = FindPanel.filter(items(), true, "", Collections.singletonList(0x12));
        assertEquals(2, shown.size());
        assertEquals("a", shown.get(0).id);
        assertEquals("b", shown.get(1).id);
    }

    @Test
    public void severalPressedKeysNarrowTheList() {
        List<FindPanel.Item> shown = FindPanel.filter(items(), true, "", Arrays.asList(0x12, 0x73));
        assertEquals(1, shown.size());
        assertEquals("a", shown.get(0).id);
    }

    @Test
    public void leftAndRightModifiersCountAsTheSameKey() {
        assertEquals(FindPanel.normalize(0xA2), FindPanel.normalize(0xA3));
        assertEquals(FindPanel.normalize(0xA4), FindPanel.normalize(0xA5));
        assertEquals(FindPanel.normalize(0xA0), FindPanel.normalize(0xA1));
        assertEquals(FindPanel.normalize(0x5B), FindPanel.normalize(0x5C));
    }

    @Test
    public void pressModeHidesItemsWithoutKeysAndKeepsEveryKeyItemWhenNothingIsPressed() {
        List<FindPanel.Item> shown = FindPanel.filter(items(), true, "", Collections.emptyList());
        assertEquals(3, shown.size());
        for (FindPanel.Item item : shown) {
            assertTrue(item.keys != null);
        }
    }

    @Test
    public void everySymbolKeyHasOneCleanName() {
        assertEquals("[", FindPanel.symbolName(0xDB));
        assertEquals("]", FindPanel.symbolName(0xDD));
        assertEquals("\\", FindPanel.symbolName(0xDC));
        assertEquals("-", FindPanel.symbolName(0xBD));
        assertEquals("=", FindPanel.symbolName(0xBB));
        assertEquals(";", FindPanel.symbolName(0xBA));
        assertEquals("'", FindPanel.symbolName(0xDE));
        assertEquals("`", FindPanel.symbolName(0xC0));
        assertEquals(",", FindPanel.symbolName(0xBC));
        assertEquals(".", FindPanel.symbolName(0xBE));
        assertEquals("/", FindPanel.symbolName(0xBF));
        // A letter or a function key is not a symbol key and keeps its layout label
        assertEquals(null, FindPanel.symbolName(0x41));
        assertEquals(null, FindPanel.symbolName(0x73));
    }

    private static int rowCount(Dialog dialog) {
        return ((LinearLayout) dialog.findViewById(R.id.find_results)).getChildCount();
    }

    @Test
    public void thePanelListsEveryItemAndFiltersAsTextIsTyped() {
        AppCompatActivity activity = Robolectric.buildActivity(AppCompatActivity.class).setup().get();
        Dialog dialog = FindPanel.show(activity, new ArrayList<>(items()), true, item -> true);
        assertEquals(4, rowCount(dialog));

        ViewGroup host = dialog.findViewById(R.id.find_field_host);
        EditText field = (EditText) host.getChildAt(0);
        field.setText("ctrl");
        assertEquals(1, rowCount(dialog));
        field.setText("nothing like this");
        assertEquals(0, rowCount(dialog));
        assertEquals(View.VISIBLE, dialog.findViewById(R.id.find_empty).getVisibility());
        field.setText("");
        assertEquals(View.GONE, dialog.findViewById(R.id.find_empty).getVisibility());
    }

    @Test
    public void aMultiChoicePanelStaysOpenAndShowsTheNewCheck() {
        AppCompatActivity activity = Robolectric.buildActivity(AppCompatActivity.class).setup().get();
        List<FindPanel.Item> list = new ArrayList<>(items());
        AtomicInteger picks = new AtomicInteger();
        Dialog dialog = FindPanel.show(activity, list, true, item -> {
            picks.incrementAndGet();
            return !item.checked;
        });
        LinearLayout results = dialog.findViewById(R.id.find_results);
        View first = results.getChildAt(0);
        assertEquals(View.INVISIBLE, first.findViewById(R.id.sa_choice_check).getVisibility());

        first.performClick();

        assertEquals(1, picks.get());
        assertTrue(dialog.isShowing());
        assertEquals(View.VISIBLE, first.findViewById(R.id.sa_choice_check).getVisibility());
        assertTrue(list.get(0).checked);
    }

    @Test
    public void aSingleChoicePanelClosesAfterThePick() {
        AppCompatActivity activity = Robolectric.buildActivity(AppCompatActivity.class).setup().get();
        AtomicInteger picks = new AtomicInteger();
        Dialog dialog = FindPanel.show(activity, new ArrayList<>(items()), false, item -> {
            picks.incrementAndGet();
            return true;
        });
        ((LinearLayout) dialog.findViewById(R.id.find_results)).getChildAt(2).performClick();
        assertEquals(1, picks.get());
        assertFalse(dialog.isShowing());
    }

    @Test
    public void theModeButtonSwitchesToPressModeAndBack() {
        AppCompatActivity activity = Robolectric.buildActivity(AppCompatActivity.class).setup().get();
        Dialog dialog = FindPanel.show(activity, new ArrayList<>(items()), true, item -> true);
        assertEquals(View.GONE, dialog.findViewById(R.id.find_keyboard_host).getVisibility());

        dialog.findViewById(R.id.find_mode).performClick();
        assertEquals(View.VISIBLE, dialog.findViewById(R.id.find_keyboard_host).getVisibility());
        assertEquals(View.VISIBLE, dialog.findViewById(R.id.find_pressed_bar).getVisibility());
        // The three items that send keys are listed, the action is not
        assertEquals(3, rowCount(dialog));
        assertEquals(activity.getString(R.string.find_mode_type), dialog.findViewById(R.id.find_mode).getContentDescription());

        dialog.findViewById(R.id.find_mode).performClick();
        assertEquals(View.GONE, dialog.findViewById(R.id.find_keyboard_host).getVisibility());
        assertEquals(4, rowCount(dialog));
    }

    @Test
    public void theBackArrowClosesThePanelAndItHasAScreenReaderName() {
        AppCompatActivity activity = Robolectric.buildActivity(AppCompatActivity.class).setup().get();
        Dialog dialog = FindPanel.show(activity, new ArrayList<>(items()), true, item -> true);
        View close = dialog.findViewById(R.id.find_close);
        assertEquals(activity.getString(R.string.find_close), close.getContentDescription());
        close.performClick();
        assertFalse(dialog.isShowing());
    }

    @Test
    public void thePanelClosesWhenTheActivityDismissesItsDialogs() {
        AppCompatActivity activity = Robolectric.buildActivity(AppCompatActivity.class).setup().get();
        Dialog dialog = FindPanel.show(activity, new ArrayList<>(items()), true, item -> true);
        Dialogs.dismissAll(activity);
        assertFalse(dialog.isShowing());
    }

    @Test
    public void everyRowIsATouchTargetOfAtLeastFortyEightDp() {
        AppCompatActivity activity = Robolectric.buildActivity(AppCompatActivity.class).setup().get();
        Dialog dialog = FindPanel.show(activity, new ArrayList<>(items()), true, item -> true);
        View row = ((LinearLayout) dialog.findViewById(R.id.find_results)).getChildAt(0);
        float density = activity.getResources().getDisplayMetrics().density;
        assertTrue(row.getMinimumHeight() >= 48 * density);
        TextView label = row.findViewById(R.id.sa_choice_label);
        assertEquals("Alt + F4", label.getText().toString().replace("⁨", "").replace("⁩", ""));
    }
}
