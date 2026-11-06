package com.limelight.menu;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.limelight.R;
import com.limelight.customkeys.BuiltInKeys;
import com.limelight.shadows.ShadowMoonBridge;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

// NOTE: The preview frame has the shape of the screen, whatever the room it is given, so a bar at a corner of the screen sits at the same corner of the preview.
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, shadows = {ShadowMoonBridge.class})
public class QuickBarPreviewTest {
    private static FrameLayout host(AppCompatActivity activity, int width, int height) {
        FrameLayout host = new FrameLayout(activity);
        host.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
        host.layout(0, 0, width, height);
        return host;
    }

    private static List<QuickBarPreview.PreviewBar> oneBar(String anchor) {
        List<String> items = new ArrayList<>();
        Map<String, Integer> icons = new LinkedHashMap<>();
        for (int i = 0; i < 3; i++) {
            String item = QuickBarLayout.customKeyItem(BuiltInKeys.ALL.get(i).id);
            items.add(item);
            icons.put(item, R.drawable.ic_ck_keyboard);
        }
        BarSets.Bar bar = new BarSets.Bar(anchor, 3, false, 0, 0, items);
        return Collections.singletonList(new QuickBarPreview.PreviewBar(bar, items, icons, "Top left, 3"));
    }

    @Test
    public void theLargestScaleThatKeepsTheWholeScreenInTheRoomWins() {
        assertEquals(0.5f, QuickBarPreview.fitScale(1000, 1000, 2000, 1000), 0.0001f);
        assertEquals(0.5f, QuickBarPreview.fitScale(2000, 500, 2000, 1000), 0.0001f);
        assertEquals(1f, QuickBarPreview.fitScale(2000, 1000, 2000, 1000), 0.0001f);
    }

    // The case from the S24 in landscape: a room much wider than the screen is
    @Test
    public void aWideRoomGetsAFrameWithTheShapeOfTheScreenNotTheShapeOfTheRoom() {
        AppCompatActivity activity = Robolectric.buildActivity(AppCompatActivity.class).setup().get();
        FrameLayout host = host(activity, 1440, 310);
        float[] screen = {851f, 393f};

        QuickBarPreview.render(activity, host, screen, oneBar(BarSets.TOP_LEFT), 0, "none", QuickBarLook.STYLE_RAIL, index -> { });

        View frame = host.getChildAt(0);
        float density = activity.getResources().getDisplayMetrics().density;
        float screenAspect = (screen[0] * density) / (screen[1] * density);
        float frameAspect = frame.getLayoutParams().width / (float) frame.getLayoutParams().height;
        assertEquals(screenAspect, frameAspect, 0.02f);
        assertTrue("The frame fits the room", frame.getLayoutParams().width <= 1440 && frame.getLayoutParams().height <= 310);
        assertEquals("The room is limited by its height", 310, frame.getLayoutParams().height, 1f);
    }

    @Test
    public void aTallRoomIsLimitedByItsWidth() {
        AppCompatActivity activity = Robolectric.buildActivity(AppCompatActivity.class).setup().get();
        FrameLayout host = host(activity, 600, 900);
        QuickBarPreview.render(activity, host, new float[] {851f, 393f}, oneBar(BarSets.TOP_LEFT), 0, "none", QuickBarLook.STYLE_RAIL, index -> { });
        assertEquals(600, host.getChildAt(0).getLayoutParams().width, 1f);
    }

    @Test
    public void theHostKeepsNoBoxOfItsOwnSoTheFrameIsTheBox() {
        AppCompatActivity activity = Robolectric.buildActivity(AppCompatActivity.class).setup().get();
        FrameLayout host = host(activity, 1000, 300);
        host.setBackgroundResource(R.drawable.bg_preview_screen);
        QuickBarPreview.render(activity, host, new float[] {851f, 393f}, oneBar(BarSets.TOP_LEFT), 0, "none", QuickBarLook.STYLE_RAIL, index -> { });
        assertNull(host.getBackground());
        assertTrue(host.getChildAt(0).getBackground() != null);
    }

