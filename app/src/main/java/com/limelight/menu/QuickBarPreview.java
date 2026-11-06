package com.limelight.menu;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.widget.TextViewCompat;

import com.limelight.R;
import com.limelight.ui.Dialogs;

import java.util.List;
import java.util.Map;
import java.util.function.IntConsumer;

// NOTE: The live preview of the quick bars in their editor. It lays the real screen out at full size with the same bars, buttons, icons and style as the stream, then shrinks that layer.
// NOTE: The frame around it has the shape of the screen, so a bar at a corner of the screen sits at the same corner of the preview in portrait, landscape and on a tablet.
final class QuickBarPreview {
    // One bar as the preview draws it: its saved layout, the items pinned to it and the icon of each item
    static final class PreviewBar {
        final BarSets.Bar bar;
        final List<String> items;
        final Map<String, Integer> icons;
        final CharSequence description;

        PreviewBar(BarSets.Bar bar, List<String> items, Map<String, Integer> icons, CharSequence description) {
            this.bar = bar;
            this.items = items;
            this.icons = icons;
            this.description = description;
        }
    }

    private static final int SELECTION_DP = 2;
    private static final int RAIL_CORNER_DP = 22;
    private static final float UNSELECTED_ALPHA = 0.6f;

    private QuickBarPreview() {
    }

    // The largest scale at which a screen of this size fits the room, so the whole screen stays visible
    static float fitScale(int roomWidth, int roomHeight, int screenWidth, int screenHeight) {
        return Math.min(roomWidth / (float) screenWidth, roomHeight / (float) screenHeight);
    }

    // NOTE: The style is passed in, so the editor can preview a style that is chosen but not saved yet.
    // NOTE: The screen is the long side by the short side because streams run in landscape.
    // NOTE: A screen wider than the room is shown as wide as the room, and one taller than it as tall as the room.
    static void render(Context context, FrameLayout host, float[] screenDp, List<PreviewBar> bars, int selected,
                       CharSequence emptyText, String style, IntConsumer onSelect) {
        host.removeAllViews();
        int roomWidth = host.getWidth() - host.getPaddingLeft() - host.getPaddingRight();
        int roomHeight = host.getHeight() - host.getPaddingTop() - host.getPaddingBottom();
        if (roomWidth <= 0 || roomHeight <= 0) {
            return;
        }
        float density = context.getResources().getDisplayMetrics().density;
        int screenW = Math.round(screenDp[0] * density);
        int screenH = Math.round(screenDp[1] * density);
        float scale = fitScale(roomWidth, roomHeight, screenW, screenH);

        // The frame is the screen in miniature, centered in the room. The host keeps no box of its own, so the frame is the box.
        FrameLayout frame = new FrameLayout(context);
        frame.setBackgroundResource(R.drawable.bg_preview_screen);
        frame.setClipChildren(true);
        host.setBackground(null);
        FrameLayout.LayoutParams frameParams = new FrameLayout.LayoutParams(Math.round(screenW * scale), Math.round(screenH * scale), Gravity.CENTER);
        host.addView(frame, frameParams);

        FrameLayout layer = new FrameLayout(context);
        layer.setPivotX(0);
        layer.setPivotY(0);
        layer.setScaleX(scale);
        layer.setScaleY(scale);
        frame.addView(layer, new FrameLayout.LayoutParams(screenW, screenH));

        for (int i = 0; i < bars.size(); i++) {
            PreviewBar preview = bars.get(i);
            LinearLayout grid = buildBar(context, preview, style, density);
            if (i == selected) {
                markSelected(context, grid, density, scale);
            }
            else if (selected >= 0) {
                grid.setAlpha(UNSELECTED_ALPHA);
            }
            int index = i;
            grid.setOnClickListener(v -> onSelect.accept(index));
            grid.setContentDescription(preview.description);
            layer.addView(grid, cornerParams(preview.bar, density));
            QuickActionBar.placeInside(grid, preview.bar.offsetXDp * density, preview.bar.offsetYDp * density);
        }
        if (bars.isEmpty()) {
            TextView empty = new TextView(context);
            empty.setText(emptyText);
            empty.setGravity(Gravity.CENTER);
            TextViewCompat.setTextAppearance(empty, R.style.TextAppearance_SuperArtemis_BodySmall);
            host.addView(empty, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
        }
    }

    // One bar at real size: the rail or round look, then the grid the stream fills by BarSets.grid
    private static LinearLayout buildBar(Context context, PreviewBar preview, String style, float density) {
        BarSets.Bar bar = preview.bar;
        LinearLayout grid = new LinearLayout(context);
        grid.setOrientation(LinearLayout.VERTICAL);
        grid.setGravity(bar.isRight() ? Gravity.END : bar.isLeft() ? Gravity.START : Gravity.CENTER_HORIZONTAL);
        QuickBarLook.styleContainer(grid, style, density);
        if (!QuickBarLook.railLook(style)) {
            int pad = (int) (4 * density);
            grid.setPadding(pad, pad, pad, pad);
        }
        int size = (int) (BarFit.BUTTON_DP * density);
        int gap = (int) (QuickBarLook.gapDp(style) * density);
        grid.setMinimumWidth(size);
        grid.setMinimumHeight(size);
        List<List<String>> cells = BarSets.grid(bar, preview.items);
        for (int r = 0; r < cells.size(); r++) {
            LinearLayout line = new LinearLayout(context);
            line.setOrientation(LinearLayout.HORIZONTAL);
            for (int c = 0; c < cells.get(r).size(); c++) {
                line.addView(cell(context, cells.get(r).get(c), preview.icons, style, density), cellParams(size, c > 0 ? gap : 0));
            }
            LinearLayout.LayoutParams lineParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            if (r > 0) {
                lineParams.topMargin = gap;
            }
            grid.addView(line, lineParams);
        }
        return grid;
    }

    // A button, or an empty cell that keeps the columns of a partly filled row lined up as in the stream
    private static View cell(Context context, String item, Map<String, Integer> icons, String style, float density) {
        if (item == null) {
            return new View(context);
        }
        Integer icon = icons.get(item);
        ImageButton button = QuickBarLook.newButton(context, icon != null ? icon : R.drawable.ic_sa_keyboard, style, density);
        // A tap on a button selects its bar, like a tap anywhere else on the bar
        button.setClickable(false);
        button.setFocusable(false);
        button.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        return button;
    }

    private static LinearLayout.LayoutParams cellParams(int size, int leftMargin) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
        params.leftMargin = leftMargin;
        return params;
    }

