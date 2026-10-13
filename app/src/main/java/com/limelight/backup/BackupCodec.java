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
    // NOTE: Oldest reader that can still apply a file of this schema. A future version that only adds sections keeps this at 1,
    // NOTE: so older apps restore everything they understand and skip the rest. Only a breaking change raises it.
    public static final int MIN_READER_VERSION = 1;

    private BackupCodec() {
    }

    public static JSONObject create(Context context, boolean includePairingKeys) throws JSONException, IOException {
        JSONObject root = new JSONObject();
        root.put("format", FORMAT);
        root.put("schemaVersion", SCHEMA_VERSION);
        root.put("minReaderVersion", MIN_READER_VERSION);
        root.put("createdAt", BackupIo.isoTimestamp(new Date()));
        root.put("appVersion", BuildConfig.VERSION_NAME);
        root.put("sourcePackage", context.getPackageName());
        // Shown in the restore preview ("Made on ..."), so the user can tell backups from different devices apart
        root.put("deviceModel", (android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL).trim());
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
            // Files without minReaderVersion come from schema 1, which every reader understands
            int minReader = root.optInt("minReaderVersion", Math.min(schema, SCHEMA_VERSION));
            if (schema < 1 || minReader > SCHEMA_VERSION) {
                throw new InvalidBackupException("This backup needs a newer Super Artemis (backup version " + schema + ")");
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
