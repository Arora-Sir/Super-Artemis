package com.limelight.presets;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

// NOTE: One performance preset: a few settings with their values, and the texts that explain what it trades.
// NOTE: Applying a preset fills a settings profile, so the settings of the app itself are never overwritten and switching the profile off undoes it.
public final class Preset {
    public final String id;
    public final int nameRes;
    public final int summaryRes;
    public final int prosRes;
    public final int consRes;
    public final Map<String, Object> values;

    Preset(String id, int nameRes, int summaryRes, int prosRes, int consRes, Map<String, Object> values) {
        this.id = id;
        this.nameRes = nameRes;
        this.summaryRes = summaryRes;
        this.prosRes = prosRes;
        this.consRes = consRes;
        this.values = Collections.unmodifiableMap(values);
    }

    // The profile of a preset keeps one ID for ever, so applying it again updates the same profile and the app can tell it from the user's own
    public UUID profileId() {
        return UUID.nameUUIDFromBytes(("super-artemis-preset-" + id).getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}
