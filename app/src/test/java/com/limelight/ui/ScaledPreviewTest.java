package com.limelight.ui;

import static org.junit.Assert.assertEquals;

import android.view.View;
import android.widget.FrameLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.limelight.shadows.ShadowMoonBridge;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

// NOTE: The scaled preview keeps its box at the height it was given and lays the content out larger by the inverse of the scale, so the shrunk content fills the box.
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, shadows = {ShadowMoonBridge.class})
public class ScaledPreviewTest {
    @Test
    public void theContentIsLaidOutLargerByTheInverseOfTheScale() {
        assertEquals(1000, ScaledPreview.contentSize(620, 0.62f));
        assertEquals(500, ScaledPreview.contentSize(500, 1f));
    }

    @Test
    public void theBoxKeepsItsHeightAndTheContentFillsItOnceScaled() {
        AppCompatActivity activity = Robolectric.buildActivity(AppCompatActivity.class).setup().get();
        FrameLayout parent = new FrameLayout(activity);
        FrameLayout host = new FrameLayout(activity);
        parent.addView(host, new FrameLayout.LayoutParams(620, FrameLayout.LayoutParams.WRAP_CONTENT));
        View content = new View(activity);

        ScaledPreview.show(host, content, 400, 0.5f);
        parent.measure(View.MeasureSpec.makeMeasureSpec(620, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(2000, View.MeasureSpec.AT_MOST));
        parent.layout(0, 0, 620, 2000);

        assertEquals(400, host.getLayoutParams().height);
        assertEquals(0.5f, content.getScaleX(), 0.0001f);
        assertEquals(800, content.getLayoutParams().height);
        assertEquals(1240, content.getLayoutParams().width);
    }
}
