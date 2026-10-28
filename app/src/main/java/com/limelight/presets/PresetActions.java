package com.limelight.presets;

import android.content.Context;
import android.content.SharedPreferences;

import com.limelight.R;
import com.limelight.profiles.ProfilesManager;
import com.limelight.profiles.SettingsProfile;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

// NOTE: Everything a preset does, in terms of settings profiles, so the settings the user tuned are never overwritten.
// NOTE: Applying a preset first saves the current value of every setting it changes as a profile named "Before <preset>", then fills and activates the profile of the preset.
// NOTE: Switching the profile off ("Use my own settings") or to the saved one brings every value back, and the preset profile only holds the settings it changes.
public final class PresetActions {
    // One line of the diff shown before applying
    public static final class Change {
        public final String key;
        public final String title;
        public final String before;
        public final String after;

        Change(String key, String title, String before, String after) {
            this.key = key;
            this.title = title;
            this.before = before;
            this.after = after;
        }

        public boolean isSame() {
            return Objects.equals(before, after);
        }
    }

    private PresetActions() {
    }

    // The value each setting has right now, with the active profile and the saved defaults taken into account
    static Map<String, Object> currentValues(Context context, Set<String> keys) {
        SharedPreferences effective = ProfilesManager.getInstance().getOverlayingSharedPreferences(context);
        Map<String, ?> all = effective.getAll();
        Map<String, Object> values = new LinkedHashMap<>();
        for (String key : keys) {
            if (all.containsKey(key)) {
                values.put(key, all.get(key));
            }
        }
        return values;
    }

    public static List<Change> diff(Context context, Preset preset) {
        Map<String, SettingInfo> infos = SettingInfo.read(context);
        Map<String, Object> current = currentValues(context, preset.values.keySet());
        List<Change> changes = new ArrayList<>();
        for (Map.Entry<String, Object> entry : preset.values.entrySet()) {
            SettingInfo info = infos.get(entry.getKey());
            Object before = current.containsKey(entry.getKey()) ? current.get(entry.getKey()) : (info != null ? info.defaultValue : null);
            changes.add(new Change(entry.getKey(), info != null ? info.title.toString() : entry.getKey(),
                    describe(context, info, before), describe(context, info, entry.getValue())));
        }
        return changes;
    }

    private static String describe(Context context, SettingInfo info, Object value) {
        if (value == null) {
            return context.getString(R.string.preset_value_default);
        }
        if (value instanceof Boolean || (info != null && info.isSwitch())) {
            return Boolean.parseBoolean(String.valueOf(value)) ? context.getString(R.string.preset_value_on) : context.getString(R.string.preset_value_off);
        }
        if (info != null && info.key.equals("seekbar_bitrate_kbps") && value instanceof Number) {
            return context.getString(R.string.preset_value_mbps, ((Number) value).intValue() / 1000.0);
        }
        String text = value instanceof Number && ((Number) value).doubleValue() == ((Number) value).intValue()
                ? String.valueOf(((Number) value).intValue()) : String.valueOf(value);
        return info != null ? info.choiceFor(text) : text;
    }

    // Saves the current values, then fills the preset profile and makes it the active one. Returns the profile of the preset.
    public static SettingsProfile apply(Context context, Preset preset) {
        ProfilesManager profiles = ProfilesManager.getInstance();
        long now = System.currentTimeMillis();

        // The snapshot is what the settings were a moment ago, so activating it later restores them
        Map<String, Object> before = currentValues(context, preset.values.keySet());
        UUID snapshotId = UUID.nameUUIDFromBytes(("super-artemis-before-" + preset.id).getBytes(StandardCharsets.UTF_8));
        String snapshotName = context.getString(R.string.preset_before_name, context.getString(preset.nameRes));
        SettingsProfile snapshot = find(profiles, snapshotId);
        if (snapshot == null) {
            profiles.add(new SettingsProfile(snapshotId, snapshotName, now, now, before));
        }
        else {
            snapshot.setName(snapshotName);
            snapshot.setOptions(before);
            snapshot.setModifiedUtc(now);
            profiles.update(snapshot);
        }

        SettingsProfile profile = find(profiles, preset.profileId());
        Map<String, Object> options = new LinkedHashMap<>(preset.values);
        if (profile == null) {
            profile = new SettingsProfile(preset.profileId(), context.getString(preset.nameRes), now, now, options);
            profiles.add(profile);
        }
        else {
            profile.setName(context.getString(preset.nameRes));
            profile.setOptions(options);
            profile.setModifiedUtc(now);
            profiles.update(profile);
        }
        profiles.setActive(profile.getUuid());
        return profile;
    }

    // The saved "Before" profile restores the settings, and switching the profile off returns to the settings of the app itself
    public static void useOwnSettings() {
        ProfilesManager.getInstance().setActive(null);
    }

    // The preset whose profile is the active one, or null when the user's own settings or another profile are in use
    public static Preset activePreset(Context context) {
        SettingsProfile active = ProfilesManager.getInstance().getActive();
        if (active == null) {
            return null;
        }
        for (Preset preset : PresetCatalog.load(context)) {
            if (preset.profileId().equals(active.getUuid())) {
                return preset;
            }
        }
        return null;
    }

    // True when the active preset profile was edited afterwards, so it no longer matches the preset
    public static boolean isCustomized(Preset preset) {
        SettingsProfile active = ProfilesManager.getInstance().getActive();
        if (active == null || !preset.profileId().equals(active.getUuid())) {
            return false;
        }
        Map<String, Object> options = active.getOptions();
        if (options == null) {
            return true;
        }
        for (Map.Entry<String, Object> entry : preset.values.entrySet()) {
            Object value = options.get(entry.getKey());
            if (value == null || !sameValue(value, entry.getValue())) {
                return true;
            }
        }
        return false;
    }

    // A number saved to a file comes back as a Double, so numbers are compared by value
    static boolean sameValue(Object a, Object b) {
        if (a instanceof Number && b instanceof Number) {
            return ((Number) a).doubleValue() == ((Number) b).doubleValue();
        }
        return Objects.equals(a, b);
    }

    // The settings every preset covers, saved as a new profile, so a hand-tuned combination can be switched back to later
    public static SettingsProfile saveCurrentAsProfile(Context context, String name) {
        Set<String> keys = new LinkedHashSet<>();
        for (Preset preset : PresetCatalog.load(context)) {
            keys.addAll(preset.values.keySet());
        }
        long now = System.currentTimeMillis();
        SettingsProfile profile = new SettingsProfile(UUID.randomUUID(), name, now, now, currentValues(context, keys));
        ProfilesManager.getInstance().add(profile);
        return profile;
    }

    private static SettingsProfile find(ProfilesManager profiles, UUID id) {
        for (SettingsProfile profile : profiles.getProfiles()) {
            if (profile.getUuid().equals(id)) {
                return profile;
            }
        }
        return null;
    }
}
