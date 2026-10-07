package com.limelight.preferences;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import androidx.preference.PreferenceManager;
import androidx.test.core.app.ApplicationProvider;

import com.limelight.computers.ComputerDatabaseManager;
import com.limelight.nvstream.http.ComputerDetails;
import com.limelight.shadows.ShadowMoonBridge;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, shadows = {ShadowMoonBridge.class})
public class BackupRestoreManagerTest {
    // Throwaway self-signed EC certificate used only to exercise X.509 parsing
    private static final String TEST_CERT_BASE64 =
            "MIIBUzCB+aADAgECAggKZIWhFrRkHzAKBggqhkjOPQQDAzAdMRswGQYDVQQDExJTdXBlciBBcnRlbWlzIFRlc3QwIBcNMjYxMDA3MTMyODMyWhgPMjEyNjA5MTMxMzI4MzJaMB0xGzAZBgNVBAMTElN1cGVyIEFydGVtaXMgVGVzdDBZMBMGByqGSM49AgEGCCqGSM49AwEHA0IABH+k0XIVeuiL6HWrM5BV+Gacy6lWwXQP4T9wxgDrfJ3yjNiWHqpOVRuvy2StnvNAFd4yLqeBXcqhv6cZdF6RuBKjITAfMB0GA1UdDgQWBBRDW2mPLgZ3b++IKY1gkcmVcRyWuDAKBggqhkjOPQQDAwNJADBGAiEAxHzAWdrE6azQvlGPv0clEVYMYcNMOwamLRNMRHXrdVQCIQDbsmEI/pC21IBj1BCf5JlGEC8kXea6O0Zl06CNj2c/kg==";

