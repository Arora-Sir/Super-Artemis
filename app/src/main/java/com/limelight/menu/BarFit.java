package com.limelight.menu;

// NOTE: How many buttons of a quick-action bar fit on a screen, and whether two bars would cover each other.
// NOTE: A bar saved with 20 per line needs 712dp, more than a phone has. The saved number is the intent, and the drawn number is the smaller of it and what fits, so every button stays visible and tappable.
public final class BarFit {
    public static final int BUTTON_DP = 36;
    public static final int GAP_DP = 8;
    // Space kept free beside a bar across the width: the side margin on each end
    public static final int SIDE_MARGIN_DP = 8;
    // Space kept free above and below a bar down the height
    public static final int EDGE_MARGIN_DP = 24;

    private BarFit() {
    }

    // Buttons that fit in a span of this many dp, between 1 and the most a bar can hold
    public static int maxPerLine(float spanDp) {
        int fit = (int) Math.floor((spanDp + GAP_DP) / (BUTTON_DP + GAP_DP));
        return Math.max(1, Math.min(BarSets.GRID_MAX, fit));
    }

    // Space a line of this bar can use on a screen of this size, less what the floating menu button covers
    public static float spanDp(boolean down, float screenWidthDp, float screenHeightDp, float reservedDp) {
        float span = down ? screenHeightDp - 2 * EDGE_MARGIN_DP : screenWidthDp - 2 * SIDE_MARGIN_DP;
        return Math.max(0, span - reservedDp);
    }

    public static int effectivePerLine(BarSets.Bar bar, float screenWidthDp, float screenHeightDp, float reservedDp) {
        return Math.min(bar.perLine, maxPerLine(spanDp(bar.down, screenWidthDp, screenHeightDp, reservedDp)));
    }

    // The bar as it is drawn: the same bar when it fits, otherwise a copy that wraps earlier
    public static BarSets.Bar fitted(BarSets.Bar bar, float screenWidthDp, float screenHeightDp, float reservedDp) {
        int perLine = effectivePerLine(bar, screenWidthDp, screenHeightDp, reservedDp);
        if (perLine == bar.perLine) {
            return bar;
        }
        BarSets.Bar copy = bar.copy();
        copy.perLine = perLine;
        return copy;
    }

    // Two rectangles (left, top, right, bottom) cover each other when they share some area
    public static boolean overlaps(float[] a, float[] b) {
        return a[0] < b[2] && b[0] < a[2] && a[1] < b[3] && b[1] < a[3];
    }

    // The size in dp of a bar's grid, buttons and gaps included
    public static float[] sizeDp(BarSets.Bar bar) {
        int columns = bar.columns();
        int rows = bar.rows();
        return new float[] {
                columns * BUTTON_DP + Math.max(0, columns - 1) * GAP_DP,
                rows * BUTTON_DP + Math.max(0, rows - 1) * GAP_DP};
    }
}
