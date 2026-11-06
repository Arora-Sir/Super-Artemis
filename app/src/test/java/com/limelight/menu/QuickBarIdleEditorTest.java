package com.limelight.menu;

import static org.junit.Assert.assertEquals;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.SeekBar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.limelight.shadows.ShadowMoonBridge;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

// NOTE: The idle dialog edits four values with a slider and a number field each, and keeps them in step until Save writes them.
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, shadows = {ShadowMoonBridge.class})
public class QuickBarIdleEditorTest {
    private static void collect(View view, Class<?> type, List<View> out) {
        if (type.isInstance(view)) {
            out.add(view);
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                collect(group.getChildAt(i), type, out);
            }
        }
    }

    private static List<View> all(AlertDialog dialog, Class<?> type) {
        List<View> out = new ArrayList<>();
        collect(dialog.findViewById(android.R.id.content), type, out);
        return out;
    }

    @Test
    public void aTypedNumberMovesTheSliderAndSaveWritesEveryValue() {
        AppCompatActivity activity = Robolectric.buildActivity(AppCompatActivity.class).get();
        activity.setTheme(com.limelight.R.style.AppTheme_MaterialYou);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        prefs.edit().clear().commit();

        QuickBarIdleEditor.show(activity, prefs, null);
        AlertDialog dialog = (AlertDialog) org.robolectric.shadows.ShadowDialog.getLatestDialog();
        List<View> inputs = all(dialog, EditText.class);
        List<View> sliders = all(dialog, SeekBar.class);
        assertEquals(4, inputs.size());
        assertEquals(4, sliders.size());
        // Defaults: dim after 3 s to 40 percent, never hide, tab at 100 percent
        assertEquals("3", ((EditText) inputs.get(0)).getText().toString());
        assertEquals(40, ((SeekBar) sliders.get(1)).getProgress());

        ((EditText) inputs.get(2)).setText("20");
        assertEquals(20, ((SeekBar) sliders.get(2)).getProgress());
        ((SeekBar) sliders.get(3)).setProgress(35);
        // A typed number above the slider range is kept
        ((EditText) inputs.get(0)).setText("200");
        assertEquals(120, ((SeekBar) sliders.get(0)).getProgress());

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        org.robolectric.shadows.ShadowLooper.idleMainLooper();
        IdlePlan saved = IdlePlan.read(prefs);
        assertEquals(200, saved.dimSeconds);
        assertEquals(40, saved.dimPercent);
        assertEquals(20, saved.hideSeconds);
    }

    @Test
    public void resetRestoresTheDefaultsWithoutClosingOrSaving() {
        AppCompatActivity activity = Robolectric.buildActivity(AppCompatActivity.class).get();
        activity.setTheme(com.limelight.R.style.AppTheme_MaterialYou);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        prefs.edit().clear().commit();
        new IdlePlan(9, 10, 50, 20).save(prefs);

        QuickBarIdleEditor.show(activity, prefs, null);
        AlertDialog dialog = (AlertDialog) org.robolectric.shadows.ShadowDialog.getLatestDialog();
        List<View> inputs = all(dialog, EditText.class);
        assertEquals("9", ((EditText) inputs.get(0)).getText().toString());

        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).performClick();
        assertEquals("3", ((EditText) inputs.get(0)).getText().toString());
        assertEquals("0", ((EditText) inputs.get(2)).getText().toString());
        assertEquals(true, dialog.isShowing());
        // Nothing is written until Save
        assertEquals(9, IdlePlan.read(prefs).dimSeconds);
    }
}
