package com.limelight.preferences;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Handler;
import android.os.Looper;
import android.text.InputFilter;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

// NOTE: Shared styling for numeric stepper rows in slider dialogs.
// NOTE: Features compact round buttons, filled boxes, auto-repeat acceleration, and theme-adaptive coloring.
final class StepperViews {
    // Button diameter, box corner, and box padding in dp.
    static final int BUTTON_DP = 42;
    // Space between the row and the bar under it in dp.
    private static final int BAR_TOP_DP = 14;
    // Side inset of the bar in dp: brings the thumb's outer edge in line with the dialog text indent.
    private static final int BAR_SIDE_DP = 16;
    // Diameter of the bar thumb in dp.
    private static final int THUMB_DP = 24;
    // Fill of the dialog button pills: text color at 10 percent.
    private static final int PILL_ALPHA = 0x1A;
    private static final int CORNER_DP = 12;
    private static final int BOX_PAD_H_DP = 8;
    private static final int BOX_PAD_V_DP = 4;
    // Fill and border of buttons and boxes: theme text color at 8 percent and 20 percent.
    private static final int FILL_ALPHA = 0x14;
    private static final int STROKE_ALPHA = 0x33;
    private static final int RIPPLE_ALPHA = 0x40;

    private StepperViews() {
    }

