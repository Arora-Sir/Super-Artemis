package com.limelight.presets;

import android.content.Context;

import com.limelight.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// NOTE: The built-in presets, read from res/raw/presets.json so the values can be checked by a test against the real settings screen.
// NOTE: Presets never touch the resolution, the host or the controls. They only set what trades latency against quality and battery.
public final class PresetCatalog {
    private PresetCatalog() {
    }

    public static List<Preset> load(Context context) {
        try (InputStream in = context.getResources().openRawResource(R.raw.presets)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int read;
            while ((read = in.read(buffer)) > 0) {
                out.write(buffer, 0, read);
            }
            return parse(out.toString(StandardCharsets.UTF_8.name()));
        }
        catch (IOException | JSONException e) {
            // The file ships inside the app, so this only happens if it is broken, and then there are simply no presets
            return Collections.emptyList();
        }
    }

    static List<Preset> parse(String json) throws JSONException {
        JSONArray array = new JSONObject(json).getJSONArray("presets");
        List<Preset> presets = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            JSONObject entry = array.getJSONObject(i);
            String id = entry.getString("id");
            JSONObject values = entry.getJSONObject("values");
            Map<String, Object> map = new LinkedHashMap<>();
            Iterator<String> keys = values.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                Object value = values.get(key);
                // Whole numbers are kept as Integer, the type the settings screen stores
                map.put(key, value instanceof Number ? (Object) ((Number) value).intValue() : value);
            }
            presets.add(new Preset(id, nameFor(id), summaryFor(id), prosFor(id), consFor(id), map));
        }
        return presets;
    }

    private static int nameFor(String id) {
        switch (id) {
            case "lowest_latency":
                return R.string.preset_lowest_latency;
            case "best_quality":
                return R.string.preset_best_quality;
            case "battery_saver":
                return R.string.preset_battery_saver;
            default:
                return R.string.preset_balanced;
        }
    }

    private static int summaryFor(String id) {
        switch (id) {
            case "lowest_latency":
                return R.string.preset_lowest_latency_summary;
            case "best_quality":
                return R.string.preset_best_quality_summary;
            case "battery_saver":
                return R.string.preset_battery_saver_summary;
            default:
                return R.string.preset_balanced_summary;
        }
    }

    private static int prosFor(String id) {
        switch (id) {
            case "lowest_latency":
                return R.string.preset_lowest_latency_pros;
            case "best_quality":
                return R.string.preset_best_quality_pros;
            case "battery_saver":
                return R.string.preset_battery_saver_pros;
            default:
                return R.string.preset_balanced_pros;
        }
    }

    private static int consFor(String id) {
        switch (id) {
            case "lowest_latency":
                return R.string.preset_lowest_latency_cons;
            case "best_quality":
                return R.string.preset_best_quality_cons;
            case "battery_saver":
                return R.string.preset_battery_saver_cons;
            default:
                return R.string.preset_balanced_cons;
        }
    }

    public static Preset find(Context context, String id) {
        for (Preset preset : load(context)) {
            if (preset.id.equals(id)) {
                return preset;
            }
        }
        return null;
    }
}
