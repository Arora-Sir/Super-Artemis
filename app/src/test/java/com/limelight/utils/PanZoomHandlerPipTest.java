package com.limelight.utils;

import static org.junit.Assert.assertEquals;

import android.content.Context;
import android.view.View;

import androidx.test.core.app.ApplicationProvider;

import com.limelight.preferences.PreferenceConfiguration;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

// NOTE: Picture-in-picture must show the whole stream, so a saved zoom and pan may not apply inside the small window.
// NOTE: The real values must survive for the way back to fullscreen and for the saved preferences.
@Config(sdk = {33}, shadows = {com.limelight.shadows.ShadowMoonBridge.class, com.limelight.shadows.ShadowGameManager.class})
@RunWith(RobolectricTestRunner.class)
public class PanZoomHandlerPipTest {
    private static final float ZOOM = 1.1642313f;
    private static final float PAN_X = -281.5061f;
    private static final float PAN_Y = -161.24078f;

    private View stream;
    private PanZoomHandler handler;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        stream = new View(context);
        View parent = new View(context);
        handler = new PanZoomHandler(context, null, stream, parent, PreferenceConfiguration.readPreferences(context));
        handler.setInitialZoomAndPan(ZOOM, PAN_X, PAN_Y);
    }

    @Test
    public void enteringShowsTheStreamUnzoomedAndUnpanned() {
        handler.enterPictureInPicture();

        assertEquals(1.0f, stream.getScaleX(), 0f);
        assertEquals(1.0f, stream.getScaleY(), 0f);
        assertEquals(0f, stream.getX(), 0f);
        assertEquals(0f, stream.getY(), 0f);
    }

    @Test
    public void theGettersKeepTheRealValuesWhileInPictureInPicture() {
        handler.enterPictureInPicture();

        assertEquals(ZOOM, handler.getScaleFactor(), 0f);
        assertEquals(PAN_X, handler.getChildX(), 0f);
        assertEquals(PAN_Y, handler.getChildY(), 0f);
    }

    @Test
    public void leavingRestoresTheZoomAndPan() {
        handler.enterPictureInPicture();
        handler.exitPictureInPicture();

        assertEquals(ZOOM, stream.getScaleX(), 0f);
        assertEquals(ZOOM, stream.getScaleY(), 0f);
        assertEquals(PAN_X, stream.getX(), 0f);
        assertEquals(PAN_Y, stream.getY(), 0f);
        assertEquals(ZOOM, handler.getScaleFactor(), 0f);
    }

    @Test
    public void surfaceChangesAreIgnoredInPictureInPicture() {
        handler.enterPictureInPicture();
        handler.handleSurfaceChange();

        assertEquals(1.0f, stream.getScaleX(), 0f);
        assertEquals(0f, stream.getX(), 0f);
        assertEquals(0f, stream.getY(), 0f);
    }

    @Test
    public void enteringTwiceDoesNotOverwriteTheParkedValues() {
        handler.enterPictureInPicture();
        handler.enterPictureInPicture();
        handler.exitPictureInPicture();

        assertEquals(ZOOM, handler.getScaleFactor(), 0f);
        assertEquals(PAN_X, handler.getChildX(), 0f);
    }
}
