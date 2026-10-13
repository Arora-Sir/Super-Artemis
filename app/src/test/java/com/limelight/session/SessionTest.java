package com.limelight.session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;
import androidx.test.core.app.ApplicationProvider;

import com.limelight.AppView;
import com.limelight.Game;
import com.limelight.ShortcutTrampoline;
import com.limelight.backup.BackupCodec;
import com.limelight.preferences.PreferenceConfiguration;
import com.limelight.shadows.ShadowMoonBridge;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, shadows = {ShadowMoonBridge.class})
public class SessionTest {
    private Context context;
    private SharedPreferences prefs;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().clear().commit();
        context.getSharedPreferences(ResumeStore.PREF_NAME, Context.MODE_PRIVATE).edit().clear().commit();
    }

    private static ResumeStore.Session session() {
        return new ResumeStore.Session("pc-uuid", "Gaming PC", "Desktop", "app-uuid", 881448767, true);
    }

    @Test
    public void policyDefaultsToDisconnectAndMigratesTheOldPipSwitch() {
        assertEquals(BackgroundPolicy.DISCONNECT, BackgroundPolicy.get(prefs));
        prefs.edit().putBoolean(BackgroundPolicy.LEGACY_PIP, true).commit();
        assertEquals(BackgroundPolicy.PIP, BackgroundPolicy.get(prefs));
        assertTrue(PreferenceConfiguration.readPreferences(context).enablePip);

        prefs.edit().putString(BackgroundPolicy.PREF_KEY, BackgroundPolicy.RESUME).commit();
        assertTrue(BackgroundPolicy.isResume(prefs));
        assertFalse(PreferenceConfiguration.readPreferences(context).enablePip);
    }

    @Test
    public void pendingResumeIsTakenOnceWithinTheWindow() {
        ResumeStore.remember(context, session());
        assertNull("nothing pending before leaving", ResumeStore.takePending(context, 1000));

        ResumeStore.markLeft(context, 1000);
        ResumeStore.Session resumed = ResumeStore.takePending(context, 1000 + ResumeStore.RESUME_WINDOW_MS);
        assertNotNull(resumed);
        assertEquals("pc-uuid", resumed.pcUuid);
        assertEquals(881448767, resumed.appId);
        assertTrue(resumed.hdr);
        assertNull("taken only once", ResumeStore.takePending(context, 2000));
    }

    @Test
    public void pendingResumeExpiresAndCanBeCleared() {
        ResumeStore.remember(context, session());
        ResumeStore.markLeft(context, 1000);
        assertNull(ResumeStore.takePending(context, 1001 + ResumeStore.RESUME_WINDOW_MS));

        ResumeStore.markLeft(context, 5000);
        ResumeStore.clearPending(context);
        assertNull(ResumeStore.takePending(context, 5001));
        assertNotNull("the last session stays for auto-connect", ResumeStore.last(context));
    }

    @Test
    public void newStreamCancelsAnOlderPendingResume() {
        ResumeStore.remember(context, session());
        ResumeStore.markLeft(context, 1000);
        ResumeStore.remember(context, new ResumeStore.Session("other-pc", "Laptop", "Steam", null, 5, false));
        assertNull(ResumeStore.takePending(context, 1001));
        assertEquals("other-pc", ResumeStore.last(context).pcUuid);
    }

    @Test
    public void resumeIntentGoesThroughTheShortcutTrampoline() {
        Activity activity = Robolectric.buildActivity(Activity.class).create().get();
        Intent intent = SessionResumer.intentFor(activity, session());
        assertEquals(ShortcutTrampoline.class.getName(), intent.getComponent().getClassName());
        assertEquals("pc-uuid", intent.getStringExtra(AppView.UUID_EXTRA));
        assertEquals("app-uuid", intent.getStringExtra(Game.EXTRA_APP_UUID));
        assertEquals("881448767", intent.getStringExtra(Game.EXTRA_APP_ID));
        assertTrue(intent.getBooleanExtra(Game.EXTRA_APP_HDR, false));
    }

    @Test
    public void sessionStateIsLeftOutOfBackups() throws Exception {
        ResumeStore.remember(context, session());
        JSONObject backup = BackupCodec.create(context, false);
        assertFalse(backup.getJSONObject("sharedPreferences").has(ResumeStore.PREF_NAME));
    }
}
