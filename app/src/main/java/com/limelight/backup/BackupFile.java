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

    // Empty for backups made before the device name was recorded
    public String deviceModel() {
        return root.optString("deviceModel", "");
    }

    // Settings files besides the main one and the per-PC files: special keys, on-screen controls and keyboard layouts
    public int layoutFileCount() {
        JSONObject prefs = prefs();
        int count = 0;
        if (prefs != null) {
            java.util.Iterator<String> names = prefs.keys();
            while (names.hasNext()) {
                String name = names.next();
                if (!PrefsSnapshot.DEFAULT_PREFS_KEY.equals(name) && !MergeHostsStrategy.PER_HOST_FILES.contains(name)
                        && prefs.optJSONObject(name) != null && prefs.optJSONObject(name).length() > 0) {
                    count++;
                }
            }
        }
        return count;
    }

    public boolean hasProfiles() {
        return files().length() > 0;
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
