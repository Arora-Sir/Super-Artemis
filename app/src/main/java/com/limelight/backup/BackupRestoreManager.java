package com.limelight.backup;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import org.json.JSONException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

// Entry point for backup and restore. It only moves bytes through the Storage Access Framework and delegates the rest.
public final class BackupRestoreManager {
    public static final String MIME_TYPE = "application/json";

    private BackupRestoreManager() {
    }

    public static String defaultFileName() {
        return "super-artemis-backup-" + new SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(new Date()) + ".json";
    }

    public static void writeBackup(Context context, Uri uri, boolean includePairingKeys) throws JSONException, IOException {
        byte[] data = BackupCodec.create(context, includePairingKeys).toString(2).getBytes(StandardCharsets.UTF_8);
        try (OutputStream out = context.getContentResolver().openOutputStream(uri, "wt")) {
            if (out == null) {
                throw new IOException("Unable to open backup destination");
            }
            out.write(data);
        }
    }

    public static BackupFile readBackup(Context context, Uri uri) throws IOException, InvalidBackupException {
        try (InputStream in = context.getContentResolver().openInputStream(uri)) {
            if (in == null) {
                throw new IOException("Unable to open backup file");
            }
            return BackupCodec.parse(BackupIo.readLimited(in));
        }
    }

    public static void applyBackup(Context context, BackupFile backup) throws IOException, JSONException, InvalidBackupException {
        RestoreStrategy.forBackup(backup).apply(context, backup);
    }

    // Host list, identity and profiles are cached by long-lived objects, so a fresh process is the reliable reload
    public static void restartApp(Activity activity) {
        Intent intent = activity.getPackageManager().getLaunchIntentForPackage(activity.getPackageName());
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            activity.startActivity(intent);
        }
        activity.finishAffinity();
        Runtime.getRuntime().exit(0);
    }
}
