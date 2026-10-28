package com.limelight.customkeys;

import com.limelight.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

// NOTE: The icons a custom key can show in menus and the quick-action bar (Material Symbols, Apache License 2.0).
// NOTE: The ID is what gets saved, so an icon can be redrawn later without breaking existing keys, and an ID is never renamed or reused.
// NOTE: Every icon belongs to a group, which the picker shows as a section, and carries search words, so "mic", "voice" and "mute" all find the microphone icons.
public final class IconCatalog {
    public enum Group {
        GENERAL(R.string.icon_group_general),
        VOICE(R.string.icon_group_voice),
        MEDIA(R.string.icon_group_media),
        WINDOW(R.string.icon_group_window),
        KEYS(R.string.icon_group_keys),
        EDIT(R.string.icon_group_edit),
        SYSTEM(R.string.icon_group_system),
        SECURITY(R.string.icon_group_security),
        WEB(R.string.icon_group_web),
        CAPTURE(R.string.icon_group_capture),
        INPUT(R.string.icon_group_input);

        public final int labelRes;

        Group(int labelRes) {
            this.labelRes = labelRes;
        }
    }

    public static final class Icon {
        public final String id;
        public final int drawableRes;
        public final Group group;
        // Lower case words the picker search matches against
        public final String keywords;

        Icon(String id, int drawableRes, Group group, String keywords) {
            this.id = id;
            this.drawableRes = drawableRes;
            this.group = group;
            this.keywords = keywords;
        }
    }

