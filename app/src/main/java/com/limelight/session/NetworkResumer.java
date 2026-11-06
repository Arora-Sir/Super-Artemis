package com.limelight.session;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import com.limelight.LimeLog;


// NOTE: Waits for the network to come back after a stream ended in a drop, checks that the PC answers on its HTTP port, and then tells the caller to start the stream again.
// NOTE: A connectivity callback checks at once when a network appears, and a timer checks with growing gaps in between, for at most ResumePlan.MAX_WAIT_MS.
// NOTE: Every listener method runs on the main thread, and the single TCP check runs on its own thread.
public final class NetworkResumer {
    private static final String TAG = "NetworkResume";
    private static final int CONNECT_TIMEOUT_MS = 1500;
    // With several addresses to try, each gets a shorter wait so one check stays quick
    private static final int ADDRESS_TIMEOUT_MS = 1000;

    public interface Listener {
        // The network is not back yet, with how long the wait has lasted
        void onWaitingForNetwork(long waitedMs);

        // The network is back and the PC is being checked, with the number of the check starting at 1
        void onReachingHost(int check);

        // The PC answered, so the stream can start again
        void onHostReachable();

        // The wait ran out
        void onGiveUp();
    }

    private final Context context;
    private final String host;
    private final int port;
    // The saved PC, so its other addresses are tried too. Null checks only the address of the stream.
    private final String pcUuid;
    private final Listener listener;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final Runnable check = this::runCheck;

    private ConnectivityManager.NetworkCallback callback;
    private boolean running;
    private long startedAtMs;
    private int attempt;
    private int checks;

    public NetworkResumer(Context context, String host, int port, Listener listener) {
        this(context, host, port, null, listener);
    }

    public NetworkResumer(Context context, String host, int port, String pcUuid, Listener listener) {
        this.context = context.getApplicationContext();
        this.host = host;
        this.port = port;
        this.pcUuid = pcUuid;
        this.listener = listener;
    }

    public void start() {
        if (running) {
            return;
        }
        running = true;
        startedAtMs = SystemClock.elapsedRealtime();
        attempt = 0;
        checks = 0;
        registerCallback();
        main.postDelayed(check, ResumePlan.FIRST_CHECK_DELAY_MS);
    }

    // Skips the wait and checks now, for the Reconnect now button
    public void checkNow() {
        if (!running) {
            return;
        }
        attempt = 0;
        main.removeCallbacks(check);
        main.post(check);
    }

    public void stop() {
        running = false;
        main.removeCallbacks(check);
        unregisterCallback();
    }

    private void registerCallback() {
        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (manager == null) {
            return;
        }
        callback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                // A network just appeared, so the PC is checked at once instead of after the timer
                main.post(() -> {
                    if (running) {
                        attempt = 0;
                        main.removeCallbacks(check);
                        main.postDelayed(check, ResumePlan.FIRST_CHECK_DELAY_MS);
                    }
                });
            }
        };
        try {
            manager.registerNetworkCallback(new NetworkRequest.Builder().build(), callback);
        }
        catch (RuntimeException e) {
            // The timer alone still works, only slower to notice a returning network
            LimeLog.warning(TAG + ": no connectivity callback: " + e.getMessage());
            callback = null;
        }
    }

    private void unregisterCallback() {
        if (callback == null) {
            return;
        }
        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        try {
            if (manager != null) {
                manager.unregisterNetworkCallback(callback);
            }
        }
        catch (RuntimeException e) {
            // Already gone
        }
        callback = null;
    }

    private boolean networkUp() {
        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = manager != null ? manager.getActiveNetworkInfo() : null;
        return info != null && info.isConnected();
    }

    private void schedule() {
        main.postDelayed(check, ResumePlan.delayBeforeCheck(attempt));
    }

    private void runCheck() {
        if (!running) {
            return;
        }
        long waited = SystemClock.elapsedRealtime() - startedAtMs;
        if (ResumePlan.expired(waited)) {
            stop();
            listener.onGiveUp();
            return;
        }
        if (!networkUp()) {
            listener.onWaitingForNetwork(waited);
            attempt++;
            schedule();
            return;
        }
        checks++;
        listener.onReachingHost(checks);
        new Thread(() -> {
            boolean reachable = canReachHost();
            main.post(() -> {
                if (!running) {
                    return;
                }
                if (reachable) {
                    LimeLog.info(TAG + ": host reachable after " + (SystemClock.elapsedRealtime() - startedAtMs) + " ms");
                    stop();
                    listener.onHostReachable();
                }
                else {
                    attempt++;
                    schedule();
                }
            });
        }, "NetworkResumeCheck").start();
    }

    private boolean canReachHost() {
        return HostProbe.reachable(context, host, port, pcUuid, CONNECT_TIMEOUT_MS, ADDRESS_TIMEOUT_MS);
    }
}
