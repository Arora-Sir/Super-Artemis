package com.limelight.backup;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.core.content.FileProvider;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

// NOTE: Entry point for backup and restore. It turns a backup into bytes (plain or password protected), writes them to a chosen place
// NOTE: or a shareable file, and reads them back. Validation, merging and replacing live in the codec, snapshots and strategies.
public final class BackupRestoreManager {
    // Our own type, so Android can offer Super Artemis when a backup file is opened or shared
    public static final String MIME_TYPE = "application/vnd.superartemis.backup";
    public static final String EXTENSION = ".sabackup";
    // Types other apps or older builds may report for a backup file
    static final String[] READABLE_MIME_TYPES = {MIME_TYPE, "application/json", "application/octet-stream", "text/plain"};
    private static final String SHARE_DIR = "backup-share";

    public static final class PasswordRequiredException extends Exception {
        PasswordRequiredException() {
            super("Password required");
        }
    }

    private BackupRestoreManager() {
    }

    public static String defaultFileName() {
        return "super-artemis-backup-" + new SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(new Date()) + EXTENSION;
    }

    // A null or empty password writes a readable JSON file, any other password an encrypted one
    static byte[] createBackupBytes(Context context, boolean includePairingKeys, char[] password)
            throws JSONException, IOException, GeneralSecurityException {
        byte[] plain = BackupCodec.create(context, includePairingKeys).toString(2).getBytes(StandardCharsets.UTF_8);
        if (password == null || password.length == 0) {
            return plain;
        }
        return BackupCrypto.encrypt(plain, password).toString(2).getBytes(StandardCharsets.UTF_8);
    }

    public static void writeBackup(Context context, Uri uri, boolean includePairingKeys, char[] password)
            throws JSONException, IOException, GeneralSecurityException {
        byte[] data = createBackupBytes(context, includePairingKeys, password);
        try (OutputStream out = context.getContentResolver().openOutputStream(uri, "wt")) {
            if (out == null) {
                throw new IOException("Unable to open backup destination");
            }
            out.write(data);
        }
    }

    // Writes the backup to a private cache file and returns a content URI the share sheet can hand to another app
    public static Uri createShareableBackup(Context context, boolean includePairingKeys, char[] password)
            throws JSONException, IOException, GeneralSecurityException {
        File dir = new File(context.getCacheDir(), SHARE_DIR);
        clearShareDir(dir);
        File file = new File(dir, defaultFileName());
        BackupIo.writeFileAtomically(file, createBackupBytes(context, includePairingKeys, password));
        return FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", file);
    }

    // Shared copies are temporary: each new share removes the previous one, so no backup lingers in the cache
    private static void clearShareDir(File dir) {
        File[] old = dir.listFiles();
        if (old != null) {
            for (File file : old) {
                file.delete();
            }
        }
    }

    public static byte[] readBytes(Context context, Uri uri) throws IOException {
        try (InputStream in = context.getContentResolver().openInputStream(uri)) {
            if (in == null) {
                throw new IOException("Unable to open backup file");
            }
            return BackupIo.readLimited(in);
        }
    }

    // Plain backups need no password. Encrypted ones throw PasswordRequiredException until a password is given.
    public static BackupFile parseBackup(byte[] data, char[] password)
            throws InvalidBackupException, PasswordRequiredException, BackupCrypto.WrongPasswordException {
        JSONObject root;
        try {
            root = new JSONObject(new String(data, StandardCharsets.UTF_8));
        }
        catch (JSONException e) {
            throw new InvalidBackupException("Not a Super Artemis backup");
        }
        if (!BackupCrypto.isEncrypted(root)) {
            return BackupCodec.validate(root);
        }
        if (password == null || password.length == 0) {
            throw new PasswordRequiredException();
        }
        return BackupCodec.parse(BackupCrypto.decrypt(root, password));
    }

    public static void applyBackup(Context context, BackupFile backup) throws IOException, JSONException, InvalidBackupException {
        applyBackup(context, backup, RestoreSelection.all(backup));
    }

    public static void applyBackup(Context context, BackupFile backup, RestoreSelection selection)
            throws IOException, JSONException, InvalidBackupException {
        RestoreStrategy.forBackup(backup, selection).apply(context, backup, selection);
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
