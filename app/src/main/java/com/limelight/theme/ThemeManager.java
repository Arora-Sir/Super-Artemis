package com.limelight.theme;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.view.ContextThemeWrapper;

import androidx.preference.PreferenceManager;

import com.google.android.material.color.DynamicColors;
import com.limelight.R;

// NOTE: Applies the app style. Classic (the default) adds neutral color roles and the rounded Super Artemis dialog to each activity's own theme.
// NOTE: The opt-in "Material You" style on Android 12+ swaps in wallpaper colors, switches instead of checkboxes and rounded settings sections.
// NOTE: Activities call apply() before super.onCreate() and recreate when the style changed while they were in the background.
public final class ThemeManager {
    public static final String PREF_KEY = "list_app_style";
    public static final String CLASSIC = "classic";
    public static final String MATERIAL_YOU = "material_you";

    private ThemeManager() {
    }

    // Dynamic color needs Android 12
    public static boolean isSupported() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S;
    }

    public static String currentStyle(Context context) {
        String style = PreferenceManager.getDefaultSharedPreferences(context).getString(PREF_KEY, CLASSIC);
        return isSupported() && MATERIAL_YOU.equals(style) ? MATERIAL_YOU : CLASSIC;
    }

    public static boolean isMaterialYou(Context context) {
        return MATERIAL_YOU.equals(currentStyle(context));
    }

    // Returns the style that was applied, which the activity keeps to notice a later change
    public static String apply(Activity activity) {
        String style = currentStyle(activity);
        if (MATERIAL_YOU.equals(style)) {
            activity.setTheme(R.style.AppTheme_MaterialYou);
            DynamicColors.applyToActivityIfAvailable(activity);
        }
        else {
            // Classic keeps each activity's own theme and adds the color roles and the rounded dialog on top
            activity.getTheme().applyStyle(R.style.ThemeOverlay_SuperArtemis_Classic, true);
        }
        return style;
    }

    // True when the user switched style while this activity was in the background, so it must be rebuilt
    public static boolean needsRecreate(Context context, String appliedStyle) {
        return !currentStyle(context).equals(appliedStyle);
    }

    // Theme for dialogs shown over the stream, which follow the app style instead of the stream activity's theme
    public static Context dialogContext(Context base, int classicTheme) {
        if (isMaterialYou(base)) {
            return DynamicColors.wrapContextIfAvailable(new ContextThemeWrapper(base, R.style.AppTheme_MaterialYou));
        }
        return new ContextThemeWrapper(new ContextThemeWrapper(base, classicTheme), R.style.ThemeOverlay_SuperArtemis_Classic);
    }

    // Context for Super Artemis dialogs opened from any screen, so they always have the color roles of the current style
    public static Context dialogContext(Context base) {
        if (isMaterialYou(base)) {
            return DynamicColors.wrapContextIfAvailable(new ContextThemeWrapper(base, R.style.AppTheme_MaterialYou));
        }
        return new ContextThemeWrapper(base, R.style.ThemeOverlay_SuperArtemis_Classic);
    }
}
