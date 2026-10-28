# SearchPreference (vendored)

This is [SearchPreference](https://github.com/ByteHamster/SearchPreference) by ByteHamster, MIT licensed (see `LICENSE`), copied from tag `v2.5.1`, the version Super Artemis used from JitPack. It powers the settings search bar.

## Why it is vendored

The library returns at most 10 results, ranked by a greedy fuzzy score that favors settings whose title starts with the query. A broad word such as "mouse" matches more than 10 settings, so settings that match only through their section name (Touchscreen Mode in "Mouse and touch") never showed. The limit is a private constant in every release up to 2.7.3, so it cannot be changed from the app.

## Changes from v2.5.1

- `PreferenceParser.searchFor` returns every exact match, ranked by where it matched (title, then section, then summary, list entries and keywords, then the same places through a synonym), and keeps the order of the settings screen within a rank. Fuzzy search still ranks by the original score, without the limit.
- `PreferenceItem.matchTier` decides that rank.
- `SearchConfiguration.setSynonyms` takes groups of words that find the same settings. The app sets them in `StreamSettings.SEARCH_SYNONYMS`.
- The per-attribute debug logging in `PreferenceParser` is removed, since it wrote a log line for every XML attribute each time Settings opened.
- `SearchPreferenceResult.closeSearchPage` pops the search page from the back stack. The page is added with `addToBackStack`, and only removing the fragment left one stale entry per search, so Back needed one extra press after every search result (BUG-044).
- `AndroidManifest.xml` has no `package` attribute and `build.gradle` sets the namespace, as Android Gradle Plugin 8 requires.
