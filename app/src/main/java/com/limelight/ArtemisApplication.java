package com.limelight;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.widget.Toast;

import com.limelight.diagnostics.CrashReporter;
import com.limelight.profiles.ProfilesManager;
import com.limelight.utils.AppLanguage;

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
        registerLanguageCallbacks();
    }

    // NOTE: Every screen takes the chosen language when it is created, and one that comes back after the choice changed recreates itself once.
    // NOTE: The stream screen is left alone, because recreating it would end a running stream, so it takes the new language with the next stream.
    private void registerLanguageCallbacks() {
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
                AppLanguage.apply(activity);
            }

            @Override
            public void onActivityResumed(Activity activity) {
                if (!(activity instanceof Game) && AppLanguage.isOutOfDate(activity) && AppLanguage.claimRecreate(activity)) {
                    activity.recreate();
                }
            }

            @Override
            public void onActivityStarted(Activity activity) {
            }

            @Override
            public void onActivityPaused(Activity activity) {
            }

            @Override
            public void onActivityStopped(Activity activity) {
            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
            }

            @Override
            public void onActivityDestroyed(Activity activity) {
            }
        });
    }
}