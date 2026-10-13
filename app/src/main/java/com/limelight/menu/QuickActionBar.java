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
import com.limelight.customkeys.CustomKeyStore;
import com.limelight.customkeys.IconCatalog;

import java.util.ArrayList;
import java.util.List;

// NOTE: A row of small round buttons over the stream for actions and custom keys the user pinned, placed in the corner they chose.
// NOTE: It fades after a few idle seconds so it does not cover the game, and it only exists while connected and outside picture-in-picture.
public final class QuickActionBar {
    public interface Host {
        StreamActionHost actionHost();

        void runAction(StreamAction action);

        void sendKeys(short[] keys);

        // The bar sits next to the menu button when both use the top left corner
        boolean isFloatingMenuButtonVisible();
    }

    private static final long FADE_DELAY_MS = 3000;
    private static final float IDLE_ALPHA = 0.4f;
    // Toggles such as the microphone change state asynchronously, so their look is refreshed a moment after a tap
    private static final long STATE_REFRESH_DELAY_MS = 400;
    private static final int BUTTON_DP = 36;
    private static final int GAP_DP = 8;
    private static final int EDGE_TOP_DP = 24;

    private final Context context;
    private final FrameLayout parent;
    private final Host host;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<Binding> bindings = new ArrayList<>();
    private final Runnable fade = this::fade;
    private LinearLayout bar;
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
        if (bar != null) {
            bar.setVisibility(hidden ? View.GONE : View.VISIBLE);
        }
    }

    // Rebuilds the bar from the saved items and position, for example after the bar editor saved
    public void reload() {
        if (bar != null) {
            parent.removeView(bar);
            bar = null;
        }
        bindings.clear();
        handler.removeCallbacks(fade);
        if (!connected) {
            return;
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        float density = context.getResources().getDisplayMetrics().density;
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        // Gaps between buttons swallow taps too, so a near miss never reaches the stream
        row.setClickable(true);
        row.setElevation(9999 * density);

        for (String item : QuickBarLayout.getItems(prefs)) {
            ImageButton button = buttonFor(item);
            if (button != null) {
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams((int) (BUTTON_DP * density), (int) (BUTTON_DP * density));
                if (row.getChildCount() > 0) {
                    params.leftMargin = (int) (GAP_DP * density);
                }
                row.addView(button, params);
            }
        }
        if (row.getChildCount() == 0) {
            return;
        }

        parent.addView(row, layoutParams(QuickBarLayout.getPosition(prefs), density));
        bar = row;
        bar.setVisibility(hidden ? View.GONE : View.VISIBLE);
        refreshStates();
        wake();
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
            CustomKey key = CustomKeyStore.find(context, keyId);
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

    private FrameLayout.LayoutParams layoutParams(String position, float density) {
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        int gap = (int) (GAP_DP * density);
        int edge = (int) (EDGE_TOP_DP * density);
        boolean top = QuickBarLayout.POSITION_TOP_LEFT.equals(position) || QuickBarLayout.POSITION_TOP_RIGHT.equals(position);
        boolean left = QuickBarLayout.POSITION_TOP_LEFT.equals(position) || QuickBarLayout.POSITION_BOTTOM_LEFT.equals(position);
        params.gravity = (top ? Gravity.TOP : Gravity.BOTTOM) | (left ? Gravity.START : Gravity.END);
        if (top) {
            params.topMargin = edge;
        }
        else {
            params.bottomMargin = edge;
        }
        // The floating menu button lives in the top left corner, so the bar starts right after it there
        int start = QuickBarLayout.POSITION_TOP_LEFT.equals(position) && host.isFloatingMenuButtonVisible()
                ? (int) ((BUTTON_DP + GAP_DP) * density) : gap;
        params.setMarginStart(left ? start : 0);
        params.setMarginEnd(left ? 0 : gap);
        return params;
    }

    private void wake() {
        if (bar == null) {
            return;
        }
        bar.animate().cancel();
        bar.setAlpha(1f);
        handler.removeCallbacks(fade);
        handler.postDelayed(fade, FADE_DELAY_MS);
    }

    private void fade() {
        if (bar != null) {
            bar.animate().alpha(IDLE_ALPHA).setDuration(300).start();
        }
    }
}
