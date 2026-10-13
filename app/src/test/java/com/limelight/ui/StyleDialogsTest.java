package com.limelight.ui;

import static org.junit.Assert.assertTrue;

import androidx.appcompat.app.AppCompatActivity;

import com.limelight.R;
import com.limelight.shadows.ShadowMoonBridge;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.RobolectricTestRunner;

// Both dialog kinds (framework and androidx) open in both app styles. A theme attribute that cannot be resolved crashes here,
// which is how the backup dialog once crashed on the tablet.
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, shadows = {ShadowMoonBridge.class})
public class StyleDialogsTest {
    private static AppCompatActivity activity(boolean materialYou) {
        ActivityController<AppCompatActivity> controller = Robolectric.buildActivity(AppCompatActivity.class);
        AppCompatActivity activity = controller.get();
        if (materialYou) {
            activity.setTheme(R.style.AppTheme_MaterialYou);
        }
        else {
            activity.setTheme(R.style.AppTheme);
            activity.getTheme().applyStyle(R.style.ThemeOverlay_SuperArtemis_Classic, true);
        }
        return controller.setup().get();
    }

    private static void showBoth(AppCompatActivity activity) {
        android.app.AlertDialog framework = new android.app.AlertDialog.Builder(activity)
                .setTitle("Title").setMessage("Message").setPositiveButton("OK", null).show();
        assertTrue(framework.isShowing());
        framework.dismiss();
        androidx.appcompat.app.AlertDialog androidx = new androidx.appcompat.app.AlertDialog.Builder(activity)
                .setTitle("Title").setMessage("Message").setPositiveButton("OK", null).setNeutralButton("Share", null).show();
        assertTrue(androidx.isShowing());
        androidx.dismiss();
    }

    @Test
    public void classicDialogsOpen() {
        showBoth(activity(false));
    }

    @Test
    public void materialYouDialogsOpen() {
        showBoth(activity(true));
    }
}
