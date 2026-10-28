package com.limelight.preferences;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewOutlineProvider;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceGroupAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.limelight.R;
import com.limelight.ui.Dialogs;

// NOTE: Gives the settings list its look for the current app style. Every style gets room to scroll the last row clear of the screen edge.
// NOTE: Material You also groups each section into one rounded container, the way Android 16 and One UI settings do.
final class SettingsListStyle {
    private SettingsListStyle() {
    }

    // Returns true when the sections are drawn as rounded containers, which then replace the divider lines between sections
    static boolean apply(RecyclerView list) {
        Context context = list.getContext();
        list.setClipToPadding(false);
        list.setPadding(list.getPaddingLeft(), list.getPaddingTop(), list.getPaddingRight(), (int) dp(context, 24));
        // NOTE: A settings list needs no item animations. Rows such as the microphone preview change all the time, and the default animator slid them from their old position (BUG-045).
        list.setItemAnimator(null);
        TypedValue value = new TypedValue();
        if (context.getTheme().resolveAttribute(R.attr.saGroupedSettings, value, true) && value.data != 0) {
            list.addItemDecoration(new GroupedSections(context));
            return true;
        }
        return false;
    }

    static float dp(Context context, float value) {
        return value * context.getResources().getDisplayMetrics().density;
    }

    static int resolveColor(Context context, int attr) {
        return Dialogs.color(context, attr);
    }

    // NOTE: Rows of one section share a container: large corners on the outside, small ones between rows, and a 2dp gap between rows.
    // NOTE: Section headers sit outside the containers, indented so their titles line up with the row text.
    static final class GroupedSections extends RecyclerView.ItemDecoration {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();
        private final RectF rect = new RectF();
        private final float outerRadius;
        private final float innerRadius;
        private final int margin;
        private final int gap;

        GroupedSections(Context context) {
            paint.setColor(resolveColor(context, R.attr.saSurfaceContainer));
            outerRadius = dp(context, 24);
            innerRadius = dp(context, 4);
            margin = (int) dp(context, 16);
            gap = (int) dp(context, 2);
        }

        @SuppressLint("RestrictedApi")
        private static boolean isRow(RecyclerView parent, int position) {
            RecyclerView.Adapter<?> adapter = parent.getAdapter();
            if (!(adapter instanceof PreferenceGroupAdapter) || position < 0 || position >= adapter.getItemCount()) {
                return false;
            }
            Preference preference = ((PreferenceGroupAdapter) adapter).getItem(position);
            return preference != null && !(preference instanceof PreferenceCategory);
        }

        @Override
        public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
            int position = parent.getChildAdapterPosition(view);
            outRect.left = margin;
            outRect.right = margin;
            if (isRow(parent, position) && isRow(parent, position + 1)) {
                outRect.bottom = gap;
            }
        }

        @Override
        public void onDraw(@NonNull Canvas canvas, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
            for (int i = 0; i < parent.getChildCount(); i++) {
                View child = parent.getChildAt(i);
                int position = parent.getChildAdapterPosition(child);
                if (!isRow(parent, position)) {
                    continue;
                }
                float top = isRow(parent, position - 1) ? innerRadius : outerRadius;
                float bottom = isRow(parent, position + 1) ? innerRadius : outerRadius;
                rect.set(child.getLeft(), child.getTop() + child.getTranslationY(),
                        child.getRight(), child.getBottom() + child.getTranslationY());
                path.reset();
                path.addRoundRect(rect, new float[] {top, top, top, top, bottom, bottom, bottom, bottom}, Path.Direction.CW);
                paint.setAlpha((int) (255 * child.getAlpha()));
                canvas.drawPath(path, paint);
                clipToCorners(child, top, bottom);
            }
        }

        // The row's ripple follows the container corners, so a tap never paints outside the rounded shape
        private static void clipToCorners(View child, float top, float bottom) {
            String shape = top + "/" + bottom;
            if (shape.equals(child.getTag(R.id.sa_settings_row_shape))) {
                return;
            }
            child.setTag(R.id.sa_settings_row_shape, shape);
            child.setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        Path outlinePath = new Path();
                        outlinePath.addRoundRect(new RectF(0, 0, view.getWidth(), view.getHeight()),
                                new float[] {top, top, top, top, bottom, bottom, bottom, bottom}, Path.Direction.CW);
                        outline.setPath(outlinePath);
                    }
                    else {
                        // Older Android clips only plain rounded rectangles, so the smaller radius keeps the ripple inside
                        outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), Math.min(top, bottom));
                    }
                }
            });
            child.setClipToOutline(true);
        }
    }
}
