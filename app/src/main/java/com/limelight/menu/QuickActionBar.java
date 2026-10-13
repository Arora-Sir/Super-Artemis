package com.limelight.menu;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.preference.PreferenceManager;

import com.limelight.R;
import com.limelight.actions.StreamAction;
import com.limelight.actions.StreamActionHost;
import com.limelight.actions.StreamActionRegistry;
import com.limelight.customkeys.CustomKey;
import com.limelight.customkeys.IconCatalog;
import com.limelight.customkeys.SpecialKeys;

import java.util.ArrayList;
import java.util.List;

// NOTE: Small round buttons over the stream for the pinned actions and keys, drawn from the bars of the active set in BarSets.
// NOTE: Each bar is a grid at one of nine anchors, nudged by its offset, and fills from the anchor's corner (see BarSets.grid).
// NOTE: Bars fade after a few idle seconds so they do not cover the game, and only exist while connected and outside picture-in-picture.
public final class QuickActionBar {
    public interface Host {
        StreamActionHost actionHost();

        void runAction(StreamAction action);

        // Runs a special key, a single shortcut or a key with several steps
        void runKey(CustomKey key);

        // A top left bar starts beside or below the menu button when that button is shown
        boolean isFloatingMenuButtonVisible();

        // Long press on a button: a menu to add keys to this bar or edit it, titled with the button's name. The index points into the active set.
        void showBarMenu(int barIndex, String buttonLabel);
    }

    private static final long FADE_DELAY_MS = 3000;
    private static final float IDLE_ALPHA = 0.4f;
    // Toggles such as the microphone change state asynchronously, so their look is refreshed a moment after a tap
    private static final long STATE_REFRESH_DELAY_MS = 400;
    private static final int BUTTON_DP = 36;
    private static final int GAP_DP = 8;
    private static final int EDGE_DP = 24;

    private final Context context;
    private final FrameLayout parent;
    private final Host host;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<Binding> bindings = new ArrayList<>();
    private final List<LinearLayout> bars = new ArrayList<>();
    private final Runnable fade = this::fade;
    private boolean connected;
    private boolean hidden;

    // Keeps the item each button runs, so state refreshes know which action a button shows
    private static final class Binding {
        final ImageButton button;
        final StreamAction action;

        Binding(ImageButton button, StreamAction action) {
            this.button = button;
            this.action = action;
        }
    }

    public QuickActionBar(Context context, FrameLayout parent, Host host) {
        this.context = context;
        this.parent = parent;
        this.host = host;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
        reload();
    }

    // Hidden in picture-in-picture, restored afterwards
    public void setHidden(boolean hidden) {
        this.hidden = hidden;
        for (LinearLayout bar : bars) {
            bar.setVisibility(hidden ? View.GONE : View.VISIBLE);
        }
    }

    // Rebuilds every corner from the saved items and directions, for example after the bar editor saved
    public void reload() {
        for (LinearLayout bar : bars) {
            parent.removeView(bar);
        }
        bars.clear();
        bindings.clear();
        handler.removeCallbacks(fade);
        if (!connected) {
            return;
        }

        List<BarSets.Bar> specs = BarSets.load(context).activeSet().bars;
        for (int i = 0; i < specs.size(); i++) {
            LinearLayout bar = buildBar(i, specs.get(i));
            if (bar != null) {
                bar.setVisibility(hidden ? View.GONE : View.VISIBLE);
                bars.add(bar);
            }
        }
        refreshStates();
        wake();
    }

    private LinearLayout buildBar(int index, BarSets.Bar spec) {
        float density = context.getResources().getDisplayMetrics().density;
        // Items whose action or key is gone are skipped before the grid is laid out, so the grid has no holes
        List<String> items = new ArrayList<>();
        List<ImageButton> buttons = new ArrayList<>();
        for (String item : spec.items) {
            ImageButton button = buttonFor(item);
            if (button != null) {
                items.add(item);
                buttons.add(button);
            }
        }
        if (buttons.isEmpty()) {
            return null;
        }
        LinearLayout bar = new LinearLayout(context);
        bar.setOrientation(LinearLayout.VERTICAL);
        bar.setGravity(spec.isRight() ? Gravity.END : spec.isLeft() ? Gravity.START : Gravity.CENTER_HORIZONTAL);
        // Gaps between buttons swallow taps too, so a near miss never reaches the stream
        bar.setClickable(true);
        bar.setElevation(9999 * density);

        int size = (int) (BUTTON_DP * density);
        int gap = (int) (GAP_DP * density);
        List<List<String>> grid = BarSets.grid(spec, items);
        for (int r = 0; r < grid.size(); r++) {
            LinearLayout row = new LinearLayout(context);
            row.setOrientation(LinearLayout.HORIZONTAL);
            List<String> cells = grid.get(r);
            for (int c = 0; c < cells.size(); c++) {
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
                if (c > 0) {
                    params.leftMargin = gap;
                }
                String item = cells.get(c);
                if (item == null) {
                    // An empty cell keeps the columns of a partly filled row lined up with the full rows
                    row.addView(new View(context), params);
                    continue;
                }
                ImageButton button = buttons.get(items.indexOf(item));
                button.setOnLongClickListener(v -> {
                    host.showBarMenu(index, String.valueOf(v.getContentDescription()));
                    return true;
                });
                row.addView(button, params);
            }
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            if (r > 0) {
                rowParams.topMargin = gap;
            }
            bar.addView(row, rowParams);
        }
        parent.addView(bar, layoutParams(spec, density));
        placeInside(bar, spec.offsetXDp * density, spec.offsetYDp * density);
        return bar;
    }