    private Context context;
    private SharedPreferences defaultPrefs;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        defaultPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        defaultPrefs.edit().clear().commit();
    }

    @Test
    public void roundTripRestoresTypedPreferencesHostsProfilesAndIdentity() throws Exception {
        Set<String> stringSet = new HashSet<>(Arrays.asList("a", "b"));
        defaultPrefs.edit()
                .putBoolean("checkbox_enable_microphone", true)
                .putInt("seekbar_bitrate_kbps", 75000)
                .putLong("some_long", 1234567890123L)
                .putFloat("some_float", 1.5f)
                .putString("list_resolution", "2560x1600")
                .putStringSet("some_set", stringSet)
                .commit();
        context.getSharedPreferences("specialPrefs", Context.MODE_PRIVATE).edit()
                .putString("special_key", "{\"data\":[]}").commit();
        context.getSharedPreferences("DecoderTombstone", Context.MODE_PRIVATE).edit()
                .putInt("CrashCount", 3).commit();
        context.getSharedPreferences("android.app.ActivityThread.IDS", Context.MODE_PRIVATE).edit()
                .putString("id", "framework").commit();
        context.getSharedPreferences("preferenceSearch", Context.MODE_PRIVATE).edit()
                .putString("history_0", "bitrate").commit();
        writeFile(new File(context.getFilesDir(), "profiles/profiles.json"), "{\"profiles\":[]}");
        writeFile(new File(context.getFilesDir(), "uniqueid"), "0123456789abcdef");
        writeBytes(new File(context.getFilesDir(), "client.crt"), Base64.decode(TEST_CERT_BASE64, Base64.NO_WRAP));
        writeFile(new File(context.getFilesDir(), "client.key"), "private-key-bytes");
        saveHost("host-uuid-1", "Gaming PC", testCertificate());

        JSONObject backup = new JSONObject(BackupRestoreManager.createBackup(context, true).toString());
        assertFalse(backup.getJSONObject("sharedPreferences").has("DecoderTombstone"));
        assertFalse(backup.getJSONObject("sharedPreferences").has("android.app.ActivityThread.IDS"));
        assertFalse(backup.getJSONObject("sharedPreferences").has("preferenceSearch"));

        // Wipe the local state, then restore from the backup
        defaultPrefs.edit().clear().putString("leftover", "x").commit();
        context.getSharedPreferences("specialPrefs", Context.MODE_PRIVATE).edit().clear().commit();
        new File(context.getFilesDir(), "profiles/profiles.json").delete();
        writeFile(new File(context.getFilesDir(), "uniqueid"), "ffffffffffffffff");
        deleteAllHosts();
        saveHost("stale-host", "Old PC", null);

        BackupRestoreManager.validate(backup);
        BackupRestoreManager.applyBackup(context, backup);

        assertTrue(defaultPrefs.getBoolean("checkbox_enable_microphone", false));
        assertEquals(75000, defaultPrefs.getInt("seekbar_bitrate_kbps", 0));
        assertEquals(1234567890123L, defaultPrefs.getLong("some_long", 0));
        assertEquals(1.5f, defaultPrefs.getFloat("some_float", 0f), 0.0001f);
        assertEquals("2560x1600", defaultPrefs.getString("list_resolution", null));
        assertEquals(stringSet, defaultPrefs.getStringSet("some_set", null));
        assertFalse(defaultPrefs.contains("leftover"));
        assertEquals("{\"data\":[]}", context.getSharedPreferences("specialPrefs", Context.MODE_PRIVATE).getString("special_key", null));
        assertEquals("{\"profiles\":[]}", readFile(new File(context.getFilesDir(), "profiles/profiles.json")));
        assertEquals("0123456789abcdef", readFile(new File(context.getFilesDir(), "uniqueid")));
        assertEquals("private-key-bytes", readFile(new File(context.getFilesDir(), "client.key")));

        ComputerDatabaseManager db = new ComputerDatabaseManager(context);
        try {
            List<ComputerDetails> hosts = db.getAllComputers();
            assertEquals(1, hosts.size());
            ComputerDetails host = hosts.get(0);
            assertEquals("host-uuid-1", host.uuid);
            assertEquals("Gaming PC", host.name);
            assertEquals("100.64.0.10", host.manualAddress.address);
            assertEquals(47989, host.manualAddress.port);
            assertNotNull(host.serverCert);
            assertArrayEquals(testCertificate().getEncoded(), host.serverCert.getEncoded());
        }
        finally {
            db.close();
        }
    }

    @Test
    public void backupWithoutIdentityOmitsPairingKeys() throws Exception {
        writeFile(new File(context.getFilesDir(), "client.key"), "private-key-bytes");
        JSONObject backup = BackupRestoreManager.createBackup(context, false);
        assertFalse(BackupRestoreManager.hasIdentity(backup));
        assertNull(backup.optJSONObject("identity"));
    }

    @Test
    public void validateRejectsForeignFormat() throws Exception {
        JSONObject backup = BackupRestoreManager.createBackup(context, false);
        backup.put("format", "something-else");
        assertInvalid(backup);
    }

    @Test
    public void validateRejectsNewerSchema() throws Exception {
        JSONObject backup = BackupRestoreManager.createBackup(context, false);
        backup.put("schemaVersion", BackupRestoreManager.SCHEMA_VERSION + 1);
        assertInvalid(backup);
    }

    @Test
    public void validateRejectsFilesOutsideTheAllowlist() throws Exception {
        JSONObject backup = BackupRestoreManager.createBackup(context, false);
        backup.getJSONObject("files").put("../shared_prefs/evil.xml", Base64.encodeToString(new byte[] {1}, Base64.NO_WRAP));
        assertInvalid(backup);
    }

    @Test
    public void restoreSkipsExcludedFilesFromOlderBackups() throws Exception {
        JSONObject backup = BackupRestoreManager.createBackup(context, false);
        JSONObject typed = new JSONObject().put("type", "string").put("value", "stale");
        backup.getJSONObject("sharedPreferences").put("android.app.ActivityThread.IDS", new JSONObject().put("id", typed));
        BackupRestoreManager.validate(backup);
        BackupRestoreManager.applyBackup(context, backup);
        assertFalse(context.getSharedPreferences("android.app.ActivityThread.IDS", Context.MODE_PRIVATE).contains("id"));
    }

    @Test
    public void validateRejectsUnknownPreferenceType() throws Exception {
        JSONObject backup = BackupRestoreManager.createBackup(context, false);
        JSONObject typed = new JSONObject().put("type", "double").put("value", 1.0);
        backup.getJSONObject("sharedPreferences").put(BackupRestoreManager.DEFAULT_PREFS_KEY, new JSONObject().put("k", typed));
        assertInvalid(backup);
    }

    private static void assertInvalid(JSONObject backup) {
        try {
            BackupRestoreManager.validate(backup);
            fail("Expected InvalidBackupException");
        }
        catch (BackupRestoreManager.InvalidBackupException expected) {
            // expected
        }
    }

    private void saveHost(String uuid, String name, X509Certificate cert) {
        ComputerDetails details = new ComputerDetails();
        details.uuid = uuid;
        details.name = name;
        details.manualAddress = new ComputerDetails.AddressTuple("100.64.0.10", 47989);
        details.macAddress = "00:11:22:33:44:55";
        details.serverCert = cert;
        ComputerDatabaseManager db = new ComputerDatabaseManager(context);
        try {
            assertTrue(db.updateComputer(details));
        }
        finally {
            db.close();
        }
    }

    private void deleteAllHosts() {
        ComputerDatabaseManager db = new ComputerDatabaseManager(context);
        try {
            for (ComputerDetails details : db.getAllComputers()) {
                db.deleteComputer(details);
            }
        }
        finally {
            db.close();
        }
    }

    private static X509Certificate testCertificate() throws Exception {
        return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(
                new ByteArrayInputStream(Base64.decode(TEST_CERT_BASE64, Base64.NO_WRAP)));
    }

    private static void writeFile(File file, String content) throws IOException {
        writeBytes(file, content.getBytes(StandardCharsets.UTF_8));
    }

    private static void writeBytes(File file, byte[] data) throws IOException {
        file.getParentFile().mkdirs();
        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write(data);
        }
    }

    private static String readFile(File file) throws IOException {
        try (FileInputStream in = new FileInputStream(file)) {
            byte[] data = new byte[(int) file.length()];
            int read = in.read(data);
            return new String(data, 0, read, StandardCharsets.UTF_8);
        }
    }
}
