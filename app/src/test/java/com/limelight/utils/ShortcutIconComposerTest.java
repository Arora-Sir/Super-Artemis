package com.limelight.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;

import com.limelight.shadows.ShadowMoonBridge;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

// NOTE: The shortcut bitmap keeps its art inside the part of the adaptive icon that the launcher mask leaves visible, centered.
@RunWith(RobolectricTestRunner.class)
@org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
@Config(sdk = 33, shadows = {ShadowMoonBridge.class})
public class ShortcutIconComposerTest {
    private static Bitmap art(int w, int h, int color) {
        Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.BLACK);
        android.graphics.Paint paint = new android.graphics.Paint();
        paint.setColor(color);
        canvas.drawRect(w / 4f, h / 4f, w * 3 / 4f, h * 3 / 4f, paint);
        return bitmap;
    }

    @Test
    public void theArtSitsInTheMiddleAndTheOuterThirdStaysBackground() {
        Bitmap out = ShortcutIconComposer.compose(art(300, 400, Color.RED), "Desktop PC");
        assertEquals(ShortcutIconComposer.SIZE_PX, out.getWidth());
        // The center shows the red art, the corners show the background of the canvas
        assertEquals(Color.RED, out.getPixel(out.getWidth() / 2, out.getHeight() / 2));
        assertEquals(0xFF1B1B1F, out.getPixel(4, 4));
        assertEquals(0xFF1B1B1F, out.getPixel(out.getWidth() - 5, out.getHeight() - 5));
        // Nothing of the art reaches the outer 17 percent on any side
        int edge = (int) (ShortcutIconComposer.SIZE_PX * (1 - ShortcutIconComposer.SAFE_FRACTION) / 2) - 2;
        for (int i = 0; i < out.getWidth(); i += 8) {
            assertEquals(0xFF1B1B1F, out.getPixel(i, edge));
            assertEquals(0xFF1B1B1F, out.getPixel(edge, i));
        }
    }

    // The case from the S24: a tall box art left dark bars at both sides of the icon
    @Test
    public void aTallArtFillsTheVisibleSquareWithNoDarkBarsAtTheSides() {
        Bitmap out = ShortcutIconComposer.compose(art(300, 400, Color.RED), "Desktop PC");
        int safeLeft = (int) (ShortcutIconComposer.SIZE_PX * (1 - ShortcutIconComposer.SAFE_FRACTION) / 2) + 3;
        int safeRight = ShortcutIconComposer.SIZE_PX - safeLeft - 1;
        int middle = out.getHeight() / 2;
        assertEquals(Color.BLACK, out.getPixel(safeLeft, middle));
        assertEquals(Color.BLACK, out.getPixel(safeRight, middle));
    }

    // The case from the S24: the icon and the word sit in the upper part of the tile, so a plain middle crop left them high in the icon
    @Test
    public void theDrawnContentIsCenteredInTheIconNotTheMiddleOfTheTile() {
        Bitmap tall = Bitmap.createBitmap(300, 400, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(tall);
        canvas.drawColor(Color.BLACK);
        android.graphics.Paint paint = new android.graphics.Paint();
        paint.setColor(Color.WHITE);
        canvas.drawRect(100f, 120f, 200f, 200f, paint);

        android.graphics.Point center = ShortcutIconComposer.contentCenter(tall);
        assertTrue(center != null);
        assertEquals(150, center.x, 8);
        assertEquals(160, center.y, 8);

        Bitmap out = ShortcutIconComposer.compose(tall, "Desktop");
        assertEquals(Color.WHITE, out.getPixel(out.getWidth() / 2, out.getHeight() / 2));
    }

    @Test
    public void aSmoothGradientHasNoContentCenter() {
        Bitmap gradient = Bitmap.createBitmap(200, 300, Bitmap.Config.ARGB_8888);
        for (int y = 0; y < 300; y++) {
            int v = 60 + y / 6;
            for (int x = 0; x < 200; x++) {
                gradient.setPixel(x, y, Color.rgb(v, v, v));
            }
        }
        org.junit.Assert.assertNull(ShortcutIconComposer.contentCenter(gradient));
    }

    @Test
    public void aFlatTileShowsTheInitialsInsteadOfAnEmptySquare() {
        Bitmap flat = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888);
        flat.eraseColor(0xFF888888);
        assertTrue(ShortcutIconComposer.isFlat(flat));
        assertFalse(ShortcutIconComposer.isFlat(art(200, 200, Color.RED)));
        assertEquals("DT", ShortcutIconComposer.initials("Desktop (TV)"));
        assertEquals("?", ShortcutIconComposer.initials("  "));

        Bitmap out = ShortcutIconComposer.compose(flat, "Desktop (TV)");
        boolean white = false;
        for (int y = 0; y < out.getHeight() && !white; y += 2) {
            for (int x = 0; x < out.getWidth(); x += 2) {
                white |= out.getPixel(x, y) == Color.WHITE;
            }
        }
        assertTrue("the initials are drawn", white);
    }
}
