package com.limelight.menu;

import android.content.Context;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import androidx.preference.PreferenceManager;

import com.limelight.R;

// NOTE: How a quick bar looks, in one place, so the bars in a stream and the preview in the bar editor draw the same container, buttons and gaps.
// NOTE: Rail is one translucent rounded container, round keeps a circle per button, and handle uses the rail look once it is open.
public final class QuickBarLook {
    public static final String PREF_STYLE = "quick_bar_style";
    public static final String STYLE_RAIL = "rail";
    public static final String STYLE_ROUND = "round";
    public static final String STYLE_HANDLE = "handle";
    public static final int RAIL_GAP_DP = 2;
    public static final int RAIL_PADDING_DP = 4;
    // Padding around the icon inside a button, the same on the stream and in the preview
    public static final int ICON_PADDING_DP = 7;

    private QuickBarLook() {
    }

    public static String normalize(String value) {
        return STYLE_ROUND.equals(value) || STYLE_HANDLE.equals(value) ? value : STYLE_RAIL;
    }

    public static String read(Context context) {
        return normalize(PreferenceManager.getDefaultSharedPreferences(context).getString(PREF_STYLE, STYLE_RAIL));
    }

    // The rail and the handle share one look, only the round style keeps a circle per button
    public static boolean railLook(String style) {
        return !STYLE_ROUND.equals(style);
    }

    public static int gapDp(String style) {
        return railLook(style) ? RAIL_GAP_DP : BarFit.GAP_DP;
    }

    public static int buttonBackground(String style, boolean active) {
        if (railLook(style)) {
            return active ? R.drawable.bg_quick_rail_button_active : R.drawable.bg_quick_rail_button;
        }
        return active ? R.drawable.floating_menu_button_active : R.drawable.floating_menu_button;
    }

    // The rail container behind a bar, nothing for the round style
    public static void styleContainer(LinearLayout bar, String style, float density) {
        if (railLook(style)) {
            int padding = (int) (RAIL_PADDING_DP * density);
            bar.setBackgroundResource(R.drawable.bg_quick_rail);
            bar.setPadding(padding, padding, padding, padding);
        }
    }

    // A button with this icon, drawn the way the stream draws it. The caller adds the click handling.
    public static ImageButton newButton(Context context, int iconRes, String style, float density) {
        ImageButton button = new ImageButton(context);
        button.setImageResource(iconRes);
        button.setBackgroundResource(buttonBackground(style, false));
        int padding = (int) (ICON_PADDING_DP * density);
        button.setPadding(padding, padding, padding, padding);
        button.setScaleType(ImageButton.ScaleType.FIT_CENTER);
        return button;
    }
}