    // The editor previews a style that is chosen but not saved yet, so the preview follows the style it is given, not the saved setting
    @Test
    public void thePreviewDrawsTheStyleItIsGivenNotTheSavedOne() {
        AppCompatActivity activity = Robolectric.buildActivity(AppCompatActivity.class).setup().get();
        FrameLayout rail = host(activity, 1000, 400);
        FrameLayout round = host(activity, 1000, 400);
        QuickBarPreview.render(activity, rail, new float[] {851f, 393f}, oneBar(BarSets.TOP_LEFT), 0, "none", QuickBarLook.STYLE_RAIL, index -> { });
        QuickBarPreview.render(activity, round, new float[] {851f, 393f}, oneBar(BarSets.TOP_LEFT), 0, "none", QuickBarLook.STYLE_ROUND, index -> { });

        ViewGroup railGrid = (ViewGroup) ((ViewGroup) ((FrameLayout) rail.getChildAt(0)).getChildAt(0)).getChildAt(0);
        ViewGroup roundGrid = (ViewGroup) ((ViewGroup) ((FrameLayout) round.getChildAt(0)).getChildAt(0)).getChildAt(0);
        // A rail is one container with a background, round buttons have none and a gap of their own
        assertTrue(railGrid.getBackground() != null);
        assertNull(roundGrid.getBackground());
    }

    @Test
    public void tappingABarReportsItsIndex() {
        AppCompatActivity activity = Robolectric.buildActivity(AppCompatActivity.class).setup().get();
        FrameLayout host = host(activity, 1000, 400);
        AtomicInteger selected = new AtomicInteger(-1);
        QuickBarPreview.render(activity, host, new float[] {851f, 393f}, oneBar(BarSets.TOP_LEFT), -1, "none", QuickBarLook.STYLE_RAIL, selected::set);

        FrameLayout frame = (FrameLayout) host.getChildAt(0);
        ViewGroup layer = (ViewGroup) frame.getChildAt(0);
        layer.getChildAt(0).performClick();

        assertEquals(0, selected.get());
    }

    @Test
    public void everyButtonOfTheBarIsDrawnAtTheRealSize() {
        AppCompatActivity activity = Robolectric.buildActivity(AppCompatActivity.class).setup().get();
        FrameLayout host = host(activity, 1000, 400);
        QuickBarPreview.render(activity, host, new float[] {851f, 393f}, oneBar(BarSets.TOP_LEFT), 0, "none", QuickBarLook.STYLE_RAIL, index -> { });

        ViewGroup layer = (ViewGroup) ((FrameLayout) host.getChildAt(0)).getChildAt(0);
        float density = activity.getResources().getDisplayMetrics().density;
        assertEquals(Math.round(851 * density), layer.getLayoutParams().width);
        assertEquals(Math.round(393 * density), layer.getLayoutParams().height);
        ViewGroup grid = (ViewGroup) layer.getChildAt(0);
        ViewGroup line = (ViewGroup) grid.getChildAt(0);
        assertEquals(3, line.getChildCount());
        assertEquals((int) (BarFit.BUTTON_DP * density), line.getChildAt(0).getLayoutParams().width);
    }

    @Test
    public void noBarsShowsTheEmptyText() {
        AppCompatActivity activity = Robolectric.buildActivity(AppCompatActivity.class).setup().get();
        FrameLayout host = host(activity, 1000, 400);
        QuickBarPreview.render(activity, host, new float[] {851f, 393f}, Collections.emptyList(), -1, "No bars yet", QuickBarLook.STYLE_RAIL, index -> { });
        View last = host.getChildAt(host.getChildCount() - 1);
        assertEquals("No bars yet", ((TextView) last).getText().toString());
    }

    @Test
    public void aRoomThatHasNoSizeYetDrawsNothing() {
        AppCompatActivity activity = Robolectric.buildActivity(AppCompatActivity.class).setup().get();
        FrameLayout host = new FrameLayout(activity);
        QuickBarPreview.render(activity, host, new float[] {851f, 393f}, oneBar(BarSets.TOP_LEFT), 0, "none", QuickBarLook.STYLE_RAIL, index -> { });
        assertEquals(0, host.getChildCount());
    }
}
