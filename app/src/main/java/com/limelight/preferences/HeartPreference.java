package com.limelight.preferences;

import android.animation.Keyframe;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.preference.Preference;

import com.limelight.R;

// NOTE: The Support development row. Its heart gives two soft beats and rests, so it reads as alive without nagging.
// NOTE: The beat lives in the icon drawable, so a recycled row can never inherit it. The drawable beats only while its view is on screen and animations are on in the system settings.
public class HeartPreference extends Preference {
    public HeartPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    public HeartPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public HeartPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public HeartPreference(Context context) {
        super(context);
        init(context);
    }

    private void init(Context context) {
        Drawable heart = AppCompatResources.getDrawable(context, R.drawable.ic_sa_favorite_red);
        if (heart != null) {
            setIcon(new BeatingDrawable(heart.mutate()));
        }
    }

    // Draws the inner drawable between 90 and 100 percent of its bounds, so the beat never grows past the icon box that clips it
    static final class BeatingDrawable extends Drawable {
        static final long CYCLE_MS = 2400;
        static final float REST = 0.9f;
        static final float PEAK = 1.0f;

        private final Drawable inner;
        private final ValueAnimator animator;
        private float scale = REST;

        BeatingDrawable(Drawable inner) {
            this.inner = inner;
            Keyframe[] frames = {
                    Keyframe.ofFloat(0f, REST), Keyframe.ofFloat(0.10f, PEAK), Keyframe.ofFloat(0.20f, REST),
                    Keyframe.ofFloat(0.30f, 0.96f), Keyframe.ofFloat(0.42f, REST), Keyframe.ofFloat(1f, REST)};
            animator = ValueAnimator.ofPropertyValuesHolder(PropertyValuesHolder.ofKeyframe("scale", frames));
            animator.setDuration(CYCLE_MS);
            animator.setRepeatCount(ValueAnimator.INFINITE);
            animator.addUpdateListener(a -> {
                scale = (float) a.getAnimatedValue();
                invalidateSelf();
            });
        }

        float scale() {
            return scale;
        }

        @Override
        public boolean setVisible(boolean visible, boolean restart) {
            boolean changed = super.setVisible(visible, restart);
            if (visible && ValueAnimator.areAnimatorsEnabled()) {
                if (!animator.isStarted()) {
                    animator.start();
                }
            }
            else {
                animator.cancel();
                scale = REST;
            }
            return changed;
        }

        @Override
        public void draw(@NonNull Canvas canvas) {
            int save = canvas.save();
            canvas.scale(scale, scale, getBounds().exactCenterX(), getBounds().exactCenterY());
            inner.setBounds(getBounds());
            inner.draw(canvas);
            canvas.restoreToCount(save);
        }

        @Override
        protected void onBoundsChange(@NonNull android.graphics.Rect bounds) {
            inner.setBounds(bounds);
        }

        @Override
        public int getIntrinsicWidth() {
            return inner.getIntrinsicWidth();
        }

        @Override
        public int getIntrinsicHeight() {
            return inner.getIntrinsicHeight();
        }

        @Override
        public void setAlpha(int alpha) {
            inner.setAlpha(alpha);
        }

        @Override
        public void setColorFilter(ColorFilter colorFilter) {
            inner.setColorFilter(colorFilter);
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }
}