    public static final List<Icon> ICONS = Collections.unmodifiableList(Arrays.asList(
            new Icon("keyboard", R.drawable.ic_ck_keyboard, Group.GENERAL, "keyboard general keys typing"),
            new Icon("bolt", R.drawable.ic_ck_bolt, Group.GENERAL, "bolt general flash lightning fast power"),
            new Icon("star", R.drawable.ic_ck_star, Group.GENERAL, "star general favorite favourite"),
            new Icon("bookmark", R.drawable.ic_ck_bookmark, Group.GENERAL, "bookmark general"),
            new Icon("rocket_launch", R.drawable.ic_ck_rocket_launch, Group.GENERAL, "rocket launch general"),
            new Icon("chat", R.drawable.ic_ck_chat, Group.GENERAL, "chat general"),
            new Icon("translate", R.drawable.ic_ck_translate, Group.GENERAL, "translate general language translation"),
            new Icon("timer", R.drawable.ic_ck_timer, Group.GENERAL, "timer general clock stopwatch wait"),
            new Icon("history", R.drawable.ic_ck_history, Group.GENERAL, "history general recent clock past"),
            new Icon("language", R.drawable.ic_ck_language, Group.GENERAL, "language general web globe world"),
            new Icon("home", R.drawable.ic_ck_home, Group.GENERAL, "home general desktop start"),
            new Icon("settings", R.drawable.ic_ck_settings, Group.GENERAL, "settings general gear options preferences"),
            new Icon("help", R.drawable.ic_ck_help, Group.GENERAL, "help general"),
            new Icon("info", R.drawable.ic_ck_info, Group.GENERAL, "info general information"),
            new Icon("warning", R.drawable.ic_ck_warning, Group.GENERAL, "warning general alert caution"),
            new Icon("check", R.drawable.ic_ck_check, Group.GENERAL, "check general done ok tick"),
            new Icon("add", R.drawable.ic_ck_add, Group.GENERAL, "add general"),
            new Icon("remove", R.drawable.ic_ck_remove, Group.GENERAL, "remove general"),
            new Icon("favorite", R.drawable.ic_ck_favorite, Group.GENERAL, "favorite general heart love like"),
            new Icon("thumb_up", R.drawable.ic_ck_thumb_up, Group.GENERAL, "thumb up general"),
            new Icon("lightbulb", R.drawable.ic_ck_lightbulb, Group.GENERAL, "lightbulb general"),
            new Icon("flag", R.drawable.ic_ck_flag, Group.GENERAL, "flag general"),
            new Icon("auto_awesome", R.drawable.ic_ck_auto_awesome, Group.GENERAL, "auto awesome general gemini ai sparkle magic assistant"),
            new Icon("smart_toy", R.drawable.ic_ck_smart_toy, Group.GENERAL, "smart toy general ai bot robot assistant"),
            new Icon("psychology", R.drawable.ic_ck_psychology, Group.GENERAL, "psychology general ai brain think assistant"),
            new Icon("assistant", R.drawable.ic_ck_assistant, Group.GENERAL, "assistant general ai google gemini assistant"),
            new Icon("person", R.drawable.ic_ck_person, Group.GENERAL, "person general"),
            new Icon("group", R.drawable.ic_ck_group, Group.GENERAL, "group general"),
            new Icon("work", R.drawable.ic_ck_work, Group.GENERAL, "work general"),
            new Icon("school", R.drawable.ic_ck_school, Group.GENERAL, "school general"),
            new Icon("shopping_cart", R.drawable.ic_ck_shopping_cart, Group.GENERAL, "shopping cart general"),
            new Icon("map", R.drawable.ic_ck_map, Group.GENERAL, "map general"),
            new Icon("location_on", R.drawable.ic_ck_location_on, Group.GENERAL, "location on general"),
            new Icon("calendar_today", R.drawable.ic_ck_calendar_today, Group.GENERAL, "calendar today general"),
            new Icon("event", R.drawable.ic_ck_event, Group.GENERAL, "event general"),
            new Icon("schedule", R.drawable.ic_ck_schedule, Group.GENERAL, "schedule general"),
            new Icon("alarm", R.drawable.ic_ck_alarm, Group.GENERAL, "alarm general"),
            new Icon("hourglass_empty", R.drawable.ic_ck_hourglass_empty, Group.GENERAL, "hourglass empty general"),
            new Icon("notifications", R.drawable.ic_ck_notifications, Group.GENERAL, "notifications general"),
            new Icon("calculate", R.drawable.ic_ck_calculate, Group.GENERAL, "calculate general"),
            new Icon("mail", R.drawable.ic_ck_mail, Group.GENERAL, "mail general"),
            new Icon("forum", R.drawable.ic_ck_forum, Group.GENERAL, "forum general"),
            new Icon("chat_bubble", R.drawable.ic_ck_chat_bubble, Group.GENERAL, "chat bubble general"),
            new Icon("task_alt", R.drawable.ic_ck_task_alt, Group.GENERAL, "task alt general"),
            new Icon("done_all", R.drawable.ic_ck_done_all, Group.GENERAL, "done all general"),
            new Icon("code", R.drawable.ic_ck_code, Group.GENERAL, "code general"),
            new Icon("bug_report", R.drawable.ic_ck_bug_report, Group.GENERAL, "bug report general"),
            new Icon("build", R.drawable.ic_ck_build, Group.GENERAL, "build general"),
            new Icon("extension", R.drawable.ic_ck_extension, Group.GENERAL, "extension general"),
            new Icon("science", R.drawable.ic_ck_science, Group.GENERAL, "science general"),
            new Icon("palette", R.drawable.ic_ck_palette, Group.GENERAL, "palette general"),
            new Icon("brush", R.drawable.ic_ck_brush, Group.GENERAL, "brush general"),
            new Icon("explore", R.drawable.ic_ck_explore, Group.GENERAL, "explore general"),
            new Icon("mic", R.drawable.ic_ck_mic, Group.VOICE, "mic mic and voice microphone voice talk record input speak"),
            new Icon("mic_off", R.drawable.ic_ck_mic_off, Group.VOICE, "mic off mic and voice mute microphone silent voice off"),
            new Icon("keyboard_voice", R.drawable.ic_ck_keyboard_voice, Group.VOICE, "keyboard voice mic and voice voice typing dictation microphone speak"),
            new Icon("record_voice_over", R.drawable.ic_ck_record_voice_over, Group.VOICE, "record voice over mic and voice speak talk voice assistant"),
            new Icon("settings_voice", R.drawable.ic_ck_settings_voice, Group.VOICE, "settings voice mic and voice voice settings microphone"),
            new Icon("voice_chat", R.drawable.ic_ck_voice_chat, Group.VOICE, "voice chat mic and voice voice chat talk discord call"),
            new Icon("graphic_eq", R.drawable.ic_ck_graphic_eq, Group.VOICE, "graphic eq mic and voice sound equalizer audio wave voice"),
            new Icon("hearing", R.drawable.ic_ck_hearing, Group.VOICE, "hearing mic and voice"),
            new Icon("campaign", R.drawable.ic_ck_campaign, Group.VOICE, "campaign mic and voice announce loudspeaker megaphone"),
            new Icon("call", R.drawable.ic_ck_call, Group.VOICE, "call mic and voice"),
            new Icon("call_end", R.drawable.ic_ck_call_end, Group.VOICE, "call end mic and voice"),
            new Icon("play_arrow", R.drawable.ic_ck_play_arrow, Group.MEDIA, "play arrow media and sound"),
            new Icon("pause", R.drawable.ic_ck_pause, Group.MEDIA, "pause media and sound"),
            new Icon("skip_next", R.drawable.ic_ck_skip_next, Group.MEDIA, "skip next media and sound"),
            new Icon("skip_previous", R.drawable.ic_ck_skip_previous, Group.MEDIA, "skip previous media and sound"),
            new Icon("stop", R.drawable.ic_ck_stop, Group.MEDIA, "stop media and sound"),
            new Icon("fast_forward", R.drawable.ic_ck_fast_forward, Group.MEDIA, "fast forward media and sound"),
            new Icon("fast_rewind", R.drawable.ic_ck_fast_rewind, Group.MEDIA, "fast rewind media and sound"),
            new Icon("replay", R.drawable.ic_ck_replay, Group.MEDIA, "replay media and sound"),
            new Icon("shuffle", R.drawable.ic_ck_shuffle, Group.MEDIA, "shuffle media and sound"),
            new Icon("repeat", R.drawable.ic_ck_repeat, Group.MEDIA, "repeat media and sound"),
            new Icon("movie", R.drawable.ic_ck_movie, Group.MEDIA, "movie media and sound"),
            new Icon("subtitles", R.drawable.ic_ck_subtitles, Group.MEDIA, "subtitles media and sound"),
            new Icon("play_circle", R.drawable.ic_ck_play_circle, Group.MEDIA, "play circle media and sound"),
            new Icon("pause_circle", R.drawable.ic_ck_pause_circle, Group.MEDIA, "pause circle media and sound"),
            new Icon("music_note", R.drawable.ic_ck_music_note, Group.MEDIA, "music note media and sound"),
            new Icon("music_off", R.drawable.ic_ck_music_off, Group.MEDIA, "music off media and sound"),
            new Icon("headphones", R.drawable.ic_ck_headphones, Group.MEDIA, "headphones media and sound"),
            new Icon("equalizer", R.drawable.ic_ck_equalizer, Group.MEDIA, "equalizer media and sound"),
            new Icon("queue_music", R.drawable.ic_ck_queue_music, Group.MEDIA, "queue music media and sound"),
            new Icon("speaker", R.drawable.ic_ck_speaker, Group.MEDIA, "speaker media and sound"),
            new Icon("volume_up", R.drawable.ic_ck_volume_up, Group.MEDIA, "volume up media and sound sound audio louder speaker"),
            new Icon("volume_off", R.drawable.ic_ck_volume_off, Group.MEDIA, "volume off media and sound mute sound silent"),
            new Icon("volume_down", R.drawable.ic_ck_volume_down, Group.MEDIA, "volume down media and sound sound audio quieter speaker"),
            new Icon("volume_mute", R.drawable.ic_ck_volume_mute, Group.MEDIA, "volume mute media and sound mute sound silent"),
            new Icon("videocam", R.drawable.ic_ck_videocam, Group.MEDIA, "videocam media and sound"),
            new Icon("videocam_off", R.drawable.ic_ck_videocam_off, Group.MEDIA, "videocam off media and sound"),
            new Icon("cast", R.drawable.ic_ck_cast, Group.MEDIA, "cast media and sound stream screen mirror"),
            new Icon("tv", R.drawable.ic_ck_tv, Group.MEDIA, "tv media and sound"),
            new Icon("window", R.drawable.ic_ck_window, Group.WINDOW, "window windows and layout windows start"),
            new Icon("apps", R.drawable.ic_ck_apps, Group.WINDOW, "apps windows and layout grid launcher"),
            new Icon("desktop_windows", R.drawable.ic_ck_desktop_windows, Group.WINDOW, "desktop windows windows and layout"),
            new Icon("monitor", R.drawable.ic_ck_monitor, Group.WINDOW, "monitor windows and layout"),
            new Icon("monitoring", R.drawable.ic_ck_monitoring, Group.WINDOW, "monitoring windows and layout"),
            new Icon("flip_to_front", R.drawable.ic_ck_flip_to_front, Group.WINDOW, "flip to front windows and layout"),
            new Icon("folder_open", R.drawable.ic_ck_folder_open, Group.WINDOW, "folder open windows and layout explorer files directory"),
            new Icon("terminal", R.drawable.ic_ck_terminal, Group.WINDOW, "terminal windows and layout command console shell cmd powershell"),
            new Icon("fullscreen", R.drawable.ic_ck_fullscreen, Group.WINDOW, "fullscreen windows and layout maximize full screen"),
            new Icon("open_in_full", R.drawable.ic_ck_open_in_full, Group.WINDOW, "open in full windows and layout"),
            new Icon("close_fullscreen", R.drawable.ic_ck_close_fullscreen, Group.WINDOW, "close fullscreen windows and layout"),
            new Icon("close", R.drawable.ic_ck_close, Group.WINDOW, "close windows and layout exit quit x cancel"),
            new Icon("tab", R.drawable.ic_ck_tab, Group.WINDOW, "tab windows and layout"),
            new Icon("tab_close", R.drawable.ic_ck_tab_close, Group.WINDOW, "tab close windows and layout"),
            new Icon("splitscreen", R.drawable.ic_ck_splitscreen, Group.WINDOW, "splitscreen windows and layout"),
            new Icon("picture_in_picture", R.drawable.ic_ck_picture_in_picture, Group.WINDOW, "picture in picture windows and layout"),
            new Icon("minimize", R.drawable.ic_ck_minimize, Group.WINDOW, "minimize windows and layout"),
            new Icon("crop_square", R.drawable.ic_ck_crop_square, Group.WINDOW, "crop square windows and layout"),
            new Icon("filter_none", R.drawable.ic_ck_filter_none, Group.WINDOW, "filter none windows and layout"),
            new Icon("grid_view", R.drawable.ic_ck_grid_view, Group.WINDOW, "grid view windows and layout"),
            new Icon("dock_to_left", R.drawable.ic_ck_dock_to_left, Group.WINDOW, "dock to left windows and layout"),
            new Icon("dock_to_right", R.drawable.ic_ck_dock_to_right, Group.WINDOW, "dock to right windows and layout"),
            new Icon("view_sidebar", R.drawable.ic_ck_view_sidebar, Group.WINDOW, "view sidebar windows and layout"),
            new Icon("view_agenda", R.drawable.ic_ck_view_agenda, Group.WINDOW, "view agenda windows and layout"),
            new Icon("view_column", R.drawable.ic_ck_view_column, Group.WINDOW, "view column windows and layout"),
            new Icon("web_asset", R.drawable.ic_ck_web_asset, Group.WINDOW, "web asset windows and layout"),
            new Icon("aspect_ratio", R.drawable.ic_ck_aspect_ratio, Group.WINDOW, "aspect ratio windows and layout"),
            new Icon("fit_screen", R.drawable.ic_ck_fit_screen, Group.WINDOW, "fit screen windows and layout"),
            new Icon("zoom_out_map", R.drawable.ic_ck_zoom_out_map, Group.WINDOW, "zoom out map windows and layout"),
            new Icon("zoom_in_map", R.drawable.ic_ck_zoom_in_map, Group.WINDOW, "zoom in map windows and layout"),
            new Icon("zoom_in", R.drawable.ic_ck_zoom_in, Group.WINDOW, "zoom in windows and layout"),
            new Icon("zoom_out", R.drawable.ic_ck_zoom_out, Group.WINDOW, "zoom out windows and layout"),
            new Icon("swap_horiz", R.drawable.ic_ck_swap_horiz, Group.WINDOW, "swap horiz windows and layout"),
            new Icon("web", R.drawable.ic_ck_web, Group.WINDOW, "web windows and layout"),
            new Icon("keyboard_command_key", R.drawable.ic_ck_keyboard_command_key, Group.KEYS, "keyboard command key keys and arrows"),
            new Icon("keyboard_tab", R.drawable.ic_ck_keyboard_tab, Group.KEYS, "keyboard tab keys and arrows"),
            new Icon("keyboard_return", R.drawable.ic_ck_keyboard_return, Group.KEYS, "keyboard return keys and arrows"),
            new Icon("space_bar", R.drawable.ic_ck_space_bar, Group.KEYS, "space bar keys and arrows"),
            new Icon("arrow_upward", R.drawable.ic_ck_arrow_upward, Group.KEYS, "arrow upward keys and arrows"),
            new Icon("arrow_downward", R.drawable.ic_ck_arrow_downward, Group.KEYS, "arrow downward keys and arrows"),
            new Icon("arrow_back", R.drawable.ic_ck_arrow_back, Group.KEYS, "arrow back keys and arrows"),
            new Icon("arrow_forward", R.drawable.ic_ck_arrow_forward, Group.KEYS, "arrow forward keys and arrows"),
            new Icon("keyboard_double_arrow_left", R.drawable.ic_ck_keyboard_double_arrow_left, Group.KEYS, "keyboard double arrow left keys and arrows"),
            new Icon("keyboard_double_arrow_right", R.drawable.ic_ck_keyboard_double_arrow_right, Group.KEYS, "keyboard double arrow right keys and arrows"),
            new Icon("keyboard_double_arrow_up", R.drawable.ic_ck_keyboard_double_arrow_up, Group.KEYS, "keyboard double arrow up keys and arrows"),
            new Icon("keyboard_double_arrow_down", R.drawable.ic_ck_keyboard_double_arrow_down, Group.KEYS, "keyboard double arrow down keys and arrows"),
            new Icon("first_page", R.drawable.ic_ck_first_page, Group.KEYS, "first page keys and arrows"),
            new Icon("last_page", R.drawable.ic_ck_last_page, Group.KEYS, "last page keys and arrows"),
            new Icon("backspace", R.drawable.ic_ck_backspace, Group.KEYS, "backspace keys and arrows"),
            new Icon("select_all", R.drawable.ic_ck_select_all, Group.KEYS, "select all keys and arrows"),
            new Icon("keyboard_alt", R.drawable.ic_ck_keyboard_alt, Group.KEYS, "keyboard alt keys and arrows"),
            new Icon("keyboard_hide", R.drawable.ic_ck_keyboard_hide, Group.KEYS, "keyboard hide keys and arrows"),
            new Icon("arrow_back_ios", R.drawable.ic_ck_arrow_back_ios, Group.KEYS, "arrow back ios keys and arrows"),
            new Icon("arrow_forward_ios", R.drawable.ic_ck_arrow_forward_ios, Group.KEYS, "arrow forward ios keys and arrows"),
            new Icon("content_copy", R.drawable.ic_ck_content_copy, Group.EDIT, "content copy edit and files copy duplicate"),
            new Icon("content_paste", R.drawable.ic_ck_content_paste, Group.EDIT, "content paste edit and files paste clipboard"),
            new Icon("content_cut", R.drawable.ic_ck_content_cut, Group.EDIT, "content cut edit and files cut scissors"),
            new Icon("content_paste_search", R.drawable.ic_ck_content_paste_search, Group.EDIT, "content paste search edit and files"),
            new Icon("undo", R.drawable.ic_ck_undo, Group.EDIT, "undo edit and files back revert"),
            new Icon("redo", R.drawable.ic_ck_redo, Group.EDIT, "redo edit and files forward repeat"),
            new Icon("save", R.drawable.ic_ck_save, Group.EDIT, "save edit and files disk store"),
            new Icon("search", R.drawable.ic_ck_search, Group.EDIT, "search edit and files find magnify"),
            new Icon("delete", R.drawable.ic_ck_delete, Group.EDIT, "delete edit and files trash remove bin"),
            new Icon("edit", R.drawable.ic_ck_edit, Group.EDIT, "edit edit and files"),
            new Icon("find_replace", R.drawable.ic_ck_find_replace, Group.EDIT, "find replace edit and files"),
            new Icon("spellcheck", R.drawable.ic_ck_spellcheck, Group.EDIT, "spellcheck edit and files"),
            new Icon("format_bold", R.drawable.ic_ck_format_bold, Group.EDIT, "format bold edit and files"),
            new Icon("format_italic", R.drawable.ic_ck_format_italic, Group.EDIT, "format italic edit and files"),
            new Icon("note_add", R.drawable.ic_ck_note_add, Group.EDIT, "note add edit and files"),
            new Icon("description", R.drawable.ic_ck_description, Group.EDIT, "description edit and files"),
            new Icon("print", R.drawable.ic_ck_print, Group.EDIT, "print edit and files"),
            new Icon("drive_file_rename_outline", R.drawable.ic_ck_drive_file_rename_outline, Group.EDIT, "drive file rename outline edit and files"),
            new Icon("text_fields", R.drawable.ic_ck_text_fields, Group.EDIT, "text fields edit and files"),
            new Icon("copy_all", R.drawable.ic_ck_copy_all, Group.EDIT, "copy all edit and files"),
            new Icon("library_add", R.drawable.ic_ck_library_add, Group.EDIT, "library add edit and files"),
            new Icon("delete_forever", R.drawable.ic_ck_delete_forever, Group.EDIT, "delete forever edit and files"),
            new Icon("folder", R.drawable.ic_ck_folder, Group.EDIT, "folder edit and files"),
            new Icon("upload_file", R.drawable.ic_ck_upload_file, Group.EDIT, "upload file edit and files"),
            new Icon("lock", R.drawable.ic_ck_lock, Group.SYSTEM, "lock system and power lock screen secure windows l"),
            new Icon("lock_open", R.drawable.ic_ck_lock_open, Group.SYSTEM, "lock open system and power unlock open"),
            new Icon("power_settings_new", R.drawable.ic_ck_power_settings_new, Group.SYSTEM, "power settings new system and power power shutdown off sleep"),
            new Icon("restart_alt", R.drawable.ic_ck_restart_alt, Group.SYSTEM, "restart alt system and power restart reboot reset"),
            new Icon("refresh", R.drawable.ic_ck_refresh, Group.SYSTEM, "refresh system and power"),
            new Icon("bedtime", R.drawable.ic_ck_bedtime, Group.SYSTEM, "bedtime system and power sleep night"),
            new Icon("light_mode", R.drawable.ic_ck_light_mode, Group.SYSTEM, "light mode system and power sun bright day"),
            new Icon("dark_mode", R.drawable.ic_ck_dark_mode, Group.SYSTEM, "dark mode system and power moon night"),
            new Icon("brightness_high", R.drawable.ic_ck_brightness_high, Group.SYSTEM, "brightness high system and power"),
            new Icon("brightness_low", R.drawable.ic_ck_brightness_low, Group.SYSTEM, "brightness low system and power"),
            new Icon("brightness_medium", R.drawable.ic_ck_brightness_medium, Group.SYSTEM, "brightness medium system and power"),
            new Icon("display_settings", R.drawable.ic_ck_display_settings, Group.SYSTEM, "display settings system and power"),
            new Icon("hdr_on", R.drawable.ic_ck_hdr_on, Group.SYSTEM, "hdr on system and power"),
            new Icon("hdr_off", R.drawable.ic_ck_hdr_off, Group.SYSTEM, "hdr off system and power"),
            new Icon("battery_full", R.drawable.ic_ck_battery_full, Group.SYSTEM, "battery full system and power"),
            new Icon("wifi", R.drawable.ic_ck_wifi, Group.SYSTEM, "wifi system and power"),
            new Icon("wifi_off", R.drawable.ic_ck_wifi_off, Group.SYSTEM, "wifi off system and power"),
            new Icon("bluetooth", R.drawable.ic_ck_bluetooth, Group.SYSTEM, "bluetooth system and power"),
            new Icon("airplanemode_active", R.drawable.ic_ck_airplanemode_active, Group.SYSTEM, "airplanemode active system and power"),
            new Icon("memory", R.drawable.ic_ck_memory, Group.SYSTEM, "memory system and power"),
            new Icon("developer_board", R.drawable.ic_ck_developer_board, Group.SYSTEM, "developer board system and power"),
            new Icon("key", R.drawable.ic_ck_key, Group.SECURITY, "key security password lock"),
            new Icon("shield", R.drawable.ic_ck_shield, Group.SECURITY, "shield security"),
            new Icon("security", R.drawable.ic_ck_security, Group.SECURITY, "security security"),
            new Icon("visibility", R.drawable.ic_ck_visibility, Group.SECURITY, "visibility security"),
            new Icon("visibility_off", R.drawable.ic_ck_visibility_off, Group.SECURITY, "visibility off security"),
            new Icon("fingerprint", R.drawable.ic_ck_fingerprint, Group.SECURITY, "fingerprint security biometric"),
            new Icon("password", R.drawable.ic_ck_password, Group.SECURITY, "password security"),
            new Icon("vpn_key", R.drawable.ic_ck_vpn_key, Group.SECURITY, "vpn key security"),
            new Icon("public", R.drawable.ic_ck_public, Group.WEB, "public web and sharing globe web world"),
            new Icon("open_in_new", R.drawable.ic_ck_open_in_new, Group.WEB, "open in new web and sharing"),
            new Icon("link", R.drawable.ic_ck_link, Group.WEB, "link web and sharing"),
            new Icon("download", R.drawable.ic_ck_download, Group.WEB, "download web and sharing"),
            new Icon("upload", R.drawable.ic_ck_upload, Group.WEB, "upload web and sharing"),
            new Icon("bookmark_add", R.drawable.ic_ck_bookmark_add, Group.WEB, "bookmark add web and sharing"),
            new Icon("add_link", R.drawable.ic_ck_add_link, Group.WEB, "add link web and sharing"),
            new Icon("travel_explore", R.drawable.ic_ck_travel_explore, Group.WEB, "travel explore web and sharing"),
            new Icon("cloud", R.drawable.ic_ck_cloud, Group.WEB, "cloud web and sharing"),
            new Icon("share", R.drawable.ic_ck_share, Group.WEB, "share web and sharing send"),
            new Icon("qr_code", R.drawable.ic_ck_qr_code, Group.WEB, "qr code web and sharing"),
            new Icon("screenshot_monitor", R.drawable.ic_ck_screenshot_monitor, Group.CAPTURE, "screenshot monitor capture screenshot screen capture print"),
            new Icon("screenshot_region", R.drawable.ic_ck_screenshot_region, Group.CAPTURE, "screenshot region capture snip region crop capture"),
            new Icon("photo_camera", R.drawable.ic_ck_photo_camera, Group.CAPTURE, "photo camera capture photo camera picture"),
            new Icon("fiber_manual_record", R.drawable.ic_ck_fiber_manual_record, Group.CAPTURE, "fiber manual record capture record dot red capture"),
            new Icon("crop", R.drawable.ic_ck_crop, Group.CAPTURE, "crop capture"),
            new Icon("screenshot", R.drawable.ic_ck_screenshot, Group.CAPTURE, "screenshot capture"),
            new Icon("video_camera_front", R.drawable.ic_ck_video_camera_front, Group.CAPTURE, "video camera front capture"),
            new Icon("image", R.drawable.ic_ck_image, Group.CAPTURE, "image capture"),
            new Icon("mouse", R.drawable.ic_ck_mouse, Group.INPUT, "mouse input and games pointer click cursor"),
            new Icon("sports_esports", R.drawable.ic_ck_sports_esports, Group.INPUT, "sports esports input and games game controller gamepad play"),
            new Icon("videogame_asset", R.drawable.ic_ck_videogame_asset, Group.INPUT, "videogame asset input and games"),
            new Icon("gamepad", R.drawable.ic_ck_gamepad, Group.INPUT, "gamepad input and games game controller play"),
            new Icon("touch_app", R.drawable.ic_ck_touch_app, Group.INPUT, "touch app input and games tap finger touch"),
            new Icon("ads_click", R.drawable.ic_ck_ads_click, Group.INPUT, "ads click input and games"),
            new Icon("pan_tool", R.drawable.ic_ck_pan_tool, Group.INPUT, "pan tool input and games"),
            new Icon("swipe", R.drawable.ic_ck_swipe, Group.INPUT, "swipe input and games"),
            new Icon("mood", R.drawable.ic_ck_mood, Group.INPUT, "mood input and games"),
            new Icon("trackpad_input", R.drawable.ic_ck_trackpad_input, Group.INPUT, "trackpad input input and games"),
            new Icon("left_click", R.drawable.ic_ck_left_click, Group.INPUT, "left click input and games"),
            new Icon("right_click", R.drawable.ic_ck_right_click, Group.INPUT, "right click input and games"),
            new Icon("laptop_windows", R.drawable.ic_ck_laptop_windows, Group.INPUT, "laptop windows input and games"),
            new Icon("desktop_mac", R.drawable.ic_ck_desktop_mac, Group.INPUT, "desktop mac input and games"),
            new Icon("smartphone", R.drawable.ic_ck_smartphone, Group.INPUT, "smartphone input and games"),
            new Icon("tablet", R.drawable.ic_ck_tablet, Group.INPUT, "tablet input and games"),
            new Icon("devices", R.drawable.ic_ck_devices, Group.INPUT, "devices input and games")
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

    // Icons whose ID or search words contain every word of the query. An empty query returns the whole catalogue in its groups.
    public static List<Icon> search(String query) {
        String trimmed = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        if (trimmed.isEmpty()) {
            return ICONS;
        }
        String[] terms = trimmed.split("\\s+");
        List<Icon> found = new ArrayList<>();
        for (Icon icon : ICONS) {
            boolean all = true;
            for (String term : terms) {
                all &= icon.keywords.contains(term);
            }
            if (all) {
                found.add(icon);
            }
        }
        return found;
    }
}
