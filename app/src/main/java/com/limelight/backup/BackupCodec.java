package com.limelight.backup;

import android.content.Context;

import com.limelight.BuildConfig;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;

// NOTE: Builds the backup JSON from the snapshots and parses it back into a validated BackupFile.
// NOTE: Validation walks every section and value, so a strategy never fails halfway through on bad input.
public final class BackupCodec {
    public static final String FORMAT = "super-artemis-backup";
    public static final int SCHEMA_VERSION = 1;

    private BackupCodec() {
    }

    public static JSONObject create(Context context, boolean includePairingKeys) throws JSONException, IOException {
        JSONObject root = new JSONObject();
        root.put("format", FORMAT);
        root.put("schemaVersion", SCHEMA_VERSION);
        root.put("createdAt", BackupIo.isoTimestamp(new Date()));
        root.put("appVersion", BuildConfig.VERSION_NAME);
        root.put("sourcePackage", context.getPackageName());
        String fingerprint = IdentitySnapshot.fingerprint(context);
        if (fingerprint != null) {
            root.put("identityFingerprint", fingerprint);
        }
        root.put("sharedPreferences", PrefsSnapshot.capture(context));
        root.put("files", FileSnapshot.capture(context));
        root.put("hosts", HostSnapshot.capture(context));
        if (includePairingKeys) {
            root.put("identity", IdentitySnapshot.capture(context));
        }
        return root;
    }

    public static BackupFile parse(byte[] data) throws InvalidBackupException {
        JSONObject root;
        try {
            root = new JSONObject(new String(data, StandardCharsets.UTF_8));
        }
        catch (JSONException e) {
            throw new InvalidBackupException("Not a JSON file");
        }
        return validate(root);
    }

    public static BackupFile validate(JSONObject root) throws InvalidBackupException {
        try {
            if (!FORMAT.equals(root.optString("format"))) {
                throw new InvalidBackupException("Not a Super Artemis backup");
            }
            int schema = root.optInt("schemaVersion", -1);
            if (schema < 1 || schema > SCHEMA_VERSION) {
                throw new InvalidBackupException("Unsupported backup version " + schema);
            }
            PrefsSnapshot.validate(root.getJSONObject("sharedPreferences"));
            JSONObject files = root.optJSONObject("files");
            if (files != null) {
                FileSnapshot.validate(files);
            }
            HostSnapshot.validate(root.getJSONArray("hosts"));
            JSONObject identity = root.optJSONObject("identity");
            if (identity != null) {
                IdentitySnapshot.validate(identity);
            }
        }
        catch (JSONException | IllegalArgumentException e) {
            throw new InvalidBackupException("Malformed backup: " + e.getMessage());
        }
        return new BackupFile(root);
    }
}
