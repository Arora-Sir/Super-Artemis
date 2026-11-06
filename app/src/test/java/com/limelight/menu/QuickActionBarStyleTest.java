package com.limelight.menu;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.FrameLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.limelight.actions.StreamAction;
import com.limelight.actions.StreamActionHost;
import com.limelight.customkeys.BuiltInKeys;
import com.limelight.customkeys.CustomKey;
import com.limelight.shadows.ShadowMoonBridge;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

// NOTE: The three looks (rail, round, handle) and the idle behavior of each: a peek tab for rail and round, a chip for the handle.
// NOTE: Each bar adds its views to the parent in the order bar, chip, peek.
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, shadows = {ShadowMoonBridge.class})
public class QuickActionBarStyleTest {
    private Context context;
    private SharedPreferences prefs;
    private FrameLayout parent;
    private QuickActionBar bar;

    @Before
    public void setUp() {
        AppCompatActivity activity = Robolectric.buildActivity(AppCompatActivity.class).setup().get();
        context = activity;
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().clear().commit();
        List<String> items = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            items.add(QuickBarLayout.customKeyItem(BuiltInKeys.ALL.get(i).id));
        }
        BarSets.save(context, new BarSets.State(Collections.singletonList(new BarSets.Set("Test",
                Collections.singletonList(new BarSets.Bar(BarSets.TOP_LEFT, 3, false, 0, 0, items)))), 0));
        parent = new FrameLayout(context);
        bar = new QuickActionBar(context, parent, new QuickActionBar.Host() {
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
    }

    private void connectWith(String style, int idleSeconds) {
        prefs.edit().putString(QuickActionBar.PREF_STYLE, style).putInt(IdlePlan.PREF_HIDE_SECONDS, idleSeconds).commit();
        bar.setConnected(true);
    }

    private View barView() {
        return parent.getChildAt(0);
    }

    private View chipView() {
        return parent.getChildAt(1);
    }

    private View peekView() {
        return parent.getChildAt(2);
    }

    @Test
    public void anUnknownStyleFallsBackToTheRail() {
        assertEquals(QuickActionBar.STYLE_RAIL, QuickActionBar.normalizeStyle(null));
        assertEquals(QuickActionBar.STYLE_RAIL, QuickActionBar.normalizeStyle("bogus"));
        assertEquals(QuickActionBar.STYLE_ROUND, QuickActionBar.normalizeStyle("round"));
        assertEquals(QuickActionBar.STYLE_HANDLE, QuickActionBar.normalizeStyle("handle"));
    }

    @Test
    public void theRailStyleDrawsOneContainerAndHidesTheChipAndTab() {
        connectWith("rail", 0);
        assertNotNull(barView().getBackground());
        assertEquals(View.VISIBLE, barView().getVisibility());
        assertEquals(View.GONE, chipView().getVisibility());
        assertEquals(View.GONE, peekView().getVisibility());
    }

    @Test
    public void theRoundStyleHasNoContainer() {
        connectWith("round", 0);
        assertNull(barView().getBackground());
        assertEquals(View.VISIBLE, barView().getVisibility());
    }

    @Test
    public void theHandleStyleStartsAsAChipAndOpensWhenTapped() {
        connectWith("handle", 0);
        assertEquals(View.GONE, barView().getVisibility());
        assertEquals(View.VISIBLE, chipView().getVisibility());

        chipView().performClick();
        assertEquals(View.VISIBLE, barView().getVisibility());
        assertEquals(View.GONE, chipView().getVisibility());
    }

    @Test
    public void anOpenHandleClosesAgainWhenTheTimeIsUp() {
        connectWith("handle", 3);
        chipView().performClick();
        assertEquals(View.VISIBLE, barView().getVisibility());

        ShadowLooper.idleMainLooper(3500, TimeUnit.MILLISECONDS);
        assertEquals(View.GONE, barView().getVisibility());
        assertEquals(View.VISIBLE, chipView().getVisibility());
    }

    @Test
    public void aHandleWithNoIdleTimeStillClosesAfterTheDefaultWait() {
        connectWith("handle", 0);
        chipView().performClick();
        ShadowLooper.idleMainLooper(5500, TimeUnit.MILLISECONDS);
        assertEquals(View.GONE, barView().getVisibility());
        assertEquals(View.VISIBLE, chipView().getVisibility());
    }

    @Test
    public void aRailThatHidesOnItsOwnLeavesATabThatBringsItBack() {
        connectWith("rail", 3);
        ShadowLooper.idleMainLooper(3500, TimeUnit.MILLISECONDS);
        assertEquals(View.GONE, barView().getVisibility());
        assertEquals(View.VISIBLE, peekView().getVisibility());

        peekView().performClick();
        assertEquals(View.VISIBLE, barView().getVisibility());
        assertEquals(View.GONE, peekView().getVisibility());
    }

    @Test
    public void aRailDimsFirstAndHidesLaterAndATouchBringsItBackAtFullOpacity() {
        new IdlePlan(2, 30, 8, 100).save(prefs);
        connectWith("rail", 8);
        ShadowLooper.idleMainLooper(2500, TimeUnit.MILLISECONDS);
        assertEquals(View.VISIBLE, barView().getVisibility());
        assertEquals(0.3f, barView().getAlpha(), 0.01f);

        ShadowLooper.idleMainLooper(6000, TimeUnit.MILLISECONDS);
        assertEquals(View.GONE, barView().getVisibility());

        bar.onStreamTouch();
        assertEquals(1f, barView().getAlpha(), 0.01f);
    }

    @Test
    public void aTouchWhileDimmedRestoresFullOpacity() {
        new IdlePlan(2, 30, 0, 100).save(prefs);
        connectWith("rail", 0);
        ShadowLooper.idleMainLooper(2500, TimeUnit.MILLISECONDS);
        assertEquals(0.3f, barView().getAlpha(), 0.01f);
        bar.onStreamTouch();
        assertEquals(1f, barView().getAlpha(), 0.01f);
    }

    @Test
    public void theEdgeTabFollowsItsOpacityAndZeroLeavesNoTab() {
        new IdlePlan(0, 40, 3, 50).save(prefs);
        connectWith("rail", 3);
        ShadowLooper.idleMainLooper(3500, TimeUnit.MILLISECONDS);
        assertEquals(View.VISIBLE, peekView().getVisibility());
        assertEquals(0.5f, peekView().getAlpha(), 0.01f);

        new IdlePlan(0, 40, 3, 0).save(prefs);
        bar.reload();
        ShadowLooper.idleMainLooper(3500, TimeUnit.MILLISECONDS);
        assertEquals(View.GONE, barView().getVisibility());
        assertEquals(View.GONE, peekView().getVisibility());

        // With no tab, a touch on the stream is the way back
        bar.onStreamTouch();
        assertEquals(View.VISIBLE, barView().getVisibility());
    }

    @Test
    public void aTouchOnTheStreamAlsoBringsBackAHiddenRail() {
        connectWith("rail", 3);
        ShadowLooper.idleMainLooper(3500, TimeUnit.MILLISECONDS);
        bar.onStreamTouch();
        assertEquals(View.VISIBLE, barView().getVisibility());
        assertEquals(View.GONE, peekView().getVisibility());
    }

    @Test
    public void theManualToggleHidesEveryViewOfTheBar() {
        connectWith("handle", 0);
        bar.toggleManualHidden();
        assertEquals(View.GONE, barView().getVisibility());
        assertEquals(View.GONE, chipView().getVisibility());
        assertEquals(View.GONE, peekView().getVisibility());

        bar.toggleManualHidden();
        assertEquals(View.VISIBLE, chipView().getVisibility());
    }
}
