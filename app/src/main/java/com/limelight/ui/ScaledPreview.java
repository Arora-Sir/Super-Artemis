package com.limelight.ui;

import android.view.View;
import android.widget.FrameLayout;

// NOTE: Shows a live view inside a box of a fixed height at a reduced scale, so a long menu preview shows more of its content without growing the dialog.
// NOTE: The content is laid out at its full, larger size and shrunk from the top left corner, so its text and tiles stay in proportion.
public final class ScaledPreview {
    private ScaledPreview() {
    }

    // The size the content is laid out at so that it fills the box once scaled
    static int contentSize(int boxSize, float scale) {
        return Math.round(boxSize / scale);
    }

    public static void show(FrameLayout host, View content, int boxHeightPx, float scale) {
        host.removeAllViews();
        host.getLayoutParams().height = boxHeightPx;
        content.setPivotX(0);
        content.setPivotY(0);
        content.setScaleX(scale);
        content.setScaleY(scale);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(1, contentSize(boxHeightPx - host.getPaddingTop() - host.getPaddingBottom(), scale));
        host.addView(content, params);
        // The width is known only once the box is on screen
        Runnable fit = () -> {
            int room = host.getWidth() - host.getPaddingLeft() - host.getPaddingRight();
            if (room > 0 && params.width != contentSize(room, scale)) {
                params.width = contentSize(room, scale);
                content.setLayoutParams(params);
            }
        };
        host.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> fit.run());
        fit.run();
    }
}
