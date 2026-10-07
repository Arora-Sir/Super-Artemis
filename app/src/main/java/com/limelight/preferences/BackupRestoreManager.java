package com.limelight.preferences;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Base64;

import com.limelight.BuildConfig;
import com.limelight.computers.ComputerDatabaseManager;
import com.limelight.nvstream.http.ComputerDetails;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

// NOTE: Single-file JSON backup of settings, paired hosts and the optional client pairing identity.
// NOTE: Restores validate the whole file before writing anything, then the app restarts so every cache reloads from disk.
public final class BackupRestoreManager {
    public static final String FORMAT = "super-artemis-backup";
    public static final int SCHEMA_VERSION = 1;
    public static final String MIME_TYPE = "application/json";

    // Logical name for the default SharedPreferences file so backups move between debug and release builds
    static final String DEFAULT_PREFS_KEY = "_default";

    // Device-specific state that must not travel to another install
    private static final Set<String> EXCLUDED_PREFS = new HashSet<>(Arrays.asList("DecoderTombstone", "GlPreferences"));

    // Only these files may be written by a restore, which blocks path traversal from a crafted backup
    private static final List<String> RESTORABLE_FILES = Arrays.asList("profiles/profiles.json");

    private static final String IDENTITY_UNIQUE_ID_FILE = "uniqueid";
    private static final String IDENTITY_CERT_FILE = "client.crt";
    private static final String IDENTITY_KEY_FILE = "client.key";

    private static final long MAX_BACKUP_BYTES = 8L * 1024 * 1024;

    private BackupRestoreManager() {
    }

    public static class InvalidBackupException extends Exception {
        public InvalidBackupException(String message) {
            super(message);
        }
    }

    // Same file name androidx PreferenceManager uses for getDefaultSharedPreferences()
    static String defaultPreferencesName(Context context) {
        return context.getPackageName() + "_preferences";
    }

    public static String defaultFileName() {
        return "super-artemis-backup-" + new SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(new Date()) + ".json";
    }

    // Backup

    public static JSONObject createBackup(Context context, boolean includeIdentity) throws JSONException, IOException {
        JSONObject root = new JSONObject();
        root.put("format", FORMAT);
        root.put("schemaVersion", SCHEMA_VERSION);
        root.put("createdAt", isoTimestamp());
        root.put("appVersion", BuildConfig.VERSION_NAME);
        root.put("sourcePackage", context.getPackageName());

        JSONObject prefsJson = new JSONObject();
        String defaultName = defaultPreferencesName(context);
        for (String name : listPreferenceFiles(context)) {
            SharedPreferences prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE);
            prefsJson.put(name.equals(defaultName) ? DEFAULT_PREFS_KEY : name, encodePreferences(prefs.getAll()));
        }
        root.put("sharedPreferences", prefsJson);

        JSONObject filesJson = new JSONObject();
        for (String relativePath : RESTORABLE_FILES) {
            File file = new File(context.getFilesDir(), relativePath);
            if (file.isFile()) {
                filesJson.put(relativePath, Base64.encodeToString(readFile(file), Base64.NO_WRAP));
            }
        }
        root.put("files", filesJson);

        JSONArray hostsJson = new JSONArray();
        ComputerDatabaseManager db = new ComputerDatabaseManager(context);
        try {
            for (ComputerDetails details : db.getAllComputers()) {
                hostsJson.put(encodeHost(details));
            }
        }
        finally {
            db.close();
        }
        root.put("hosts", hostsJson);

        if (includeIdentity) {
            JSONObject identity = new JSONObject();
            File filesDir = context.getFilesDir();
            putFileIfPresent(identity, "uniqueId", new File(filesDir, IDENTITY_UNIQUE_ID_FILE));
            putFileIfPresent(identity, "clientCert", new File(filesDir, IDENTITY_CERT_FILE));
            putFileIfPresent(identity, "clientKey", new File(filesDir, IDENTITY_KEY_FILE));
            root.put("identity", identity);
        }

