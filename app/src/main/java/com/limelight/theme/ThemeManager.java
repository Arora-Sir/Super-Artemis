package com.limelight.theme;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.view.ContextThemeWrapper;

import androidx.preference.PreferenceManager;

import com.google.android.material.color.DynamicColors;
import com.google.android.material.color.DynamicColorsOptions;
import com.limelight.R;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

// NOTE: Applies the app style. Material You (the default on Android 12+) uses Material 3 with switches, rounded settings sections and a color scheme.
// NOTE: The color scheme is the wallpaper colors or one of the fixed seeds below, turned into a full Material 3 palette by the same dynamic color engine.
// NOTE: Classic adds neutral color roles and the rounded Super Artemis dialog to each activity's own theme, and stays for anyone who picks it.
// NOTE: Activities call apply() before super.onCreate() and recreate when the style or scheme changed while they were in the background.
public final class ThemeManager {
    public static final String PREF_KEY = "list_app_style";
    public static final String PREF_SCHEME = "list_color_scheme";
    // Set when the user picks a style, so a stored "classic" written by the old default is told apart from a real choice
    static final String PREF_CHOSEN = "app_style_chosen";
    public static final String CLASSIC = "classic";
    public static final String MATERIAL_YOU = "material_you";
    // Pure #000000 window and dialogs for OLED panels, on top of either style
    public static final String PREF_AMOLED = "checkbox_amoled_black";

    public static final String SCHEME_WALLPAPER = "wallpaper";
    // The color the user picked in the color picker, stored as "#RRGGBB"
    public static final String SCHEME_CUSTOM = "custom";
    public static final String PREF_CUSTOM_COLOR = "custom_accent_color_hex";
    public static final List<String> SCHEMES = Collections.unmodifiableList(Arrays.asList(
            SCHEME_WALLPAPER, "blue", "teal", "green", "purple", "orange", "rose", "mono", SCHEME_CUSTOM));
    // Seed colors for the fixed schemes, in the order of SCHEMES after Wallpaper. Mono is a neutral grey, which gives an almost colorless palette.
    private static final int[] SEEDS = {0xFF2196F3, 0xFF00897B, 0xFF388E3C, 0xFF7E57C2, 0xFFEF6C00, 0xFFD81B60, 0xFF808080};

    private ThemeManager() {
    }

