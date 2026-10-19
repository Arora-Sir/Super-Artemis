package com.limelight.ui;

import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.widget.NestedScrollView;

// NOTE: Scroll container for editor dialogs on a phone in landscape, where a header sits above a list of fixed height.
// NOTE: Dragging the list up scrolls this page first until the header is gone, then the list itself, like a collapsing toolbar.
// NOTE: Dragging down scrolls the list back to its top first, and the rest moves the page, which NestedScrollView already does.
public class PageFirstScrollView extends NestedScrollView {
    public PageFirstScrollView(@NonNull Context context) {
        super(context);
    }

    @Override
    public void onNestedPreScroll(@NonNull View target, int dx, int dy, @NonNull int[] consumed, int type) {
        if (dy > 0 && canScrollVertically(1)) {
            int before = getScrollY();
            scrollBy(0, dy);
            consumed[1] = getScrollY() - before;
            return;
        }
        super.onNestedPreScroll(target, dx, dy, consumed, type);
    }
}
