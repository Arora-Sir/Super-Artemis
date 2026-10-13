package com.limelight.customkeys;

import com.limelight.R;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

// NOTE: The icons a custom key can show in menus and the quick-action bar (Material Symbols, Apache License 2.0).
// NOTE: The ID is what gets saved, so an icon can be redrawn later without breaking existing keys.
public final class IconCatalog {
    public static final class Icon {
        public final String id;
        public final int drawableRes;

        Icon(String id, int drawableRes) {
            this.id = id;
            this.drawableRes = drawableRes;
        }
    }

    public static final List<Icon> ICONS = Collections.unmodifiableList(Arrays.asList(
            new Icon("keyboard", R.drawable.ic_ck_keyboard),
            new Icon("bolt", R.drawable.ic_ck_bolt),
            new Icon("star", R.drawable.ic_ck_star),
            new Icon("play_arrow", R.drawable.ic_ck_play_arrow),
            new Icon("pause", R.drawable.ic_ck_pause),
            new Icon("skip_next", R.drawable.ic_ck_skip_next),
            new Icon("skip_previous", R.drawable.ic_ck_skip_previous),
            new Icon("volume_up", R.drawable.ic_ck_volume_up),
            new Icon("volume_off", R.drawable.ic_ck_volume_off),
            new Icon("screenshot_monitor", R.drawable.ic_ck_screenshot_monitor),
            new Icon("videocam", R.drawable.ic_ck_videocam),
            new Icon("save", R.drawable.ic_ck_save),
            new Icon("content_copy", R.drawable.ic_ck_content_copy),
            new Icon("content_paste", R.drawable.ic_ck_content_paste),
            new Icon("undo", R.drawable.ic_ck_undo),
            new Icon("redo", R.drawable.ic_ck_redo),
            new Icon("search", R.drawable.ic_ck_search),
            new Icon("home", R.drawable.ic_ck_home),
            new Icon("settings", R.drawable.ic_ck_settings),
            new Icon("close", R.drawable.ic_ck_close),
            new Icon("fullscreen", R.drawable.ic_ck_fullscreen),
            new Icon("desktop_windows", R.drawable.ic_ck_desktop_windows),
            new Icon("sports_esports", R.drawable.ic_ck_sports_esports),
            new Icon("terminal", R.drawable.ic_ck_terminal),
            new Icon("language", R.drawable.ic_ck_language),
            new Icon("lock", R.drawable.ic_ck_lock),
            new Icon("refresh", R.drawable.ic_ck_refresh),
            new Icon("swap_horiz", R.drawable.ic_ck_swap_horiz),
            // Added in round 2: keys, windows, media and system shortcuts
            new Icon("keyboard_command_key", R.drawable.ic_ck_keyboard_command_key),
            new Icon("window", R.drawable.ic_ck_window),
            new Icon("apps", R.drawable.ic_ck_apps),
            new Icon("tab", R.drawable.ic_ck_tab),
            new Icon("keyboard_tab", R.drawable.ic_ck_keyboard_tab),
            new Icon("keyboard_return", R.drawable.ic_ck_keyboard_return),
            new Icon("space_bar", R.drawable.ic_ck_space_bar),
            new Icon("arrow_upward", R.drawable.ic_ck_arrow_upward),
            new Icon("arrow_downward", R.drawable.ic_ck_arrow_downward),
            new Icon("arrow_back", R.drawable.ic_ck_arrow_back),
            new Icon("arrow_forward", R.drawable.ic_ck_arrow_forward),
            new Icon("keyboard_double_arrow_left", R.drawable.ic_ck_keyboard_double_arrow_left),
            new Icon("keyboard_double_arrow_right", R.drawable.ic_ck_keyboard_double_arrow_right),
            new Icon("monitor", R.drawable.ic_ck_monitor),
            new Icon("open_in_full", R.drawable.ic_ck_open_in_full),
            new Icon("close_fullscreen", R.drawable.ic_ck_close_fullscreen),
            new Icon("hdr_on", R.drawable.ic_ck_hdr_on),
            new Icon("mouse", R.drawable.ic_ck_mouse),
            new Icon("videogame_asset", R.drawable.ic_ck_videogame_asset),
            new Icon("mic", R.drawable.ic_ck_mic),
            new Icon("headphones", R.drawable.ic_ck_headphones),
            new Icon("music_note", R.drawable.ic_ck_music_note),
            new Icon("photo_camera", R.drawable.ic_ck_photo_camera),
            new Icon("zoom_in", R.drawable.ic_ck_zoom_in),
            new Icon("zoom_out", R.drawable.ic_ck_zoom_out),
            new Icon("brightness_high", R.drawable.ic_ck_brightness_high),
            new Icon("chat", R.drawable.ic_ck_chat),
            new Icon("translate", R.drawable.ic_ck_translate),
            new Icon("timer", R.drawable.ic_ck_timer),
            new Icon("bookmark", R.drawable.ic_ck_bookmark),
            new Icon("rocket_launch", R.drawable.ic_ck_rocket_launch),
            new Icon("restart_alt", R.drawable.ic_ck_restart_alt),
            new Icon("power_settings_new", R.drawable.ic_ck_power_settings_new)
    ));

    private IconCatalog() {
    }

    // Unknown or missing IDs fall back to the keyboard icon, so a key always has something to show in the bar
    public static int drawableFor(String id) {
        for (Icon icon : ICONS) {
            if (icon.id.equals(id)) {
                return icon.drawableRes;
            }
        }
        return R.drawable.ic_ck_keyboard;
    }
}
