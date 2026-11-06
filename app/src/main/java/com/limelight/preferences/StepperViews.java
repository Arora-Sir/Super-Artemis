package com.limelight.preferences;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.text.InputFilter;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

// NOTE: The look of the "-" field "+" row shared by the slider dialog and the resolution dialog: round filled buttons and a
// rounded filled box around the number with its unit inside. Colours come from the text colour of the current theme, so the
// row fits the light, dark and black themes alike. [NOT-TESTED]
final class StepperViews {
    // Button diameter, box corner, box padding, in dp
    static final int BUTTON_DP = 48;
    // Space between the row and the bar under it, in dp
    private static final int BAR_TOP_DP = 14;
    // Diameter of the bar thumb, in dp (the stock one is about 14)
    private static final int THUMB_DP = 24;
    // Fill of the dialog button pills: their text colour at 10 %
    private static final int PILL_ALPHA = 0x1A;
    private static final int CORNER_DP = 12;
    private static final int BOX_PAD_H_DP = 12;
    private static final int BOX_PAD_V_DP = 4;
    // Fill and border of buttons and boxes: the theme's text colour at 8 % and 20 %
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

    // A round "-" or "+" button; an icon rather than a character, because the font draws "-" much shorter than "+"
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

    // A number field without its own underline; it sits inside a box
    static EditText numberField(Context context, int widthDp, int maxLength) {
        EditText f = new EditText(context);
        f.setBackground(null);
        f.setGravity(Gravity.CENTER);
        f.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
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

    // The rounded filled box around a field, with the unit inside it when there is one ("15,0  Mbps")
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
        // A tap anywhere on the box puts the cursor in the field
        box.setOnClickListener(v -> field.requestFocus());
        return box;
    }

    // Margins of the row's middle part, so the buttons stand 12 dp away from the box as in the mockup
    static LinearLayout.LayoutParams middleParams(Context context) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        p.setMargins(dp(context, 12), 0, dp(context, 12), 0);
        return p;
    }

    // The bar under the row, with some air above it
    static LinearLayout.LayoutParams barParams(Context context) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        p.setMargins(0, dp(context, BAR_TOP_DP), 0, 0);
        return p;
    }

    // Description under the dialog title with the same left and right indent as the title (the dialog theme's
    // dialogPreferredPadding, 24 dp by default); parentPaddingPx is the padding the dialog layout already gives
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

    // After the dialog is drawn, gives the description the exact left indent of the title text, measured on screen, and the
    // same indent on the right; the theme value alone did not match the title on Titan 1 (owner's screenshot 2026-11-06)
    static void matchTitleIndent(androidx.appcompat.app.AlertDialog dialog, TextView description) {
        if (description == null) {
            return;
        }
        description.post(() -> {
            android.view.View title = dialog.findViewById(androidx.appcompat.R.id.alertTitle);
            if (title == null || title.getWidth() == 0) {
                return;
            }
            int[] t = new int[2];
            int[] d = new int[2];
            title.getLocationOnScreen(t);
            description.getLocationOnScreen(d);
            // Where the title text starts, against where the description view starts
            int indent = (t[0] + title.getPaddingLeft()) - d[0];
            if (indent > 0) {
                description.setPadding(indent, description.getPaddingTop(), indent, description.getPaddingBottom());
            }
            // The "OK" pill ends where the description text ends, and the button row stands a little off the bottom edge
            android.view.View ok = dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE);
            if (ok != null && ok.getParent() instanceof android.view.View) {
                android.view.View row = (android.view.View) ok.getParent();
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

    // Thumb and filled part of the bar in one colour (the dialog buttons'), as the accent bar of the mockup
    static void tintBar(SeekBar bar, int color) {
        ColorStateList c = ColorStateList.valueOf(color);
        // A round thumb of THUMB_DP, larger than the stock one, so it is easy to catch with a finger
        Context context = bar.getContext();
        GradientDrawable thumb = new GradientDrawable();
        thumb.setShape(GradientDrawable.OVAL);
        thumb.setColor(color);
        thumb.setSize(dp(context, THUMB_DP), dp(context, THUMB_DP));
        bar.setThumb(thumb);
        bar.setThumbOffset(dp(context, THUMB_DP) / 2);
        bar.setProgressTintList(c);
    }

    // "Cancel" and "OK" of the dialog on barely visible pill-shaped backgrounds of their own colour
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
                // A MaterialButton ignores a plain background, so the pill is drawn by its own tint and corner radius
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
