package com.limelight.menu;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.view.View;
import android.widget.FrameLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.limelight.actions.StreamAction;
import com.limelight.actions.StreamActionHost;
import com.limelight.actions.StreamActionRegistry;
import com.limelight.customkeys.BuiltInKeys;
import com.limelight.customkeys.CustomKey;
import com.limelight.shadows.ShadowMoonBridge;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// NOTE: The toggle action hides the bars until it runs again, and a touch on the stream never overrides that.
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, shadows = {ShadowMoonBridge.class})
public class QuickActionBarToggleTest {
    private FrameLayout parent;
    private QuickActionBar bar;

    @Before
    public void setUp() {
        AppCompatActivity activity = Robolectric.buildActivity(AppCompatActivity.class).setup().get();
        List<String> items = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            items.add(QuickBarLayout.customKeyItem(BuiltInKeys.ALL.get(i).id));
        }
        BarSets.save(activity, new BarSets.State(Collections.singletonList(new BarSets.Set("Test",
                Collections.singletonList(new BarSets.Bar(BarSets.TOP_LEFT, 3, false, 0, 0, items)))), 0));
        parent = new FrameLayout(activity);
        bar = new QuickActionBar(activity, parent, new QuickActionBar.Host() {
            @Override
            public StreamActionHost actionHost() {
                return null;
            }

            @Override
            public void runAction(StreamAction action) {
            }

            @Override
            public void runKey(CustomKey key) {
            }

            @Override
            public boolean isFloatingMenuButtonVisible() {
                return false;
            }

            @Override
            public void showBarMenu(int barIndex, String item, String buttonLabel) {
            }
        });
        bar.setConnected(true);
    }

    @Test
    public void theToggleActionIsInTheCatalog() {
        assertNotNull(StreamActionRegistry.find(StreamActionRegistry.ID_TOGGLE_QUICK_BAR));
        assertEquals("toggle_quick_bar", StreamActionRegistry.ID_TOGGLE_QUICK_BAR);
    }

    @Test
    public void barsAreVisibleAtFirst() {
        assertEquals(View.VISIBLE, parent.getChildAt(0).getVisibility());
        assertFalse(bar.isManuallyHidden());
    }

    @Test
    public void toggleHidesAndShowsTheBars() {
        bar.toggleManualHidden();
        assertTrue(bar.isManuallyHidden());
        assertEquals(View.GONE, parent.getChildAt(0).getVisibility());

        bar.toggleManualHidden();
        assertFalse(bar.isManuallyHidden());
        assertEquals(View.VISIBLE, parent.getChildAt(0).getVisibility());
    }

    @Test
    public void aTouchDoesNotBringBackManuallyHiddenBars() {
        bar.toggleManualHidden();
        bar.onStreamTouch();
        assertEquals(View.GONE, parent.getChildAt(0).getVisibility());
    }

    @Test
    public void pictureInPictureStillHidesBarsThatWereShown() {
        bar.setHidden(true);
        assertEquals(View.GONE, parent.getChildAt(0).getVisibility());
        bar.setHidden(false);
        assertEquals(View.VISIBLE, parent.getChildAt(0).getVisibility());
    }

    @Test
    public void leavingPictureInPictureKeepsManuallyHiddenBarsHidden() {
        bar.toggleManualHidden();
        bar.setHidden(true);
        bar.setHidden(false);
        assertEquals(View.GONE, parent.getChildAt(0).getVisibility());
    }
}
