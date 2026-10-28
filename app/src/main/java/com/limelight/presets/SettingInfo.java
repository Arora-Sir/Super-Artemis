package com.limelight.presets;

import android.content.Context;
import android.content.res.XmlResourceParser;
import android.util.AttributeSet;
import android.util.Xml;

import com.limelight.R;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

// NOTE: What the preset screens need to say about a setting in words: its title from the real settings screen, and its choices when it is a list.
// NOTE: It reads res/xml/preferences.xml directly, so a title or a choice name is never written twice and cannot drift from what Settings shows.
final class SettingInfo {
    private static final String ANDROID = "http://schemas.android.com/apk/res/android";

    final String key;
    final String tag;
    final CharSequence title;
    final CharSequence[] entries;
    final CharSequence[] entryValues;
    final String defaultValue;

    private SettingInfo(String key, String tag, CharSequence title, CharSequence[] entries, CharSequence[] entryValues, String defaultValue) {
        this.key = key;
        this.tag = tag;
        this.title = title;
        this.entries = entries;
        this.entryValues = entryValues;
        this.defaultValue = defaultValue;
    }

    boolean isSwitch() {
        return tag.endsWith("CheckBoxPreference") || tag.endsWith("SwitchPreference") || tag.endsWith("SwitchPreferenceCompat");
    }

    // The choice name for a stored value of a list setting, or the value itself
    String choiceFor(String value) {
        if (entries != null && entryValues != null) {
            for (int i = 0; i < Math.min(entries.length, entryValues.length); i++) {
                if (entryValues[i].toString().equals(value)) {
                    return entries[i].toString();
                }
            }
        }
        return value;
    }

    static Map<String, SettingInfo> read(Context context) {
        Map<String, SettingInfo> infos = new HashMap<>();
        try (XmlResourceParser parser = context.getResources().getXml(R.xml.preferences)) {
            AttributeSet attrs = Xml.asAttributeSet(parser);
            int event = parser.getEventType();
            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG) {
                    String key = attrs.getAttributeValue(ANDROID, "key");
                    if (key != null) {
                        infos.put(key, new SettingInfo(key, parser.getName(), text(context, attrs, "title"),
                                array(context, attrs, "entries"), array(context, attrs, "entryValues"),
                                attrs.getAttributeValue(ANDROID, "defaultValue")));
                    }
                }
                event = parser.next();
            }
        }
        catch (XmlPullParserException | IOException e) {
            // The settings file ships inside the app, so this only happens if it is broken, and the screens then show the raw keys
        }
        return infos;
    }

    private static CharSequence text(Context context, AttributeSet attrs, String name) {
        int id = attrs.getAttributeResourceValue(ANDROID, name, 0);
        return id != 0 ? context.getString(id) : attrs.getAttributeValue(ANDROID, name);
    }

    private static CharSequence[] array(Context context, AttributeSet attrs, String name) {
        int id = attrs.getAttributeResourceValue(ANDROID, name, 0);
        return id != 0 ? context.getResources().getTextArray(id) : null;
    }
}
