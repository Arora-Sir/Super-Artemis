package com.limelight.theme;

// NOTE: The greys of the three card steps of Pure black in ten levels. Level 4 is the look that shipped first, and the other levels move only these three steps.
// NOTE: The page itself stays #000000 and every accent role is untouched, so contrast of the accent colors never changes with the level.
// NOTE: The ends follow the Surface lift of DualClone (flat 4, 8 and 12, recommended 13, 22 and 31, lifted 27, 42 and 52), with straight steps between them.
// NOTE: The overlays ThemeOverlay.SuperArtemis.Amoled.Lift0 to Lift9 in styles.xml hold the same colors, and AmoledBlackTest compares them with these numbers.
public final class AmoledLadder {
    public static final int LEVELS = 10;
    public static final int RECOMMENDED = 4;

    private AmoledLadder() {
    }

    public static int clamp(int level) {
        return Math.max(0, Math.min(LEVELS - 1, level));
    }

    // Cards and sections
    public static int container(int level) {
        return grey(step(level, 4, 13, 27));
    }

    // Fields and the search bar
    public static int high(int level) {
        return grey(step(level, 8, 22, 42));
    }

    // The highest step, such as a field on a card
    public static int highest(int level) {
        return grey(step(level, 12, 31, 52));
    }

    // Straight steps from flat up to the recommended level and from there up to lifted
    private static int step(int level, int flat, int recommended, int lifted) {
        int at = clamp(level);
        if (at <= RECOMMENDED) {
            return Math.round(flat + (recommended - flat) * at / (float) RECOMMENDED);
        }
        return Math.round(recommended + (lifted - recommended) * (at - RECOMMENDED) / (float) (LEVELS - 1 - RECOMMENDED));
    }

    private static int grey(int value) {
        return 0xFF000000 | (value << 16) | (value << 8) | value;
    }
}
