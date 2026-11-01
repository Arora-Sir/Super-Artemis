Confirmed and reproduced.

### Root cause
In `UiHelper.setLocale()`, when the in-app language preference was set to Default on Android 13 and newer, the method called `LocaleManager.getSystemLocales()` directly. It never inspected `LocaleManager.getApplicationLocales()`. As a result, when a user selected a per-app language in Android system settings (`Settings > System > Languages > App languages`), Super Artemis overrode that choice with the primary system locale.

### Fix
`UiHelper.setLocale()` now checks `getApplicationLocales()` first. If a per-app locale is configured, that locale is applied to the configuration. The method falls back to `getSystemLocales()` only when no per-app locale has been set.

The fix has been verified against our test suite. It is integrated cleanly in pull request #8, and merging PR #8 will close this issue.
