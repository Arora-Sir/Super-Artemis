package com.limelight.session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import androidx.test.core.app.ApplicationProvider;

import com.limelight.shadows.ShadowMoonBridge;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;
import org.robolectric.shadows.ShadowConnectivityManager;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

// NOTE: The resumer waits for the network, checks that the PC answers on its port, and gives up after the wait limit. A real loopback server stands in for the PC.
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, shadows = {ShadowMoonBridge.class})
public class NetworkResumerTest {
    private Context context;
    private ShadowConnectivityManager connectivity;
    private NetworkInfo connected;
    private ServerSocket server;
    private final AtomicInteger waiting = new AtomicInteger();
    private final AtomicInteger reaching = new AtomicInteger();
    private final AtomicBoolean reachable = new AtomicBoolean();
    private final AtomicBoolean gaveUp = new AtomicBoolean();
    private final AtomicLong lastWaitedMs = new AtomicLong();
    private NetworkResumer resumer;

    @Before
    public void setUp() throws IOException {
        context = ApplicationProvider.getApplicationContext();
        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        connectivity = Shadows.shadowOf(manager);
        connected = manager.getActiveNetworkInfo();
        server = new ServerSocket(0, 5, InetAddress.getLoopbackAddress());
    }

    @After
    public void tearDown() throws IOException {
        if (resumer != null) {
            resumer.stop();
        }
        server.close();
    }

    private NetworkResumer resumerFor(int port) {
        resumer = new NetworkResumer(context, "127.0.0.1", port, new NetworkResumer.Listener() {
            @Override
            public void onWaitingForNetwork(long waitedMs) {
                waiting.incrementAndGet();
                lastWaitedMs.set(waitedMs);
            }

            @Override
            public void onReachingHost(int check) {
                reaching.incrementAndGet();
            }

            @Override
            public void onHostReachable() {
                reachable.set(true);
            }

            @Override
            public void onGiveUp() {
                gaveUp.set(true);
            }
        });
        return resumer;
    }

    // The TCP check runs on its own thread, so the main looper is stepped until the result lands
    private void idleUntil(AtomicBoolean flag) throws InterruptedException {
        for (int i = 0; i < 100 && !flag.get(); i++) {
            Thread.sleep(30);
            ShadowLooper.idleMainLooper();
        }
    }

    @Test
    public void reportsTheHostWhenTheNetworkIsUpAndThePcAnswers() throws InterruptedException {
        resumerFor(server.getLocalPort()).start();
        ShadowLooper.idleMainLooper(ResumePlan.FIRST_CHECK_DELAY_MS + 100, TimeUnit.MILLISECONDS);
        idleUntil(reachable);

        assertTrue(reachable.get());
        assertEquals(1, reaching.get());
        assertFalse(gaveUp.get());
    }

    @Test
    public void waitsWhileTheNetworkIsDownAndContinuesWhenItReturns() throws InterruptedException {
        connectivity.setActiveNetworkInfo(null);
        resumerFor(server.getLocalPort()).start();
        ShadowLooper.idleMainLooper(ResumePlan.FIRST_CHECK_DELAY_MS + 100, TimeUnit.MILLISECONDS);
        assertTrue(waiting.get() >= 1);
        assertEquals(0, reaching.get());
        assertFalse(reachable.get());

        connectivity.setActiveNetworkInfo(connected);
        // One wait already ran, so the next check comes after the second delay of the plan
        ShadowLooper.idleMainLooper(ResumePlan.delayBeforeCheck(1) + 100, TimeUnit.MILLISECONDS);
        idleUntil(reachable);
        assertTrue(reachable.get());
    }

    @Test
    public void givesUpWhenTheNetworkNeverReturns() {
        connectivity.setActiveNetworkInfo(null);
        resumerFor(server.getLocalPort()).start();
        ShadowLooper.idleMainLooper(ResumePlan.MAX_WAIT_MS + 10_000, TimeUnit.MILLISECONDS);

        assertTrue(gaveUp.get());
        assertFalse(reachable.get());
        assertTrue(lastWaitedMs.get() > 0);
    }

    @Test
    public void stopCancelsEveryPendingCheck() {
        connectivity.setActiveNetworkInfo(null);
        NetworkResumer started = resumerFor(server.getLocalPort());
        started.start();
        started.stop();
        ShadowLooper.idleMainLooper(ResumePlan.MAX_WAIT_MS + 10_000, TimeUnit.MILLISECONDS);

        assertEquals(0, waiting.get());
        assertFalse(gaveUp.get());
    }

    @Test
    public void startTwiceDoesNotDoubleTheChecks() {
        connectivity.setActiveNetworkInfo(null);
        NetworkResumer started = resumerFor(server.getLocalPort());
        started.start();
        started.start();
        ShadowLooper.idleMainLooper(ResumePlan.FIRST_CHECK_DELAY_MS + 100, TimeUnit.MILLISECONDS);

        assertEquals(1, waiting.get());
    }

    @Test
    public void aClosedPortKeepsRetryingWithoutReportingTheHost() throws IOException, InterruptedException {
        int closedPort = server.getLocalPort();
        server.close();
        resumerFor(closedPort).start();
        ShadowLooper.idleMainLooper(ResumePlan.FIRST_CHECK_DELAY_MS + 100, TimeUnit.MILLISECONDS);
        Thread.sleep(300);
        ShadowLooper.idleMainLooper(ResumePlan.delayBeforeCheck(0) + 100, TimeUnit.MILLISECONDS);
        Thread.sleep(300);
        ShadowLooper.idleMainLooper();

        assertTrue(reaching.get() >= 1);
        assertFalse(reachable.get());
        assertFalse(gaveUp.get());
    }
}
