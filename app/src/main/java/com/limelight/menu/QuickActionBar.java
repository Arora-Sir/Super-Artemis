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

// NOTE: Small round buttons over the stream for the actions and keys the user pinned, one bar per screen corner, as a row or a column.
// NOTE: Each bar starts in its corner, fades after a few idle seconds so it does not cover the game, and only exists while connected and outside picture-in-picture.
public final class QuickActionBar {
    public interface Host {
        StreamActionHost actionHost();

        void runAction(StreamAction action);

        void sendKeys(short[] keys);

        // The top left bar starts beside or below the menu button when that button is shown
        boolean isFloatingMenuButtonVisible();
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

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        for (String corner : QuickBarLayout.POSITIONS) {
            LinearLayout bar = buildBar(corner, QuickBarLayout.getItems(prefs, corner), QuickBarLayout.getDirection(prefs, corner));
            if (bar != null) {
                bar.setVisibility(hidden ? View.GONE : View.VISIBLE);
                bars.add(bar);
            }
        }
        refreshStates();
        wake();
    }

    private LinearLayout buildBar(String corner, List<String> items, String direction) {
        float density = context.getResources().getDisplayMetrics().density;
        boolean column = QuickBarLayout.DIRECTION_COLUMN.equals(direction);
        LinearLayout bar = new LinearLayout(context);
        bar.setOrientation(column ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
        // Gaps between buttons swallow taps too, so a near miss never reaches the stream
        bar.setClickable(true);
        bar.setElevation(9999 * density);

        List<ImageButton> buttons = new ArrayList<>();
        for (String item : items) {
            ImageButton button = buttonFor(item);
            if (button != null) {
                buttons.add(button);
            }
        }
        if (buttons.isEmpty()) {
            return null;
        }
        // The first item always sits in the corner, so bars on the right or bottom are filled from the far end
        boolean reverse = column ? !QuickBarLayout.isTop(corner) : !QuickBarLayout.isLeft(corner);
        int size = (int) (BUTTON_DP * density);
        int gap = (int) (GAP_DP * density);
        for (int i = 0; i < buttons.size(); i++) {
            ImageButton button = buttons.get(reverse ? buttons.size() - 1 - i : i);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
            if (i > 0) {
                if (column) {
                    params.topMargin = gap;
                }
                else {
                    params.leftMargin = gap;
                }
            }
            bar.addView(button, params);
        }
        parent.addView(bar, layoutParams(corner, column, density));
        return bar;
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
            short[] codes = key.keyCodes();
            return makeButton(IconCatalog.drawableFor(key.icon), key.name, null, () -> host.sendKeys(codes));
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
        // Long press names the button, since the bar shows icons only
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            button.setTooltipText(label);
        }
        else {
            button.setOnLongClickListener(v -> {
                Toast.makeText(context, label, Toast.LENGTH_SHORT).show();
                return true;
            });
        }
        button.setOnClickListener(v -> {
            wake();
            onTap.run();
            handler.postDelayed(this::refreshStates, STATE_REFRESH_DELAY_MS);
        });
        bindings.add(new Binding(button, action));
        return button;
    }

    private FrameLayout.LayoutParams layoutParams(String corner, boolean column, float density) {
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        int gap = (int) (GAP_DP * density);
        int edge = (int) (EDGE_DP * density);
        boolean top = QuickBarLayout.isTop(corner);
        boolean left = QuickBarLayout.isLeft(corner);
        params.gravity = (top ? Gravity.TOP : Gravity.BOTTOM) | (left ? Gravity.START : Gravity.END);
        int vertical = edge;
        int horizontal = gap;
        // The menu button owns the top left corner, so that bar starts right of it as a row or under it as a column
        if (QuickBarLayout.POSITION_TOP_LEFT.equals(corner) && host.isFloatingMenuButtonVisible()) {
            int step = (int) ((BUTTON_DP + GAP_DP) * density);
            if (column) {
                vertical += step;
                horizontal = 0;
            }
            else {
                horizontal = step;
            }
        }
        if (top) {
            params.topMargin = vertical;
        }
        else {
            params.bottomMargin = vertical;
        }
        params.setMarginStart(left ? horizontal : 0);
        params.setMarginEnd(left ? 0 : horizontal);
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
