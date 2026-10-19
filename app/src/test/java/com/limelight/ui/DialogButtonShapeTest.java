package com.limelight.ui;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.AlertDialog;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.limelight.R;
import com.limelight.shadows.ShadowMoonBridge;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

// NOTE: Material 3 text buttons are fully rounded. The platform borderless button pressed and focused as a sharp box (seen on the Reconnect dialog).
// NOTE: This builds the stream's dialogs the way the stream does, through ThemedDialogs, in both app styles, and checks every button has the rounded ripple.
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, shadows = {ShadowMoonBridge.class})
public class DialogButtonShapeTest {
    private static AppCompatActivity activity(boolean materialYou) {
        org.robolectric.android.controller.ActivityController<AppCompatActivity> controller = Robolectric.buildActivity(AppCompatActivity.class);
        AppCompatActivity activity = controller.get();
        activity.setTheme(materialYou ? R.style.AppTheme_MaterialYou : R.style.AppTheme);
        if (!materialYou) {
            activity.getTheme().applyStyle(R.style.ThemeOverlay_SuperArtemis_Classic, true);
        }
        return controller.setup().get();
    }

    @Test
    public void streamDialogButtonsUseTheRoundedRipple() {
        for (boolean materialYou : new boolean[] {false, true}) {
            AppCompatActivity activity = activity(materialYou);
            AlertDialog dialog = ThemedDialogs.builder(activity)
                    .setTitle("Connection Terminated")
                    .setMessage("The connection was terminated.")
                    .setPositiveButton("Reconnect", null)
                    .setNegativeButton("OK", null)
                    .setNeutralButton("Help", null)
                    .show();
            ShadowLooper.idleMainLooper();
            Drawable expected = activity.getDrawable(R.drawable.bg_button_text);
            for (int which : new int[] {AlertDialog.BUTTON_POSITIVE, AlertDialog.BUTTON_NEGATIVE, AlertDialog.BUTTON_NEUTRAL}) {
                Button button = dialog.getButton(which);
                assertNotNull(button);
                Drawable background = button.getBackground();
                assertTrue("button " + which + " style " + materialYou + " has " + background, background instanceof RippleDrawable);
                assertTrue(expected instanceof RippleDrawable);
                Drawable mask = ((RippleDrawable) background).findDrawableByLayerId(android.R.id.mask);
                assertTrue("button " + which + " mask " + mask, mask instanceof android.graphics.drawable.GradientDrawable);
                // A rounded mask: the platform default is a sharp rectangle with no corner radius
                float radius = ((android.graphics.drawable.GradientDrawable) mask).getCornerRadius();
                assertTrue("button " + which + " corner radius " + radius, radius >= 20 * activity.getResources().getDisplayMetrics().density);
            }
            dialog.dismiss();
        }
    }
    @Test
    public void disabledDialogButtonsLookDisabled() {
        AppCompatActivity activity = activity(true);
        AlertDialog dialog = ThemedDialogs.builder(activity).setTitle("Key").setPositiveButton("Save", null).setNegativeButton("Cancel", null).create();
        dialog.show();
        Dialogs.fitWidth(dialog, Dialogs.WIDTH_STANDARD_DP);
        Button save = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        int enabled = save.getTextColors().getColorForState(new int[] {android.R.attr.state_enabled}, 0);
        int disabled = save.getTextColors().getColorForState(new int[] {-android.R.attr.state_enabled}, 0);
        assertTrue(android.graphics.Color.alpha(disabled) < android.graphics.Color.alpha(enabled) / 2);
        dialog.dismiss();
    }
}
