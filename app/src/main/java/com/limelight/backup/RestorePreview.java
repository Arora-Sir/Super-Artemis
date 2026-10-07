package com.limelight.backup;

import android.content.Context;

import org.json.JSONArray;

import java.util.Date;
import java.util.Set;

// What a restore will do, computed before anything is written so the user can confirm it
public final class RestorePreview {
    public final Date createdAt;
    public final String appVersion;
    public final int settingsCount;
    public final int backupPcCount;
    public final int localPcCount;
    public final int newPcCount;
    public final boolean replacesPairingKeys;
    public final boolean newPcsNeedPairing;

    private RestorePreview(Date createdAt, String appVersion, int settingsCount, int backupPcCount, int localPcCount,
                           int newPcCount, boolean replacesPairingKeys, boolean newPcsNeedPairing) {
        this.createdAt = createdAt;
        this.appVersion = appVersion;
        this.settingsCount = settingsCount;
        this.backupPcCount = backupPcCount;
        this.localPcCount = localPcCount;
        this.newPcCount = newPcCount;
        this.replacesPairingKeys = replacesPairingKeys;
        this.newPcsNeedPairing = newPcsNeedPairing;
    }

    public static RestorePreview of(Context context, BackupFile backup) {
        JSONArray hosts = backup.hosts();
        Set<String> local = HostSnapshot.localUuids(context);
        int newPcs = 0;
        for (int i = 0; i < hosts.length(); i++) {
            if (!local.contains(hosts.optJSONObject(i).optString("uuid"))) {
                newPcs++;
            }
        }

        // Added PCs only trust the identity that paired with them, so they work as is only when the backup came from this identity
        String backupFingerprint = backup.identityFingerprint();
        boolean sameIdentity = backup.hasPairingKeys()
                || (!backupFingerprint.isEmpty() && backupFingerprint.equals(IdentitySnapshot.fingerprint(context)));

        return new RestorePreview(BackupIo.parseIsoTimestamp(backup.createdAt()), backup.appVersion(), backup.settingsCount(),
                hosts.length(), local.size(), newPcs, backup.hasPairingKeys(), !sameIdentity);
    }
}
