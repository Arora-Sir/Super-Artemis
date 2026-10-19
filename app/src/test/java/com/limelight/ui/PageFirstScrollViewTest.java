package com.limelight.ui;

import static org.junit.Assert.assertEquals;

import android.view.View;
import android.widget.FrameLayout;

import androidx.core.view.ViewCompat;
import androidx.test.core.app.ApplicationProvider;

import com.limelight.shadows.ShadowMoonBridge;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

// NOTE: On a phone in landscape the menu editor list is shorter than the dialog body, so a drag on the list must move the page first.
// NOTE: Seen on the S24: only two rows showed and the header never scrolled away while the list scrolled under it.
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, shadows = {ShadowMoonBridge.class})
public class PageFirstScrollViewTest {
    @Test
    public void listDragMovesThePageFirstThenTheList() {
        PageFirstScrollView page = new PageFirstScrollView(ApplicationProvider.getApplicationContext());
        View content = new View(page.getContext());
        content.setMinimumHeight(1000);
        page.addView(content, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, 1000));
        page.measure(View.MeasureSpec.makeMeasureSpec(500, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY));
        page.layout(0, 0, 500, 400);

        int[] consumed = new int[2];
        page.onNestedPreScroll(content, 0, 250, consumed, ViewCompat.TYPE_TOUCH);
        assertEquals(250, consumed[1]);
        assertEquals(250, page.getScrollY());

        consumed = new int[2];
        page.onNestedPreScroll(content, 0, 500, consumed, ViewCompat.TYPE_TOUCH);
        assertEquals(350, consumed[1]);
        assertEquals(600, page.getScrollY());

        // At the end of the page the list gets the whole drag
        consumed = new int[2];
        page.onNestedPreScroll(content, 0, 100, consumed, ViewCompat.TYPE_TOUCH);
        assertEquals(0, consumed[1]);

        // A drag down is left for the list first
        consumed = new int[2];
        page.onNestedPreScroll(content, 0, -100, consumed, ViewCompat.TYPE_TOUCH);
        assertEquals(0, consumed[1]);
        assertEquals(600, page.getScrollY());
    }
}
