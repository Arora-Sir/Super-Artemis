package com.limelight.preferences;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

// NOTE: Bar of the Custom Resolution dialog: a track with one dot per stop, the filled part up to the thumb and the stop
// NOTE: labels under it. The thumb may stand between two stops for a typed size; a touch, a drag or a key always lands on a
// NOTE: stop. A plain SeekBar cannot show a position between its ticks and has no labels, so the bar draws itself.
final class ResolutionStopsBar extends View {

    interface Listener {
        void onStopPicked(int index);
    }

    // Track thickness, stop dot and thumb size, gap between the track and the labels, in dp
    private static final float TRACK_DP = 4;
    private static final float DOT_DP = 3;
    // Round thumb, the same as in the other slider dialogs (StepperViews.tintBar: an accent circle of 24 dp), so every bar
    // of the app looks alike (owner's word)
    private static final float THUMB_DP = 24;
    private static final float LABEL_GAP_DP = 8;
    private static final float LABEL_SP = 12;
    // Text indent of the dialog from its edge, in dp: the end labels stop on this line, not at the dialog edge (owner's mark
    // on the build: "25%" and "100%" stood at the edges)
    private static final float SIDE_DP = 22;
    private static final int TRACK_ALPHA = 0x33;

    private final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dot = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint label = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint current = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();

    private final List<String> labels = new ArrayList<>();
    // Thumb position from 0 to labels.size() - 1; the stop under the thumb, or -1 for a typed size between stops
    private float position;
    private int currentIndex = -1;
    private Listener listener;

    ResolutionStopsBar(Context context, int accent, int textColor, int secondaryColor) {
        super(context);
        setFocusable(true);
        track.setColor((textColor & 0x00FFFFFF) | (TRACK_ALPHA << 24));
        fill.setColor(accent);
        dot.setColor(textColor);
        label.setColor(secondaryColor);
        label.setTextAlign(Paint.Align.CENTER);
        label.setTextSize(sp(LABEL_SP));
        current.set(label);
        current.setColor(accent);
        current.setTypeface(Typeface.DEFAULT_BOLD);
    }

    private float dp(float v) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
    }

    private float sp(float v) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, v, getResources().getDisplayMetrics());
    }

    // Compact height: the labels stand closer to the track
    private boolean compact;

    void setCompact(boolean compact) {
        this.compact = compact;
        requestLayout();
    }

    private float labelGap() {
        return dp(compact ? LABEL_GAP_DP / 2 : LABEL_GAP_DP);
    }

    void setListener(Listener listener) {
        this.listener = listener;
    }

    void setStops(List<String> stopLabels, float position, int currentIndex) {
        labels.clear();
        labels.addAll(stopLabels);
        this.position = position;
        this.currentIndex = currentIndex;
        String here = currentIndex >= 0 ? labels.get(currentIndex) : null;
        setContentDescription(here);
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        Paint.FontMetrics fm = label.getFontMetrics();
        int h = Math.round(dp(THUMB_DP) + labelGap() + (fm.descent - fm.ascent) + dp(4));
        setMeasuredDimension(getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec), resolveSize(h, heightMeasureSpec));
    }

    // Left end of the track: the end label's outer edge (and the thumb's) stands on the text indent of the dialog
    private float left() {
        float half = labels.isEmpty() ? 0 : current.measureText(labels.get(0)) / 2f;
        return dp(SIDE_DP) + Math.max(half, dp(THUMB_DP) / 2f);
    }

    private float right() {
        float half = labels.isEmpty() ? 0 : current.measureText(labels.get(labels.size() - 1)) / 2f;
        return getWidth() - dp(SIDE_DP) - Math.max(half, dp(THUMB_DP) / 2f);
    }

    private float xOf(float pos) {
        int n = labels.size();
        if (n < 2) {
            return left();
        }
        return left() + (right() - left()) * pos / (n - 1);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int n = labels.size();
        if (n == 0) {
            return;
        }
        float cy = dp(THUMB_DP) / 2f;
        float t = dp(TRACK_DP) / 2f;
        float l = left();
        float r = right();
        float x = xOf(position);

        rect.set(l, cy - t, r, cy + t);
        canvas.drawRoundRect(rect, t, t, track);
        rect.set(l, cy - t, x, cy + t);
        canvas.drawRoundRect(rect, t, t, fill);

        float labelY = dp(THUMB_DP) + labelGap() - label.getFontMetrics().ascent;
        for (int i = 0; i < n; i++) {
            float sx = xOf(i);
            // The dots on the filled part take the background colour, so they stay visible on the accent
            dot.setAlpha(i <= position ? 0x99 : 0x66);
            canvas.drawCircle(sx, cy, dp(DOT_DP) / 2f, dot);
            canvas.drawText(labels.get(i), sx, labelY, i == currentIndex ? current : label);
        }

        canvas.drawCircle(x, cy, dp(THUMB_DP) / 2f, fill);
    }

    private int nearest(float touchX) {
        int n = labels.size();
        if (n < 2) {
            return 0;
        }
        float f = (touchX - left()) / (right() - left()) * (n - 1);
        return Math.max(0, Math.min(n - 1, Math.round(f)));
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (labels.isEmpty() || !isEnabled()) {
            return false;
        }
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                getParent().requestDisallowInterceptTouchEvent(true);
                // fall through
            case MotionEvent.ACTION_MOVE:
                int i = nearest(e.getX());
                if (i != currentIndex && listener != null) {
                    listener.onStopPicked(i);
                }
                return true;
            case MotionEvent.ACTION_UP:
                performClick();
                return true;
            default:
                return true;
        }
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    // Left and right keys of a keyboard or a gamepad move one stop
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (listener != null && !labels.isEmpty()) {
            int base = currentIndex >= 0 ? currentIndex : Math.round(position);
            if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT && base > 0) {
                listener.onStopPicked(currentIndex >= 0 ? base - 1 : (int) Math.floor(position));
                return true;
            }
            if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT && base < labels.size() - 1) {
                listener.onStopPicked(currentIndex >= 0 ? base + 1 : (int) Math.ceil(position));
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }
}
