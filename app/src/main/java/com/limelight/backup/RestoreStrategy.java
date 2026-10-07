package com.limelight.backup;

import android.content.Context;

import org.json.JSONException;

import java.io.IOException;

// How a validated backup is applied. The choice depends only on whether the backup carries pairing keys.
public interface RestoreStrategy {
    void apply(Context context, BackupFile backup) throws IOException, JSONException, InvalidBackupException;

    static RestoreStrategy forBackup(BackupFile backup) {
        return backup.hasPairingKeys() ? new ReplaceAllStrategy() : new MergeHostsStrategy();
    }
}
