package com.limelight.backup;

import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

// File and encoding helpers shared by the backup snapshots
final class BackupIo {
    static final long MAX_BACKUP_BYTES = 8L * 1024 * 1024;

    private BackupIo() {
    }

    static byte[] readFile(File file) throws IOException {
        try (InputStream in = new FileInputStream(file)) {
            return readLimited(in);
        }
    }

    // Caps the size so a wrong or hostile file cannot exhaust memory
    static byte[] readLimited(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[16 * 1024];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
            if (out.size() > MAX_BACKUP_BYTES) {
                throw new IOException("Backup file is too large");
            }
        }
        return out.toByteArray();
    }

    // Writes to a temporary sibling first, so a crash never leaves a half-written file behind
    static void writeFileAtomically(File target, byte[] data) throws IOException {
        File parent = target.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
            throw new IOException("Unable to create " + parent);
        }
        File temp = new File(target.getPath() + ".restore-tmp");
        try (FileOutputStream out = new FileOutputStream(temp)) {
            out.write(data);
            out.getFD().sync();
        }
        if (!temp.renameTo(target)) {
            temp.delete();
            throw new IOException("Unable to replace " + target.getName());
        }
    }

    static String encodeBase64(byte[] data) {
        return Base64.encodeToString(data, Base64.NO_WRAP);
    }

    static byte[] decodeBase64(String value) {
        return Base64.decode(value, Base64.NO_WRAP);
    }

    static String isoTimestamp(Date date) {
        return isoFormat().format(date);
    }

    // Returns null for a missing or unreadable timestamp, since it is only shown to the user
    static Date parseIsoTimestamp(String value) {
        try {
            return isoFormat().parse(value);
        }
        catch (ParseException e) {
            return null;
        }
    }

    private static SimpleDateFormat isoFormat() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format;
    }
}
