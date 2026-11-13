package com.limelight.profiles;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.robolectric.annotation.Config;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.File;
import java.util.UUID;

import static org.junit.Assert.*;

import com.limelight.TestLogSuppressor;

@Config(sdk = {33}, shadows = {com.limelight.shadows.ShadowMoonBridge.class, com.limelight.shadows.ShadowGameManager.class})
@RunWith(RobolectricTestRunner.class)
public class ProfilesManagerTest {
    private Context context;
    private ProfilesManager manager;
    private File profilesDir;

    @BeforeClass
    public static void suppressInvalidIdLogs() {
        TestLogSuppressor.install();
    }

    @Before
    public void setUp() {
        ProfilesManager.instance = null; // Reset the singleton before each test
        context = ApplicationProvider.getApplicationContext();
        profilesDir = new File(context.getFilesDir(), "profiles");
        deleteRecursively(profilesDir);
        manager = ProfilesManager.getInstance();
        manager.load(context);
    }

    @After
    public void tearDown() {
        deleteRecursively(profilesDir);
    }

    @Test
    public void addAndRetrieveProfile() {
        SettingsProfile p = new SettingsProfile(UUID.randomUUID(), "Test", System.currentTimeMillis(), System.currentTimeMillis(), null);
        manager.add(p);
        assertEquals(1, manager.getProfiles().size());
        assertEquals(p.getUuid(), manager.getProfiles().get(0).getUuid());
    }

    // NOTE: The default template is the profile that new profiles start from. It survives a reload, never touches other profiles, and clears itself when that profile is deleted.
    @Test
    public void theDefaultTemplateSurvivesAReloadAndClearsWhenItsProfileIsDeleted() {
        java.util.Map<String, Object> options = new java.util.HashMap<>();
        options.put("list_fps", "90");
        SettingsProfile template = new SettingsProfile(UUID.randomUUID(), "Base", 1L, 1L, options);
        SettingsProfile other = new SettingsProfile(UUID.randomUUID(), "Other", 1L, 1L, new java.util.HashMap<>());
        manager.add(template);
        manager.add(other);
        assertNull(manager.getDefaultTemplateId());
        assertNull(manager.defaultTemplateOptions());

        manager.setDefaultTemplate(template.getUuid());
        assertTrue(manager.isDefaultTemplate(template.getUuid()));
        assertFalse(manager.isDefaultTemplate(other.getUuid()));
        assertEquals("90", manager.defaultTemplateOptions().get("list_fps"));
        // The copy handed out is its own, so editing it never changes the template
        manager.defaultTemplateOptions().put("list_fps", "30");
        assertEquals("90", manager.defaultTemplateOptions().get("list_fps"));

        assertTrue(manager.save(context));
        ProfilesManager.instance = null;
        ProfilesManager reloaded = ProfilesManager.getInstance();
        reloaded.load(context);
        assertEquals(template.getUuid(), reloaded.getDefaultTemplateId());

        reloaded.delete(template.getUuid());
        assertNull(reloaded.getDefaultTemplateId());
        assertNull(reloaded.defaultTemplateOptions());
    }

    @Test
    public void anUnknownProfileCannotBecomeTheTemplate() {
        manager.setDefaultTemplate(UUID.randomUUID());
        assertNull(manager.getDefaultTemplateId());
    }

    @Test
    public void setActivePersists() {
        SettingsProfile p = new SettingsProfile(UUID.randomUUID(), "Active", System.currentTimeMillis(), System.currentTimeMillis(), null);
        manager.add(p);
        manager.setActive(p.getUuid());
        // Reload from disk to verify persistence
        ProfilesManager fresh = ProfilesManager.getInstance();
        fresh.load(context);
        assertNotNull(fresh.getActive());
        assertEquals(p.getUuid(), fresh.getActive().getUuid());
    }

    @Test
    public void updateAndSaveProfile() {
        SettingsProfile p = new SettingsProfile(UUID.randomUUID(), "Original", System.currentTimeMillis(), System.currentTimeMillis(), null);
        manager.add(p);
        p.setName("Updated");
        manager.update(p);

        ProfilesManager fresh = ProfilesManager.getInstance();
        fresh.load(context);
        assertEquals("Updated", fresh.getProfiles().get(0).getName());
    }

    @Test
    public void deleteProfile() {
        SettingsProfile p = new SettingsProfile(UUID.randomUUID(), "ToDelete", System.currentTimeMillis(), System.currentTimeMillis(), null);
        manager.add(p);
        assertEquals(1, manager.getProfiles().size());

        manager.delete(p.getUuid());
        assertEquals(0, manager.getProfiles().size());
    }

    @Test
    public void deleteActiveProfile_resetsActive() {
        SettingsProfile p = new SettingsProfile(UUID.randomUUID(), "ActiveToDelete", System.currentTimeMillis(), System.currentTimeMillis(), null);
        manager.add(p);
        manager.setActive(p.getUuid());
        assertNotNull(manager.getActive());

        manager.delete(p.getUuid());
        assertNull(manager.getActive());

        // Verify persistence
        ProfilesManager fresh = ProfilesManager.getInstance();
        fresh.load(context);
        assertNull(fresh.getActive());
    }

    // A drag ends with move steps and one save: the new order must come back from profiles.json, and the active profile with it
    @Test
    public void moveKeepsNewOrderAfterReload() {
        SettingsProfile a = new SettingsProfile(UUID.randomUUID(), "A", System.currentTimeMillis(), System.currentTimeMillis(), null);
        SettingsProfile b = new SettingsProfile(UUID.randomUUID(), "B", System.currentTimeMillis(), System.currentTimeMillis(), null);
        SettingsProfile c = new SettingsProfile(UUID.randomUUID(), "C", System.currentTimeMillis(), System.currentTimeMillis(), null);
        manager.add(a);
        manager.add(b);
        manager.add(c);
        manager.setActive(b.getUuid());

        assertTrue(manager.move(2, 0));
        manager.save(context);

        ProfilesManager.instance = null;
        ProfilesManager fresh = ProfilesManager.getInstance();
        fresh.load(context);
        assertEquals("C", fresh.getProfiles().get(0).getName());
        assertEquals("A", fresh.getProfiles().get(1).getName());
        assertEquals("B", fresh.getProfiles().get(2).getName());
        assertEquals(b.getUuid(), fresh.getActive().getUuid());
    }

    @Test
    public void moveToEndAndOutOfRange() {
        SettingsProfile a = new SettingsProfile(UUID.randomUUID(), "A", System.currentTimeMillis(), System.currentTimeMillis(), null);
        SettingsProfile b = new SettingsProfile(UUID.randomUUID(), "B", System.currentTimeMillis(), System.currentTimeMillis(), null);
        manager.add(a);
        manager.add(b);

        assertTrue(manager.move(0, 1));
        assertEquals("B", manager.getProfiles().get(0).getName());
        assertEquals("A", manager.getProfiles().get(1).getName());

        assertFalse(manager.move(0, 2));
        assertFalse(manager.move(-1, 0));
        assertEquals("B", manager.getProfiles().get(0).getName());
    }

    private void deleteRecursively(File f) {
        if (f == null || !f.exists()) return;
        if (f.isDirectory()) {
            for (File c : f.listFiles()) {
                deleteRecursively(c);
            }
        }
        f.delete();
    }
}