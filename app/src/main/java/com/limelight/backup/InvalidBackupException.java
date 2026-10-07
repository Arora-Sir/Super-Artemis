package com.limelight.backup;

// Thrown when a backup file is not a Super Artemis backup, uses an unsupported schema or contains malformed data
public class InvalidBackupException extends Exception {
    public InvalidBackupException(String message) {
        super(message);
    }
}
