package com.limelight;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceManager;

import com.limelight.preferences.PreferenceConfiguration;
import com.limelight.preferences.StreamSettings;
import com.limelight.profiles.ProfilesManager;
import com.limelight.profiles.SettingsProfile;
import com.limelight.theme.ThemeManager;
import com.limelight.utils.UiHelper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class EditProfileActivity extends AppCompatActivity implements com.limelight.preferences.SettingsSections.Host {
    private String appliedStyle;
    private String profileUuid;
    private SettingsProfile currentProfile;
    private InMemorySharedPreferences inMemoryPrefs;
    private ProfilePreferenceFragment prefsFragment;
    private String pendingProfileName; // Holds new name for unsaved profiles
    // The same section list as Settings on wide screens
    private com.limelight.preferences.SettingsSections sections;

    @Override
    public void onSettingsListReady(androidx.preference.PreferenceFragmentCompat fragment) {
        if (sections != null) {
            sections.attach(fragment);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // The app style changed in settings while this screen was in the background
        if (ThemeManager.needsRecreate(this, appliedStyle)) {
            recreate();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Classic or Material You, applied before any view is inflated
        appliedStyle = ThemeManager.apply(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);
        sections = new com.limelight.preferences.SettingsSections(this, findViewById(R.id.settings_sections));

        UiHelper.setLocale(this);

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        loadProfile();
        if (profileUuid != null && currentProfile == null) {
            Toast.makeText(this, R.string.profile_manager_profile_not_found, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        setTitle(currentProfile != null
                ? getString(R.string.profile_manager_edit_profile) + currentProfile.getName()
                : getString(R.string.profile_manager_new_profile));

        // A fragment the system rebuilt (theme switch, rotation, process death) is kept, a fresh start adds a new one
        androidx.fragment.app.Fragment restored = getSupportFragmentManager().findFragmentById(R.id.preferences_container);
        if (restored instanceof ProfilePreferenceFragment) {
            prefsFragment = (ProfilePreferenceFragment) restored;
        }
        else {
            prefsFragment = new ProfilePreferenceFragment(this, inMemoryPrefs);
            getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.preferences_container, prefsFragment)
                .commit();
        }

        UiHelper.notifyNewRootView(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.edit_profile_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            finish();
            return true;
        } else if (id == R.id.action_save) {
            saveProfile();
            return true;
        } else if (id == R.id.action_rename) {
            showRenameDialog();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    void reloadSettings() {
        prefsFragment = new ProfilePreferenceFragment(this, prefsFragment.getPrefs());
        getSupportFragmentManager().beginTransaction().replace(
                R.id.preferences_container, prefsFragment
        ).commitAllowingStateLoss();
    }

    private void saveProfile() {
        // Get the profile options from our in-memory prefs
        Map<String, Object> profileOptions = new HashMap<>(inMemoryPrefs.getAll());

        String displayName;

        if (currentProfile != null) {
            // Update existing profile
            currentProfile.setOptions(profileOptions);
            currentProfile.setModifiedUtc(System.currentTimeMillis());
            displayName = currentProfile.getName();
            ProfilesManager.getInstance().update(currentProfile);
        } else {
            // Create new profile
            String profileName = pendingProfileName != null ? pendingProfileName.trim() : null;
            if (profileName == null || profileName.isEmpty()) {
                profileName = getString(R.string.profile_manager_profile) + (ProfilesManager.getInstance().getProfiles().size() + 1);
            }
            long now = System.currentTimeMillis();
            SettingsProfile newProfile = new SettingsProfile(
                UUID.randomUUID(),
                profileName,
                now,
                now,
                profileOptions
            );
            displayName = profileName;
            ProfilesManager.getInstance().add(newProfile);
        }

        if (ProfilesManager.getInstance().save(this)) {
            // Show confirmation Toast
            Toast.makeText(this,
                    getString(R.string.profile_manager_profile_saved, displayName),
                    Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, R.string.profile_manager_failed_to_save, Toast.LENGTH_LONG).show();
        }

        finish();
    }

    private void showRenameDialog() {
        final android.widget.EditText input = new android.widget.EditText(this);
        String initial = currentProfile != null ? currentProfile.getName() : (pendingProfileName != null ? pendingProfileName : "");
        input.setText(initial);
        input.setSelection(initial.length());

        new AlertDialog.Builder(this)
                .setTitle(R.string.profile_manager_edit_profile_name)
                .setView(input)
                .setPositiveButton("OK", (dialog, which) -> {
                    String newName = input.getText().toString().trim();
                    if (newName.isEmpty()) {
                        Toast.makeText(this, R.string.profile_manager_name_cannot_be_blank, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (currentProfile != null) {
                        currentProfile.setName(newName);
                        currentProfile.setModifiedUtc(System.currentTimeMillis());
                        ProfilesManager.getInstance().update(currentProfile);
                        setTitle(getString(R.string.profile_manager_edit_profile_with, newName));
                    } else {
                        pendingProfileName = newName;
                        setTitle(getString(R.string.profile_manager_new_profile_with, newName));
                    }
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    // NOTE: Finds the edited profile from the intent and copies its options into memory, once.
    // NOTE: It runs on first use, because a fragment rebuilt by the system asks for the settings during super.onCreate, before onCreate reaches this point.
    private void loadProfile() {
        if (inMemoryPrefs != null) {
            return;
        }
        profileUuid = getIntent().getStringExtra("profileUuid");
        if (profileUuid != null) {
            for (SettingsProfile profile : ProfilesManager.getInstance().getProfiles()) {
                if (profile.getUuid().toString().equals(profileUuid)) {
                    currentProfile = profile;
                    break;
                }
            }
            // A missing profile edits an empty copy only until onCreate closes the screen
            inMemoryPrefs = new InMemorySharedPreferences(currentProfile != null ? currentProfile.getOptions() : new HashMap<>());
        }
        else {
            // A new profile starts from the current settings
            inMemoryPrefs = new InMemorySharedPreferences(PreferenceManager.getDefaultSharedPreferences(this).getAll());
        }
    }

    public SharedPreferences getInMemoryPrefs() {
        loadProfile();
        return inMemoryPrefs;
    }

    public static class ProfilePreferenceFragment extends StreamSettings.SettingsFragment {
        private static class InMemoryPreferenceDataStore extends androidx.preference.PreferenceDataStore {
            private final SharedPreferences prefs;
            InMemoryPreferenceDataStore(SharedPreferences prefs) {
                this.prefs = prefs;
            }
            @Override public void putString(String key, String value) { prefs.edit().putString(key, value).apply(); }
            @Override public void putStringSet(String key, java.util.Set<String> values) { prefs.edit().putStringSet(key, values).apply(); }
            @Override public void putInt(String key, int value) { prefs.edit().putInt(key, value).apply(); }
            @Override public void putBoolean(String key, boolean value) { prefs.edit().putBoolean(key, value).apply(); }
            @Override public void putFloat(String key, float value) { prefs.edit().putFloat(key, value).apply(); }
            @Override public void putLong(String key, long value) { prefs.edit().putLong(key, value).apply(); }

            @Override public String getString(String key, String defValue) { return prefs.getString(key, defValue); }
            @Override public java.util.Set<String> getStringSet(String key, java.util.Set<String> defValues) { return prefs.getStringSet(key, defValues); }
            @Override public int getInt(String key, int defValue) {
                Object value = prefs.getAll().get(key);
                if (value instanceof Number) {
                    return ((Number) value).intValue();
                }
                return defValue;
            }
            @Override public boolean getBoolean(String key, boolean defValue) { return prefs.getBoolean(key, defValue); }
            @Override public float getFloat(String key, float defValue) { return prefs.getFloat(key, defValue); }
            @Override public long getLong(String key, long defValue) {
                Object value = prefs.getAll().get(key);
                if (value instanceof Number) {
                    return ((Number) value).longValue();
                }
                return defValue;
            }

            public SharedPreferences getPrefs() {
                return prefs;
            }
        }

        private static Map<String, Object> diff(Map<String, ?> target, Map<String, ?> newPrefs) {
            Map<String, Object> patch = new HashMap<>();
            for (Map.Entry<String, ?> entry : target.entrySet()) {
                String k = entry.getKey();
                Object v = entry.getValue();
                if (newPrefs.containsKey(k)) {
                    Object def = newPrefs.get(k);
                    if (v == null || !v.equals(def)) {
                        patch.put(k, v);
                    }
                } else {
                    // unknown key, include
                    patch.put(k, v);
                }
            }
            return patch;
        }

        @Override
        protected SharedPreferences getPrefs() {
            return ((InMemoryPreferenceDataStore)getPreferenceManager().getPreferenceDataStore()).getPrefs();
        }

        public ProfilePreferenceFragment(EditProfileActivity context, SharedPreferences prefs) {
            super(PreferenceConfiguration.readPreferences(context, prefs));
        }

        // NOTE: The system rebuilds fragments by class name after a theme switch, a rotation or process death, which needs a public constructor without arguments.
        // NOTE: Without it the profile editor crashed with "could not find Fragment constructor". The rebuilt fragment reads the profile's settings from the activity in onCreatePreferences.
        public ProfilePreferenceFragment() {
            super();
        }

        @NonNull
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            return super.onCreateView(inflater, container, savedInstanceState, true);
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            EditProfileActivity act = (EditProfileActivity) requireActivity();
            SharedPreferences memPrefs = act.getInMemoryPrefs();
            // Read the profile's own values, not the default settings, when the system rebuilt this fragment
            setPrevPrefConfig(PreferenceConfiguration.readPreferences(act, memPrefs));
            // Route preference storage to in-memory prefs
            getPreferenceManager().setPreferenceDataStore(new InMemoryPreferenceDataStore(memPrefs));

            super.onCreatePreferences(savedInstanceState, rootKey);

            Preference prefScreen = getPreferenceScreen();

            // Same screen as Settings: app-wide settings stay visible with a note, and only the app sections (backup, About) are hidden
            com.limelight.profiles.ProfileScope.applyToProfileEditor(this);

            // Highlight changed preferences
            java.util.Map<String, ?> patch = diff(
                    PreferenceManager.getDefaultSharedPreferences(act).getAll(),
                    memPrefs.getAll()
            );
            highlightPreferences(prefScreen, patch.keySet());
        }

        @Override
        protected void reloadSettings() {
            ((EditProfileActivity)requireActivity()).reloadSettings();
        }

        private void highlightPreferences(Preference pref, java.util.Set<String> changedKeys) {
            if (pref == null) return;

            if (pref instanceof PreferenceGroup) {
                androidx.preference.PreferenceGroup group = (androidx.preference.PreferenceGroup) pref;
                for (int i = 0; i < group.getPreferenceCount(); i++) {
                    highlightPreferences(group.getPreference(i), changedKeys);
                }
            } else {
                String key = pref.getKey();
                if (key != null && changedKeys.contains(key)) {
                    pref.setTitle("*" + pref.getTitle());
                }
            }
        }
    }

    /**
     * Simple in-memory SharedPreferences implementation for profile editing
     */
    private static class InMemorySharedPreferences implements SharedPreferences {
        private final Map<String, Object> values;

        public InMemorySharedPreferences(Map<String, ?> initialValues) {
            this.values = new HashMap<>(initialValues != null ? initialValues : new HashMap<>());
        }

        @Override
        public Map<String, ?> getAll() {
            return new HashMap<>(values);
        }

        @Override
        public String getString(String key, String defValue) {
            Object value = values.get(key);
            return value instanceof String ? (String) value : defValue;
        }

        @Override
        public int getInt(String key, int defValue) {
            Object value = values.get(key);
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
            return defValue;
        }

        @Override
        public long getLong(String key, long defValue) {
            Object value = values.get(key);
            if (value instanceof Number) {
                return ((Number) value).longValue();
            }
            return defValue;
        }

        @Override
        public float getFloat(String key, float defValue) {
            Object value = values.get(key);
            return value instanceof Float ? (Float) value : defValue;
        }

        @Override
        public boolean getBoolean(String key, boolean defValue) {
            Object value = values.get(key);
            return value instanceof Boolean ? (Boolean) value : defValue;
        }

        @Override
        public java.util.Set<String> getStringSet(String key, java.util.Set<String> defValues) {
            Object value = values.get(key);
            return value instanceof java.util.Set ? (java.util.Set<String>) value : defValues;
        }

        @Override
        public boolean contains(String key) {
            return values.containsKey(key);
        }

        @Override
        public Editor edit() {
            return new InMemoryEditor();
        }

        @Override
        public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        }

        @Override
        public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        }

        private class InMemoryEditor implements Editor {
            private final Map<String, Object> changes = new HashMap<>();

            @Override
            public Editor putString(String key, String value) {
                changes.put(key, value);
                return this;
            }

            @Override
            public Editor putInt(String key, int value) {
                changes.put(key, value);
                return this;
            }

            @Override
            public Editor putLong(String key, long value) {
                changes.put(key, value);
                return this;
            }

            @Override
            public Editor putFloat(String key, float value) {
                changes.put(key, value);
                return this;
            }

            @Override
            public Editor putBoolean(String key, boolean value) {
                changes.put(key, value);
                return this;
            }

            @Override
            public Editor putStringSet(String key, java.util.Set<String> values) {
                changes.put(key, values);
                return this;
            }

            @Override
            public Editor remove(String key) {
                changes.put(key, null);
                return this;
            }

            @Override
            public Editor clear() {
                values.clear();
                return this;
            }

            @Override
            public boolean commit() {
                apply();
                return true;
            }

            @Override
            public void apply() {
                for (Map.Entry<String, Object> entry : changes.entrySet()) {
                    if (entry.getValue() == null) {
                        values.remove(entry.getKey());
                    } else {
                        values.put(entry.getKey(), entry.getValue());
                    }
                }
                changes.clear();
            }
        }
    }
}