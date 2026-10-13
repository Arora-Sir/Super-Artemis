package com.limelight;

import android.app.Application;
import android.widget.Toast;

import com.limelight.diagnostics.CrashReporter;
import com.limelight.profiles.ProfilesManager;

public class ArtemisApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // First, so a crash anywhere after this point leaves a report the user can share
        CrashReporter.install(this);
        ProfilesManager profilesManager = ProfilesManager.getInstance();
        if (!profilesManager.load(this)) {
            Toast.makeText(this, R.string.profile_manager_failed_to_load, Toast.LENGTH_LONG).show();
        }
    }
}