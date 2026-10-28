package com.limelight.theme;

import android.graphics.Color;

import androidx.core.graphics.ColorUtils;

import java.util.Locale;

// NOTE: Pure helpers for the custom accent color: reading a hex code the way people type it, converting to and from hue, saturation and value, and judging contrast.
// NOTE: The chosen color is a seed. The Material dynamic color engine grows the readable tones of the app from it, so even a very dark seed ends up as a light enough accent.
public final class AccentColors {
    // Shown when the user has not picked a color yet, the same blue family as the Blue scheme
    public static final int DEFAULT_SEED = 0xFF00BCD4;
    // Surfaces the accent is shown against in the preview
    public static final int PREVIEW_BLACK = 0xFF000000;
    public static final int PREVIEW_CARD = 0xFF1F1F1F;
    // Below this ratio a seed is hard to see next to a dark surface, which is worth a hint (3 is the Material minimum for large shapes)
    public static final double MIN_SEED_CONTRAST = 3.0;

    private AccentColors() {
    }

    // Accepts "#RRGGBB", "RRGGBB" and "#RGB" in any case, and returns "#RRGGBB" in upper case. Anything else returns null.
    public static String normalize(String input) {
        if (input == null) {
            return null;
        }
        String hex = input.trim();
        if (hex.startsWith("#")) {
            hex = hex.substring(1);
        }
        if (hex.length() == 3) {
            hex = "" + hex.charAt(0) + hex.charAt(0) + hex.charAt(1) + hex.charAt(1) + hex.charAt(2) + hex.charAt(2);
        }
        if (hex.length() != 6) {
            return null;
        }
        for (int i = 0; i < hex.length(); i++) {
            if (Character.digit(hex.charAt(i), 16) < 0) {
                return null;
            }
        }
        return "#" + hex.toUpperCase(Locale.ROOT);
    }

    // Keeps only hex digits while the user types, at most six, so no other character ever reaches the field
    public static String sanitize(CharSequence typed) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < typed.length() && out.length() < 6; i++) {
            char c = typed.charAt(i);
            if (Character.digit(c, 16) >= 0) {
                out.append(Character.toUpperCase(c));
            }
        }
        return out.toString();
    }

    // Fully opaque color of a normalized or typed code, or the default seed when the code is not valid
    public static int parseOrDefault(String code) {
        String normalized = normalize(code);
        return normalized == null ? DEFAULT_SEED : 0xFF000000 | Integer.parseInt(normalized.substring(1), 16);
    }

    public static String toHex(int color) {
        return String.format(Locale.ROOT, "#%06X", color & 0xFFFFFF);
    }

    // Hue 0 to 360, saturation and value 0 to 1
    public static float[] toHsv(int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        return hsv;
    }

    // Values outside the ranges are clamped, so a touch past the edge of the picker still gives a valid color
    public static int fromHsv(float hue, float saturation, float value) {
        float h = Math.max(0f, Math.min(360f, hue));
        float s = Math.max(0f, Math.min(1f, saturation));
        float v = Math.max(0f, Math.min(1f, value));
        return Color.HSVToColor(new float[] {h >= 360f ? 359.99f : h, s, v});
    }

    public static double contrast(int foreground, int background) {
        return ColorUtils.calculateContrast(0xFF000000 | foreground, 0xFF000000 | background);
    }

    public static boolean lowContrastOnDark(int seed) {
        return contrast(seed, PREVIEW_CARD) < MIN_SEED_CONTRAST;
    }

    // Black or white text, whichever reads better on this color
    public static int onColor(int background) {
        return contrast(Color.WHITE, background) >= contrast(Color.BLACK, background) ? Color.WHITE : Color.BLACK;
    }
}
