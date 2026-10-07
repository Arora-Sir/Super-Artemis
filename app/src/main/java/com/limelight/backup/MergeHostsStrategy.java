package com.limelight.backup;

import android.content.Context;

import com.limelight.AppView;
import com.limelight.binding.audio.MicrophoneStateStore;
import com.limelight.computers.ComputerDatabaseManager;
import com.limelight.nvstream.http.ComputerDetails;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

// NOTE: Used when the backup has no pairing keys, so this device keeps its own identity and every PC it already knows.
// NOTE: Settings are replaced, PCs that only exist in the backup are added, and per-PC data for kept PCs stays local.
final class MergeHostsStrategy implements RestoreStrategy {
    // Files keyed by PC UUID, where local entries must survive for the PCs this device keeps
    static final Set<String> PER_HOST_FILES = new HashSet<>(Arrays.asList(
            AppView.HIDDEN_APPS_PREF_FILENAME, MicrophoneStateStore.PREF_NAME));

    @Override
    public void apply(Context context, BackupFile backup) throws IOException, JSONException, InvalidBackupException {
        PrefsSnapshot.restore(context, backup.prefs(), PER_HOST_FILES);
        FileSnapshot.restore(context, backup.files());

        JSONArray hosts = backup.hosts();
        ComputerDatabaseManager db = new ComputerDatabaseManager(context);
        try {
            Set<String> local = new HashSet<>();
            for (ComputerDetails details : db.getAllComputers()) {
                local.add(details.uuid);
            }
            for (int i = 0; i < hosts.length(); i++) {
                ComputerDetails details = HostSnapshot.decode(hosts.getJSONObject(i));
                if (local.contains(details.uuid)) {
                    continue;
                }
                if (!db.updateComputer(details)) {
                    throw new IOException("Failed to add PC " + details.name);
                }
            }
        }
        finally {
            db.close();
        }
    }
}
