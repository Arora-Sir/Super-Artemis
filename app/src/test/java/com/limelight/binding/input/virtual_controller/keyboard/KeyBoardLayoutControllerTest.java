package com.limelight.binding.input.virtual_controller.keyboard;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.preference.PreferenceManager;
import androidx.test.core.app.ApplicationProvider;

import com.limelight.preferences.PreferenceConfiguration;
import com.limelight.shadows.ShadowMoonBridge;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

// NOTE: The on-screen keyboard gets an Fn key that swaps F1 to F12 for F13 to F24, an optional F13 to F24 row, and an elevation above the quick bars.
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, shadows = {ShadowMoonBridge.class})
public class KeyBoardLayoutControllerTest {
    private Context context;
    private FrameLayout frame;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        PreferenceManager.getDefaultSharedPreferences(context).edit().clear().commit();
        frame = new FrameLayout(context);
    }

    private KeyBoardLayoutController controller() {
        KeyBoardLayoutController controller = new KeyBoardLayoutController(frame, context, PreferenceConfiguration.readPreferences(context));
        controller.refreshLayout();
        return controller;
    }

    private LinearLayout keyboard() {
        return (LinearLayout) frame.getChildAt(0);
    }

    private static MotionEvent touch(int action) {
        return MotionEvent.obtain(0, 0, action, 1, 1, 0);
    }

    private static void tap(View key) {
        key.dispatchTouchEvent(touch(MotionEvent.ACTION_DOWN));
        key.dispatchTouchEvent(touch(MotionEvent.ACTION_UP));
    }

    private View fnKey() {
        LinearLayout firstRow = (LinearLayout) keyboard().getChildAt(0);
        return firstRow.getChildAt(firstRow.getChildCount() - 1);
    }

    @Test
    public void theFirstRowEndsWithAnFnKey() {
        controller();
        assertEquals("fn", fnKey().getTag());
        assertEquals("Fn", ((TextView) fnKey()).getText().toString());
    }

    @Test
    public void fnSwapsTheFunctionRowForF13ToF24AndBack() {
        controller();
        LinearLayout firstRow = (LinearLayout) keyboard().getChildAt(0);
        // ESC first, then F1 to F12
        TextView f1 = (TextView) firstRow.getChildAt(1);
        TextView f12 = (TextView) firstRow.getChildAt(12);
        assertEquals("F1", f1.getText().toString());

        tap(fnKey());
        assertEquals("F13", f1.getText().toString());
        assertEquals("vk:7c", f1.getTag());
        assertEquals("F24", f12.getText().toString());
        assertEquals("vk:87", f12.getTag());

        tap(fnKey());
        assertEquals("F1", f1.getText().toString());
        assertEquals("131", f1.getTag());
        assertEquals("F12", f12.getText().toString());
        assertEquals("142", f12.getTag());
    }

    @Test
    public void theExtraRowIsOffByDefault() {
        controller();
        assertEquals(6, keyboard().getChildCount());
    }

    @Test
    public void theExtraRowHoldsF13ToF24UnderTheFunctionRow() {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean("keyboard_show_f13_f24_row", true).commit();
        controller();
        assertEquals(7, keyboard().getChildCount());
        LinearLayout extra = (LinearLayout) keyboard().getChildAt(1);
        assertEquals(12, extra.getChildCount());
        assertEquals("F13", ((TextView) extra.getChildAt(0)).getText().toString());
        assertEquals("vk:7c", extra.getChildAt(0).getTag());
        assertEquals("F24", ((TextView) extra.getChildAt(11)).getText().toString());
    }

    @Test
    public void theKeyboardRisesAboveTheQuickBarsWhileOpen() {
        KeyBoardLayoutController controller = controller();
        controller.show();
        assertTrue(keyboard().getElevation() > 9999 * context.getResources().getDisplayMetrics().density);
        controller.hide();
        assertEquals(0f, keyboard().getElevation(), 0f);
    }

    @Test
    public void theKeyboardStaysLowWhenTheOptionIsOff() {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean("keyboard_render_on_top", false).commit();
        KeyBoardLayoutController controller = controller();
        controller.show();
        assertEquals(0f, keyboard().getElevation(), 0f);
    }
}
