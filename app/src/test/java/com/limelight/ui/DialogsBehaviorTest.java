package com.limelight.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import android.app.AlertDialog;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.limelight.shadows.ShadowMoonBridge;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

// NOTE: Dialogs close when the stream shrinks into picture-in-picture, keep their scroll dividers off the inner lists of the big editors, resize above the keyboard, and the search box never asks for a fullscreen keyboard.
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, shadows = {ShadowMoonBridge.class})
public class DialogsBehaviorTest {
    private static AlertDialog show(AppCompatActivity activity, View content) {
        return Dialogs.show(new AlertDialog.Builder(activity).setView(content), Dialogs.WIDTH_STANDARD_DP);
    }

    @Test
    public void dismissAllClosesTheOpenDialogsOfAnActivity() {
        AppCompatActivity activity = Robolectric.buildActivity(AppCompatActivity.class).setup().get();
        AlertDialog first = show(activity, new LinearLayout(activity));
        AlertDialog second = show(activity, new LinearLayout(activity));
        assertTrue(first.isShowing());
        assertTrue(second.isShowing());

        Dialogs.dismissAll(activity);

        assertFalse(first.isShowing());
        assertFalse(second.isShowing());
    }

    @Test
    public void dismissAllLeavesTheDialogsOfAnotherActivityAlone() {
        AppCompatActivity stream = Robolectric.buildActivity(AppCompatActivity.class).setup().get();
        AppCompatActivity other = Robolectric.buildActivity(AppCompatActivity.class).setup().get();
        AlertDialog elsewhere = show(other, new LinearLayout(other));

        Dialogs.dismissAll(stream);

        assertTrue(elsewhere.isShowing());
    }

    @Test
    public void aTaggedEditorBodyKeepsTheDividersOffItsInnerLists() {
        AppCompatActivity activity = Robolectric.buildActivity(AppCompatActivity.class).setup().get();
        LinearLayout body = new LinearLayout(activity);
        body.setTag(Dialogs.NO_LIST_EDGES);
        RecyclerView inner = new RecyclerView(activity);
        inner.setLayoutManager(new LinearLayoutManager(activity));
        body.addView(inner);
        show(activity, body);
        assertEquals(0, inner.getScrollIndicators());
    }

    @Test
    public void aPlainListStillShowsTheScrollDividers() {
        AppCompatActivity activity = Robolectric.buildActivity(AppCompatActivity.class).setup().get();
        RecyclerView list = new RecyclerView(activity);
        list.setLayoutManager(new LinearLayoutManager(activity));
        ScrollView scroll = new ScrollView(activity);
        show(activity, list);
        assertNotEquals(0, list.getScrollIndicators());

        ScrollView page = new ScrollView(activity);
        page.addView(new LinearLayout(activity));
        show(activity, page);
        assertNotEquals(0, page.getScrollIndicators());
    }

    @Test
    public void theSearchBoxNeverAsksForAFullscreenKeyboard() {
        AppCompatActivity activity = Robolectric.buildActivity(AppCompatActivity.class).setup().get();
        EditText field = SearchField.create(activity, query -> { });
        int options = field.getImeOptions();
        assertTrue((options & android.view.inputmethod.EditorInfo.IME_FLAG_NO_EXTRACT_UI) != 0);
        assertTrue((options & android.view.inputmethod.EditorInfo.IME_FLAG_NO_FULLSCREEN) != 0);
    }
}