    // NOTE: Applies the offset once the bar is measured and clamps it so the whole bar stays on screen.
    // NOTE: An offset saved on a wide tablet can point past the edge of a phone screen, and a bar off screen could never be long pressed to fix it.
    public static void placeInside(View bar, float offsetX, float offsetY) {
        bar.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                View parent = (View) v.getParent();
                if (parent == null) {
                    return;
                }
                int minX = parent.getPaddingLeft() - left;
                int maxX = parent.getWidth() - parent.getPaddingRight() - right;
                int minY = parent.getPaddingTop() - top;
                int maxY = parent.getHeight() - parent.getPaddingBottom() - bottom;
                v.setTranslationX(Math.max(minX, Math.min(maxX, offsetX)));
                v.setTranslationY(Math.max(minY, Math.min(maxY, offsetY)));
            }
        });
    }

    public void refreshSoon() {
        handler.postDelayed(this::refreshStates, STATE_REFRESH_DELAY_MS);
    }

    public void refreshStates() {
        for (Binding binding : bindings) {
            boolean active = binding.action != null && binding.action.isActive(host.actionHost());
            binding.button.setBackgroundResource(active ? R.drawable.floating_menu_button_active : R.drawable.floating_menu_button);
            if (binding.action != null) {
                binding.button.setContentDescription(context.getString(binding.action.labelRes(host.actionHost())));
            }
        }
    }

    private ImageButton buttonFor(String item) {
        String keyId = QuickBarLayout.customKeyId(item);
        if (keyId != null) {
            CustomKey key = SpecialKeys.find(context, keyId);
            if (key == null) {
                return null;
            }
            return makeButton(IconCatalog.drawableFor(key.icon), key.name, null, () -> host.runKey(key));
        }
        StreamAction action = StreamActionRegistry.find(item);
        if (action == null || action.destructive || !action.isAvailable(host.actionHost())) {
            return null;
        }
        return makeButton(action.iconRes, context.getString(action.nameRes), action, () -> host.runAction(action));
    }

    private ImageButton makeButton(int iconRes, String label, StreamAction action, Runnable onTap) {
        float density = context.getResources().getDisplayMetrics().density;
        ImageButton button = new ImageButton(context);
        button.setImageResource(iconRes);
        button.setBackgroundResource(R.drawable.floating_menu_button);
        int padding = (int) (7 * density);
        button.setPadding(padding, padding, padding, padding);
        button.setScaleType(ImageButton.ScaleType.FIT_CENTER);
        button.setContentDescription(label);
        // A mouse hover names the button. A long press opens the bar menu, which is titled with the same name.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            button.setTooltipText(label);
        }
        button.setOnClickListener(v -> {
            wake();
            com.limelight.ui.SafeRun.run(context, label, onTap);
            handler.postDelayed(this::refreshStates, STATE_REFRESH_DELAY_MS);
        });
        bindings.add(new Binding(button, action));
        return button;
    }

    private FrameLayout.LayoutParams layoutParams(BarSets.Bar spec, float density) {
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        int gap = (int) (GAP_DP * density);
        int edge = (int) (EDGE_DP * density);
        int vertical = spec.isTop() ? Gravity.TOP : spec.isBottom() ? Gravity.BOTTOM : Gravity.CENTER_VERTICAL;
        int horizontal = spec.isLeft() ? Gravity.START : spec.isRight() ? Gravity.END : Gravity.CENTER_HORIZONTAL;
        params.gravity = vertical | horizontal;
        int edgeMargin = edge;
        int sideMargin = gap;
        // The menu button owns the top left corner, so a wide bar there starts right of it and a tall one starts under it
        if (BarSets.TOP_LEFT.equals(spec.anchor) && host.isFloatingMenuButtonVisible()) {
            int step = (int) ((BUTTON_DP + GAP_DP) * density);
            if (spec.rows > spec.columns) {
                edgeMargin += step;
                sideMargin = 0;
            }
            else {
                sideMargin = step;
            }
        }
        if (spec.isTop()) {
            params.topMargin = edgeMargin;
        }
        else if (spec.isBottom()) {
            params.bottomMargin = edgeMargin;
        }
        if (spec.isLeft()) {
            params.setMarginStart(sideMargin);
        }
        else if (spec.isRight()) {
            params.setMarginEnd(sideMargin);
        }
        return params;
    }

    private void wake() {
        handler.removeCallbacks(fade);
        for (LinearLayout bar : bars) {
            bar.animate().cancel();
            bar.setAlpha(1f);
        }
        if (!bars.isEmpty()) {
            handler.postDelayed(fade, FADE_DELAY_MS);
        }
    }

    private void fade() {
        for (LinearLayout bar : bars) {
            bar.animate().alpha(IDLE_ALPHA).setDuration(300).start();
        }
    }
}
