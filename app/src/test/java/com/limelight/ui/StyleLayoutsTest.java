package com.limelight.ui;

import static org.junit.Assert.assertNotNull;

import android.content.Context;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.widget.FrameLayout;

import androidx.test.core.app.ApplicationProvider;

import com.limelight.R;
import com.limelight.shadows.ShadowMoonBridge;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

// Every layout of the round 2 design inflates in both app styles, which proves each color role and style it uses resolves in each theme
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, shadows = {ShadowMoonBridge.class})
public class StyleLayoutsTest {
    private static final int[] LAYOUTS = {
            R.layout.sa_dialog_header, R.layout.dialog_support, R.layout.sa_link_row, R.layout.credit_card, R.layout.sa_chip,
            R.layout.settings_section_item, R.layout.activity_stream_settings, R.layout.dialog_quick_bar_editor,
            R.layout.custom_key_row, R.layout.special_keys_header, R.layout.dialog_icon_picker, R.layout.custom_key_icon_item,
            R.layout.gesture_row, R.layout.sa_choice_row, R.layout.dialog_app_style, R.layout.app_style_card,
            R.layout.dialog_game_menu_editor, R.layout.game_menu_editor_item, R.layout.dialog_custom_key_editor,
    };

    private static void inflateAll(Context context) {
        FrameLayout parent = new FrameLayout(context);
        for (int layout : LAYOUTS) {
            assertNotNull(LayoutInflater.from(context).inflate(layout, parent, false));
        }
    }

    @Test
    public void classicStyle() {
        Context base = ApplicationProvider.getApplicationContext();
        inflateAll(new ContextThemeWrapper(new ContextThemeWrapper(base, R.style.AppTheme), R.style.ThemeOverlay_SuperArtemis_Classic));
    }

    @Test
    public void classicDialogTheme() {
        Context base = ApplicationProvider.getApplicationContext();
        Context classic = new ContextThemeWrapper(new ContextThemeWrapper(base, R.style.AppTheme), R.style.ThemeOverlay_SuperArtemis_Classic);
        inflateAll(new ContextThemeWrapper(classic, R.style.Theme_SuperArtemis_Dialog_Classic));
    }

    @Test
    public void materialYouStyle() {
        Context base = ApplicationProvider.getApplicationContext();
        Context materialYou = new ContextThemeWrapper(base, R.style.AppTheme_MaterialYou);
        inflateAll(materialYou);
        inflateAll(new ContextThemeWrapper(materialYou, R.style.Theme_SuperArtemis_Dialog_MaterialYou));
    }
}