    static int dp(Context context, int value) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, context.getResources().getDisplayMetrics()));
    }

    private static int themeColor(Context context, int attr, int fallback) {
        TypedArray a = context.obtainStyledAttributes(new int[]{attr});
        try {
            return a.getColor(0, fallback);
        } finally {
            a.recycle();
        }
    }

    static int textColor(Context context) {
        return themeColor(context, android.R.attr.textColorPrimary, Color.WHITE);
    }

    static int secondaryTextColor(Context context) {
        return themeColor(context, android.R.attr.textColorSecondary, Color.GRAY);
    }

    private static int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | (alpha << 24);
    }

    private static GradientDrawable shape(Context context, int kind, int fill, int stroke) {
        GradientDrawable d = new GradientDrawable();
        d.setShape(kind);
        if (kind == GradientDrawable.RECTANGLE) {
            d.setCornerRadius(dp(context, CORNER_DP));
        }
        d.setColor(fill);
        d.setStroke(dp(context, 1), stroke);
        return d;
    }

    // Round minus or plus button with ripple and centered vector icon.
    static ImageView roundButton(Context context, int icon, String description) {
        int on = textColor(context);
        ImageView b = new ImageView(context);
        b.setImageResource(icon);
        b.setImageTintList(ColorStateList.valueOf(on));
        b.setScaleType(ImageView.ScaleType.CENTER);
        b.setContentDescription(description);
        b.setClickable(true);
        b.setFocusable(true);
        GradientDrawable oval = shape(context, GradientDrawable.OVAL, withAlpha(on, FILL_ALPHA), withAlpha(on, STROKE_ALPHA));
        b.setBackground(new RippleDrawable(ColorStateList.valueOf(withAlpha(on, RIPPLE_ALPHA)), oval, null));
        return b;
    }

    static LinearLayout.LayoutParams buttonParams(Context context) {
        int size = dp(context, BUTTON_DP);
        return new LinearLayout.LayoutParams(size, size);
    }

    // Number field without underline, placed inside a rounded box.
    static EditText numberField(Context context, int widthDp, int maxLength) {
        EditText f = new EditText(context);
        f.setBackground(null);
        f.setGravity(Gravity.CENTER);
        f.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        f.setTextColor(textColor(context));
        f.setSingleLine(true);
        f.setSelectAllOnFocus(true);
        f.setInputType(InputType.TYPE_CLASS_NUMBER);
        f.setImeOptions(EditorInfo.IME_ACTION_DONE);
        f.setPadding(0, dp(context, 4), 0, dp(context, 4));
        f.setMinWidth(dp(context, widthDp));
        if (maxLength > 0) {
            f.setFilters(new InputFilter[]{new InputFilter.LengthFilter(maxLength)});
        }
        return f;
    }

    // Rounded filled box around a field, with optional trailing unit.
    static LinearLayout box(Context context, EditText field, String unit) {
        int on = textColor(context);
        LinearLayout box = new LinearLayout(context);
        box.setOrientation(LinearLayout.HORIZONTAL);
        box.setGravity(Gravity.CENTER_VERTICAL);
        box.setBackground(shape(context, GradientDrawable.RECTANGLE, withAlpha(on, FILL_ALPHA), withAlpha(on, STROKE_ALPHA)));
        box.setPadding(dp(context, BOX_PAD_H_DP), dp(context, BOX_PAD_V_DP), dp(context, BOX_PAD_H_DP), dp(context, BOX_PAD_V_DP));
        box.addView(field, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        if (unit != null && !unit.isEmpty()) {
            TextView u = new TextView(context);
            u.setText(unit);
            u.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
            u.setTextColor(secondaryTextColor(context));
            u.setPadding(dp(context, 6), 0, 0, 0);
            box.addView(u, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        }
        box.setOnClickListener(v -> field.requestFocus());
        return box;
    }

    // Margins of the row middle part, maintaining clean separation on compact phone screens.
    static LinearLayout.LayoutParams middleParams(Context context) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        p.setMargins(dp(context, 6), 0, dp(context, 6), 0);
        return p;
    }

    // Layout params for the slider bar placed underneath the stepper row.
    // Inset from both sides so the thumb at the minimum or maximum stops on the text line, not at the dialog edge.
    static LinearLayout.LayoutParams barParams(Context context) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        p.setMargins(dp(context, BAR_SIDE_DP), dp(context, BAR_TOP_DP), dp(context, BAR_SIDE_DP), 0);
        return p;
    }

    // Attaches tap-and-hold auto-repeat acceleration with haptic feedback to a stepper button.
    static void attachAutoRepeat(View button, Runnable action) {
        button.setOnTouchListener(new View.OnTouchListener() {
            private static final int INITIAL_DELAY_MS = 380;
            private static final int INITIAL_INTERVAL_MS = 100;
            private static final int MIN_INTERVAL_MS = 35;
            private static final int ACCEL_STEP_MS = 10;

            private final Handler handler = new Handler(Looper.getMainLooper());
            private int currentInterval;
            private View pressedView;

            private final Runnable repeatRunnable = new Runnable() {
                @Override
                public void run() {
                    if (pressedView == null || !pressedView.isPressed()) {
                        return;
                    }
                    action.run();
                    pressedView.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                    currentInterval = Math.max(MIN_INTERVAL_MS, currentInterval - ACCEL_STEP_MS);
                    handler.postDelayed(this, currentInterval);
                }
            };

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        pressedView = v;
                        currentInterval = INITIAL_INTERVAL_MS;
                        action.run();
                        v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                        v.setPressed(true);
                        handler.removeCallbacks(repeatRunnable);
                        handler.postDelayed(repeatRunnable, INITIAL_DELAY_MS);
                        return true;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        if (pressedView != null) {
                            pressedView.setPressed(false);
                            pressedView = null;
                        }
                        handler.removeCallbacks(repeatRunnable);
                        return true;
                }
                return false;
            }
        });
    }

    // Description under dialog title with the same left and right indent as the title.
    static void alignWithTitle(Context context, TextView text, int parentPaddingPx) {
        Context dialogContext = new androidx.appcompat.app.AlertDialog.Builder(context).getContext();
        TypedArray a = dialogContext.obtainStyledAttributes(new int[]{androidx.appcompat.R.attr.dialogPreferredPadding});
        int indent;
        try {
            indent = a.getDimensionPixelSize(0, dp(context, 24));
        } finally {
            a.recycle();
        }
        int side = Math.max(0, indent - parentPaddingPx);
        text.setPadding(side, dp(context, 4), side, dp(context, 4));
    }

    // Matches description side indent to dialog title after layout is drawn.
    static void matchTitleIndent(androidx.appcompat.app.AlertDialog dialog, TextView description) {
        if (description == null) {
            return;
        }
        description.post(() -> {
            View title = dialog.findViewById(androidx.appcompat.R.id.alertTitle);
            if (title == null || title.getWidth() == 0) {
                return;
            }
            int[] t = new int[2];
            int[] d = new int[2];
            title.getLocationOnScreen(t);
            description.getLocationOnScreen(d);
            int indent = (t[0] + title.getPaddingLeft()) - d[0];
            if (indent > 0) {
                description.setPadding(indent, description.getPaddingTop(), indent, description.getPaddingBottom());
            }
            View ok = dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE);
            if (ok != null && ok.getParent() instanceof View) {
                View row = (View) ok.getParent();
                int[] o = new int[2];
                ok.getLocationOnScreen(o);
                int textRight = d[0] + description.getWidth() - Math.max(indent, 0);
                int shift = (o[0] + ok.getWidth()) - textRight;
                int extraBottom = row.getTag() == null ? dp(description.getContext(), 8) : 0;
                if (shift > 0 || extraBottom > 0) {
                    row.setPadding(row.getPaddingLeft(), row.getPaddingTop(), row.getPaddingRight() + Math.max(shift, 0),
                            row.getPaddingBottom() + extraBottom);
                    row.setTag("aligned");
                }
            }
        });
    }

    // Tints thumb and progress track in the primary accent color.
    static void tintBar(SeekBar bar, int color) {
        ColorStateList c = ColorStateList.valueOf(color);
        Context context = bar.getContext();
        GradientDrawable thumb = new GradientDrawable();
        thumb.setShape(GradientDrawable.OVAL);
        thumb.setColor(color);
        thumb.setSize(dp(context, THUMB_DP), dp(context, THUMB_DP));
        bar.setThumb(thumb);
        bar.setThumbOffset(dp(context, THUMB_DP) / 2);
        bar.setProgressTintList(c);
    }

    // Pill shaped backgrounds for Cancel and OK dialog buttons.
    static void pillButtons(androidx.appcompat.app.AlertDialog dialog) {
        int[] which = {androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE, androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE};
        for (int w : which) {
            android.widget.Button b = dialog.getButton(w);
            if (b == null) {
                continue;
            }
            Context context = b.getContext();
            int color = b.getCurrentTextColor();
            if (b instanceof com.google.android.material.button.MaterialButton) {
                com.google.android.material.button.MaterialButton mb = (com.google.android.material.button.MaterialButton) b;
                mb.setBackgroundTintList(ColorStateList.valueOf(withAlpha(color, PILL_ALPHA)));
                mb.setCornerRadius(dp(context, 100));
                mb.setRippleColor(ColorStateList.valueOf(withAlpha(color, RIPPLE_ALPHA)));
                mb.setPadding(dp(context, 20), mb.getPaddingTop(), dp(context, 20), mb.getPaddingBottom());
                continue;
            }
            GradientDrawable pill = new GradientDrawable();
            pill.setShape(GradientDrawable.RECTANGLE);
            pill.setCornerRadius(dp(context, 100));
            pill.setColor(withAlpha(color, PILL_ALPHA));
            b.setBackground(new RippleDrawable(ColorStateList.valueOf(withAlpha(color, RIPPLE_ALPHA)), pill, null));
            b.setPadding(dp(context, 20), b.getPaddingTop(), dp(context, 20), b.getPaddingBottom());
            if (b.getLayoutParams() instanceof android.view.ViewGroup.MarginLayoutParams) {
                android.view.ViewGroup.MarginLayoutParams p = (android.view.ViewGroup.MarginLayoutParams) b.getLayoutParams();
                p.setMargins(dp(context, 6), p.topMargin, dp(context, 6), p.bottomMargin);
                b.setLayoutParams(p);
            }
        }
    }
}
