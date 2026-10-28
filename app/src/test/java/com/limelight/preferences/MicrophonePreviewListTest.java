package com.limelight.preferences;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import android.content.Context;

import androidx.preference.PreferenceGroupAdapter;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ApplicationProvider;

import com.limelight.shadows.ShadowMoonBridge;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

// NOTE: The microphone preview row once slid from the bottom of Settings to its place, because it was rebound 20 times per second and the list animated it (BUG-045).
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, shadows = {ShadowMoonBridge.class})
public class MicrophonePreviewListTest {
    @Test
    public void settingsListHasNoItemAnimator() {
        Context context = ApplicationProvider.getApplicationContext();
        RecyclerView list = new RecyclerView(context);

        SettingsListStyle.apply(list);

        assertNull(list.getItemAnimator());
    }

    @Test
    public void levelTicksWithTheSameStatusDoNotRebindTheRow() {
        Context context = ApplicationProvider.getApplicationContext();
        PreferenceScreen screen = new PreferenceManager(context).createPreferenceScreen(context);
        MicrophonePreviewPreference preview = new MicrophonePreviewPreference(context);
        preview.setKey("microphone_preview");
        screen.addPreference(preview);
        PreferenceGroupAdapter adapter = new PreferenceGroupAdapter(screen);
        int[] changes = {0};
        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                changes[0]++;
            }

            @Override
            public void onItemRangeChanged(int positionStart, int itemCount) {
                changes[0]++;
            }

            @Override
            public void onItemRangeChanged(int positionStart, int itemCount, Object payload) {
                changes[0]++;
            }
        });

        // The first tick brings the status text, so the row is rebound once
        preview.updatePreviewState("Listening", 0.10, true);
        int afterFirst = changes[0];

        for (int i = 1; i <= 100; i++) {
            preview.updatePreviewState("Listening", (i % 100) / 100.0, true);
        }

        assertEquals("Level ticks rebound the row", afterFirst, changes[0]);
    }
}
