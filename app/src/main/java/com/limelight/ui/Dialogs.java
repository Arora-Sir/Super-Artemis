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
    // NOTE: An editor body with this tag keeps the scroll dividers for the dialog itself, but not for the lists and previews inside it.
    // NOTE: Those inner lists are boxes of their own, and a divider drawn on their edge looked like a stray line cutting through a row.
    public static final String NO_LIST_EDGES = "sa_no_list_edges";
    // NOTE: A scroll view with this tag draws no divider of its own, for a pane that sits beside another pane where the line would run across the dialog.
    public static final String NO_SCROLL_EDGES = "sa_no_scroll_edges";
    // Dialogs that are open, so a stream that shrinks into picture-in-picture can close them instead of drawing them in the small window
    private static final java.util.Set<Dialog> OPEN = java.util.Collections.newSetFromMap(new java.util.WeakHashMap<>());

    public static AlertDialog show(AlertDialog.Builder builder, int maxWidthDp) {
        AlertDialog dialog = builder.show();
        fitWidth(dialog, maxWidthDp);
        return dialog;
    }

    // NOTE: Editors whose panes have fixed heights are built again when the screen rotates, because those heights belong to the old orientation.
    private static final java.util.Map<Dialog, Runnable> RESHOW = new java.util.WeakHashMap<>();

    public static void reshowOnRotation(Dialog dialog, Runnable reshow) {
        RESHOW.put(dialog, reshow);
    }

    public static void fitWidth(Dialog dialog, int maxWidthDp) {
        Window window = dialog.getWindow();
        if (window == null) {
            return;
        }
        sizeWindow(dialog, window, maxWidthDp);
        // The dialog shrinks above the keyboard, so a search or a text field never ends up hidden behind it
        if (OPEN.add(dialog)) {
            followRotation(dialog, window, maxWidthDp);
        }
        softenScrollEdges(window.getDecorView(), (int) (FADE_EDGE_DP * dialog.getContext().getResources().getDisplayMetrics().density), false);
        if (dialog instanceof AlertDialog) {
            for (int which : new int[] {AlertDialog.BUTTON_POSITIVE, AlertDialog.BUTTON_NEGATIVE, AlertDialog.BUTTON_NEUTRAL}) {
                fadeWhenDisabled(((AlertDialog) dialog).getButton(which));
            }
        }
    }

    // NOTE: The dialog button style uses one accent color, so a disabled Save looked the same as an enabled one.
    // NOTE: This gives the label the Material 3 disabled look (38 percent of its color) while keeping the enabled color. Android 5 and 6 cannot read theme colors in a color list file, so it is built here.
    private static void sizeWindow(Dialog dialog, Window window, int maxWidthDp) {
        DisplayMetrics metrics = dialog.getContext().getResources().getDisplayMetrics();
        int max = (int) ((maxWidthDp + BACKGROUND_INSET_DP) * metrics.density);
        window.setLayout(Math.min(metrics.widthPixels, max), WindowManager.LayoutParams.WRAP_CONTENT);
    }

    // NOTE: A dialog that stays open through a rotation kept the width of the old orientation and looked too small or cut off.
    // NOTE: It is fitted again, or built again when its editor asked for that.
    private static void followRotation(Dialog dialog, Window window, int maxWidthDp) {
        Context app = dialog.getContext().getApplicationContext();
        if (app == null) {
            return;
        }
        android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
        android.content.ComponentCallbacks callbacks = new android.content.ComponentCallbacks() {
            @Override
            public void onConfigurationChanged(android.content.res.Configuration newConfig) {
                handler.post(() -> {
                    if (!dialog.isShowing()) {
                        return;
                    }
                    Runnable reshow = RESHOW.get(dialog);
                    if (reshow != null) {
                        dialog.dismiss();
                        reshow.run();
                    }
                    else {
                        sizeWindow(dialog, window, maxWidthDp);
                    }
                });
            }

            @Override
            public void onLowMemory() {
            }
        };
        app.registerComponentCallbacks(callbacks);
        window.getDecorView().addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View v) {
            }

            @Override
            public void onViewDetachedFromWindow(View v) {
                app.unregisterComponentCallbacks(callbacks);
            }
        });
    }

    public static void fadeWhenDisabled(android.widget.Button button) {
        if (button == null) {
            return;
        }
        int color = button.getTextColors().getDefaultColor();
        int faded = (color & 0x00FFFFFF) | (Math.round(0.38f * android.graphics.Color.alpha(color)) << 24);
        button.setTextColor(new android.content.res.ColorStateList(
                new int[][] {new int[] {-android.R.attr.state_enabled}, new int[] {}},
                new int[] {faded, color}));
    }

    // NOTE: Every scrolling area in a dialog shows a thin divider on an edge that hides more content, as Material 3 dialogs do.
    // NOTE: A hard edge alone shows a sliver of the next card, and a fade dims the last row so it looks covered by an overlay.
    // NOTE: The divider only appears on a side with more content, so a dialog that fits shows none. Android 5 and 6 get a short fade instead.
    private static void softenScrollEdges(View view, int length, boolean skipLists) {
        skipLists |= NO_LIST_EDGES.equals(view.getTag());
        boolean list = view instanceof androidx.recyclerview.widget.RecyclerView;
        if (!NO_SCROLL_EDGES.equals(view.getTag()) && (view instanceof android.widget.ScrollView || view instanceof androidx.core.widget.NestedScrollView
                || (list && !skipLists))) {
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
                softenScrollEdges(group.getChildAt(i), length, skipLists);
            }
        }
    }

    // Dialogs that are not built through show() register here, so picture-in-picture can close them too
    public static void track(Dialog dialog) {
        OPEN.add(dialog);
    }

    // NOTE: Closes every open Super Artemis dialog that belongs to this screen.
    // NOTE: Picture-in-picture shows the whole window of the activity, so a menu or editor left open was drawn oversized and cut off in the small window.
    public static void dismissAll(android.app.Activity activity) {
        for (Dialog dialog : new java.util.ArrayList<>(OPEN)) {
            Context context = dialog.getContext();
            while (context instanceof android.content.ContextWrapper && !(context instanceof android.app.Activity)) {
                context = ((android.content.ContextWrapper) context).getBaseContext();
            }
            if (context == activity && dialog.isShowing()) {
                dialog.dismiss();
            }
        }
        OPEN.clear();
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

    // NOTE: One-line title with a small tonal icon, for the large editors (bars, menu, keys).
    // NOTE: The tall centered header of the smaller dialogs would cost too much height there, above all on a phone in landscape.
    public static View compactTitle(Context context, int iconRes, CharSequence title) {
        View view = LayoutInflater.from(context).inflate(R.layout.sa_dialog_compact_title, null);
        ((ImageView) view.findViewById(R.id.sa_dialog_compact_icon)).setImageResource(iconRes);
        ((TextView) view.findViewById(R.id.sa_dialog_compact_title)).setText(title);
        return view;
    }

    // Resolves a color role such as R.attr.saPrimary in the context's theme
    public static int color(Context context, int attr) {
        TypedValue value = new TypedValue();
        context.getTheme().resolveAttribute(attr, value, true);
        return value.resourceId != 0 ? androidx.core.content.ContextCompat.getColor(context, value.resourceId) : value.data;
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

    // NOTE: Focuses the only field of a dialog and opens the keyboard, called after the dialog is shown.
    // NOTE: The window soft input state alone does not open the keyboard when focus arrives after show(), so the field asks the input method itself once it is attached.
    public static void openKeyboard(android.widget.EditText field) {
        field.requestFocus();
        field.postDelayed(() -> {
            android.view.inputmethod.InputMethodManager input =
                    (android.view.inputmethod.InputMethodManager) field.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (input != null && field.isAttachedToWindow() && field.hasFocus()) {
                input.showSoftInput(field, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
            }
        }, 150);
    }

    public static float dp(Context context, float value) {
        return value * context.getResources().getDisplayMetrics().density;
    }
}
