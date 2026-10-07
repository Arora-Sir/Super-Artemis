package com.limelight.backup;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

// Backs up app files that are not SharedPreferences, limited to an allowlist so a crafted backup cannot write elsewhere
final class FileSnapshot {
    static final List<String> RESTORABLE_FILES = Collections.singletonList("profiles/profiles.json");

    private FileSnapshot() {
    }

    static JSONObject capture(Context context) throws JSONException, IOException {
        JSONObject json = new JSONObject();
        for (String relativePath : RESTORABLE_FILES) {
            File file = new File(context.getFilesDir(), relativePath);
            if (file.isFile()) {
                json.put(relativePath, BackupIo.encodeBase64(BackupIo.readFile(file)));
            }
        }
        return json;
    }

    static void validate(JSONObject filesJson) throws JSONException, InvalidBackupException {
        Iterator<String> paths = filesJson.keys();
        while (paths.hasNext()) {
            String path = paths.next();
            if (!RESTORABLE_FILES.contains(path)) {
                throw new InvalidBackupException("Unexpected file " + path);
            }
            BackupIo.decodeBase64(filesJson.getString(path));
        }
    }

    static void restore(Context context, JSONObject filesJson) throws JSONException, IOException {
        Iterator<String> paths = filesJson.keys();
        while (paths.hasNext()) {
            String path = paths.next();
            BackupIo.writeFileAtomically(new File(context.getFilesDir(), path), BackupIo.decodeBase64(filesJson.getString(path)));
        }
    }
}
