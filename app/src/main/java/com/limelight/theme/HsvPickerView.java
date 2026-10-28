package com.limelight.theme;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;


// NOTE: A saturation and value square above a hue bar. A finger or a mouse drags a round marker on either, and the color is reported on every move.
// NOTE: Both areas are drawn from gradients with no bitmap, so there is no pixel buffer to refill while a finger drags.
public class HsvPickerView extends View {
    public interface OnColorChanged {
        void onColorChanged(int color, boolean fromUser);
    }

    private static final float MARKER_DP = 11;
    private static final float BAR_DP = 32;
    private static final float GAP_DP = 14;
    private static final float CORNER_DP = 12;

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF square = new RectF();
    private final RectF bar = new RectF();
    private final float density;
    private final int[] hueColors = new int[7];

    private float hue = 220f;
    private float saturation = 0.8f;
    private float value = 0.85f;
    private boolean draggingBar;
    private boolean draggingSquare;
    private OnColorChanged listener;

    public HsvPickerView(Context context) {
        this(context, null);
    }

    public HsvPickerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        density = context.getResources().getDisplayMetrics().density;
        for (int i = 0; i < hueColors.length; i++) {
            hueColors[i] = Color.HSVToColor(new float[] {i * 60f, 1f, 1f});
        }
        ring.setStyle(Paint.Style.STROKE);
        ring.setStrokeWidth(3 * density);
        setFocusable(true);
        setContentDescription(context.getString(com.limelight.R.string.color_picker_area));
    }

    public void setOnColorChanged(OnColorChanged listener) {
        this.listener = listener;
    }

    public int getColor() {
        return AccentColors.fromHsv(hue, saturation, value);
    }

    public void setColor(int color) {
        float[] hsv = AccentColors.toHsv(color);
        // A grey has no hue of its own, so the hue the user was on stays put
        if (hsv[1] > 0.001f && hsv[2] > 0.001f) {
            hue = hsv[0];
        }
        saturation = hsv[1];
        value = hsv[2];
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = (int) ((width * 0.62f) + (GAP_DP + BAR_DP) * density);
        setMeasuredDimension(width, Math.min(height, (int) (300 * density)));
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        float margin = MARKER_DP * density;
        float barHeight = BAR_DP * density;
        square.set(margin, margin, w - margin, h - margin - barHeight - GAP_DP * density);
        bar.set(margin, square.bottom + GAP_DP * density, w - margin, square.bottom + GAP_DP * density + barHeight);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float corner = CORNER_DP * density;

        // Saturation runs left to right from white to the pure hue, value runs top to bottom from the color to black
        fill.setShader(new LinearGradient(square.left, 0, square.right, 0, Color.WHITE,
                Color.HSVToColor(new float[] {hue, 1f, 1f}), Shader.TileMode.CLAMP));
        canvas.drawRoundRect(square, corner, corner, fill);
        fill.setShader(new LinearGradient(0, square.top, 0, square.bottom, 0x00000000, Color.BLACK, Shader.TileMode.CLAMP));
        canvas.drawRoundRect(square, corner, corner, fill);

        fill.setShader(new LinearGradient(bar.left, 0, bar.right, 0, hueColors, null, Shader.TileMode.CLAMP));
        canvas.drawRoundRect(bar, bar.height() / 2, bar.height() / 2, fill);
        fill.setShader(null);

        float markerX = square.left + saturation * square.width();
        float markerY = square.top + (1f - value) * square.height();
        drawMarker(canvas, markerX, markerY, getColor());
        drawMarker(canvas, bar.left + hue / 360f * bar.width(), bar.centerY(), Color.HSVToColor(new float[] {hue, 1f, 1f}));
    }

    private void drawMarker(Canvas canvas, float x, float y, int color) {
        float radius = MARKER_DP * density;
        fill.setColor(color);
        canvas.drawCircle(x, y, radius, fill);
        ring.setColor(Color.WHITE);
        canvas.drawCircle(x, y, radius, ring);
        ring.setColor(0x66000000);
        canvas.drawCircle(x, y, radius + 1.5f * density, ring);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                // The area a drag starts on keeps the drag, so the finger can leave it without switching to the other one
                draggingBar = bar.contains(x, y) || (y > square.bottom && !square.contains(x, y));
                draggingSquare = !draggingBar;
                // The color picker sits in a scrolling dialog, so a drag on it must not scroll the dialog
                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                update(x, y);
                return true;
            case MotionEvent.ACTION_MOVE:
                update(x, y);
                return true;
            case MotionEvent.ACTION_UP:
                update(x, y);
                performClick();
                draggingBar = false;
                draggingSquare = false;
                return true;
            case MotionEvent.ACTION_CANCEL:
                draggingBar = false;
                draggingSquare = false;
                return true;
            default:
                return super.onTouchEvent(event);
        }
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    private void update(float x, float y) {
        if (draggingBar) {
            hue = clamp((x - bar.left) / bar.width()) * 360f;
        }
        else if (draggingSquare) {
            saturation = clamp((x - square.left) / square.width());
            value = 1f - clamp((y - square.top) / square.height());
        }
        invalidate();
        if (listener != null) {
            listener.onColorChanged(getColor(), true);
        }
    }

    private static float clamp(float v) {
        return Math.max(0f, Math.min(1f, v));
    }
}
