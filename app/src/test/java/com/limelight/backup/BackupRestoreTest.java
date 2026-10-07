package com.limelight.backup;

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

import com.limelight.AppView;
import com.limelight.binding.audio.MicrophoneStateStore;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, shadows = {ShadowMoonBridge.class})
public class BackupRestoreTest {
    // Throwaway self-signed EC certificate used only to exercise X.509 parsing
    private static final String TEST_CERT_BASE64 =
            "MIIBUzCB+aADAgECAggKZIWhFrRkHzAKBggqhkjOPQQDAzAdMRswGQYDVQQDExJTdXBlciBBcnRlbWlzIFRlc3QwIBcNMjYxMDA3MTMyODMyWhgPMjEyNjA5MTMxMzI4MzJaMB0xGzAZBgNVBAMTElN1cGVyIEFydGVtaXMgVGVzdDBZMBMGByqGSM49AgEGCCqGSM49AwEHA0IABH+k0XIVeuiL6HWrM5BV+Gacy6lWwXQP4T9wxgDrfJ3yjNiWHqpOVRuvy2StnvNAFd4yLqeBXcqhv6cZdF6RuBKjITAfMB0GA1UdDgQWBBRDW2mPLgZ3b++IKY1gkcmVcRyWuDAKBggqhkjOPQQDAwNJADBGAiEAxHzAWdrE6azQvlGPv0clEVYMYcNMOwamLRNMRHXrdVQCIQDbsmEI/pC21IBj1BCf5JlGEC8kXea6O0Zl06CNj2c/kg==";

    private Context context;
    private SharedPreferences defaultPrefs;

    @Before
    public void setUp() throws IOException {
        context = ApplicationProvider.getApplicationContext();
        defaultPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        defaultPrefs.edit().clear().commit();
        hiddenApps().edit().clear().commit();
        micState().edit().clear().commit();
        deleteAllHosts();
        writeIdentity("0123456789abcdef", "private-key-bytes");
    }

    @Test
    public void backupWithKeysReplacesSettingsHostsProfilesAndIdentity() throws Exception {
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
        saveHost("host-uuid-1", "Gaming PC", testCertificate());

        BackupFile backup = roundTrip(true);
        assertTrue(backup.hasPairingKeys());
        JSONObject prefs = backup.prefs();
        assertFalse(prefs.has("DecoderTombstone"));
        assertFalse(prefs.has("android.app.ActivityThread.IDS"));
        assertFalse(prefs.has("preferenceSearch"));

        // Change the local state, then restore from the backup
        defaultPrefs.edit().clear().putString("leftover", "x").commit();
        context.getSharedPreferences("specialPrefs", Context.MODE_PRIVATE).edit().clear().commit();
        new File(context.getFilesDir(), "profiles/profiles.json").delete();
        writeIdentity("ffffffffffffffff", "other-key");
        deleteAllHosts();
        saveHost("stale-host", "Old PC", null);

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

        Map<String, ComputerDetails> hosts = hostsByUuid();
        assertEquals(Collections.singleton("host-uuid-1"), hosts.keySet());
        ComputerDetails host = hosts.get("host-uuid-1");
        assertEquals("Gaming PC", host.name);
        assertEquals("100.64.0.10", host.manualAddress.address);
        assertEquals(47989, host.manualAddress.port);
        assertNotNull(host.serverCert);
        assertArrayEquals(testCertificate().getEncoded(), host.serverCert.getEncoded());
    }

    @Test
    public void backupWithoutKeysOmitsIdentityButRecordsItsFingerprint() throws Exception {
        JSONObject json = BackupCodec.create(context, false);
        assertNull(json.optJSONObject("identity"));
        assertEquals(IdentitySnapshot.fingerprint(context), json.getString("identityFingerprint"));
        assertFalse(BackupCodec.validate(json).hasPairingKeys());
    }

    @Test
    public void restoreWithoutKeysKeepsLocalPcsAndIdentityAndAddsNewPcs() throws Exception {
        defaultPrefs.edit().putString("list_fps", "120").commit();
        saveHost("shared-pc", "Gaming PC", testCertificate());
        saveHost("backup-only-pc", "Office PC", testCertificate());
        BackupFile backup = roundTrip(false);

        // Locally the shared PC was renamed, the office PC removed and a new PC paired
        defaultPrefs.edit().putString("list_fps", "60").commit();
        deleteAllHosts();
        saveHost("shared-pc", "Renamed locally", testCertificate());
        saveHost("local-only-pc", "Laptop", null);
        writeIdentity("local-unique-id", "local-key");

        BackupRestoreManager.applyBackup(context, backup);

        assertEquals("120", defaultPrefs.getString("list_fps", null));
        Map<String, ComputerDetails> hosts = hostsByUuid();
        assertEquals(new HashSet<>(Arrays.asList("shared-pc", "backup-only-pc", "local-only-pc")), hosts.keySet());
        assertEquals("Renamed locally", hosts.get("shared-pc").name);
        assertEquals("Office PC", hosts.get("backup-only-pc").name);
        assertArrayEquals(testCertificate().getEncoded(), hosts.get("shared-pc").serverCert.getEncoded());
        assertEquals("local-unique-id", readFile(new File(context.getFilesDir(), "uniqueid")));
        assertEquals("local-key", readFile(new File(context.getFilesDir(), "client.key")));
    }

