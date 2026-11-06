package com.limelight.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.test.core.app.ApplicationProvider;

import com.limelight.R;
import com.limelight.preferences.PreferenceConfiguration;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.concurrent.atomic.AtomicInteger;

// NOTE: The connection status card shows the waiting stream without buttons and the automatic reconnect with Cancel and Reconnect now.
@Config(sdk = {33}, shadows = {com.limelight.shadows.ShadowMoonBridge.class, com.limelight.shadows.ShadowGameManager.class})
@RunWith(RobolectricTestRunner.class)
public class NetworkStatusCardTest {
    private Context context;
    private View root;
    private NetworkStatusCard card;
    private final AtomicInteger cancels = new AtomicInteger();
    private final AtomicInteger nows = new AtomicInteger();

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        FrameLayout parent = new FrameLayout(context);
        root = LayoutInflater.from(context).inflate(R.layout.view_network_status, parent, false);
        card = new NetworkStatusCard(root, cancels::incrementAndGet, nows::incrementAndGet);
    }

    private String text(int id) {
        return ((TextView) root.findViewById(id)).getText().toString();
    }

    @Test
    public void startsHidden() {
        assertEquals(NetworkStatusCard.Mode.HIDDEN, card.mode());
        assertFalse(card.isShowing());
        assertEquals(View.GONE, root.getVisibility());
    }

    @Test
    public void waitingStreamShowsTheSecondsWithoutButtons() {
        card.showWaitingStream(12);

        assertEquals(NetworkStatusCard.Mode.WAITING_STREAM, card.mode());
        assertEquals(View.VISIBLE, root.getVisibility());
        assertEquals(context.getString(R.string.network_drop_title), text(R.id.networkStatusTitle));
        assertEquals(context.getString(R.string.network_drop_detail, 12), text(R.id.networkStatusDetail));
        assertEquals(View.GONE, root.findViewById(R.id.networkStatusActions).getVisibility());
    }

    @Test
    public void waitingForTheNetworkOffersCancelAndReconnectNow() {
        card.showWaitingNetwork(7);

        assertEquals(context.getString(R.string.network_resume_title), text(R.id.networkStatusTitle));
        assertEquals(context.getString(R.string.network_resume_waiting_network, 7), text(R.id.networkStatusDetail));
        assertEquals(View.VISIBLE, root.findViewById(R.id.networkStatusActions).getVisibility());

        root.findViewById(R.id.networkStatusCancel).performClick();
        root.findViewById(R.id.networkStatusNow).performClick();
        assertEquals(1, cancels.get());
        assertEquals(1, nows.get());
    }

    @Test
    public void reachingTheHostNamesThePcAndFallsBackToAGenericName() {
        card.showReachingHost("Predator");
        assertEquals(context.getString(R.string.network_resume_reaching_host, "Predator"), text(R.id.networkStatusDetail));

        card.showReachingHost(null);
        assertEquals(context.getString(R.string.network_resume_reaching_host, context.getString(R.string.network_resume_pc_fallback)),
                text(R.id.networkStatusDetail));
    }

    @Test
    public void startingHidesTheButtonsBecauseTheLaunchIsOnItsWay() {
        card.showStarting();

        assertEquals(NetworkStatusCard.Mode.STARTING, card.mode());
        assertEquals(View.GONE, root.findViewById(R.id.networkStatusActions).getVisibility());
        assertEquals(context.getString(R.string.network_resume_starting), text(R.id.networkStatusDetail));
    }

    @Test
    public void hideNowClearsTheCardAtOnce() {
        card.showWaitingNetwork(1);
        card.hideNow();

        assertFalse(card.isShowing());
        assertEquals(View.GONE, root.getVisibility());
    }

    @Test
    public void reconnectsAutomaticallyByDefault() {
        PreferenceConfiguration config = PreferenceConfiguration.readPreferences(context);
        assertTrue(config.autoResumeNetwork);
    }
}
