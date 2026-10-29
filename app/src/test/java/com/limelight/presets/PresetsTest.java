package com.limelight.presets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;
import androidx.test.core.app.ApplicationProvider;

import com.limelight.profiles.ProfileScope;
import com.limelight.profiles.ProfilesManager;
import com.limelight.profiles.ProfilesTestAccess;
import com.limelight.profiles.SettingsProfile;
import com.limelight.shadows.ShadowMoonBridge;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

// NOTE: Presets are profile patches. These tests check the values against the real settings screen, and that applying one never overwrites the settings the user tuned.
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, shadows = {ShadowMoonBridge.class})
public class PresetsTest {
    private Context context;
    private SharedPreferences prefs;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        ProfilesTestAccess.reset(context);
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().clear().commit();
        prefs.edit()
                .putString("frame_pacing", "latency")
                .putBoolean("pref_low_latency_frame_balance", false)
                .putBoolean("checkbox_ultra_low_latency", false)
                .putBoolean("checkbox_enable_hdr", true)
                .putString("video_format", "forceh265")
                .putInt("seekbar_bitrate_kbps", 80000)
                .putString("list_fps", "120")
                .commit();
    }

    private Preset preset(String id) {
        Preset preset = PresetCatalog.find(context, id);
        assertNotNull(preset);
        return preset;
    }

    @Test
    public void everyPresetValueMatchesARealSettingOfTheRightType() {
        Map<String, SettingInfo> infos = SettingInfo.read(context);
        List<Preset> presets = PresetCatalog.load(context);
        assertEquals(4, presets.size());
        Set<String> ids = new HashSet<>();
        Set<UUID> profileIds = new HashSet<>();
        for (Preset preset : presets) {
            assertTrue("Duplicate preset " + preset.id, ids.add(preset.id));
            assertTrue(profileIds.add(preset.profileId()));
            assertFalse(preset.values.isEmpty());
            for (Map.Entry<String, Object> entry : preset.values.entrySet()) {
                String where = preset.id + "." + entry.getKey();
                SettingInfo info = infos.get(entry.getKey());
                assertNotNull("No such setting: " + where, info);
                assertFalse("An app-wide setting cannot be in a profile: " + where, ProfileScope.APP_WIDE.contains(entry.getKey()));
                if (info.isSwitch()) {
                    assertTrue("A switch needs true or false: " + where, entry.getValue() instanceof Boolean);
                }
                else if (info.entryValues != null) {
                    boolean known = false;
                    for (CharSequence value : info.entryValues) {
                        known |= value.toString().equals(entry.getValue());
                    }
                    assertTrue("Not a choice of the list: " + where + "=" + entry.getValue(), known);
                }
                else {
                    assertTrue("A slider needs a whole number: " + where, entry.getValue() instanceof Integer);
                }
            }
        }
    }

    @Test
    public void presetsNeverTouchTheResolutionTheCodecOrTheHost() {
        for (Preset preset : PresetCatalog.load(context)) {
            assertFalse(preset.values.containsKey("video_format"));
            assertFalse(preset.values.containsKey("list_resolution"));
            assertFalse(preset.values.containsKey("checkbox_mouse_local_cursor"));
        }
    }

    @Test
    public void theDiffListsEachSettingBeforeAndAfter() {
        List<PresetActions.Change> changes = PresetActions.diff(context, preset("lowest_latency"));
        assertEquals(preset("lowest_latency").values.size(), changes.size());
        PresetActions.Change bitrate = null;
        PresetActions.Change hdr = null;
        for (PresetActions.Change change : changes) {
            if (change.key.equals("seekbar_bitrate_kbps")) {
                bitrate = change;
            }
            if (change.key.equals("checkbox_enable_hdr")) {
                hdr = change;
            }
        }
        assertNotNull(bitrate);
        assertEquals("80 Mbps", bitrate.before);
        assertEquals("30 Mbps", bitrate.after);
        assertNotNull(hdr);
        assertEquals("On", hdr.before);
        assertEquals("Off", hdr.after);
    }

    @Test
    public void applyingAPresetKeepsTheBaseSettingsAndSavesABeforeProfile() {
        Preset preset = preset("lowest_latency");
        PresetActions.apply(context, preset);

        // The settings of the app are untouched, and the overlay shows the preset
        assertEquals(80000, prefs.getInt("seekbar_bitrate_kbps", 0));
        assertEquals("120", prefs.getString("list_fps", ""));
        SharedPreferences effective = ProfilesManager.getInstance().getOverlayingSharedPreferences(context);
        assertEquals(30000, effective.getInt("seekbar_bitrate_kbps", 0));
        assertTrue(effective.getBoolean("checkbox_ultra_low_latency", false));
        // A setting the preset does not cover keeps its own value
        assertEquals("120", effective.getString("list_fps", ""));

        assertEquals(preset.id, PresetActions.activePreset(context).id);
        assertFalse(PresetActions.isCustomized(preset));

        SettingsProfile before = null;
        for (SettingsProfile profile : ProfilesManager.getInstance().getProfiles()) {
            if (profile.getName().equals("Before Competitive play")) {
                before = profile;
            }
        }
        assertNotNull(before);
        assertEquals(80000, ((Number) before.getOptions().get("seekbar_bitrate_kbps")).intValue());
        assertEquals(Boolean.TRUE, before.getOptions().get("checkbox_enable_hdr"));
    }

    @Test
    public void usingMyOwnSettingsBringsEveryValueBack() {
        PresetActions.apply(context, preset("battery_saver"));
        assertEquals("60", ProfilesManager.getInstance().getOverlayingSharedPreferences(context).getString("list_fps", ""));

        PresetActions.useOwnSettings();

        assertNull(PresetActions.activePreset(context));
        SharedPreferences effective = ProfilesManager.getInstance().getOverlayingSharedPreferences(context);
        assertEquals("120", effective.getString("list_fps", ""));
        assertEquals(80000, effective.getInt("seekbar_bitrate_kbps", 0));
        assertTrue(effective.getBoolean("checkbox_enable_hdr", false));
    }

    @Test
    public void applyingTwiceUpdatesTheSameProfilesInsteadOfAddingMore() {
        Preset preset = preset("balanced");
        PresetActions.apply(context, preset);
        int count = ProfilesManager.getInstance().getProfiles().size();
        PresetActions.apply(context, preset);
        assertEquals(count, ProfilesManager.getInstance().getProfiles().size());
        assertEquals(2, count);
    }

    @Test
    public void editingThePresetProfileAfterwardsShowsAsCustomized() {
        Preset preset = preset("balanced");
        SettingsProfile profile = PresetActions.apply(context, preset);
        assertFalse(PresetActions.isCustomized(preset));

        profile.getOptions().put("seekbar_bitrate_kbps", 65000);
        assertTrue(PresetActions.isCustomized(preset));
        assertEquals(preset.id, PresetActions.activePreset(context).id);
    }

    @Test
    public void aSavedProfileHoldsTheCurrentValuesOfEveryPresetSetting() {
        SettingsProfile saved = PresetActions.saveCurrentAsProfile(context, "Desk");
        assertEquals("Desk", saved.getName());
        assertEquals(80000, ((Number) saved.getOptions().get("seekbar_bitrate_kbps")).intValue());
        assertEquals("120", saved.getOptions().get("list_fps"));
    }

    @Test
    public void numbersReadBackFromAFileStillCompareEqual() {
        assertTrue(PresetActions.sameValue(30000, 30000.0));
        assertFalse(PresetActions.sameValue(30000, 30001.0));
        assertTrue(PresetActions.sameValue("latency", "latency"));
        assertTrue(PresetActions.sameValue(Boolean.TRUE, true));
    }
}
