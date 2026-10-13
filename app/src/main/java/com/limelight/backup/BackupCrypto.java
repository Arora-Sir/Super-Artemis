package com.limelight.backup;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

// NOTE: Password protection for backup files: AES-256-GCM with a key derived by PBKDF2-HMAC-SHA256 (310,000 iterations, the OWASP 2023 figure).
// NOTE: Every file gets a fresh random salt and IV. The header is authenticated too, so changing the iteration count or format breaks decryption.
// NOTE: PBKDF2 is written out on top of HmacSHA256 because the platform PBKDF2WithHmacSHA256 factory only exists from Android 8.
public final class BackupCrypto {
    public static final String FORMAT = "super-artemis-backup-encrypted";
    static final int VERSION = 1;
    static final int ITERATIONS = 310_000;
    private static final int SALT_BYTES = 16;
    private static final int IV_BYTES = 12;
    private static final int KEY_BYTES = 32;
    private static final int TAG_BITS = 128;

    public static final class WrongPasswordException extends Exception {
        WrongPasswordException() {
            super("Wrong password");
        }
    }

    private BackupCrypto() {
    }

    public static boolean isEncrypted(JSONObject root) {
        return FORMAT.equals(root.optString("format"));
    }

    public static JSONObject encrypt(byte[] plain, char[] password) throws GeneralSecurityException, JSONException {
        return encrypt(plain, password, ITERATIONS);
    }

    static JSONObject encrypt(byte[] plain, char[] password, int iterations) throws GeneralSecurityException, JSONException {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[SALT_BYTES];
        byte[] iv = new byte[IV_BYTES];
        random.nextBytes(salt);
        random.nextBytes(iv);

        JSONObject root = new JSONObject();
        root.put("format", FORMAT);
        root.put("version", VERSION);
        root.put("kdf", "PBKDF2-HMAC-SHA256");
        root.put("iterations", iterations);
        root.put("salt", BackupIo.encodeBase64(salt));
        root.put("iv", BackupIo.encodeBase64(iv));

        byte[] key = pbkdf2(password, salt, iterations);
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(TAG_BITS, iv));
            cipher.updateAAD(header(iterations));
            root.put("data", BackupIo.encodeBase64(cipher.doFinal(plain)));
        }
        finally {
            Arrays.fill(key, (byte) 0);
        }
        return root;
    }

    public static byte[] decrypt(JSONObject root, char[] password) throws WrongPasswordException, InvalidBackupException {
        try {
            if (root.getInt("version") != VERSION || !"PBKDF2-HMAC-SHA256".equals(root.getString("kdf"))) {
                throw new InvalidBackupException("Unsupported encrypted backup");
            }
            int iterations = root.getInt("iterations");
            // Guards against a crafted file that would keep the phone busy for minutes
            if (iterations < 10_000 || iterations > 5_000_000) {
                throw new InvalidBackupException("Unsupported encrypted backup");
            }
            byte[] salt = BackupIo.decodeBase64(root.getString("salt"));
            byte[] iv = BackupIo.decodeBase64(root.getString("iv"));
            byte[] data = BackupIo.decodeBase64(root.getString("data"));
            byte[] key = pbkdf2(password, salt, iterations);
            try {
                Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
                cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(TAG_BITS, iv));
                cipher.updateAAD(header(iterations));
                return cipher.doFinal(data);
            }
            finally {
                Arrays.fill(key, (byte) 0);
            }
        }
        catch (AEADBadTagException e) {
            throw new WrongPasswordException();
        }
        catch (JSONException | IllegalArgumentException | GeneralSecurityException e) {
            throw new InvalidBackupException("Damaged encrypted backup");
        }
    }

    private static byte[] header(int iterations) {
        return (FORMAT + "|" + VERSION + "|PBKDF2-HMAC-SHA256|" + iterations).getBytes(StandardCharsets.UTF_8);
    }

    // RFC 8018 PBKDF2 with HMAC-SHA256, one 32 byte block
    static byte[] pbkdf2(char[] password, byte[] salt, int iterations) throws GeneralSecurityException {
        if (password.length == 0) {
            throw new IllegalArgumentException("Empty password");
        }
        ByteBuffer encoded = StandardCharsets.UTF_8.encode(java.nio.CharBuffer.wrap(password));
        byte[] passwordBytes = new byte[encoded.remaining()];
        encoded.get(passwordBytes);
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(passwordBytes, "HmacSHA256"));
        Arrays.fill(passwordBytes, (byte) 0);
        if (encoded.hasArray()) {
            Arrays.fill(encoded.array(), (byte) 0);
        }

        mac.update(salt);
        mac.update(ByteBuffer.allocate(4).putInt(1).array());
        byte[] u = mac.doFinal();
        byte[] result = u.clone();
        for (int i = 1; i < iterations; i++) {
            u = mac.doFinal(u);
            for (int j = 0; j < result.length; j++) {
                result[j] ^= u[j];
            }
        }
        return Arrays.copyOf(result, KEY_BYTES);
    }
}
