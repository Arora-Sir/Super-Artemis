package com.limelight.menu;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.limelight.R;
import com.limelight.customkeys.CustomKey;
import com.limelight.customkeys.CustomKeyStore;
import com.limelight.shadows.ShadowMoonBridge;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

import java.util.ArrayList;
import java.util.Arrays;

// NOTE: A key made in the bar editor is pinned at once, scrolled into view, and kept when the user switches sets and comes back (it used to vanish from the bar on Save).
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, shadows = {ShadowMoonBridge.class})
public class QuickBarEditorKeysTest {
    private AppCompatActivity activity;
    private SharedPreferences prefs;

    @Before
    public void setUp() {
        org.robolectric.android.controller.ActivityController<AppCompatActivity> controller = Robolectric.buildActivity(AppCompatActivity.class);
        controller.get().setTheme(R.style.AppTheme_MaterialYou);
        activity = controller.setup().get();
        prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        prefs.edit().clear().commit();
        BarSets.save(activity, new BarSets.State(Arrays.asList(
                new BarSets.Set("Desktop", Arrays.asList(
                        new BarSets.Bar(BarSets.TOP_LEFT, 3, false, 0, 0, new ArrayList<>()))),
                new BarSets.Set("Game", Arrays.asList(
                        new BarSets.Bar(BarSets.TOP_RIGHT, 3, false, 0, 0, new ArrayList<>())))), 0));
    }

    private QuickBarEditor.Session open() {
        QuickBarEditor.Session session = new QuickBarEditor.Session(activity, prefs, null, 0);
        session.open();
        ShadowLooper.idleMainLooper();
        return session;
    }

    @Test
    public void aNewKeyIsPinnedToTheSelectedBarAtOnce() throws Exception {
        QuickBarEditor.Session session = open();
        CustomKey key = CustomKeyStore.upsert(activity, new CustomKey(null, "Gemini Talk", false, "mic", Arrays.asList(0xA2, 0x4D)));

        session.keyCreated(key);

        assertTrue(QuickBarEditor.pinnedItems(session.rowsByBar.get(0)).contains(QuickBarLayout.customKeyItem(key.id)));
    }

    @Test
    public void aNewKeyStaysPinnedAfterSwitchingSetsAndBack() throws Exception {
        QuickBarEditor.Session session = open();
        CustomKey key = CustomKeyStore.upsert(activity, new CustomKey(null, "Gemini Talk", false, "mic", Arrays.asList(0xA2, 0x4D)));
        session.keyCreated(key);

        session.showSet(1);
        session.showSet(0);

        assertEquals(Arrays.asList(QuickBarLayout.customKeyItem(key.id)), QuickBarEditor.pinnedItems(session.rowsByBar.get(0)));
    }
}
