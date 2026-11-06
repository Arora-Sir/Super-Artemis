package com.limelight.utils;

import android.app.Activity;
import android.app.LocaleManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.LocaleList;

import androidx.preference.PreferenceManager;

import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.WeakHashMap;

// NOTE: The app language is applied by the app itself and never through the system app locale API.
// NOTE: Changing the system app locale while a screen is visible makes the system relayout the window, which shows as a black flash and a late redraw.
// NOTE: Every screen applies the language when it is created and recreates itself once when it comes back after the choice changed, so the new language shows at once.
public final class AppLanguage {
    // The same preference the language picker has always used, so earlier choices and backups keep working
    public static final String PREF_KEY = "list_languages";
    public static final String DEFAULT = "default";

    // Screens that already recreated themselves once for a language change, so a device that cannot apply the locale never loops
    private static final Set<Activity> recreated = Collections.newSetFromMap(new WeakHashMap<>());

    private AppLanguage() {
    }

    public static String choice(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(PREF_KEY, DEFAULT);
    }

    // NOTE: Default follows the language picked for this app in the Android settings (App languages) on Android 13 and newer, and the system language otherwise.
    // NOTE: The system language comes from Resources.getSystem(), because the resources of this process may still hold an earlier in-app choice.
    public static Locale effectiveLocale(Context context) {
        String choice = choice(context);
        if (!DEFAULT.equals(choice)) {
            return fromTag(choice);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            LocaleManager manager = context.getSystemService(LocaleManager.class);
            if (manager != null) {
                LocaleList app = manager.getApplicationLocales();
                LocaleList list = app.isEmpty() ? manager.getSystemLocales() : app;
                if (!list.isEmpty()) {
                    return list.get(0);
                }
            }
        }
        return systemLocale();
    }

    // Some locales include both language and country, which must be separated before the Locale constructor
    static Locale fromTag(String tag) {
        int dash = tag.indexOf('-');
        if (dash > 0) {
            return new Locale(tag.substring(0, dash), tag.substring(dash + 1));
        }
        return new Locale(tag);
    }

    @SuppressWarnings("deprecation")
    private static Locale systemLocale() {
        Configuration config = Resources.getSystem().getConfiguration();
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N ? config.getLocales().get(0) : config.locale;
    }

    @SuppressWarnings("deprecation")
    public static void apply(Activity activity) {
        Locale locale = effectiveLocale(activity);
        Locale.setDefault(locale);
        Resources resources = activity.getResources();
        Configuration config = new Configuration(resources.getConfiguration());
        config.setLocale(locale);
        resources.updateConfiguration(config, resources.getDisplayMetrics());
    }

    // True when the resources of this screen are not in the language that is chosen now
    @SuppressWarnings("deprecation")
    public static boolean isOutOfDate(Activity activity) {
        Locale wanted = effectiveLocale(activity);
        Configuration config = activity.getResources().getConfiguration();
        Locale current = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N ? config.getLocales().get(0) : config.locale;
        if (!wanted.getLanguage().equals(current.getLanguage())) {
            return true;
        }
        return !wanted.getCountry().isEmpty() && !wanted.getCountry().equals(current.getCountry());
    }

    // True once per screen, which is the one recreate a language change may cause
    public static boolean claimRecreate(Activity activity) {
        return recreated.add(activity);
    }
}
