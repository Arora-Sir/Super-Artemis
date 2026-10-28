package com.limelight.about;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// NOTE: Asks GitHub for the newest release when the user taps "Check for updates", and never at any other time.
// NOTE: The request is one anonymous GET with a plain User-Agent. It carries no account, device ID, app version or other tracking, and nothing is stored from the answer.
// NOTE: The parsing and the version comparison are pure and tested, and the network call sits behind Fetcher so a test never goes online.
public final class UpdateChecker {
    public static final String API_URL = "https://api.github.com/repos/Arora-Sir/Super-Artemis/releases/latest";
    public static final String PREF_CONSENT = "update_check_consented";
    private static final int TIMEOUT_MS = 10_000;
    // A release answer is a few kilobytes, so anything larger is not what was asked for
    private static final int MAX_BYTES = 512 * 1024;

    public enum Status {
        UP_TO_DATE, UPDATE_AVAILABLE, NO_RELEASE, FAILED
    }

    public static final class Release {
        public final String tag;
        public final String name;
        public final String notes;
        public final String pageUrl;

        Release(String tag, String name, String notes, String pageUrl) {
            this.tag = tag;
            this.name = name;
            this.notes = notes;
            this.pageUrl = pageUrl;
        }
    }

    public static final class Result {
        public final Status status;
        public final Release release;

        Result(Status status, Release release) {
            this.status = status;
            this.release = release;
        }
    }

    public interface Fetcher {
        String fetch(String url) throws IOException;
    }

    public interface Callback {
        void onResult(Result result);
    }

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "UpdateCheck");
        thread.setDaemon(true);
        return thread;
    });

    private UpdateChecker() {
    }

    // Runs the check off the main thread and reports on the main thread
    public static void check(String currentVersion, Callback callback) {
        check(currentVersion, UpdateChecker::download, callback);
    }

    static void check(String currentVersion, Fetcher fetcher, Callback callback) {
        Handler main = new Handler(Looper.getMainLooper());
        EXECUTOR.execute(() -> {
            Result result;
            try {
                result = evaluate(fetcher.fetch(API_URL), currentVersion);
            }
            catch (NoReleaseException e) {
                result = new Result(Status.NO_RELEASE, null);
            }
            catch (IOException | RuntimeException e) {
                result = new Result(Status.FAILED, null);
            }
            Result done = result;
            main.post(() -> callback.onResult(done));
        });
    }

    // GitHub answers 404 for a repository that has no published release yet
    static final class NoReleaseException extends IOException {
    }

    static String download(String address) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(address).openConnection();
        try {
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(TIMEOUT_MS);
            connection.setReadTimeout(TIMEOUT_MS);
            connection.setUseCaches(false);
            connection.setRequestProperty("Accept", "application/vnd.github+json");
            connection.setRequestProperty("User-Agent", "SuperArtemis");
            int code = connection.getResponseCode();
            if (code == HttpURLConnection.HTTP_NOT_FOUND) {
                throw new NoReleaseException();
            }
            if (code != HttpURLConnection.HTTP_OK) {
                throw new IOException("GitHub answered " + code);
            }
            try (InputStream in = connection.getInputStream()) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) > 0) {
                    out.write(buffer, 0, read);
                    if (out.size() > MAX_BYTES) {
                        throw new IOException("The answer is too large");
                    }
                }
                return out.toString(StandardCharsets.UTF_8.name());
            }
        }
        finally {
            connection.disconnect();
        }
    }

    // Reads the "latest release" answer and compares its tag with the installed version
    static Result evaluate(String json, String currentVersion) throws IOException {
        JSONObject object;
        try {
            object = new JSONObject(json);
        }
        catch (JSONException e) {
            throw new IOException("Unreadable answer", e);
        }
        if (object.optBoolean("draft") || object.optBoolean("prerelease")) {
            return new Result(Status.NO_RELEASE, null);
        }
        String tag = object.optString("tag_name", "");
        String page = object.optString("html_url", "");
        if (tag.isEmpty()) {
            return new Result(Status.NO_RELEASE, null);
        }
        // Only a link back to this project is ever opened, whatever the answer says
        if (!page.startsWith(Links.RELEASES)) {
            page = Links.RELEASES;
        }
        Release release = new Release(tag, object.optString("name", tag), object.optString("body", ""), page);
        return new Result(isNewer(tag, currentVersion) ? Status.UPDATE_AVAILABLE : Status.UP_TO_DATE, release);
    }

    // "v1.2.0" is newer than "1.1.9". Text after the numbers, such as "-beta", is ignored.
    static boolean isNewer(String tag, String current) {
        int[] latest = numbers(tag);
        int[] installed = numbers(current);
        for (int i = 0; i < Math.max(latest.length, installed.length); i++) {
            int a = i < latest.length ? latest[i] : 0;
            int b = i < installed.length ? installed[i] : 0;
            if (a != b) {
                return a > b;
            }
        }
        return false;
    }

    private static int[] numbers(String version) {
        String cleaned = version == null ? "" : version.trim();
        if (cleaned.startsWith("v") || cleaned.startsWith("V")) {
            cleaned = cleaned.substring(1);
        }
        int end = 0;
        while (end < cleaned.length() && (Character.isDigit(cleaned.charAt(end)) || cleaned.charAt(end) == '.')) {
            end++;
        }
        String[] parts = cleaned.substring(0, end).split("\\.");
        int[] values = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            try {
                values[i] = parts[i].isEmpty() ? 0 : Integer.parseInt(parts[i]);
            }
            catch (NumberFormatException e) {
                values[i] = 0;
            }
        }
        return values;
    }
}
