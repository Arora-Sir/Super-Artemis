package com.limelight.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

// NOTE: Lays out children left to right and wraps to a new line when a row is full, like a chip group.
// NOTE: Kept independent of the Material chip classes so it works in every app style and on every Android version.
public class FlowRow extends ViewGroup {
    private final int gap;

    public FlowRow(Context context) {
        this(context, null);
    }

    public FlowRow(Context context, AttributeSet attrs) {
        super(context, attrs);
        gap = (int) Dialogs.dp(context, 8);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int maxWidth = MeasureSpec.getSize(widthMeasureSpec) - getPaddingLeft() - getPaddingRight();
        boolean bounded = MeasureSpec.getMode(widthMeasureSpec) != MeasureSpec.UNSPECIFIED;
        int x = 0;
        int y = 0;
        int rowHeight = 0;
        int widest = 0;
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == GONE) {
                continue;
            }
            // A chip never gets wider than the row, long labels are shortened in the middle instead
            int childWidthSpec = bounded ? MeasureSpec.makeMeasureSpec(maxWidth, MeasureSpec.AT_MOST) : MeasureSpec.UNSPECIFIED;
            child.measure(childWidthSpec, MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
            int w = child.getMeasuredWidth();
            if (bounded && x > 0 && x + w > maxWidth) {
                x = 0;
                y += rowHeight + gap;
                rowHeight = 0;
            }
            x += w + gap;
            widest = Math.max(widest, x - gap);
            rowHeight = Math.max(rowHeight, child.getMeasuredHeight());
        }
        int width = bounded ? MeasureSpec.getSize(widthMeasureSpec) : widest + getPaddingLeft() + getPaddingRight();
        setMeasuredDimension(width, y + rowHeight + getPaddingTop() + getPaddingBottom());
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int maxWidth = r - l - getPaddingLeft() - getPaddingRight();
        int x = 0;
        int y = 0;
        int rowHeight = 0;
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == GONE) {
                continue;
            }
            int w = child.getMeasuredWidth();
            int h = child.getMeasuredHeight();
            if (x > 0 && x + w > maxWidth) {
                x = 0;
                y += rowHeight + gap;
                rowHeight = 0;
            }
            int left = getPaddingLeft() + x;
            int top = getPaddingTop() + y;
            child.layout(left, top, left + w, top + h);
            x += w + gap;
            rowHeight = Math.max(rowHeight, h);
        }
    }
}
