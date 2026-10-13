package com.limelight.profiles;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;
import androidx.test.core.app.ApplicationProvider;

import com.limelight.menu.BarSets;
import com.limelight.shadows.ShadowMoonBridge;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, shadows = {ShadowMoonBridge.class})
public class ProfileCopyTest {
    private Context context;
    private SharedPreferences prefs;
    private ProfilesManager manager;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().clear().commit();
        ProfilesManager.instance = null;
        manager = ProfilesManager.getInstance();
    }

    private SettingsProfile profile(String name, Map<String, Object> options) {
        SettingsProfile profile = new SettingsProfile(UUID.randomUUID(), name, 1, 1, options);
        manager.add(profile);
        return profile;
    }

    @Test
    public void duplicateCopiesEveryOptionIntoANewProfile() {
        Map<String, Object> options = new HashMap<>();
        options.put("list_resolution", "1920x1080");
        options.put("some_set", new ArrayList<>(Arrays.asList("a", "b")));
        SettingsProfile game = profile("Game", options);

        SettingsProfile copy = manager.duplicate(game.getUuid(), "Game copy");

        assertNotEquals(game.getUuid(), copy.getUuid());
        assertEquals("Game copy", copy.getName());
        assertEquals("1920x1080", copy.getOptions().get("list_resolution"));
        ((List<String>) copy.getOptions().get("some_set")).add("c");
        assertEquals(2, ((List<?>) game.getOptions().get("some_set")).size());
        assertEquals(2, manager.getProfiles().size());
    }

    @Test
    public void copyIntoReplacesOptionsAndKeepsTheName() {
        SettingsProfile game = profile("Game", new HashMap<>(Collections.singletonMap("list_fps", "120")));
        SettingsProfile desk = profile("Desk", new HashMap<>(Collections.singletonMap("list_fps", "60")));

        assertTrue(manager.copyOptionsInto(game.getUuid(), desk.getUuid()));
        assertEquals("120", desk.getOptions().get("list_fps"));
        assertEquals("Desk", desk.getName());
        assertFalse(manager.copyOptionsInto(game.getUuid(), game.getUuid()));
    }

    @Test
    public void barSetsBelongToTheActiveProfile() {
        BarSets.State defaults = new BarSets.State(Collections.singletonList(new BarSets.Set("Default", new ArrayList<>())), 0);
        BarSets.save(context, defaults);
        String saved = prefs.getString(BarSets.PREF_SETS, null);

        SettingsProfile game = profile("Game", new HashMap<>());
        manager.setActive(game.getUuid());
        BarSets.State state = BarSets.load(context);
        state.sets.add(new BarSets.Set("Racing", new ArrayList<>()));
        state.active = 1;
        BarSets.save(context, state);

        // The profile holds its own sets, and the default settings stay as they were
        assertEquals(saved, prefs.getString(BarSets.PREF_SETS, null));
        assertEquals("Racing", BarSets.load(context).activeSet().name);
        manager.setActive(null);
        assertEquals(1, BarSets.load(context).sets.size());
    }
}
