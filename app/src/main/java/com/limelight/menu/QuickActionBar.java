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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// NOTE: Small round buttons over the stream for the pinned actions and keys, drawn from the bars of the active set in BarSets.
// NOTE: Each bar is a grid at one of nine anchors, nudged by its offset, and fills from the anchor's corner (see BarSets.grid).
// NOTE: Bars dim, then hide, after idle seconds so they do not cover the game (see IdlePlan), and only exist while connected and outside picture-in-picture.
public final class QuickActionBar {
    public interface Host {
        StreamActionHost actionHost();

        void runAction(StreamAction action);

        // Runs a special key, a single shortcut or a key with several steps
        void runKey(CustomKey key);

        // A top left bar starts beside or below the menu button when that button is shown
        boolean isFloatingMenuButtonVisible();

        // Long press on a button: a menu to add keys to this bar, edit that button's key or edit the bar, titled with the button's name. The index points into the active set.
        void showBarMenu(int barIndex, String item, String buttonLabel);
    }

    // NOTE: How the bars look. Rail is one translucent rounded container, round is a circle per button, and handle is a single chip that expands the rail when tapped.
    public static final String PREF_STYLE = QuickBarLook.PREF_STYLE;
    public static final String STYLE_RAIL = QuickBarLook.STYLE_RAIL;
    public static final String STYLE_ROUND = QuickBarLook.STYLE_ROUND;
    public static final String STYLE_HANDLE = QuickBarLook.STYLE_HANDLE;
    // A handle that was opened closes again after this long without a touch when no hide time is set
    private static final long HANDLE_COLLAPSE_MS = 5000;
    private static final int PEEK_LONG_DP = 36;
    private static final int PEEK_THICK_DP = 6;
    // Toggles such as the microphone change state asynchronously, so their look is refreshed a moment after a tap
    private static final long STATE_REFRESH_DELAY_MS = 400;
    private static final int BUTTON_DP = BarFit.BUTTON_DP;
    private static final int GAP_DP = BarFit.GAP_DP;
    private static final int EDGE_DP = BarFit.EDGE_MARGIN_DP;

