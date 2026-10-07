package com.limelight.backup;

import android.content.Context;

import com.limelight.computers.ComputerDatabaseManager;
import com.limelight.nvstream.http.ComputerDetails;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.util.Collections;

// Used when the backup carries pairing keys: the device becomes the backed-up client, so its PC list must match the backup exactly
final class ReplaceAllStrategy implements RestoreStrategy {
    @Override
    public void apply(Context context, BackupFile backup) throws IOException, JSONException, InvalidBackupException {
        PrefsSnapshot.restore(context, backup.prefs(), Collections.emptySet());
        FileSnapshot.restore(context, backup.files());

        JSONArray hosts = backup.hosts();
        ComputerDatabaseManager db = new ComputerDatabaseManager(context);
        try {
            for (ComputerDetails existing : db.getAllComputers()) {
                db.deleteComputer(existing);
            }
            for (int i = 0; i < hosts.length(); i++) {
                ComputerDetails details = HostSnapshot.decode(hosts.getJSONObject(i));
                if (!db.updateComputer(details)) {
                    throw new IOException("Failed to restore PC " + details.name);
                }
            }
        }
        finally {
            db.close();
        }

        IdentitySnapshot.restore(context, backup.identity());
    }
}
