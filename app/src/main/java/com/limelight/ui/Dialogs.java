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

    public static float dp(Context context, float value) {
        return value * context.getResources().getDisplayMetrics().density;
    }
}