    // Dynamic color, which also builds the fixed schemes, needs Android 12
    public static boolean isSupported() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S;
    }

    // NOTE: Settings once wrote "classic" as the default value, so a stored "classic" without a recorded choice moves to Material You once.
    // NOTE: Picking a style in the picker records the choice, so Classic picked on purpose always stays.
    public static String currentStyle(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String style = prefs.getString(PREF_KEY, null);
        if (style == null || (CLASSIC.equals(style) && !prefs.getBoolean(PREF_CHOSEN, false))) {
            style = MATERIAL_YOU;
            if (isSupported()) {
                prefs.edit().putString(PREF_KEY, MATERIAL_YOU).apply();
            }
        }
        return isSupported() && MATERIAL_YOU.equals(style) ? MATERIAL_YOU : CLASSIC;
    }

    public static boolean isMaterialYou(Context context) {
        return MATERIAL_YOU.equals(currentStyle(context));
    }

    public static boolean isAmoled(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PREF_AMOLED, false);
    }

    // The AMOLED black roles over a context that already has the style, which dialogs over the stream and in Settings use
    private static Context withAmoled(Context context) {
        return isAmoled(context) ? new ContextThemeWrapper(context, R.style.ThemeOverlay_SuperArtemis_Amoled) : context;
    }

    public static String currentScheme(Context context) {
        String scheme = PreferenceManager.getDefaultSharedPreferences(context).getString(PREF_SCHEME, SCHEME_WALLPAPER);
        return SCHEMES.contains(scheme) ? scheme : SCHEME_WALLPAPER;
    }

    // Stores a choice made in the style picker, which also marks the style as chosen
    public static void choose(Context context, String style, String scheme) {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putString(PREF_KEY, style)
                .putString(PREF_SCHEME, SCHEMES.contains(scheme) ? scheme : SCHEME_WALLPAPER)
                .putBoolean(PREF_CHOSEN, true)
                .apply();
    }

    // True once the user picked a custom color, so the picker can offer it as a swatch next to the plus
    public static boolean hasCustomColor(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).contains(PREF_CUSTOM_COLOR);
    }

    // The custom accent as a color, the default seed until the user picks one
    public static int customSeed(Context context) {
        return AccentColors.parseOrDefault(PreferenceManager.getDefaultSharedPreferences(context).getString(PREF_CUSTOM_COLOR, null));
    }

    // NOTE: The seed color of the chosen scheme for the on-screen controller, or 0 when the scheme is Wallpaper and the controller keeps its own colors.
    public static int streamAccent(Context context) {
        String scheme = currentScheme(context);
        if (SCHEME_CUSTOM.equals(scheme)) {
            return customSeed(context);
        }
        int index = SCHEMES.indexOf(scheme) - 1;
        return index >= 0 && index < SEEDS.length ? SEEDS[index] : 0;
    }

    // Stores the picked color, which also makes it the scheme in use
    public static void chooseCustomColor(Context context, int color) {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putString(PREF_CUSTOM_COLOR, AccentColors.toHex(color))
                .apply();
    }

    // Wallpaper colors, or the palette grown from the scheme's seed color
    public static DynamicColorsOptions colorOptions(Context context, String scheme) {
        DynamicColorsOptions.Builder builder = new DynamicColorsOptions.Builder();
        if (SCHEME_CUSTOM.equals(scheme)) {
            builder.setContentBasedSource(customSeed(context));
        }
        else {
            int index = SCHEMES.indexOf(scheme);
            if (index > 0) {
                builder.setContentBasedSource(SEEDS[index - 1]);
            }
        }
        return builder.build();
    }

    // A Material You context in the given scheme, used by the picker to draw each swatch in its real primary color
    public static Context materialYouContext(Context base, String scheme) {
        return DynamicColors.wrapContextIfAvailable(new ContextThemeWrapper(base, R.style.AppTheme_MaterialYou), colorOptions(base, scheme));
    }

    // Returns what was applied (style and scheme), which the activity keeps to notice a later change
    public static String apply(Activity activity) {
        String style = currentStyle(activity);
        if (MATERIAL_YOU.equals(style)) {
            activity.setTheme(R.style.AppTheme_MaterialYou);
            DynamicColors.applyToActivityIfAvailable(activity, colorOptions(activity, currentScheme(activity)));
        }
        else {
            // Classic keeps each activity's own theme and adds the color roles and the rounded dialog on top
            activity.getTheme().applyStyle(R.style.ThemeOverlay_SuperArtemis_Classic, true);
        }
        if (isAmoled(activity)) {
            activity.getTheme().applyStyle(R.style.ThemeOverlay_SuperArtemis_Amoled, true);
        }
        return applied(activity);
    }

    private static String applied(Context context) {
        String style = currentStyle(context);
        String scheme = currentScheme(context);
        // A new custom color rebuilds open screens too, since the scheme name stays "custom"
        String applied = MATERIAL_YOU.equals(style)
                ? style + ":" + scheme + (SCHEME_CUSTOM.equals(scheme) ? ":" + AccentColors.toHex(customSeed(context)) : "")
                : style;
        return isAmoled(context) ? applied + ":amoled" : applied;
    }

    // True when the user switched style or scheme while this activity was in the background, so it must be rebuilt
    public static boolean needsRecreate(Context context, String appliedStyle) {
        return !applied(context).equals(appliedStyle);
    }

    // Theme for dialogs shown over the stream, which follow the app style instead of the stream activity's theme
    public static Context dialogContext(Context base, int classicTheme) {
        if (isMaterialYou(base)) {
            return withAmoled(materialYouContext(base, currentScheme(base)));
        }
        return withAmoled(new ContextThemeWrapper(new ContextThemeWrapper(base, classicTheme), R.style.ThemeOverlay_SuperArtemis_Classic));
    }

    // Context for Super Artemis dialogs opened from any screen, so they always have the color roles of the current style
    public static Context dialogContext(Context base) {
        if (isMaterialYou(base)) {
            return withAmoled(materialYouContext(base, currentScheme(base)));
        }
        return withAmoled(new ContextThemeWrapper(base, R.style.ThemeOverlay_SuperArtemis_Classic));
    }
}
