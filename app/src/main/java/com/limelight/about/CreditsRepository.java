package com.limelight.about;

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
import java.util.List;

// NOTE: Reads res/raw/credits.json, the same file the README credits and the release notes Thanks section are rendered from.
// NOTE: The app, the README and the release notes therefore always credit the same people for the same work.
public final class CreditsRepository {
    public static final class Link {
        public final String label;
        public final String url;

        Link(String label, String url) {
            this.label = label;
            this.url = url;
        }
    }

    public static final class Entry {
        public final String feature;
        public final String role;
        public final List<String> handles;
        public final List<Link> links;

        Entry(String feature, String role, List<String> handles, List<Link> links) {
            this.feature = feature;
            this.role = role;
            this.handles = Collections.unmodifiableList(handles);
            this.links = Collections.unmodifiableList(links);
        }
    }

    private CreditsRepository() {
    }

    public static List<Entry> load(Context context) throws IOException, JSONException {
        try (InputStream in = context.getResources().openRawResource(R.raw.credits)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            return parse(new String(out.toByteArray(), StandardCharsets.UTF_8));
        }
    }

    // Role IDs become their display names from the file's own roles table
    static List<Entry> parse(String json) throws JSONException {
        JSONObject root = new JSONObject(json);
        JSONObject roles = root.getJSONObject("roles");
        JSONArray entries = root.getJSONArray("entries");
        List<Entry> result = new ArrayList<>();
        for (int i = 0; i < entries.length(); i++) {
            JSONObject entry = entries.getJSONObject(i);
            String role = entry.getString("role");
            List<String> handles = new ArrayList<>();
            JSONArray people = entry.optJSONArray("people");
            for (int p = 0; people != null && p < people.length(); p++) {
                handles.add(people.getJSONObject(p).getString("handle"));
            }
            List<Link> links = new ArrayList<>();
            JSONArray linkArray = entry.optJSONArray("links");
            for (int l = 0; linkArray != null && l < linkArray.length(); l++) {
                JSONObject link = linkArray.getJSONObject(l);
                links.add(new Link(link.getString("label"), link.getString("url")));
            }
            result.add(new Entry(entry.getString("feature"), roles.optString(role, role), handles, links));
        }
        return result;
    }
}
