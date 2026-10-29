package com.limelight.theme;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;

import com.limelight.R;
import com.limelight.ui.Dialogs;

// NOTE: A pill with one dot per level, filled up to the chosen level, as in the Surface lift of DualClone. A touch or a drag picks the nearest dot.
// NOTE: It is focusable and answers the arrow keys and the accessibility scroll actions, so a controller or a screen reader reaches every level.
public class LiftSlider extends View {
    public interface Listener {
        void onLevel(int level);
    }

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF box = new RectF();
    private int max = AmoledLadder.LEVELS - 1;
    private int progress;
    private Listener listener;

    public LiftSlider(Context context, AttributeSet attrs) {
        super(context, attrs);
        setFocusable(true);
        setClickable(true);
    }

    public int getMax() {
        return max;
    }

    public int getProgress() {
        return progress;
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void setProgress(int level) {
        int next = Math.max(0, Math.min(max, level));
        if (next == progress) {
            return;
        }
        progress = next;
        invalidate();
        if (listener != null) {
            listener.onLevel(progress);
        }
    }

    // Sets the level without telling the listener, for the first draw
    public void setInitial(int level) {
        progress = Math.max(0, Math.min(max, level));
        invalidate();
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int height = (int) Dialogs.dp(getContext(), 48);
        setMeasuredDimension(resolveSize(0, widthSpec), resolveSize(height, heightSpec));
    }

    private float dotX(int level, float pad, float width) {
        return pad + (width - 2 * pad) * level / max;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float width = getWidth();
        float height = getHeight();
        float pad = height / 2f;
        float radius = height / 2f;
        box.set(0, 0, width, height);
        paint.setColor(Dialogs.color(getContext(), R.attr.saSurfaceContainerHighest));
        canvas.drawRoundRect(box, radius, radius, paint);
        box.set(0, 0, dotX(progress, pad, width) + pad, height);
        paint.setColor(Dialogs.color(getContext(), R.attr.saPrimary));
        canvas.drawRoundRect(box, radius, radius, paint);
        float dot = Dialogs.dp(getContext(), 3);
        for (int level = 0; level <= max; level++) {
            paint.setColor(level <= progress ? Dialogs.color(getContext(), R.attr.saOnPrimary) : Dialogs.color(getContext(), R.attr.saOnSurfaceVariant));
            canvas.drawCircle(dotX(level, pad, width), height / 2f, dot, paint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                getParent().requestDisallowInterceptTouchEvent(true);
                // fall through
            case MotionEvent.ACTION_MOVE:
                float pad = getHeight() / 2f;
                float span = Math.max(1f, getWidth() - 2 * pad);
                setProgress(Math.round((event.getX() - pad) / span * max));
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

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
            setProgress(progress - 1);
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            setProgress(progress + 1);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setRangeInfo(AccessibilityNodeInfo.RangeInfo.obtain(AccessibilityNodeInfo.RangeInfo.RANGE_TYPE_INT, 1, max + 1, progress + 1));
        info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD);
        info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_BACKWARD);
    }

    @Override
    public boolean performAccessibilityAction(int action, Bundle arguments) {
        if (action == AccessibilityNodeInfo.ACTION_SCROLL_FORWARD) {
            setProgress(progress + 1);
            return true;
        }
        if (action == AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD) {
            setProgress(progress - 1);
            return true;
        }
        return super.performAccessibilityAction(action, arguments);
    }
}
