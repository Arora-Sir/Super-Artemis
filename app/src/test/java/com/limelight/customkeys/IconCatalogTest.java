package com.limelight.customkeys;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import com.limelight.R;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// NOTE: Icon IDs are saved in custom keys and backups, so the catalogue may grow but an ID is never renamed, dropped or reused.
public class IconCatalogTest {
    // Every ID that shipped before the catalogue got sections and search
    private static final List<String> ORIGINAL_IDS = Arrays.asList(
            "keyboard", "bolt", "star", "play_arrow", "pause", "skip_next", "skip_previous", "volume_up", "volume_off", "screenshot_monitor",
            "videocam", "save", "content_copy", "content_paste", "undo", "redo", "search", "home", "settings", "close", "fullscreen",
            "desktop_windows", "sports_esports", "terminal", "language", "lock", "refresh", "swap_horiz", "keyboard_command_key", "window",
            "apps", "tab", "keyboard_tab", "keyboard_return", "space_bar", "arrow_upward", "arrow_downward", "arrow_back", "arrow_forward",
            "keyboard_double_arrow_left", "keyboard_double_arrow_right", "monitor", "open_in_full", "close_fullscreen", "hdr_on", "mouse",
            "videogame_asset", "mic", "headphones", "music_note", "photo_camera", "zoom_in", "zoom_out", "brightness_high", "chat", "translate",
            "timer", "bookmark", "rocket_launch", "restart_alt", "power_settings_new", "select_all", "content_cut", "content_paste_search",
            "screenshot_region", "monitoring", "tab_close", "history", "folder_open", "first_page", "last_page", "keyboard_double_arrow_up",
            "keyboard_double_arrow_down", "backspace", "flip_to_front");

    private static Set<String> ids() {
        Set<String> ids = new HashSet<>();
        for (IconCatalog.Icon icon : IconCatalog.ICONS) {
            ids.add(icon.id);
        }
        return ids;
    }

    @Test
    public void noShippedIdIsEverRemoved() {
        assertTrue(ids().containsAll(ORIGINAL_IDS));
        assertEquals(75, ORIGINAL_IDS.size());
    }

    @Test
    public void idsAreUniqueAndEveryIconHasADrawableGroupAndWords() {
        Set<String> seen = new HashSet<>();
        for (IconCatalog.Icon icon : IconCatalog.ICONS) {
            assertTrue("Duplicate ID " + icon.id, seen.add(icon.id));
            assertNotEquals("No drawable for " + icon.id, 0, icon.drawableRes);
            assertTrue(icon.group != null);
            assertFalse("No search words for " + icon.id, icon.keywords.trim().isEmpty());
        }
        assertTrue("Expected a large catalogue, got " + IconCatalog.ICONS.size(), IconCatalog.ICONS.size() >= 200);
    }

    @Test
    public void everyGroupHasIconsAndAGroupsIconsStayTogether() {
        Set<IconCatalog.Group> used = new HashSet<>();
        IconCatalog.Group previous = null;
        for (IconCatalog.Icon icon : IconCatalog.ICONS) {
            if (icon.group != previous) {
                assertTrue("Group " + icon.group + " is split into two sections", used.add(icon.group));
                previous = icon.group;
            }
        }
        assertEquals(IconCatalog.Group.values().length, used.size());
    }

    // NOTE: Some Material Symbols come with a 24 unit viewport. Saved with the usual 960 viewport they draw as a dot, which showed as blank cells in the picker.
    @Test
    public void everyIconFitsItsViewport() throws Exception {
        java.io.File dir = new java.io.File("src/main/res/drawable");
        if (!dir.exists()) {
            dir = new java.io.File("app/src/main/res/drawable");
        }
        java.util.regex.Pattern number = java.util.regex.Pattern.compile("-?[0-9]+[.]?[0-9]*");
        for (IconCatalog.Icon icon : IconCatalog.ICONS) {
            java.io.File file = new java.io.File(dir, "ic_ck_" + icon.id + ".xml");
            String xml = new String(java.nio.file.Files.readAllBytes(file.toPath()), java.nio.charset.StandardCharsets.UTF_8);
            java.util.regex.Matcher viewport = java.util.regex.Pattern.compile("viewportWidth=\"([0-9.]+)\"").matcher(xml);
            assertTrue("No viewport in " + file, viewport.find());
            double size = Double.parseDouble(viewport.group(1));
            double largest = 0;
            java.util.regex.Matcher path = java.util.regex.Pattern.compile("pathData=\"([^\"]*)\"").matcher(xml);
            while (path.find()) {
                java.util.regex.Matcher values = number.matcher(path.group(1));
                while (values.find()) {
                    largest = Math.max(largest, Math.abs(Double.parseDouble(values.group())));
                }
            }
            assertTrue(icon.id + " draws up to " + largest + " in a viewport of " + size, largest > size * 0.5);
        }
    }

    @Test
    public void searchFindsRelatedIconsByNameOrWord() {
        Set<String> mic = new HashSet<>();
        for (IconCatalog.Icon icon : IconCatalog.search("mic")) {
            mic.add(icon.id);
        }
        assertTrue(mic.containsAll(Arrays.asList("mic", "mic_off", "keyboard_voice", "settings_voice")));

        Set<String> mute = new HashSet<>();
        for (IconCatalog.Icon icon : IconCatalog.search("mute")) {
            mute.add(icon.id);
        }
        assertTrue(mute.containsAll(Arrays.asList("mic_off", "volume_off", "volume_mute")));

        assertEquals(IconCatalog.ICONS.size(), IconCatalog.search("  ").size());
        assertTrue(IconCatalog.search("zzzzqq").isEmpty());
        assertEquals(R.drawable.ic_ck_mic, IconCatalog.drawableFor("mic"));
        assertEquals(R.drawable.ic_ck_keyboard, IconCatalog.drawableFor("unknown_id"));
    }

    @Test
    public void rowsPutATitleBeforeEachGroupAndAMessageWhenNothingMatches() {
        List<Object> rows = IconPicker.rowsFor(IconCatalog.ICONS, "none");
        assertTrue(rows.get(0) instanceof IconCatalog.Group);
        List<Object> empty = IconPicker.rowsFor(IconCatalog.search("zzzzqq"), "none");
        assertEquals(1, empty.size());
        assertEquals("none", empty.get(0));
    }
}
