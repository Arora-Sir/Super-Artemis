package com.limelight.customkeys;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.test.core.app.ApplicationProvider;

import com.limelight.R;
import com.limelight.shadows.ShadowMoonBridge;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// NOTE: The phone portrait recorder (layout D3): every key records a real Windows key code, labels share one size, and the switch swaps F1-F12 for F13-F24.
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, shadows = {ShadowMoonBridge.class}, qualifiers = "w411dp-h891dp-port-xxhdpi")
public class RecorderKeyboardTest {
    private static Context context() {
        return new android.view.ContextThemeWrapper(ApplicationProvider.getApplicationContext(), R.style.AppTheme_MaterialYou);
    }

    @Test
    public void everyKeyRecordsADistinctWindowsKeyCode() {
        KeyNames names = new KeyNames(context());
        Set<Integer> codes = new HashSet<>();
        List<RecorderKeyboard.Key[][]> blocks = new ArrayList<>();
        blocks.add(RecorderKeyboard.functionRows(false));
        blocks.add(RecorderKeyboard.functionRows(true));
        blocks.add(RecorderKeyboard.typingRows());
        blocks.add(RecorderKeyboard.symbolRows());
        blocks.add(RecorderKeyboard.navigationRows());
        int keys = 0;
        for (RecorderKeyboard.Key[][] block : blocks) {
            for (RecorderKeyboard.Key[] row : block) {
                for (RecorderKeyboard.Key key : row) {
                    if (key.label == null) {
                        continue;
                    }
                    int vk = RecorderKeyboard.windowsCode(key, names);
                    assertTrue(key.label + " records no key", vk > 0);
                    assertTrue(key.label + " records the same code as another key", codes.add(vk));
                    keys++;
                }
            }
        }
        // 24 function keys, 10 digits, 26 letters, 11 symbols and 21 control and navigation keys
        assertEquals(24 + 10 + 26 + 11 + 21, keys);
    }

    @Test
    public void theSwitchShowsF13ToF24AndTheirHint() {
        Context context = context();
        List<Integer> recorded = new ArrayList<>();
        ViewGroup keyboard = (ViewGroup) RecorderKeyboard.build(context, new KeyNames(context), recorded::add);
        TextView high = find(keyboard, "F13 - F24");
        assertTrue(find(keyboard, "F1") != null);
        high.performClick();
        TextView f24 = find(keyboard, "F24");
        f24.performClick();
        assertEquals(KeyNames.VK_F24, (int) recorded.get(0));
        assertTrue(find(keyboard, "F1") == null);
        assertEquals(View.VISIBLE, find(keyboard, context.getString(R.string.recorder_high_function_hint)).getVisibility());
    }

    @Test
    public void labelsShareOneSizePerKind() {
        Context context = context();
        ViewGroup keyboard = (ViewGroup) RecorderKeyboard.build(context, new KeyNames(context), vk -> { });
        Set<Float> sizes = new HashSet<>();
        collectKeySizes(keyboard, sizes);
        assertEquals(sizes.toString(), 2, sizes.size());
    }

    private static TextView find(View view, String text) {
        if (view instanceof TextView && text.contentEquals(((TextView) view).getText())) {
            return (TextView) view;
        }
        if (view instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                TextView found = find(((ViewGroup) view).getChildAt(i), text);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static void collectKeySizes(View view, Set<Float> sizes) {
        if (view instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                collectKeySizes(((ViewGroup) view).getChildAt(i), sizes);
            }
        }
        else if (view instanceof TextView && view.isClickable() && view.getContentDescription() != null) {
            sizes.add(((TextView) view).getTextSize());
        }
    }
}