    @Test
    public void restoreWithoutKeysKeepsLocalPerPcDataAndAddsMissingEntries() throws Exception {
        hiddenApps().edit()
                .putStringSet("shared-pc", new HashSet<>(Arrays.asList("1", "2")))
                .putStringSet("backup-only-pc", Collections.singleton("3"))
                .commit();
        micState().edit().putBoolean("host_shared-pc", true).commit();
        BackupFile backup = roundTrip(false);

        hiddenApps().edit().clear()
                .putStringSet("shared-pc", Collections.singleton("9"))
                .putStringSet("local-only-pc", Collections.singleton("7"))
                .commit();
        micState().edit().clear().putBoolean("host_shared-pc", false).commit();

        BackupRestoreManager.applyBackup(context, backup);

        assertEquals(Collections.singleton("9"), hiddenApps().getStringSet("shared-pc", null));
        assertEquals(Collections.singleton("3"), hiddenApps().getStringSet("backup-only-pc", null));
        assertEquals(Collections.singleton("7"), hiddenApps().getStringSet("local-only-pc", null));
        assertFalse(micState().getBoolean("host_shared-pc", true));
    }

    @Test
    public void restoreWithKeysMirrorsPerPcData() throws Exception {
        hiddenApps().edit().putStringSet("shared-pc", Collections.singleton("1")).commit();
        BackupFile backup = roundTrip(true);

        hiddenApps().edit().clear().putStringSet("local-only-pc", Collections.singleton("7")).commit();
        BackupRestoreManager.applyBackup(context, backup);

        assertEquals(Collections.singleton("1"), hiddenApps().getStringSet("shared-pc", null));
        assertFalse(hiddenApps().contains("local-only-pc"));
    }

    @Test
    public void previewWithoutKeysCountsKeptAndNewPcs() throws Exception {
        defaultPrefs.edit().putString("list_fps", "120").putBoolean("checkbox_enable_pip", true).commit();
        saveHost("shared-pc", "Gaming PC", testCertificate());
        saveHost("backup-only-pc", "Office PC", testCertificate());
        BackupFile backup = roundTrip(false);
        deleteAllHosts();
        saveHost("shared-pc", "Gaming PC", testCertificate());
        saveHost("local-only-pc", "Laptop", null);

        RestorePreview preview = RestorePreview.of(context, backup);

        assertNotNull(preview.createdAt);
        assertFalse(preview.appVersion.isEmpty());
        assertTrue(preview.settingsCount >= 2);
        assertEquals(2, preview.backupPcCount);
        assertEquals(2, preview.localPcCount);
        assertEquals(1, preview.newPcCount);
        assertFalse(preview.replacesPairingKeys);
        assertFalse(preview.newPcsNeedPairing);
        assertEquals("Your 2 PCs are kept as they are.\n1 PC from the backup is added.",
                BackupRestoreController.describeOutcome(context.getResources(), preview));
    }

    @Test
    public void previewFromAnotherIdentityWarnsThatNewPcsNeedPairing() throws Exception {
        saveHost("backup-only-pc", "Office PC", testCertificate());
        BackupFile backup = roundTrip(false);
        deleteAllHosts();
        writeBytes(new File(context.getFilesDir(), "client.crt"), "another-device-cert".getBytes(StandardCharsets.UTF_8));

        RestorePreview preview = RestorePreview.of(context, backup);

        assertTrue(preview.newPcsNeedPairing);
        assertEquals("1 PC from the backup is added. Pair it again to stream, because this backup has no pairing keys.",
                BackupRestoreController.describeOutcome(context.getResources(), preview));
    }

    @Test
    public void previewOfOlderBackupWithoutFingerprintAssumesPairingIsNeeded() throws Exception {
        saveHost("backup-only-pc", "Office PC", testCertificate());
        JSONObject json = BackupCodec.create(context, false);
        json.remove("identityFingerprint");
        BackupFile backup = BackupCodec.validate(json);
        deleteAllHosts();

        assertTrue(RestorePreview.of(context, backup).newPcsNeedPairing);
    }

