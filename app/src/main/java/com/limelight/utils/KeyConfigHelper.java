package com.limelight.utils;

import com.google.gson.Gson;

import java.util.List;
import java.util.ArrayList;

public class KeyConfigHelper {
    public static class ShortcutFile {
        public List<Shortcut> data;
        
        // Default constructor for Gson
        public ShortcutFile() {
            this.data = new ArrayList<>();
        }
        public ShortcutFile(List<Shortcut> data) {
            this.data = data;
        }
    }

    // One step of a special key: type is combo, text, wait, hold, release, repeat or action
    public static class Step {
        public String type;
        public List<String> keys;
        public String text;
        public Integer amount;
    }

    public static class Shortcut {
        public String id;
        public String name;
        public boolean sticky = false;  // Default to false
        public List<String> keys;
        // Optional icon ID from customkeys.IconCatalog, absent in files written by older versions
        public String icon;
        // Optional steps run one after another. Absent for a plain shortcut, and older versions read only keys.
        public List<Step> steps;
        // Phone-side hotkey as virtual key codes ("0xA2", "0x31"), and whether the key also reaches the PC. Older versions ignore both.
        public List<String> hotkey;
        public Boolean hotkeyToPc;

        // Default constructor for Gson
        public Shortcut() {
            this.keys = new ArrayList<>();
        }
        
        public Shortcut(String id, String name, boolean sticky, List<String> keys) {
            this.id = id;
            this.name = name;
            this.sticky = sticky;
            this.keys = keys;
        }
    }

    public static ShortcutFile parseShortcutFile(String json) {
        Gson gson = new Gson();
        return gson.fromJson(json, ShortcutFile.class);
    }
}
