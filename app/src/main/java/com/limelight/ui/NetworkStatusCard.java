package com.limelight.ui;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import com.limelight.R;

// NOTE: The connection status card over the stream. While the stream still waits for a returning network it shows the seconds without buttons.
// NOTE: After the stream ended it shows the automatic reconnect steps with Cancel and Reconnect now.
// NOTE: The card fades in and out, and it is also exposed to screen readers as a live region so changes are announced.
public final class NetworkStatusCard {
    private static final long FADE_MS = 250L;

    public enum Mode {
        HIDDEN,
        // The stream is alive but the host is silent, counting the seconds
        WAITING_STREAM,
        // The stream ended and the app waits for the network to return
        WAITING_NETWORK,
        // The network is back and the PC is being checked
        REACHING_HOST,
        // The PC answered and the stream is starting again
        STARTING
    }

    private final View card;
    private final TextView title;
    private final TextView detail;
    private final View progress;
    private final View actions;
    private Mode mode = Mode.HIDDEN;

    public NetworkStatusCard(View root, Runnable onCancel, Runnable onReconnectNow) {
        card = root;
        title = root.findViewById(R.id.networkStatusTitle);
        detail = root.findViewById(R.id.networkStatusDetail);
        progress = root.findViewById(R.id.networkStatusProgress);
        actions = root.findViewById(R.id.networkStatusActions);
        root.findViewById(R.id.networkStatusCancel).setOnClickListener(v -> onCancel.run());
        root.findViewById(R.id.networkStatusNow).setOnClickListener(v -> onReconnectNow.run());
        card.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
    }

    public Mode mode() {
        return mode;
    }

    public boolean isShowing() {
        return mode != Mode.HIDDEN;
    }

    public void showWaitingStream(int seconds) {
        Context context = card.getContext();
        set(Mode.WAITING_STREAM, context.getString(R.string.network_drop_title),
                context.getString(R.string.network_drop_detail, seconds), false);
    }

    public void showWaitingNetwork(int seconds) {
        Context context = card.getContext();
        set(Mode.WAITING_NETWORK, context.getString(R.string.network_resume_title),
                context.getString(R.string.network_resume_waiting_network, seconds), true);
    }

    public void showReachingHost(String pcName) {
        Context context = card.getContext();
        String name = pcName != null && !pcName.isEmpty() ? pcName : context.getString(R.string.network_resume_pc_fallback);
        set(Mode.REACHING_HOST, context.getString(R.string.network_resume_title),
                context.getString(R.string.network_resume_reaching_host, name), true);
    }

    public void showStarting() {
        Context context = card.getContext();
        set(Mode.STARTING, context.getString(R.string.network_resume_title),
                context.getString(R.string.network_resume_starting), false);
    }

    public void hide() {
        if (mode == Mode.HIDDEN) {
            return;
        }
        mode = Mode.HIDDEN;
        card.animate().cancel();
        card.animate().alpha(0f).setDuration(FADE_MS).withEndAction(() -> {
            if (mode == Mode.HIDDEN) {
                card.setVisibility(View.GONE);
                card.setAlpha(1f);
            }
        }).start();
    }

    // Hides at once without a fade, for when the stream ends and the card would otherwise stay over a dialog
    public void hideNow() {
        mode = Mode.HIDDEN;
        card.animate().cancel();
        card.setVisibility(View.GONE);
        card.setAlpha(1f);
    }

    private void set(Mode newMode, String titleText, String detailText, boolean withActions) {
        boolean wasHidden = mode == Mode.HIDDEN;
        mode = newMode;
        title.setText(titleText);
        detail.setText(detailText);
        actions.setVisibility(withActions ? View.VISIBLE : View.GONE);
        progress.setVisibility(newMode == Mode.WAITING_STREAM || newMode == Mode.WAITING_NETWORK
                || newMode == Mode.REACHING_HOST || newMode == Mode.STARTING ? View.VISIBLE : View.GONE);
        card.animate().cancel();
        if (wasHidden || card.getVisibility() != View.VISIBLE) {
            card.setAlpha(0f);
            card.setVisibility(View.VISIBLE);
        }
        card.animate().alpha(1f).setDuration(FADE_MS).start();
    }
}
