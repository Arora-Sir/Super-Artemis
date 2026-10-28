package com.limelight.menu;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;

import com.limelight.customkeys.BuiltInKeys;
import com.limelight.customkeys.CustomKey;
import com.limelight.actions.StreamAction;
import com.limelight.actions.StreamActionHost;
import com.limelight.shadows.ShadowMoonBridge;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// NOTE: A bar saved with 20 buttons per line must stay on a phone screen, with every button shown and tappable.
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, shadows = {ShadowMoonBridge.class})
public class QuickActionBarFitTest {
    private static final class Host implements QuickActionBar.Host {
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
    }

    private static void layout(View view, int width, int height) {
        view.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
        view.layout(0, 0, width, height);
    }

    private static void collect(View view, List<ImageButton> out) {
        if (view instanceof ImageButton) {
            out.add((ImageButton) view);
        }
        else if (view instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                collect(((ViewGroup) view).getChildAt(i), out);
            }
        }
    }

    private void checkOn(String qualifiers, int widthDp, int heightDp) {
        RuntimeEnvironment.setQualifiers(qualifiers);
        AppCompatActivity activity = Robolectric.buildActivity(AppCompatActivity.class).setup().get();
        List<String> items = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            items.add(QuickBarLayout.customKeyItem(BuiltInKeys.ALL.get(i).id));
        }
        BarSets.save(activity, new BarSets.State(Collections.singletonList(new BarSets.Set("Test",
                Collections.singletonList(new BarSets.Bar(BarSets.TOP_LEFT, 20, false, 0, 0, items)))), 0));

        FrameLayout parent = new FrameLayout(activity);
        QuickActionBar bar = new QuickActionBar(activity, parent, new Host());
        bar.setConnected(true);
        float density = activity.getResources().getDisplayMetrics().density;
        int width = Math.round(widthDp * density);
        layout(parent, width, Math.round(heightDp * density));

        List<ImageButton> buttons = new ArrayList<>();
        collect(parent, buttons);
        assertEquals("Every button is built", 20, buttons.size());
        View barView = parent.getChildAt(0);
        assertTrue("The bar is " + barView.getWidth() + "px wide on a " + width + "px screen", barView.getWidth() <= width);
    }

    @Test
    public void aTwentyPerLineBarStaysOnAPhone() {
        checkOn("w360dp-h640dp-xxhdpi", 360, 640);
    }

    @Test
    public void aTwentyPerLineBarStaysOnAPhoneInLandscape() {
        checkOn("w832dp-h384dp-land-xxhdpi", 832, 384);
    }

    @Test
    public void aTwentyPerLineBarStaysOnATablet() {
        checkOn("w1480dp-h924dp-xhdpi", 1480, 924);
    }
}
