package com.limelight.ui;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Dialog;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.limelight.R;
import com.limelight.about.AboutDialogs;
import com.limelight.about.SupportDialog;
import com.limelight.customkeys.CustomKeyEditor;
import com.limelight.customkeys.SpecialKeysManager;
import com.limelight.gestures.GestureEditor;
import com.limelight.menu.GameMenuEditor;
import com.limelight.menu.QuickBarEditor;
import com.limelight.shadows.ShadowMoonBridge;
import com.limelight.theme.AppStylePicker;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowDialog;
import org.robolectric.shadows.ShadowLooper;

// NOTE: Opens every Super Artemis dialog and editor in both app styles and lays it out, the way a tap in Settings or the stream does.
// NOTE: A view cast that no longer matches its layout (a CheckBox turned into a Switch) or a missing theme attr crashes here instead of on a device.
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, shadows = {ShadowMoonBridge.class})
public class EditorsSmokeTest {
    private interface Opener {
        void open(AppCompatActivity activity, SharedPreferences prefs);
    }

    // The style goes on before the activity is created, as ThemeManager does in a real activity
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

    // A phone like the S24 Ultra and a tablet like the Tab S10 Ultra, so compact and wide layouts both run
    private static final String[] SCREENS = {"w411dp-h891dp-xxhdpi", "w1480dp-h924dp-xhdpi"};

    private static void openInBothStyles(Opener opener) {
        for (String screen : SCREENS) {
            org.robolectric.RuntimeEnvironment.setQualifiers(screen);
            openOnThisScreen(opener);
        }
    }

    private static void openOnThisScreen(Opener opener) {
        for (boolean materialYou : new boolean[] {false, true}) {
            AppCompatActivity activity = activity(materialYou);
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
            opener.open(activity, prefs);
            ShadowLooper.idleMainLooper();
            Dialog dialog = ShadowDialog.getLatestDialog();
            assertNotNull(dialog);
            assertTrue(dialog.isShowing());
            // Measure and lay out like the window would, which creates every RecyclerView row and runs its view casts
            android.view.View decor = dialog.getWindow().getDecorView();
            android.util.DisplayMetrics metrics = activity.getResources().getDisplayMetrics();
            decor.measure(android.view.View.MeasureSpec.makeMeasureSpec(metrics.widthPixels, android.view.View.MeasureSpec.AT_MOST),
                    android.view.View.MeasureSpec.makeMeasureSpec(metrics.heightPixels, android.view.View.MeasureSpec.AT_MOST));
            decor.layout(0, 0, decor.getMeasuredWidth(), decor.getMeasuredHeight());
            dialog.dismiss();
        }
    }

    @Test
    public void quickBarEditorOpens() {
        openInBothStyles((activity, prefs) -> QuickBarEditor.show(activity, prefs, () -> { }));
    }

    @Test
    public void quickMenuEditorOpens() {
        openInBothStyles((activity, prefs) -> GameMenuEditor.show(activity, prefs, () -> { }));
    }

    @Test
    public void gestureEditorOpens() {
        openInBothStyles(GestureEditor::show);
    }

    @Test
    public void specialKeysManagerOpens() {
        openInBothStyles((activity, prefs) -> SpecialKeysManager.show(activity, () -> { }));
    }

    @Test
    public void customKeyEditorOpens() {
        openInBothStyles((activity, prefs) -> CustomKeyEditor.show(activity, null, key -> { }));
    }

    @Test
    public void customKeyEditorShowsEveryStepType() {
        com.limelight.customkeys.CustomKey flow = com.limelight.customkeys.CustomKey.withSteps("f", "Flow", false, null, java.util.Arrays.asList(
                com.limelight.customkeys.KeyStep.combo(java.util.Arrays.asList(0xA2, 0x43)),
                com.limelight.customkeys.KeyStep.text("hello"),
                com.limelight.customkeys.KeyStep.waitMs(200),
                com.limelight.customkeys.KeyStep.hold(0xA0),
                com.limelight.customkeys.KeyStep.release(0xA0),
                com.limelight.customkeys.KeyStep.repeat(2),
                com.limelight.customkeys.KeyStep.action("pan_zoom")));
        openInBothStyles((activity, prefs) -> {
            CustomKeyEditor.show(activity, flow, key -> { });
            android.widget.LinearLayout steps = ShadowDialog.getLatestDialog().findViewById(R.id.custom_key_steps);
            org.junit.Assert.assertEquals(7, steps.getChildCount());
        });
    }

    @Test
    public void appStylePickerOpens() {
        openInBothStyles((activity, prefs) -> AppStylePicker.show(activity, "classic", style -> { }));
    }

    @Test
    public void quickBarEditorOpensOnAGivenCorner() {
        openInBothStyles((activity, prefs) -> QuickBarEditor.show(activity, prefs, () -> { }, com.limelight.menu.QuickBarLayout.POSITION_BOTTOM_RIGHT));
    }

    @Test
    public void logSendOptionsOpen() {
        openInBothStyles((activity, prefs) -> com.limelight.diagnostics.CrashReporter.showSendOptions(activity, "Super Artemis Java crash"));
    }

    @Test
    public void aboutDialogsOpen() {
        openInBothStyles((activity, prefs) -> SupportDialog.show(activity));
        openInBothStyles((activity, prefs) -> AboutDialogs.showProfiles(activity));
        openInBothStyles((activity, prefs) -> AboutDialogs.showCredits(activity));
    }
}