    private final Context context;
    private final FrameLayout parent;
    private final Host host;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<Binding> bindings = new ArrayList<>();
    private final List<LinearLayout> bars = new ArrayList<>();
    // One peek tab and one handle chip per bar, in the same order as the bars. Only the one that fits the style and state is shown.
    private final List<View> peeks = new ArrayList<>();
    private final List<View> chips = new ArrayList<>();
    private final Set<LinearLayout> expanded = new HashSet<>();
    private String style = STYLE_RAIL;
    private final Runnable collapse = this::collapseHandle;
    private final Runnable dim = this::dim;
    private final Runnable hideIdle = this::hideIdle;
    private boolean connected;
    private boolean hidden;
    // NOTE: Hidden on purpose by the toggle action, and only the same action shows the bars again. Touch never wakes them.
    private boolean manuallyHidden;
    // NOTE: Hidden after the idle time ran out, and the next touch on the stream brings the bars back.
    private boolean idleHidden;
    // A finger is on the stream right now, so the idle stages wait
    private boolean touching;

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
        // A rotation or a window change moves what fits, so the bars are built again for the new size
        parent.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> {
            if (connected && !bars.isEmpty() && (r - l != or - ol || b - t != ob - ot) && or - ol > 0) {
                handler.removeCallbacks(rebuild);
                handler.post(rebuild);
            }
        });
    }

    private final Runnable rebuild = this::reload;

    public void setConnected(boolean connected) {
        this.connected = connected;
        reload();
    }

    // Hidden in picture-in-picture, restored afterwards
    public void setHidden(boolean hidden) {
        this.hidden = hidden;
        touching = false;
        applyVisibility();
    }

    public boolean isManuallyHidden() {
        return manuallyHidden;
    }

    // The toggle action: hides every bar until it is called again
    public void toggleManualHidden() {
        manuallyHidden = !manuallyHidden;
        touching = false;
        idleHidden = false;
        cancelIdle();
        for (LinearLayout bar : bars) {
            bar.animate().cancel();
            bar.setAlpha(1f);
        }
        applyVisibility();
        if (!manuallyHidden) {
            wake();
        }
    }

    // NOTE: While a finger is on the stream the bars stay as they are and no idle timer runs, so a long press or a scroll never dims or hides them.
    // NOTE: The idle clock starts again when the last finger lifts. A lost lift event is cleared by a reload, a manual hide or a hidden state, so the bars cannot stay awake for good.
    public void onStreamTouchAction(int actionMasked) {
        if (actionMasked == android.view.MotionEvent.ACTION_DOWN) {
            touching = true;
            onStreamTouch();
        }
        else if (actionMasked == android.view.MotionEvent.ACTION_UP || actionMasked == android.view.MotionEvent.ACTION_CANCEL) {
            touching = false;
            if (!manuallyHidden && !bars.isEmpty()) {
                wake();
            }
        }
    }

    // Called for a touch on the stream, which brings back bars that went idle
    public void onStreamTouch() {
        if (manuallyHidden || bars.isEmpty()) {
            return;
        }
        if (idleHidden) {
            idleHidden = false;
            applyVisibility();
        }
        wake();
    }

    static String normalizeStyle(String value) {
        return QuickBarLook.normalize(value);
    }

    private boolean handleStyle() {
        return STYLE_HANDLE.equals(style);
    }

    // The rail and the handle share one look, only the round style keeps a circle per button
    private boolean railLook() {
        return QuickBarLook.railLook(style);
    }

    private int buttonBackground(boolean active) {
        return QuickBarLook.buttonBackground(style, active);
    }

    // NOTE: Rail and round bars hide completely when idle and leave a peek tab on their edge. A handle bar shows its chip while collapsed and its rail while open.
    private void applyVisibility() {
        boolean off = hidden || manuallyHidden;
        boolean handle = handleStyle();
        IdlePlan plan = idlePlan();
        for (int i = 0; i < bars.size(); i++) {
            LinearLayout bar = bars.get(i);
            boolean open = handle ? expanded.contains(bar) : !idleHidden;
            bar.setVisibility(!off && open ? View.VISIBLE : View.GONE);
            peeks.get(i).setAlpha(plan.tabAlpha());
            peeks.get(i).setVisibility(!off && !handle && idleHidden && plan.showsTab() ? View.VISIBLE : View.GONE);
            chips.get(i).setVisibility(!off && handle && !open ? View.VISIBLE : View.GONE);
        }
    }

    private IdlePlan idlePlan() {
        return IdlePlan.read(PreferenceManager.getDefaultSharedPreferences(context));
    }

    private void cancelIdle() {
        handler.removeCallbacks(collapse);
        handler.removeCallbacks(dim);
        handler.removeCallbacks(hideIdle);
    }

    // Rebuilds every corner from the saved items and directions, for example after the bar editor saved
    public void reload() {
        touching = false;
        for (LinearLayout bar : bars) {
            parent.removeView(bar);
        }
        for (View view : peeks) {
            parent.removeView(view);
        }
        for (View view : chips) {
            parent.removeView(view);
        }
        bars.clear();
        peeks.clear();
        chips.clear();
        expanded.clear();
        bindings.clear();
        cancelIdle();
        if (!connected) {
            return;
        }
        style = normalizeStyle(PreferenceManager.getDefaultSharedPreferences(context).getString(PREF_STYLE, STYLE_RAIL));

        List<BarSets.Bar> specs = BarSets.load(context).activeSet().bars;
        for (int i = 0; i < specs.size(); i++) {
            LinearLayout bar = buildBar(i, specs.get(i));
            if (bar != null) {
                bars.add(bar);
            }
        }
        idleHidden = false;
        applyVisibility();
        refreshStates();
        wake();
    }

    private LinearLayout buildBar(int index, BarSets.Bar savedSpec) {
        float density = context.getResources().getDisplayMetrics().density;
        // NOTE: The saved line length is the intent. A phone cannot show 20 buttons in a line, so the line wraps earlier instead of clipping the rest.
        android.util.DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        boolean beside = BarSets.TOP_LEFT.equals(savedSpec.anchor) && host.isFloatingMenuButtonVisible();
        BarSets.Bar spec = BarFit.fitted(savedSpec, metrics.widthPixels / density, metrics.heightPixels / density,
                beside ? BUTTON_DP + GAP_DP : 0);
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
        int gap = (int) (QuickBarLook.gapDp(style) * density);
        QuickBarLook.styleContainer(bar, style, density);
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
                    String name = String.valueOf(v.getContentDescription());
                    com.limelight.ui.SafeRun.run(context, name, () -> host.showBarMenu(index, item, name));
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
        buildChip(bar, spec, density);
        buildPeek(spec, density);
        return bar;
    }

    // NOTE: The chip stands in for a bar of the handle style while it is collapsed, at the same corner and offset, and tapping it opens the rail.
    private void buildChip(LinearLayout bar, BarSets.Bar spec, float density) {
        ImageButton chip = new ImageButton(context);
        chip.setImageResource(R.drawable.ic_sa_more_horiz);
        chip.setBackgroundResource(R.drawable.floating_menu_button);
        int padding = (int) (7 * density);
        chip.setPadding(padding, padding, padding, padding);
        chip.setScaleType(ImageButton.ScaleType.FIT_CENTER);
        chip.setContentDescription(context.getString(R.string.quick_bar_handle_label));
        chip.setElevation(9999 * density);
        chip.setOnClickListener(v -> {
            expanded.add(bar);
            applyVisibility();
            wake();
        });
        FrameLayout.LayoutParams params = layoutParams(spec, density);
        params.width = (int) (BUTTON_DP * density);
        params.height = (int) (BUTTON_DP * density);
        parent.addView(chip, params);
        placeInside(chip, spec.offsetXDp * density, spec.offsetYDp * density);
        chips.add(chip);
    }

    // NOTE: The peek tab stays on the screen edge of a bar that hid on its own, so the user can see where it lives and tap it back without a touch reaching the game.
    private void buildPeek(BarSets.Bar spec, float density) {
        View peek = new View(context);
        peek.setBackgroundResource(R.drawable.bg_quick_peek);
        peek.setContentDescription(context.getString(R.string.quick_bar_peek_label));
        peek.setElevation(9999 * density);
        peek.setClickable(true);
        boolean vertical = spec.isLeft() || spec.isRight() || (!spec.isTop() && !spec.isBottom());
        int longSide = (int) (PEEK_LONG_DP * density);
        int thick = (int) (PEEK_THICK_DP * density);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(vertical ? thick : longSide, vertical ? longSide : thick);
        int edge = (int) (EDGE_DP * density);
        if (vertical) {
            params.gravity = (spec.isRight() ? Gravity.END : Gravity.START)
                    | (spec.isTop() ? Gravity.TOP : spec.isBottom() ? Gravity.BOTTOM : Gravity.CENTER_VERTICAL);
            if (spec.isTop()) {
                params.topMargin = edge;
            }
            else if (spec.isBottom()) {
                params.bottomMargin = edge;
            }
        }
        else {
            params.gravity = (spec.isTop() ? Gravity.TOP : Gravity.BOTTOM) | Gravity.CENTER_HORIZONTAL;
        }
        peek.setOnClickListener(v -> {
            idleHidden = false;
            applyVisibility();
            wake();
        });
        parent.addView(peek, params);
        peeks.add(peek);
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
            binding.button.setBackgroundResource(buttonBackground(active));
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
            return makeButton(IconCatalog.drawableFor(key.icon), com.limelight.customkeys.KeyLabels.displayLabel(context, key), null, () -> host.runKey(key));
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
        button.setBackgroundResource(buttonBackground(false));
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
            if (spec.rows() > spec.columns()) {
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

    // NOTE: Every touch restarts both stages. A handle bar has one stage only, it closes into its chip after the hide time.
    private void wake() {
        cancelIdle();
        for (LinearLayout bar : bars) {
            bar.animate().cancel();
            bar.setAlpha(1f);
        }
        if (bars.isEmpty() || touching) {
            return;
        }
        IdlePlan plan = idlePlan();
        if (handleStyle()) {
            handler.postDelayed(collapse, plan.hideDelayMs() > 0 ? plan.hideDelayMs() : HANDLE_COLLAPSE_MS);
            return;
        }
        if (plan.dimDelayMs() >= 0) {
            handler.postDelayed(dim, plan.dimDelayMs());
        }
        if (plan.hideDelayMs() >= 0) {
            handler.postDelayed(hideIdle, plan.hideDelayMs());
        }
    }

    // A handle bar closes back into its chip, so there is nothing to dim or hide
    private void collapseHandle() {
        expanded.clear();
        applyVisibility();
    }

    private void dim() {
        if (touching) {
            return;
        }
        float alpha = idlePlan().dimAlpha();
        for (LinearLayout bar : bars) {
            bar.animate().alpha(alpha).setDuration(300).start();
        }
    }

    private void hideIdle() {
        if (touching) {
            return;
        }
        for (LinearLayout bar : bars) {
            bar.animate().alpha(0f).setDuration(300).withEndAction(() -> {
                if (bar.getAlpha() == 0f && !idleHidden) {
                    idleHidden = true;
                    applyVisibility();
                }
            }).start();
        }
    }
}
