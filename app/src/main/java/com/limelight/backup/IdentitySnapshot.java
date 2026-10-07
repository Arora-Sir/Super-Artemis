package com.limelight.backup;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

// The client pairing identity (unique ID, certificate and private key) that paired hosts trust
final class IdentitySnapshot {
    private static final String UNIQUE_ID_FILE = "uniqueid";
    private static final String CERT_FILE = "client.crt";
    private static final String KEY_FILE = "client.key";

    private IdentitySnapshot() {
    }

    static JSONObject capture(Context context) throws JSONException, IOException {
        JSONObject json = new JSONObject();
        File filesDir = context.getFilesDir();
        putIfPresent(json, "uniqueId", new File(filesDir, UNIQUE_ID_FILE));
        putIfPresent(json, "clientCert", new File(filesDir, CERT_FILE));
        putIfPresent(json, "clientKey", new File(filesDir, KEY_FILE));
        return json;
    }

    static void validate(JSONObject identity) throws JSONException, InvalidBackupException {
        if (!identity.has("uniqueId") || !identity.has("clientCert") || !identity.has("clientKey")) {
            throw new InvalidBackupException("Incomplete pairing identity");
        }
        BackupIo.decodeBase64(identity.getString("uniqueId"));
        HostSnapshot.decodeCertificate(BackupIo.decodeBase64(identity.getString("clientCert")));
        BackupIo.decodeBase64(identity.getString("clientKey"));
    }

    // SHA-256 of the public client certificate. It tells whether a backup came from this identity without storing any secret.
    static String fingerprint(Context context) {
        File cert = new File(context.getFilesDir(), CERT_FILE);
        if (!cert.isFile()) {
            return null;
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(BackupIo.readFile(cert));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                hex.append(String.format(Locale.US, "%02x", b));
            }
            return hex.toString();
        }
        catch (IOException | NoSuchAlgorithmException e) {
            return null;
        }
    }

    static void restore(Context context, JSONObject identity) throws JSONException, IOException {
        File filesDir = context.getFilesDir();
        BackupIo.writeFileAtomically(new File(filesDir, UNIQUE_ID_FILE), BackupIo.decodeBase64(identity.getString("uniqueId")));
        BackupIo.writeFileAtomically(new File(filesDir, CERT_FILE), BackupIo.decodeBase64(identity.getString("clientCert")));
        BackupIo.writeFileAtomically(new File(filesDir, KEY_FILE), BackupIo.decodeBase64(identity.getString("clientKey")));
    }

    private static void putIfPresent(JSONObject json, String key, File file) throws IOException, JSONException {
        if (file.isFile()) {
            json.put(key, BackupIo.encodeBase64(BackupIo.readFile(file)));
        }
    }
}
