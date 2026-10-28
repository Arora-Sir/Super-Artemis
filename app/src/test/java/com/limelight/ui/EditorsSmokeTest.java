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

    // The S24 Ultra in portrait and in landscape (wide but short), and a tablet like the Tab S10 Ultra, so compact, short and wide layouts all run
    private static final String[] SCREENS = {"w411dp-h891dp-xxhdpi", "w832dp-h384dp-land-xxhdpi", "w1480dp-h924dp-xhdpi"};

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

    // On a phone in landscape the menu editor keeps both panes but scrolls as one page, so the preview is never cut off at the bottom
    @Test
    public void menuEditorScrollsOnAPhoneInLandscape() {
        org.robolectric.RuntimeEnvironment.setQualifiers("w832dp-h384dp-land-xxhdpi");
        AppCompatActivity activity = activity(true);
        GameMenuEditor.show(activity, PreferenceManager.getDefaultSharedPreferences(activity), () -> { });
        ShadowLooper.idleMainLooper();
        android.view.View panes = ShadowDialog.getLatestDialog().findViewById(R.id.game_menu_editor_panes);
        boolean scrolls = false;
        for (android.view.ViewParent parent = panes.getParent(); parent != null; parent = parent.getParent()) {
            scrolls |= parent instanceof PageFirstScrollView;
        }
        assertTrue(scrolls);
        ShadowDialog.getLatestDialog().dismiss();
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

    // NOTE: Done and Set once showed a light label on a light fill. An AppCompat inflater turns the layout Button into a MaterialButton, which tinted the tonal fill with the primary color.
    @Test
    public void tonalEditorButtonsKeepTheirFillAndStayReadable() {
        openInBothStyles((activity, prefs) -> {
            CustomKeyEditor.show(activity, null, key -> { });
            Dialog dialog = ShadowDialog.getLatestDialog();
            for (int id : new int[] {R.id.custom_key_record_done, R.id.custom_key_hotkey_set}) {
                android.widget.Button button = dialog.findViewById(id);
                assertNotNull(button);
                if (button instanceof com.google.android.material.button.MaterialButton) {
                    org.junit.Assert.assertNull("The tint repaints the tonal fill of button " + id,
                            ((com.google.android.material.button.MaterialButton) button).getBackgroundTintList());
                }
                int fill = Dialogs.color(button.getContext(), R.attr.saSecondaryContainer);
                double contrast = androidx.core.graphics.ColorUtils.calculateContrast(button.getCurrentTextColor(), fill);
                assertTrue("Label contrast " + contrast + " on button " + id, contrast >= 4.5);
            }
        });
    }

    // NOTE: The add-step chips were 32dp high, which feels untappable. Material 3 asks for a 48dp touch target.
    @Test
    public void addStepChipsAreTouchTargets() {
        for (String screen : SCREENS) {
            org.robolectric.RuntimeEnvironment.setQualifiers(screen);
            AppCompatActivity activity = activity(true);
            CustomKeyEditor.show(activity, null, key -> { });
            ShadowLooper.idleMainLooper();
            Dialog dialog = ShadowDialog.getLatestDialog();
            android.view.View decor = dialog.getWindow().getDecorView();
            android.util.DisplayMetrics metrics = activity.getResources().getDisplayMetrics();
            decor.measure(android.view.View.MeasureSpec.makeMeasureSpec(metrics.widthPixels, android.view.View.MeasureSpec.AT_MOST),
                    android.view.View.MeasureSpec.makeMeasureSpec(metrics.heightPixels, android.view.View.MeasureSpec.AT_MOST));
            decor.layout(0, 0, decor.getMeasuredWidth(), decor.getMeasuredHeight());
            android.view.ViewGroup chips = dialog.findViewById(R.id.custom_key_add_step);
            assertTrue(chips.getChildCount() >= 7);
            for (int i = 0; i < chips.getChildCount(); i++) {
                int dp = Math.round(chips.getChildAt(i).getHeight() / metrics.density);
                assertTrue("Chip " + i + " is " + dp + "dp high on " + screen, dp >= 48);
            }
            dialog.dismiss();
        }
    }

    @Test
    public void colorPickerOpensAndTakesAHexCode() {
        for (boolean materialYou : new boolean[] {false, true}) {
            AppCompatActivity activity = activity(materialYou);
            int[] picked = {0};
            com.limelight.theme.ColorPickerDialog.show(activity, 0xFF3D6FD8, color -> picked[0] = color);
            Dialog dialog = ShadowDialog.getLatestDialog();
            android.widget.EditText hex = dialog.findViewById(R.id.color_picker_hex);
            org.junit.Assert.assertEquals("#3D6FD8", hex.getText().toString());
            hex.setText("ff8800");
            ShadowLooper.idleMainLooper();
            org.junit.Assert.assertEquals("#FF8800", hex.getText().toString());
            ((android.app.AlertDialog) dialog).getButton(android.app.AlertDialog.BUTTON_POSITIVE).performClick();
            ShadowLooper.idleMainLooper();
            org.junit.Assert.assertEquals(0xFFFF8800, picked[0]);
        }
    }

    @Test
    public void presetDialogsOpen() {
        openInBothStyles((activity, prefs) -> com.limelight.presets.PresetsDialog.show(activity, () -> { }));
        openInBothStyles((activity, prefs) -> com.limelight.presets.PresetsDialog.showDetails(activity,
                com.limelight.presets.PresetCatalog.load(activity).get(0), () -> { }));
    }

    @Test
    public void appStylePickerOpens() {
        openInBothStyles((activity, prefs) -> {
            AppStylePicker.show(activity, "classic", (style, scheme) -> { });
            android.view.ViewGroup swatches = ShadowDialog.getLatestDialog().findViewById(R.id.app_style_schemes);
            // The fixed schemes and the plus, and the custom color joins them once one was picked
            org.junit.Assert.assertEquals(com.limelight.theme.ThemeManager.SCHEMES.size(), swatches.getChildCount());
            com.limelight.theme.ThemeManager.chooseCustomColor(activity, 0xFFAA3355);
            AppStylePicker.show(activity, "classic", (style, scheme) -> { });
            android.view.ViewGroup withCustom = ShadowDialog.getLatestDialog().findViewById(R.id.app_style_schemes);
            org.junit.Assert.assertEquals(com.limelight.theme.ThemeManager.SCHEMES.size() + 1, withCustom.getChildCount());
            prefs.edit().remove(com.limelight.theme.ThemeManager.PREF_CUSTOM_COLOR).commit();
        });
    }

    @Test
    public void quickBarEditorOpensOnAGivenBarOfASet() {
        openInBothStyles((activity, prefs) -> {
            com.limelight.menu.BarSets.save(prefs, new com.limelight.menu.BarSets.State(java.util.Arrays.asList(
                    new com.limelight.menu.BarSets.Set("Desktop", java.util.Arrays.asList(
                            new com.limelight.menu.BarSets.Bar(com.limelight.menu.BarSets.TOP_LEFT, 3, false, 0, 0, java.util.Arrays.asList("microphone", "zoom")),
                            new com.limelight.menu.BarSets.Bar(com.limelight.menu.BarSets.BOTTOM_RIGHT, 2, true, -40, 20, java.util.Arrays.asList("key:builtin_esc")))),
                    new com.limelight.menu.BarSets.Set("Game", new java.util.ArrayList<>())), 0));
            QuickBarEditor.show(activity, prefs, () -> { }, 1);
            android.view.View controls = ShadowDialog.getLatestDialog().findViewById(R.id.quick_bar_bar_controls);
            org.junit.Assert.assertEquals(android.view.View.VISIBLE, controls.getVisibility());
            com.limelight.ui.FlowRow chips = ShadowDialog.getLatestDialog().findViewById(R.id.quick_bar_bars);
            // Two bars and the Add bar chip
            org.junit.Assert.assertEquals(3, chips.getChildCount());
        });
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

    @Test
    public void lockKeyHelpOpensWithTheScript() {
        openInBothStyles((activity, prefs) -> {
            com.limelight.hosthelp.LockKeyHelp.show(activity);
            android.widget.TextView script = ShadowDialog.getLatestDialog().findViewById(R.id.lock_help_script);
            org.junit.Assert.assertTrue(script.getText().toString().contains("LockWorkStation"));
        });
    }

    @Test
    public void appStylePickerAppliesOnlyOnTheButton() {
        // The Apply button closes the dialog, so this test opens it by hand instead of through the open and stay harness
        AppCompatActivity activity = activity(false);
        String[] picked = new String[2];
        AppStylePicker.show(activity, "classic", (style, scheme) -> {
            picked[0] = style;
            picked[1] = scheme;
        });
        android.app.AlertDialog dialog = (android.app.AlertDialog) ShadowDialog.getLatestDialog();
        android.view.ViewGroup swatches = dialog.findViewById(R.id.app_style_schemes);
        // The Wallpaper dot keeps Robolectric away from the dynamic color overlay it cannot apply
        swatches.getChildAt(0).performClick();
        ShadowLooper.idleMainLooper();
        org.junit.Assert.assertNull("A tap on a dot only previews", picked[0]);
        org.junit.Assert.assertTrue(dialog.isShowing());
        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).performClick();
        // The dialog hands button clicks to its handler, which runs when the looper idles
        ShadowLooper.idleMainLooper();
        org.junit.Assert.assertEquals("material_you", picked[0]);
        org.junit.Assert.assertEquals(com.limelight.theme.ThemeManager.SCHEMES.get(0), picked[1]);
    }
}
