package com.bytehamster.lib.preferencesearch;

import android.content.Context;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import org.xmlpull.v1.XmlPullParser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

// Super Artemis patch: exact search returns every match, ranked by where it matched, and also matches everyday synonyms (see README.md)
class PreferenceParser {
    private static final String NS_ANDROID = "http://schemas.android.com/apk/res/android";
    private static final String NS_SEARCH = "http://schemas.android.com/apk/com.bytehamster.lib.preferencesearch";
    private static final List<String> BLACKLIST = Arrays.asList(SearchPreference.class.getName(), "PreferenceCategory");
    private static final List<String> CONTAINERS = Arrays.asList("PreferenceCategory", "PreferenceScreen");
    private Context context;
    private ArrayList<PreferenceItem> allEntries = new ArrayList<>();

    PreferenceParser(Context context) {
        this.context = context;
    }

    void addResourceFile(SearchConfiguration.SearchIndexItem item) {
        allEntries.addAll(parseFile(item));
    }

    void addPreferenceItems(ArrayList<PreferenceItem> preferenceItems) {
        allEntries.addAll(preferenceItems);
    }

    private ArrayList<PreferenceItem> parseFile(SearchConfiguration.SearchIndexItem item) {
        java.util.ArrayList<PreferenceItem> results = new ArrayList<>();
        XmlPullParser xpp = context.getResources().getXml(item.getResId());
        List<String> bannedKeys = item.getSearchConfiguration().getBannedKeys();

        try {
            xpp.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
            xpp.setFeature(XmlPullParser.FEATURE_REPORT_NAMESPACE_ATTRIBUTES, true);
            ArrayList<String> breadcrumbs = new ArrayList<>();
            ArrayList<String> keyBreadcrumbs = new ArrayList<>();
            if (!TextUtils.isEmpty(item.getBreadcrumb())) {
                breadcrumbs.add(item.getBreadcrumb());
            }
            while (xpp.getEventType() != XmlPullParser.END_DOCUMENT) {
                if (xpp.getEventType() == XmlPullParser.START_TAG) {
                    PreferenceItem result = parseSearchResult(xpp);
                    result.resId = item.getResId();

                    if (!BLACKLIST.contains(xpp.getName())
                            && result.hasData()
                            && !"true".equals(getAttribute(xpp, NS_SEARCH, "ignore"))
                            && !bannedKeys.contains(result.key)) {
                        result.breadcrumbs = joinBreadcrumbs(breadcrumbs);
                        result.keyBreadcrumbs = cleanupKeyBreadcrumbs(keyBreadcrumbs);
                        results.add(result);
                    }
                    if (CONTAINERS.contains(xpp.getName())) {
                        breadcrumbs.add(result.title == null ? "" : result.title);
                    }
                    if (xpp.getName().equals("PreferenceScreen")) {
                        keyBreadcrumbs.add(getAttribute(xpp, "key"));
                    }
                } else if (xpp.getEventType() == XmlPullParser.END_TAG && CONTAINERS.contains(xpp.getName())) {
                    breadcrumbs.remove(breadcrumbs.size() - 1);
                    if (xpp.getName().equals("PreferenceScreen")) {
                        keyBreadcrumbs.remove(keyBreadcrumbs.size() - 1);
                    }
                }

                xpp.next();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return results;
    }

    private ArrayList<String> cleanupKeyBreadcrumbs(ArrayList<String> keyBreadcrumbs) {
        ArrayList<String> result = new ArrayList<>();
        for (String keyBreadcrumb : keyBreadcrumbs) {
            if (keyBreadcrumb != null) {
                result.add(keyBreadcrumb);
            }
        }
        return result;
    }

    private String joinBreadcrumbs(ArrayList<String> breadcrumbs) {
        String result = "";
        for (String crumb : breadcrumbs) {
            if (!TextUtils.isEmpty(crumb)) {
                result = Breadcrumb.concat(result, crumb);
            }
        }
        return result;
    }

    private String getAttribute(XmlPullParser xpp, @Nullable String namespace, @NonNull String attribute) {
        for (int i = 0; i < xpp.getAttributeCount(); i++) {
            if (attribute.equals(xpp.getAttributeName(i)) &&
                    (namespace == null || namespace.equals(xpp.getAttributeNamespace(i)))) {
                return xpp.getAttributeValue(i);
            }
        }
        return null;
    }

    private String getAttribute(XmlPullParser xpp, @NonNull String attribute) {
        if (hasAttribute(xpp, NS_SEARCH, attribute)) {
            return getAttribute(xpp, NS_SEARCH, attribute);
        } else {
            return getAttribute(xpp, NS_ANDROID, attribute);
        }
    }

    private boolean hasAttribute(XmlPullParser xpp, @Nullable String namespace, @NonNull String attribute) {
        return getAttribute(xpp, namespace, attribute) != null;
    }

    private PreferenceItem parseSearchResult(XmlPullParser xpp) {
        PreferenceItem result = new PreferenceItem();
        result.title = readString(getAttribute(xpp, "title"));
        result.summary = readString(getAttribute(xpp, "summary"));
        result.key = readString(getAttribute(xpp, "key"));
        result.entries = readStringArray(getAttribute(xpp, "entries"));
        result.keywords = readString(getAttribute(xpp, NS_SEARCH, "keywords"));
        return result;
    }

    private String readStringArray(@Nullable String s) {
        if (s == null) {
            return null;
        }
        if (s.startsWith("@")) {
            try {
                int id = Integer.parseInt(s.substring(1));
                String[] elements = context.getResources().getStringArray(id);
                return TextUtils.join(",", elements);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return s;
    }

    private String readString(@Nullable String s) {
        if (s == null) {
            return null;
        }
        if (s.startsWith("@")) {
            try {
                int id = Integer.parseInt(s.substring(1));
                return context.getString(id);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return s;
    }

    List<PreferenceItem> searchFor(final String keyword, boolean fuzzy) {
        if (TextUtils.isEmpty(keyword) || TextUtils.isEmpty(keyword.trim())) {
            return new ArrayList<>();
        }
        ArrayList<PreferenceItem> results = new ArrayList<>();
        if (fuzzy) {
            for (PreferenceItem item : allEntries) {
                if (item.matchesFuzzy(keyword)) {
                    results.add(item);
                }
            }
            Collections.sort(results, new Comparator<PreferenceItem>() {
                @Override
                public int compare(PreferenceItem i1, PreferenceItem i2) {
                    return floatCompare(i2.getScore(keyword), i1.getScore(keyword));
                }
            });
            return results;
        }

        // NOTE: The original returned only the 10 best fuzzy scores, so a broad word like "mouse" hid settings that match only by their section.
        // NOTE: Every match is returned now: title matches first, then section matches, then summary, list entries and keywords, then synonym matches.
        // NOTE: Within a tier results keep the order of the settings screen, because Collections.sort is stable.
        final String query = keyword.trim().toLowerCase(Locale.getDefault());
        final List<String> synonyms = SearchConfiguration.synonymsFor(query);
        final java.util.Map<PreferenceItem, Integer> tiers = new java.util.HashMap<>();
        for (PreferenceItem item : allEntries) {
            int tier = item.matchTier(query, synonyms);
            if (tier >= 0) {
                tiers.put(item, tier);
                results.add(item);
            }
        }
        Collections.sort(results, new Comparator<PreferenceItem>() {
            @Override
            public int compare(PreferenceItem i1, PreferenceItem i2) {
                return Integer.compare(tiers.get(i1), tiers.get(i2));
            }
        });
        return results;
    }

    @SuppressWarnings("UseCompareMethod")
    private static int floatCompare(float x, float y) {
        return (x < y) ? -1 : ((x == y) ? 0 : 1);
    }
}
