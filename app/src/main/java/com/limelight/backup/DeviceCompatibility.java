package com.limelight.backup;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.os.Vibrator;
import android.view.Display;

import com.limelight.R;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// NOTE: Finds settings in a backup that this device cannot use as they are, such as a resolution above its screen or HDR on a screen without it.
// NOTE: The restore preview lists them unticked, so by default this device keeps its own value for each of them.
public final class DeviceCompatibility {
    public static final class Issue {
        public final String key;
        public final String message;

        Issue(String key, String message) {
            this.key = key;
            this.message = message;
        }
    }

    // What the checks need to know about this device, so tests can describe any device
    public static final class Device {
        final int maxWidth;
        final int maxHeight;
        final float maxRefreshRate;
        final boolean hdr;
        final boolean motionSensors;
        final boolean vibrator;

        public Device(int maxWidth, int maxHeight, float maxRefreshRate, boolean hdr, boolean motionSensors, boolean vibrator) {
            this.maxWidth = Math.max(maxWidth, maxHeight);
            this.maxHeight = Math.min(maxWidth, maxHeight);
            this.maxRefreshRate = maxRefreshRate;
            this.hdr = hdr;
            this.motionSensors = motionSensors;
            this.vibrator = vibrator;
        }

        public static Device of(Context context) {
            int width = 0;
            int height = 0;
            float refresh = 60;
            boolean hdr = false;
            DisplayManager displays = (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
            Display display = displays != null ? displays.getDisplay(Display.DEFAULT_DISPLAY) : null;
            if (display != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    for (Display.Mode mode : display.getSupportedModes()) {
                        width = Math.max(width, Math.max(mode.getPhysicalWidth(), mode.getPhysicalHeight()));
                        height = Math.max(height, Math.min(mode.getPhysicalWidth(), mode.getPhysicalHeight()));
                        refresh = Math.max(refresh, mode.getRefreshRate());
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && display.getHdrCapabilities() != null) {
                    hdr = display.getHdrCapabilities().getSupportedHdrTypes().length > 0;
                }
            }
            SensorManager sensors = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
            boolean motion = sensors != null && sensors.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null
                    && sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null;
            Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            return new Device(width, height, refresh, hdr, motion, vibrator != null && vibrator.hasVibrator());
        }
    }

    private static final String[] VIBRATION_KEYS = {"checkbox_vibrate_osc", "checkbox_vibrate_fallback", "checkbox_vibrate_keyboard",
            "checkbox_enable_device_rumble"};
    private static final String[] MOTION_KEYS = {"checkbox_gamepad_motion_sensors", "checkbox_force_device_motion"};

    private DeviceCompatibility() {
    }

    public static List<Issue> check(Context context, BackupFile backup) {
        JSONObject defaults = backup.prefs() != null ? backup.prefs().optJSONObject(PrefsSnapshot.DEFAULT_PREFS_KEY) : null;
        if (defaults == null) {
            return Collections.emptyList();
        }
        return check(context, defaults, Device.of(context));
    }

    static List<Issue> check(Context context, JSONObject defaults, Device device) {
        List<Issue> issues = new ArrayList<>();

        String resolution = stringValue(defaults, "list_resolution");
        int[] size = parseSize(resolution);
        if (size != null && device.maxWidth > 0
                && (Math.max(size[0], size[1]) > device.maxWidth || Math.min(size[0], size[1]) > device.maxHeight)) {
            issues.add(new Issue("list_resolution", context.getString(R.string.restore_issue_resolution,
                    resolution, device.maxWidth + "x" + device.maxHeight)));
        }

        String fps = stringValue(defaults, "list_fps");
        try {
            if (fps != null && Integer.parseInt(fps) > Math.round(device.maxRefreshRate)) {
                issues.add(new Issue("list_fps", context.getString(R.string.restore_issue_fps, fps, Math.round(device.maxRefreshRate))));
            }
        }
        catch (NumberFormatException ignored) {
            // A custom or unknown value is left to the user
        }

        if (booleanValue(defaults, "checkbox_enable_hdr") && !device.hdr) {
            issues.add(new Issue("checkbox_enable_hdr", context.getString(R.string.restore_issue_hdr)));
        }
        if (!device.motionSensors) {
            for (String key : MOTION_KEYS) {
                if (booleanValue(defaults, key)) {
                    issues.add(new Issue(key, context.getString(R.string.restore_issue_motion)));
                }
            }
        }
        if (!device.vibrator) {
            for (String key : VIBRATION_KEYS) {
                if (booleanValue(defaults, key)) {
                    issues.add(new Issue(key, context.getString(R.string.restore_issue_vibration)));
                }
            }
        }
        // Microphones are saved by an ID that only means something on the device that made the backup
        String microphone = stringValue(defaults, "list_microphone_device");
        if (microphone != null && !microphone.isEmpty() && !"0".equals(microphone)) {
            issues.add(new Issue("list_microphone_device", context.getString(R.string.restore_issue_microphone)));
        }
        return issues;
    }

    static int[] parseSize(String value) {
        if (value == null) {
            return null;
        }
        String[] parts = value.toLowerCase().split("x");
        if (parts.length != 2) {
            return null;
        }
        try {
            return new int[] {Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim())};
        }
        catch (NumberFormatException e) {
            return null;
        }
    }

    private static String stringValue(JSONObject defaults, String key) {
        JSONObject typed = defaults.optJSONObject(key);
        return typed != null && "string".equals(typed.optString("type")) ? typed.optString("value", null) : null;
    }

    private static boolean booleanValue(JSONObject defaults, String key) {
        JSONObject typed = defaults.optJSONObject(key);
        return typed != null && "boolean".equals(typed.optString("type")) && typed.optBoolean("value");
    }
}