        return root;
    }

    public static void writeBackup(Context context, Uri uri, boolean includeIdentity) throws JSONException, IOException {
        byte[] data = createBackup(context, includeIdentity).toString(2).getBytes(StandardCharsets.UTF_8);
        try (OutputStream out = context.getContentResolver().openOutputStream(uri, "wt")) {
            if (out == null) {
                throw new IOException("Unable to open backup destination");
            }
            out.write(data);
        }
    }

    // Restore

    public static JSONObject readBackup(Context context, Uri uri) throws IOException, InvalidBackupException {
        byte[] data;
        try (InputStream in = context.getContentResolver().openInputStream(uri)) {
            if (in == null) {
                throw new IOException("Unable to open backup file");
            }
            data = readLimited(in);
        }

        JSONObject root;
        try {
            root = new JSONObject(new String(data, StandardCharsets.UTF_8));
        }
        catch (JSONException e) {
            throw new InvalidBackupException("Not a JSON file");
        }
        validate(root);
        return root;
    }

    // Walks every section and value so that applyBackup never fails halfway on bad input
    public static void validate(JSONObject root) throws InvalidBackupException {
        try {
            if (!FORMAT.equals(root.optString("format"))) {
                throw new InvalidBackupException("Not a Super Artemis backup");
            }
            int schema = root.optInt("schemaVersion", -1);
            if (schema < 1 || schema > SCHEMA_VERSION) {
                throw new InvalidBackupException("Unsupported backup version " + schema);
            }

            JSONObject prefsJson = root.getJSONObject("sharedPreferences");
            Iterator<String> files = prefsJson.keys();
            while (files.hasNext()) {
                String name = files.next();
                if (!isValidPreferenceFileName(name)) {
                    throw new InvalidBackupException("Invalid preference file name " + name);
                }
                JSONObject entries = prefsJson.getJSONObject(name);
                Iterator<String> keys = entries.keys();
                while (keys.hasNext()) {
                    decodeValue(entries.getJSONObject(keys.next()));
                }
            }

            JSONObject filesJson = root.optJSONObject("files");
            if (filesJson != null) {
                Iterator<String> paths = filesJson.keys();
                while (paths.hasNext()) {
                    String path = paths.next();
                    if (!RESTORABLE_FILES.contains(path)) {
                        throw new InvalidBackupException("Unexpected file " + path);
                    }
                    decodeBase64(filesJson.getString(path));
                }
            }

            JSONArray hostsJson = root.getJSONArray("hosts");
            for (int i = 0; i < hostsJson.length(); i++) {
                decodeHost(hostsJson.getJSONObject(i));
            }

            JSONObject identity = root.optJSONObject("identity");
            if (identity != null) {
                if (!identity.has("uniqueId") || !identity.has("clientCert") || !identity.has("clientKey")) {
                    throw new InvalidBackupException("Incomplete pairing identity");
                }
                decodeBase64(identity.getString("uniqueId"));
                decodeCertificate(decodeBase64(identity.getString("clientCert")));
                decodeBase64(identity.getString("clientKey"));
            }
        }
        catch (JSONException | IllegalArgumentException e) {
            throw new InvalidBackupException("Malformed backup: " + e.getMessage());
        }
    }

    public static boolean hasIdentity(JSONObject root) {
        return root.optJSONObject("identity") != null;
    }

    // Replaces settings, hosts and (if present) the pairing identity. Call validate() first.
    public static void applyBackup(Context context, JSONObject root) throws IOException, JSONException, InvalidBackupException {
        String defaultName = defaultPreferencesName(context);
        JSONObject prefsJson = root.getJSONObject("sharedPreferences");

        Set<String> restoredFiles = new HashSet<>();
        Iterator<String> files = prefsJson.keys();
        while (files.hasNext()) {
            String key = files.next();
            String name = DEFAULT_PREFS_KEY.equals(key) ? defaultName : key;
            restoredFiles.add(name);
            writePreferences(context.getSharedPreferences(name, Context.MODE_PRIVATE), prefsJson.getJSONObject(key));
        }

        // Settings files that exist locally but not in the backup are cleared so the result mirrors the backup
        for (String name : listPreferenceFiles(context)) {
            if (!restoredFiles.contains(name)) {
                if (!context.getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear().commit()) {
                    throw new IOException("Failed to clear preferences " + name);
                }
            }
        }

        JSONObject filesJson = root.optJSONObject("files");
        if (filesJson != null) {
            Iterator<String> paths = filesJson.keys();
            while (paths.hasNext()) {
                String path = paths.next();
                writeFileAtomically(new File(context.getFilesDir(), path), decodeBase64(filesJson.getString(path)));
            }
        }

        JSONArray hostsJson = root.getJSONArray("hosts");
        ComputerDatabaseManager db = new ComputerDatabaseManager(context);
        try {
            for (ComputerDetails existing : db.getAllComputers()) {
                db.deleteComputer(existing);
            }
            for (int i = 0; i < hostsJson.length(); i++) {
                if (!db.updateComputer(decodeHost(hostsJson.getJSONObject(i)))) {
                    throw new IOException("Failed to restore host " + i);
                }
            }
        }
        finally {
            db.close();
        }

        JSONObject identity = root.optJSONObject("identity");
        if (identity != null) {
            File filesDir = context.getFilesDir();
            writeFileAtomically(new File(filesDir, IDENTITY_UNIQUE_ID_FILE), decodeBase64(identity.getString("uniqueId")));
            writeFileAtomically(new File(filesDir, IDENTITY_CERT_FILE), decodeBase64(identity.getString("clientCert")));
            writeFileAtomically(new File(filesDir, IDENTITY_KEY_FILE), decodeBase64(identity.getString("clientKey")));
        }
    }

    // Host list, identity and profiles are cached by long-lived objects, so a fresh process is the reliable reload
    public static void restartApp(Activity activity) {
        Intent intent = activity.getPackageManager().getLaunchIntentForPackage(activity.getPackageName());
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            activity.startActivity(intent);
        }
        activity.finishAffinity();
        Runtime.getRuntime().exit(0);
    }

    // Preferences encoding

    static JSONObject encodePreferences(Map<String, ?> all) throws JSONException {
        JSONObject json = new JSONObject();
        for (Map.Entry<String, ?> entry : all.entrySet()) {
            Object value = entry.getValue();
            JSONObject typed = new JSONObject();
            if (value instanceof Boolean) {
                typed.put("type", "boolean");
                typed.put("value", value);
            }
            else if (value instanceof Integer) {
                typed.put("type", "int");
                typed.put("value", value);
            }
            else if (value instanceof Long) {
                typed.put("type", "long");
                typed.put("value", value);
            }
            else if (value instanceof Float) {
                typed.put("type", "float");
                typed.put("value", ((Float) value).doubleValue());
            }
            else if (value instanceof String) {
                typed.put("type", "string");
                typed.put("value", value);
            }
            else if (value instanceof Set) {
                JSONArray array = new JSONArray();
                for (Object item : (Set<?>) value) {
                    array.put(String.valueOf(item));
                }
                typed.put("type", "stringSet");
                typed.put("value", array);
            }
            else {
                continue;
            }
            json.put(entry.getKey(), typed);
        }
        return json;
    }

    private static void writePreferences(SharedPreferences prefs, JSONObject entries) throws JSONException, IOException, InvalidBackupException {
        SharedPreferences.Editor editor = prefs.edit().clear();
        Iterator<String> keys = entries.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            Object value = decodeValue(entries.getJSONObject(key));
            if (value instanceof Boolean) {
                editor.putBoolean(key, (Boolean) value);
            }
            else if (value instanceof Integer) {
                editor.putInt(key, (Integer) value);
            }
            else if (value instanceof Long) {
                editor.putLong(key, (Long) value);
            }
            else if (value instanceof Float) {
                editor.putFloat(key, (Float) value);
            }
            else if (value instanceof String) {
                editor.putString(key, (String) value);
            }
            else {
                @SuppressWarnings("unchecked")
                Set<String> set = (Set<String>) value;
                editor.putStringSet(key, set);
            }
        }
        if (!editor.commit()) {
            throw new IOException("Failed to write preferences");
        }
    }

    static Object decodeValue(JSONObject typed) throws JSONException, InvalidBackupException {
        String type = typed.getString("type");
        switch (type) {
            case "boolean":
                return typed.getBoolean("value");
            case "int":
                return typed.getInt("value");
            case "long":
                return typed.getLong("value");
            case "float":
                return (float) typed.getDouble("value");
            case "string":
                return typed.getString("value");
            case "stringSet":
                JSONArray array = typed.getJSONArray("value");
                Set<String> set = new HashSet<>();
                for (int i = 0; i < array.length(); i++) {
                    set.add(array.getString(i));
                }
                return set;
            default:
                throw new InvalidBackupException("Unknown preference type " + type);
        }
    }

    // Every non-excluded SharedPreferences file of this app, including dynamically named keyboard layouts
    static Set<String> listPreferenceFiles(Context context) {
        Set<String> names = new HashSet<>();
        File dir = new File(context.getApplicationInfo().dataDir, "shared_prefs");
        String[] fileNames = dir.list();
        if (fileNames != null) {
            for (String fileName : fileNames) {
                if (fileName.endsWith(".xml")) {
                    String name = fileName.substring(0, fileName.length() - 4);
                    if (!EXCLUDED_PREFS.contains(name)) {
                        names.add(name);
                    }
                }
            }
        }
        return names;
    }

    private static boolean isValidPreferenceFileName(String name) {
        return !name.isEmpty() && !name.contains("/") && !name.contains("\\") && !name.contains("..")
                && !EXCLUDED_PREFS.contains(name);
    }

    // Hosts

    private static JSONObject encodeHost(ComputerDetails details) throws JSONException, IOException {
        JSONObject json = new JSONObject();
        json.put("uuid", details.uuid);
        json.put("name", details.name);

        JSONObject addresses = new JSONObject();
        putTuple(addresses, "local", details.localAddress);
        putTuple(addresses, "remote", details.remoteAddress);
        putTuple(addresses, "manual", details.manualAddress);
        putTuple(addresses, "ipv6", details.ipv6Address);
        json.put("addresses", addresses);

        if (details.macAddress != null) {
            json.put("macAddress", details.macAddress);
        }
        if (details.serverCert != null) {
            try {
                json.put("serverCert", Base64.encodeToString(details.serverCert.getEncoded(), Base64.NO_WRAP));
            }
            catch (java.security.cert.CertificateEncodingException e) {
                throw new IOException("Unable to encode server certificate for " + details.name);
            }
        }
        return json;
    }

    private static ComputerDetails decodeHost(JSONObject json) throws JSONException, InvalidBackupException {
        ComputerDetails details = new ComputerDetails();
        details.uuid = json.getString("uuid");
        details.name = json.getString("name");
        if (details.uuid.isEmpty()) {
            throw new InvalidBackupException("Host without UUID");
        }

        JSONObject addresses = json.getJSONObject("addresses");
        details.localAddress = ComputerDatabaseManager.tupleFromJson(addresses, "local");
        details.remoteAddress = ComputerDatabaseManager.tupleFromJson(addresses, "remote");
        details.manualAddress = ComputerDatabaseManager.tupleFromJson(addresses, "manual");
        details.ipv6Address = ComputerDatabaseManager.tupleFromJson(addresses, "ipv6");
        details.macAddress = json.has("macAddress") ? json.getString("macAddress") : null;

        if (json.has("serverCert")) {
            details.serverCert = decodeCertificate(decodeBase64(json.getString("serverCert")));
        }
        return details;
    }

    private static void putTuple(JSONObject addresses, String name, ComputerDetails.AddressTuple tuple) throws JSONException {
        JSONObject json = ComputerDatabaseManager.tupleToJson(tuple);
        if (json != null) {
            addresses.put(name, json);
        }
    }

    private static X509Certificate decodeCertificate(byte[] data) throws InvalidBackupException {
        try {
            return (X509Certificate) CertificateFactory.getInstance("X.509")
                    .generateCertificate(new ByteArrayInputStream(data));
        }
        catch (CertificateException | ClassCastException e) {
            throw new InvalidBackupException("Invalid certificate");
        }
    }

    // Files

    private static void putFileIfPresent(JSONObject json, String key, File file) throws IOException, JSONException {
        if (file.isFile()) {
            json.put(key, Base64.encodeToString(readFile(file), Base64.NO_WRAP));
        }
    }

    private static byte[] readFile(File file) throws IOException {
        try (InputStream in = new java.io.FileInputStream(file)) {
            return readLimited(in);
        }
    }

    private static byte[] readLimited(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[16 * 1024];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
            if (out.size() > MAX_BACKUP_BYTES) {
                throw new IOException("Backup file is too large");
            }
        }
        return out.toByteArray();
    }

    private static void writeFileAtomically(File target, byte[] data) throws IOException {
        File parent = target.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
            throw new IOException("Unable to create " + parent);
        }
        File temp = new File(target.getPath() + ".restore-tmp");
        try (FileOutputStream out = new FileOutputStream(temp)) {
            out.write(data);
            out.getFD().sync();
        }
        if (!temp.renameTo(target)) {
            temp.delete();
            throw new IOException("Unable to replace " + target.getName());
        }
    }

    private static byte[] decodeBase64(String value) {
        return Base64.decode(value, Base64.NO_WRAP);
    }

    private static String isoTimestamp() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format.format(new Date());
    }
}
