package com.limelight.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.limelight.R;

// NOTE: Shared building blocks for Super Artemis dialogs, so every dialog has the same header, width rules and type scale.
// NOTE: Phones get the full screen width minus the dialog margins, tablets cap the width so lines stay readable.
public final class Dialogs {
    // Material 3 reading width for lists, settings and short messages
    public static final int WIDTH_STANDARD_DP = 560;
    // Content-heavy dialogs such as credits or editors with a live preview
    public static final int WIDTH_WIDE_DP = 840;
    // The dialog background keeps 16dp clear on each side, which is part of the window width
    private static final int BACKGROUND_INSET_DP = 32;

    private Dialogs() {
    }

    private static final int FADE_EDGE_DP = 16;

    public static AlertDialog show(AlertDialog.Builder builder, int maxWidthDp) {
        AlertDialog dialog = builder.show();
        fitWidth(dialog, maxWidthDp);
        return dialog;
    }

    public static void fitWidth(Dialog dialog, int maxWidthDp) {
        Window window = dialog.getWindow();
        if (window == null) {
            return;
        }
        DisplayMetrics metrics = dialog.getContext().getResources().getDisplayMetrics();
        int max = (int) ((maxWidthDp + BACKGROUND_INSET_DP) * metrics.density);
        window.setLayout(Math.min(metrics.widthPixels, max), WindowManager.LayoutParams.WRAP_CONTENT);
        softenScrollEdges(window.getDecorView(), (int) (FADE_EDGE_DP * metrics.density));
    }

    // NOTE: Every scrolling area in a dialog shows a thin divider on an edge that hides more content, as Material 3 dialogs do.
    // NOTE: A hard edge alone shows a sliver of the next card, and a fade dims the last row so it looks covered by an overlay.
    // NOTE: The divider only appears on a side with more content, so a dialog that fits shows none. Android 5 and 6 get a short fade instead.
    private static void softenScrollEdges(View view, int length) {
        if (view instanceof android.widget.ScrollView || view instanceof androidx.core.widget.NestedScrollView
                || view instanceof androidx.recyclerview.widget.RecyclerView) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                view.setVerticalFadingEdgeEnabled(false);
                view.setScrollIndicators(View.SCROLL_INDICATOR_TOP | View.SCROLL_INDICATOR_BOTTOM,
                        View.SCROLL_INDICATOR_TOP | View.SCROLL_INDICATOR_BOTTOM);
            }
            else {
                view.setVerticalFadingEdgeEnabled(true);
                view.setFadingEdgeLength(length);
            }
        }
        if (view instanceof android.view.ViewGroup) {
            android.view.ViewGroup group = (android.view.ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                softenScrollEdges(group.getChildAt(i), length);
            }
        }
    }

    // True on phone sized screens, where side by side content inside a dialog gets too narrow and should stack
    public static boolean isCompact(Context context) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return metrics.widthPixels / metrics.density < 600;
    }

    // True when a dialog of this width has room for two columns, such as credits cards on a tablet
    public static boolean isWide(Context context) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return metrics.widthPixels / metrics.density >= WIDTH_WIDE_DP;
    }

    // Material 3 dialog header: a tonal icon above a centered title and an optional supporting line
    public static View header(Context context, int iconRes, CharSequence title, CharSequence subtitle) {
        View header = LayoutInflater.from(context).inflate(R.layout.sa_dialog_header, null);
        ImageView icon = header.findViewById(R.id.sa_dialog_header_icon);
        if (iconRes != 0) {
            icon.setImageResource(iconRes);
        }
        else {
            icon.setVisibility(View.GONE);
        }
        ((TextView) header.findViewById(R.id.sa_dialog_header_title)).setText(title);
        TextView subtitleView = header.findViewById(R.id.sa_dialog_header_subtitle);
        if (TextUtils.isEmpty(subtitle)) {
            subtitleView.setVisibility(View.GONE);
        }
        else {
            subtitleView.setText(subtitle);
        }
        return header;
    }

    // Resolves a color role such as R.attr.saPrimary in the context's theme
    public static int color(Context context, int attr) {
        TypedValue value = new TypedValue();
        context.getTheme().resolveAttribute(attr, value, true);
        return value.resourceId != 0 ? context.getColor(value.resourceId) : value.data;
    }

    // Puts a small icon before a dialog button label, tinted like the label, such as the share icon on Share
    public static void setButtonIcon(android.widget.Button button, int iconRes) {
        if (button == null) {
            return;
        }
        android.graphics.drawable.Drawable icon = androidx.core.content.ContextCompat.getDrawable(button.getContext(), iconRes);
        if (icon == null) {
            return;
        }
        icon = androidx.core.graphics.drawable.DrawableCompat.wrap(icon.mutate());
        int size = (int) dp(button.getContext(), 18);
        icon.setBounds(0, 0, size, size);
        androidx.core.graphics.drawable.DrawableCompat.setTintList(icon, button.getTextColors());
        button.setCompoundDrawablesRelative(icon, null, null, null);
        button.setCompoundDrawablePadding((int) dp(button.getContext(), 8));
    }

    public static float dp(Context context, float value) {
        return value * context.getResources().getDisplayMetrics().density;
    }
}
