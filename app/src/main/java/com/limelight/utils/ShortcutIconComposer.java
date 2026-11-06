package com.limelight.utils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

// NOTE: Builds the bitmap of a pinned app shortcut. A launcher shows an adaptive icon through a mask that hides the outer third of the image, so plain box art was cropped and sat off center.
// NOTE: The middle square of the art fills the visible middle of a larger dark canvas. An app with no real art (a flat tile) shows the first letters of its name instead of an empty square.
public final class ShortcutIconComposer {
    // An adaptive icon layer is 108 dp and the launcher keeps the middle 72 dp, so art inside 66 percent is always visible
    static final int SIZE_PX = 432;
    static final float SAFE_FRACTION = 0.66f;
    private static final int BACKGROUND = 0xFF1B1B1F;
    private static final int FLAT_LUMA_SPREAD = 12;
    // A step in brightness this big between neighbouring samples is lettering or a drawn shape, not a gradient
    private static final int EDGE_LUMA_STEP = 40;
    private static final int MIN_EDGE_SAMPLES = 6;

    private ShortcutIconComposer() {
    }

    public static Bitmap compose(Bitmap source, String label) {
        Bitmap art = software(source);
        Bitmap out = Bitmap.createBitmap(SIZE_PX, SIZE_PX, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(out);
        canvas.drawColor(BACKGROUND);
        float safe = SIZE_PX * SAFE_FRACTION;
        float left = (SIZE_PX - safe) / 2f;
        RectF box = new RectF(left, left, left + safe, left + safe);
        if (art == null || art.getWidth() == 0 || art.getHeight() == 0 || isFlat(art)) {
            drawInitials(canvas, box, label);
            return out;
        }
        // NOTE: A square of the art fills the whole visible square, so a tall or wide box art leaves no dark bars at the sides.
        // NOTE: The square is placed around the drawn content (the icon and the word), not around the middle of the tile, so the content sits in the center of the icon.
        int side = Math.min(art.getWidth(), art.getHeight());
        android.graphics.Point content = contentCenter(art);
        int wantedLeft = (content != null ? content.x : art.getWidth() / 2) - side / 2;
        int wantedTop = (content != null ? content.y : art.getHeight() / 2) - side / 2;
        int srcLeft = Math.max(0, Math.min(art.getWidth() - side, wantedLeft));
        int srcTop = Math.max(0, Math.min(art.getHeight() - side, wantedTop));
        android.graphics.Rect crop = new android.graphics.Rect(srcLeft, srcTop, srcLeft + side, srcTop + side);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        canvas.drawBitmap(art, crop, box, paint);
        return out;
    }

    // NOTE: A bitmap decoded for the screen can be a hardware bitmap, which cannot be read pixel by pixel or drawn on a software canvas, so it is copied first.
    static Bitmap software(Bitmap bitmap) {
        if (bitmap != null && bitmap.getConfig() == Bitmap.Config.HARDWARE) {
            Bitmap copy = bitmap.copy(Bitmap.Config.ARGB_8888, false);
            // A copy that cannot be made leaves no art to draw, so the initials are shown
            return copy;
        }
        return bitmap;
    }

    // NOTE: The center of the box that holds every strong edge of the art, which is the drawn icon and lettering. A smooth gradient or a glossy highlight has no strong edges, so it never moves the center.
    // NOTE: Null when there is too little to measure, and the caller then uses the middle of the tile.
    static android.graphics.Point contentCenter(Bitmap bitmap) {
        int cols = 96;
        int rows = Math.max(8, Math.round(cols * bitmap.getHeight() / (float) bitmap.getWidth()));
        int[][] luma = new int[rows][cols];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int pixel = bitmap.getPixel(c * (bitmap.getWidth() - 1) / (cols - 1), r * (bitmap.getHeight() - 1) / (rows - 1));
                luma[r][c] = (Color.red(pixel) * 3 + Color.green(pixel) * 6 + Color.blue(pixel)) / 10;
            }
        }
        int marginC = cols * 6 / 100;
        int marginR = rows * 6 / 100;
        int minC = cols;
        int maxC = -1;
        int minR = rows;
        int maxR = -1;
        int edges = 0;
        for (int r = marginR; r < rows - marginR - 1; r++) {
            for (int c = marginC; c < cols - marginC - 1; c++) {
                boolean edge = Math.abs(luma[r][c] - luma[r][c + 1]) > EDGE_LUMA_STEP || Math.abs(luma[r][c] - luma[r + 1][c]) > EDGE_LUMA_STEP;
                if (edge) {
                    edges++;
                    minC = Math.min(minC, c);
                    maxC = Math.max(maxC, c);
                    minR = Math.min(minR, r);
                    maxR = Math.max(maxR, r);
                }
            }
        }
        if (edges < MIN_EDGE_SAMPLES) {
            return null;
        }
        float x = (minC + maxC + 1) / 2f / cols * bitmap.getWidth();
        float y = (minR + maxR + 1) / 2f / rows * bitmap.getHeight();
        return new android.graphics.Point(Math.round(x), Math.round(y));
    }

    // A tile that is one colour all over has no art, so a sample of its pixels barely differs in brightness
    static boolean isFlat(Bitmap bitmap) {
        int min = 255;
        int max = 0;
        int steps = 12;
        for (int y = 0; y < steps; y++) {
            for (int x = 0; x < steps; x++) {
                int pixel = bitmap.getPixel(x * (bitmap.getWidth() - 1) / (steps - 1), y * (bitmap.getHeight() - 1) / (steps - 1));
                int luma = (Color.red(pixel) * 3 + Color.green(pixel) * 6 + Color.blue(pixel)) / 10;
                min = Math.min(min, luma);
                max = Math.max(max, luma);
            }
        }
        return max - min < FLAT_LUMA_SPREAD;
    }

    // The first letter of up to two words, such as "DT" for "Desktop TV"
    static String initials(String label) {
        StringBuilder out = new StringBuilder();
        if (label != null) {
            for (String word : label.trim().split("[\\s()]+")) {
                if (!word.isEmpty() && Character.isLetterOrDigit(word.charAt(0)) && out.length() < 2) {
                    out.append(Character.toUpperCase(word.charAt(0)));
                }
            }
        }
        return out.length() == 0 ? "?" : out.toString();
    }

    private static void drawInitials(Canvas canvas, RectF box, String label) {
        Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        fill.setColor(0xFF3A3A44);
        canvas.drawRoundRect(box, box.width() * 0.18f, box.width() * 0.18f, fill);
        Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
        text.setColor(Color.WHITE);
        text.setTextAlign(Paint.Align.CENTER);
        text.setFakeBoldText(true);
        text.setTextSize(box.height() * 0.42f);
        // Centered on the glyph box, not on the baseline, so the letters sit in the middle of the tile
        float baseline = box.centerY() - (text.ascent() + text.descent()) / 2f;
        canvas.drawText(initials(label), box.centerX(), baseline, text);
    }
}
