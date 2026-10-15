package com.limelight.preferences;

import java.util.List;

// Gives tests in other packages the search synonyms, which StreamSettings keeps package-private
public final class StreamSettingsAccess {
    private StreamSettingsAccess() {
    }

    public static List<List<String>> synonyms() {
        return StreamSettings.SEARCH_SYNONYMS;
    }
}