    // The gravity and margins the stream uses for a bar at this corner
    private static FrameLayout.LayoutParams cornerParams(BarSets.Bar bar, float density) {
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        int vertical = bar.isTop() ? Gravity.TOP : bar.isBottom() ? Gravity.BOTTOM : Gravity.CENTER_VERTICAL;
        int horizontal = bar.isLeft() ? Gravity.START : bar.isRight() ? Gravity.END : Gravity.CENTER_HORIZONTAL;
        params.gravity = vertical | horizontal;
        int edge = (int) (BarFit.EDGE_MARGIN_DP * density);
        int side = (int) (BarFit.GAP_DP * density);
        if (bar.isTop()) {
            params.topMargin = edge;
        }
        else if (bar.isBottom()) {
            params.bottomMargin = edge;
        }
        if (bar.isLeft()) {
            params.setMarginStart(side);
        }
        else if (bar.isRight()) {
            params.setMarginEnd(side);
        }
        return params;
    }

    // NOTE: An accent outline around the selected bar. The layer is shrunk, so the stroke is made thicker by the same factor and still reads as 2dp.
    private static void markSelected(Context context, LinearLayout grid, float density, float scale) {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M) {
            return;
        }
        GradientDrawable outline = new GradientDrawable();
        outline.setShape(GradientDrawable.RECTANGLE);
        outline.setCornerRadius(RAIL_CORNER_DP * density);
        outline.setStroke(Math.max(1, Math.round(SELECTION_DP * density / Math.max(0.01f, scale))), Dialogs.color(context, R.attr.saPrimary));
        grid.setForeground(outline);
    }
}
