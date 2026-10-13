package com.limelight.backup;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;
import androidx.test.core.app.ApplicationProvider;

import com.limelight.GameMenu;
import com.limelight.shadows.ShadowMoonBridge;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

// Password protection, choosing what to restore, keeping settings this device cannot use, and the device checks
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, shadows = {ShadowMoonBridge.class})
public class BackupRound2Test {
    private Context context;
    private SharedPreferences defaults;
    private SharedPreferences special;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        defaults = PreferenceManager.getDefaultSharedPreferences(context);
        defaults.edit().clear().commit();
        special = context.getSharedPreferences(GameMenu.PREF_NAME, Context.MODE_PRIVATE);
        special.edit().clear().commit();
    }

    @Test
    public void encryptedBackupOpensOnlyWithItsPassword() throws Exception {
        byte[] plain = "{\"hello\":\"world\"}".getBytes(StandardCharsets.UTF_8);
        JSONObject sealed = BackupCrypto.encrypt(plain, "correct horse".toCharArray(), 20_000);
        assertTrue(BackupCrypto.isEncrypted(sealed));
        assertFalse(sealed.toString().contains("world"));
        assertArrayEquals(plain, BackupCrypto.decrypt(sealed, "correct horse".toCharArray()));
        try {
            BackupCrypto.decrypt(sealed, "wrong horse".toCharArray());
            fail("Expected WrongPasswordException");
        }
        catch (BackupCrypto.WrongPasswordException expected) {
            // Expected
        }
    }

    // The iteration count is part of the authenticated header, so lowering it to speed up guessing breaks the file
    @Test
    public void changedHeaderIsRejected() throws Exception {
        JSONObject sealed = BackupCrypto.encrypt(new byte[] {1, 2, 3}, "password1".toCharArray(), 20_000);
        sealed.put("iterations", 10_000);
        try {
            BackupCrypto.decrypt(sealed, "password1".toCharArray());
            fail("Expected WrongPasswordException");
        }
        catch (BackupCrypto.WrongPasswordException expected) {
            // Expected
        }
    }

    // PBKDF2-HMAC-SHA256 test vector from RFC 7914, section 11
    @Test
    public void keyDerivationMatchesTheStandard() throws Exception {
        byte[] key = BackupCrypto.pbkdf2("passwd".toCharArray(), "salt".getBytes(StandardCharsets.UTF_8), 1);
        assertEquals("55ac046e56e3089fec1691c22544b605f94185216dde0465e68b9d57c20dacbc", hex(key));
    }

    @Test
    public void encryptedFullBackupRestoresThroughTheManager() throws Exception {
        defaults.edit().putString("list_fps", "120").commit();
        byte[] file = BackupRestoreManager.createBackupBytes(context, false, "a long password".toCharArray());
        defaults.edit().putString("list_fps", "60").commit();
        try {
            BackupRestoreManager.parseBackup(file, null);
            fail("Expected PasswordRequiredException");
        }
        catch (BackupRestoreManager.PasswordRequiredException expected) {
            // Expected
        }
        BackupFile backup = BackupRestoreManager.parseBackup(file, "a long password".toCharArray());
        BackupRestoreManager.applyBackup(context, backup);
        assertEquals("120", defaults.getString("list_fps", null));
    }

    @Test
    public void unticked_partsStayAsTheyAre() throws Exception {
        defaults.edit().putString("list_fps", "120").commit();
        special.edit().putString(GameMenu.KEY_NAME, "{\"data\":[]}").commit();
        BackupFile backup = BackupCodec.validate(BackupCodec.create(context, false));

        defaults.edit().putString("list_fps", "60").commit();
        special.edit().putString(GameMenu.KEY_NAME, "local keys").commit();
        // Only settings are restored, so special keys keep this device's version
        BackupRestoreManager.applyBackup(context, backup, new RestoreSelection(true, false, false, false, false));
        assertEquals("120", defaults.getString("list_fps", null));
        assertEquals("local keys", special.getString(GameMenu.KEY_NAME, null));
    }

    @Test
    public void keptSettingsKeepThisDevicesValue() throws Exception {
        defaults.edit().putString("list_resolution", "2960x1848").putString("list_fps", "120").commit();
        BackupFile backup = BackupCodec.validate(BackupCodec.create(context, false));

        defaults.edit().putString("list_resolution", "2340x1080").putString("list_fps", "60").commit();
        BackupRestoreManager.applyBackup(context, backup,
                new RestoreSelection(true, true, true, true, false, Collections.singleton("list_resolution")));
        assertEquals("2340x1080", defaults.getString("list_resolution", null));
        assertEquals("120", defaults.getString("list_fps", null));
    }

    @Test
    public void deviceChecksFlagWhatThisDeviceCannotUse() throws Exception {
        JSONObject prefs = new JSONObject()
                .put("list_resolution", typed("string", "2960x1848"))
                .put("list_fps", typed("string", "144"))
                .put("checkbox_enable_hdr", typed("boolean", true))
                .put("checkbox_gamepad_motion_sensors", typed("boolean", true))
                .put("list_microphone_device", typed("string", "7"));
        // A 2340x1080 phone at 120 Hz with HDR, no gyroscope and a vibrator
        DeviceCompatibility.Device phone = new DeviceCompatibility.Device(1080, 2340, 120, true, false, true);
        List<DeviceCompatibility.Issue> issues = DeviceCompatibility.check(context, prefs, phone);
        assertEquals("list_resolution", issues.get(0).key);
        assertEquals("list_fps", issues.get(1).key);
        assertEquals("checkbox_gamepad_motion_sensors", issues.get(2).key);
        assertEquals("list_microphone_device", issues.get(3).key);
        assertEquals(4, issues.size());

        // The same backup on the tablet that made it raises nothing but the microphone
        DeviceCompatibility.Device tablet = new DeviceCompatibility.Device(2960, 1848, 144, true, true, true);
        assertEquals(1, DeviceCompatibility.check(context, prefs, tablet).size());
        assertNull(DeviceCompatibility.parseSize("native"));
    }

    @Test
    public void pairingKeysAlwaysBringThePcList() {
        RestoreSelection selection = new RestoreSelection(false, false, false, false, true);
        assertTrue(selection.pcs);
        assertFalse(selection.isEmpty());
        assertTrue(new RestoreSelection(false, false, false, false, false).isEmpty());
    }

    private static JSONObject typed(String type, Object value) throws Exception {
        return new JSONObject().put("type", type).put("value", value);
    }

    private static String hex(byte[] bytes) {
        StringBuilder out = new StringBuilder();
        for (byte b : bytes) {
            out.append(String.format("%02x", b));
        }
        return out.toString();
    }
}
