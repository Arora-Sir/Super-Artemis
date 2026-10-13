package com.limelight.backup;

import android.content.Context;

import org.json.JSONException;

import java.io.IOException;

// How a validated backup is applied. Replacing everything happens only when the backup carries pairing keys and the user chose to restore them.
public interface RestoreStrategy {
    void apply(Context context, BackupFile backup, RestoreSelection selection) throws IOException, JSONException, InvalidBackupException;

    static RestoreStrategy forBackup(BackupFile backup, RestoreSelection selection) {
        return backup.hasPairingKeys() && selection.pairingKeys ? new ReplaceAllStrategy() : new MergeHostsStrategy();
    }
}