    @Test
    public void previewWithKeysWarnsThatPairingKeysAreReplaced() throws Exception {
        saveHost("backup-only-pc", "Office PC", testCertificate());
        BackupFile backup = roundTrip(true);

        RestorePreview preview = RestorePreview.of(context, backup);

        assertTrue(preview.replacesPairingKeys);
        assertFalse(preview.newPcsNeedPairing);
        assertTrue(BackupRestoreController.describeOutcome(context.getResources(), preview).contains("pairing keys are replaced"));
    }

    @Test
    public void previewWithNoPcsSaysNothingChanges() throws Exception {
        BackupFile backup = roundTrip(false);
        assertEquals("No PCs are added or removed.",
                BackupRestoreController.describeOutcome(context.getResources(), RestorePreview.of(context, backup)));
    }

    @Test
    public void validateRejectsForeignFormat() throws Exception {
        JSONObject json = BackupCodec.create(context, false);
        json.put("format", "something-else");
        assertInvalid(json);
    }

    @Test
    public void validateRejectsNewerSchema() throws Exception {
        JSONObject json = BackupCodec.create(context, false);
        json.put("schemaVersion", BackupCodec.SCHEMA_VERSION + 1);
        assertInvalid(json);
    }

    @Test
    public void validateRejectsFilesOutsideTheAllowlist() throws Exception {
        JSONObject json = BackupCodec.create(context, false);
        json.getJSONObject("files").put("../shared_prefs/evil.xml", Base64.encodeToString(new byte[] {1}, Base64.NO_WRAP));
        assertInvalid(json);
    }

    @Test
    public void validateRejectsUnknownPreferenceType() throws Exception {
        JSONObject json = BackupCodec.create(context, false);
        JSONObject typed = new JSONObject().put("type", "double").put("value", 1.0);
        json.getJSONObject("sharedPreferences").put(PrefsSnapshot.DEFAULT_PREFS_KEY, new JSONObject().put("k", typed));
        assertInvalid(json);
    }

    @Test
    public void parseRejectsNonJson() {
        try {
            BackupCodec.parse("not json".getBytes(StandardCharsets.UTF_8));
            fail("Expected InvalidBackupException");
        }
        catch (InvalidBackupException expected) {
            // expected
        }
    }

    @Test
    public void restoreSkipsExcludedFilesFromOlderBackups() throws Exception {
        JSONObject json = BackupCodec.create(context, false);
        JSONObject typed = new JSONObject().put("type", "string").put("value", "stale");
        json.getJSONObject("sharedPreferences").put("android.app.ActivityThread.IDS", new JSONObject().put("id", typed));
        BackupRestoreManager.applyBackup(context, BackupCodec.validate(json));
        assertFalse(context.getSharedPreferences("android.app.ActivityThread.IDS", Context.MODE_PRIVATE).contains("id"));
    }

    // Serializes and parses again, the same path a backup file takes
    private BackupFile roundTrip(boolean includePairingKeys) throws Exception {
        String json = BackupCodec.create(context, includePairingKeys).toString(2);
        return BackupCodec.parse(json.getBytes(StandardCharsets.UTF_8));
    }

    private static void assertInvalid(JSONObject json) {
        try {
            BackupCodec.validate(json);
            fail("Expected InvalidBackupException");
        }
        catch (InvalidBackupException expected) {
            // expected
        }
    }

    private SharedPreferences hiddenApps() {
        return context.getSharedPreferences(AppView.HIDDEN_APPS_PREF_FILENAME, Context.MODE_PRIVATE);
    }

    private SharedPreferences micState() {
        return context.getSharedPreferences(MicrophoneStateStore.PREF_NAME, Context.MODE_PRIVATE);
    }

    private void writeIdentity(String uniqueId, String key) throws IOException {
        writeFile(new File(context.getFilesDir(), "uniqueid"), uniqueId);
        writeBytes(new File(context.getFilesDir(), "client.crt"), Base64.decode(TEST_CERT_BASE64, Base64.NO_WRAP));
        writeFile(new File(context.getFilesDir(), "client.key"), key);
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

    private Map<String, ComputerDetails> hostsByUuid() {
        Map<String, ComputerDetails> hosts = new HashMap<>();
        ComputerDatabaseManager db = new ComputerDatabaseManager(context);
        try {
            for (ComputerDetails details : db.getAllComputers()) {
                hosts.put(details.uuid, details);
            }
        }
        finally {
            db.close();
        }
        return hosts;
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
