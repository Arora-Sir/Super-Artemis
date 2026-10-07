package com.limelight.backup;

import org.json.JSONArray;
import org.json.JSONObject;

// A parsed and fully validated backup. Only BackupCodec creates instances, so every BackupFile is safe to apply.
public final class BackupFile {
    private final JSONObject root;

    BackupFile(JSONObject root) {
        this.root = root;
    }

    public boolean hasPairingKeys() {
        return root.optJSONObject("identity") != null;
    }

    public String createdAt() {
        return root.optString("createdAt", "");
    }

    public String appVersion() {
        return root.optString("appVersion", "");
    }

    // Empty for backups made before the fingerprint existed
    String identityFingerprint() {
        return root.optString("identityFingerprint", "");
    }

    public int settingsCount() {
        return PrefsSnapshot.countEntries(prefs());
    }

    JSONObject prefs() {
        return root.optJSONObject("sharedPreferences");
    }

    JSONObject files() {
        JSONObject files = root.optJSONObject("files");
        return files != null ? files : new JSONObject();
    }

    JSONArray hosts() {
        return root.optJSONArray("hosts");
    }

    JSONObject identity() {
        return root.optJSONObject("identity");
    }
}
