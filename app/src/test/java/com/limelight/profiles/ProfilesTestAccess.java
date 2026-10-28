package com.limelight.profiles;

import android.content.Context;

import java.io.File;

// Lets tests in other packages start from an empty profiles store
public final class ProfilesTestAccess {
    private ProfilesTestAccess() {
    }

    public static void reset(Context context) {
        ProfilesManager.instance = null;
        deleteRecursively(new File(context.getFilesDir(), "profiles"));
    }

    private static void deleteRecursively(File file) {
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) {
                deleteRecursively(child);
            }
        }
        file.delete();
    }
}
