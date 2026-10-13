package com.limelight.backup;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

// NOTE: Which parts of a backup the user chose to restore. Settings are the main preferences file, layouts are every other settings file
// NOTE: (special keys, on-screen controls, keyboard layouts), and PCs include the per-PC files such as hidden apps and the last microphone state.
// NOTE: Pairing keys replace this device's identity, so they always bring the backup's PC list with them.
public final class RestoreSelection {
    public final boolean settings;
    public final boolean layouts;
    public final boolean profiles;
    public final boolean pcs;
    public final boolean pairingKeys;
    // Main settings this device keeps its own value for, such as a resolution its screen cannot show
    public final Set<String> keepLocalSettings;

    public RestoreSelection(boolean settings, boolean layouts, boolean profiles, boolean pcs, boolean pairingKeys) {
        this(settings, layouts, profiles, pcs, pairingKeys, Collections.emptySet());
    }

    public RestoreSelection(boolean settings, boolean layouts, boolean profiles, boolean pcs, boolean pairingKeys,
                            Set<String> keepLocalSettings) {
        this.keepLocalSettings = Collections.unmodifiableSet(new HashSet<>(keepLocalSettings));
        this.settings = settings;
        this.layouts = layouts;
        this.profiles = profiles;
        this.pcs = pcs || pairingKeys;
        this.pairingKeys = pairingKeys;
    }

    public static RestoreSelection all(BackupFile backup) {
        return new RestoreSelection(true, true, true, true, backup.hasPairingKeys());
    }

    public boolean isEmpty() {
        return !settings && !layouts && !profiles && !pcs;
    }

    // Whether a settings file (by its backup key, "_default" for the main file) belongs to the chosen parts
    boolean includesPrefsFile(String key) {
        if (PrefsSnapshot.DEFAULT_PREFS_KEY.equals(key)) {
            return settings;
        }
        if (MergeHostsStrategy.PER_HOST_FILES.contains(key)) {
            return pcs;
        }
        return layouts;
    }
}
