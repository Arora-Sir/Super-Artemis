package com.limelight.diagnostics;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.ApplicationExitInfo;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Process;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.limelight.BuildConfig;
import com.limelight.LimeLog;
import com.limelight.R;
import com.limelight.about.Links;
import com.limelight.ui.Dialogs;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

// NOTE: Keeps a report of the last crash so a user can send it without root, Shizuku or a PC.
// NOTE: A Java crash is written by an uncaught exception handler, with the app's own recent log, before the system closes the app.
// NOTE: Native crashes and freezes (ANR) cannot run code at that moment, so Android 11 and later read the system's exit record of the last process instead.
// NOTE: The next launch offers the report once. Settings > More shares a fresh diagnostic log at any time.
public final class CrashReporter {
    private static final String DIR = "diagnostics";
    private static final String CRASH_FILE = "last-crash.txt";
    // Errors SafeRun caught, newest last, so the diagnostic log shows them although the app kept running
    private static final String HANDLED_FILE = "handled-errors.txt";
    private static final long HANDLED_MAX_BYTES = 64 * 1024;
    private static final String PREFS = "CrashReporter";
    // Time of the newest system exit record already offered, so one native crash is offered once
    private static final String KEY_LAST_EXIT = "last_exit_ms";
    private static final int LOG_LINES = 800;
    private static final int TRACE_LIMIT = 64 * 1024;

    private CrashReporter() {
    }

    // Installed first in Application.onCreate. The previous handler still runs, so the system crash handling is unchanged.
    public static void install(Context context) {
        Context app = context.getApplicationContext();
        Thread.UncaughtExceptionHandler previous = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> {
            try {
                writeCrash(app, thread, error);
            }
            catch (Throwable ignored) {
                // A failure while reporting must never hide the original crash
            }
            if (previous != null) {
                previous.uncaughtException(thread, error);
            }
            else {
                Process.killProcess(Process.myPid());
                System.exit(10);
            }
        });
    }

    static void writeCrash(Context context, Thread thread, Throwable error) throws IOException {
        StringWriter trace = new StringWriter();
        error.printStackTrace(new PrintWriter(trace));
        String report = header(context, "Java crash")
                + "Thread: " + thread.getName() + "\n\n"
                + trace + "\n"
                + "Recent log of this app:\n" + ownLog();
        write(new File(dir(context), CRASH_FILE), report);
    }

    // NOTE: Shows the last crash once, on the first screen after it. Share sends the report through any app (mail, chat, a file).
    // NOTE: Report on GitHub opens the issue form in the browser. The report itself is never sent anywhere without the user choosing to.
    public static void offerPendingReport(Activity activity) {
        String report;
        try {
            report = pendingReport(activity);
        }
        catch (Throwable e) {
            LimeLog.warning("Unable to read the last crash report: " + e);
            return;
        }
        if (report == null || activity.isFinishing()) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setCustomTitle(Dialogs.header(builder.getContext(), R.drawable.ic_sa_bug_report,
                        activity.getString(R.string.crash_title), activity.getString(R.string.crash_message)))
                .setPositiveButton(R.string.crash_send, (d, w) -> showSendOptions(activity, report))
                .setNegativeButton(R.string.crash_dismiss, null)
                .setOnDismissListener(d -> clearPending(activity));
        Dialogs.show(builder, Dialogs.WIDTH_STANDARD_DP);
    }

    // The saved Java crash, or else a native crash or ANR newer than the last one offered, or null when nothing happened
    static String pendingReport(Context context) throws IOException {
        File crash = new File(dir(context), CRASH_FILE);
        if (crash.exists()) {
            return read(crash);
        }
        return lastAbnormalExit(context, true);
    }

    static void clearPending(Context context) {
        //noinspection ResultOfMethodCallIgnored
        new File(dir(context), CRASH_FILE).delete();
        markExitsSeen(context);
    }

    public static void recordHandled(Context context, String what, Throwable error) {
        if (context == null) {
            return;
        }
        try {
            StringWriter trace = new StringWriter();
            error.printStackTrace(new PrintWriter(trace));
            File file = new File(dir(context), HANDLED_FILE);
            String old = file.exists() && file.length() < HANDLED_MAX_BYTES ? read(file) : "";
            write(file, old + stamp(System.currentTimeMillis()) + " " + what + "\n" + trace + "\n");
        }
        catch (Throwable ignored) {
            // Recording is best effort and must never fail the action that already failed
        }
    }

    // Shares a fresh diagnostic report: device and app details, the last crash or exit record, and the app's own recent log
    public static void shareDiagnostics(Activity activity) {
        StringBuilder report = new StringBuilder(header(activity, "Diagnostic log"));
        try {
            File crash = new File(dir(activity), CRASH_FILE);
            if (crash.exists()) {
                report.append("Last crash:\n").append(read(crash)).append("\n");
            }
            File handled = new File(dir(activity), HANDLED_FILE);
            if (handled.exists()) {
                report.append("Handled errors:\n").append(read(handled)).append("\n");
            }
            String exit = lastAbnormalExit(activity, false);
            if (exit != null) {
                report.append(exit).append("\n");
            }
        }
        catch (Throwable e) {
            report.append("Could not read older reports: ").append(e).append("\n");
        }
        report.append("Recent log of this app:\n").append(ownLog());
        showSendOptions(activity, report.toString());
    }

    // NOTE: One list for every way to hand over a log: a new bug report or an existing issue on GitHub, a file in Downloads,
    // NOTE: any app through the share sheet, or the clipboard. The GitHub entries copy the log first, because a full log is too long for a link.
    public static void showSendOptions(Activity activity, String report) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(activity);
        android.content.Context dialogContext = builder.getContext();
        android.widget.LinearLayout rows = new android.widget.LinearLayout(dialogContext);
        rows.setOrientation(android.widget.LinearLayout.VERTICAL);
        int side = (int) Dialogs.dp(dialogContext, 12);
        rows.setPadding(side, 0, side, (int) Dialogs.dp(dialogContext, 8));
        android.app.AlertDialog[] dialog = new android.app.AlertDialog[1];

        addOption(rows, R.drawable.ic_sa_bug_report, R.string.crash_option_new_issue, R.string.crash_option_new_issue_summary, true, () -> {
            copy(activity, report);
            Links.openInBrowser(activity, bugReportUrl(activity));
        }, dialog);
        addOption(rows, R.drawable.ic_sa_comment, R.string.crash_option_existing_issue, R.string.crash_option_existing_issue_summary, true, () -> {
            copy(activity, report);
            Links.openInBrowser(activity, Links.ISSUES);
        }, dialog);
        addOption(rows, R.drawable.ic_sa_download, R.string.crash_option_save, R.string.crash_option_save_summary, false,
                () -> saveToDownloads(activity, report), dialog);
        addOption(rows, R.drawable.ic_sa_share, R.string.crash_option_share, R.string.crash_option_share_summary, false,
                () -> share(activity, report), dialog);
        addOption(rows, R.drawable.ic_sa_content_copy, R.string.crash_option_copy, R.string.crash_option_copy_summary, false,
                () -> copy(activity, report), dialog);

        android.widget.ScrollView scroll = new android.widget.ScrollView(dialogContext);
        scroll.addView(rows);
        builder.setCustomTitle(Dialogs.header(dialogContext, R.drawable.ic_sa_bug_report,
                        activity.getString(R.string.crash_send_title), activity.getString(R.string.crash_send_message)))
                .setView(scroll)
                .setNegativeButton(android.R.string.cancel, null);
        dialog[0] = Dialogs.show(builder, Dialogs.WIDTH_STANDARD_DP);
    }

    private static void addOption(android.widget.LinearLayout parent, int iconRes, int titleRes, int summaryRes, boolean external,
                                  Runnable action, android.app.AlertDialog[] dialog) {
        android.view.View row = android.view.LayoutInflater.from(parent.getContext()).inflate(R.layout.sa_link_row, parent, false);
        ((android.widget.ImageView) row.findViewById(R.id.sa_link_icon)).setImageResource(iconRes);
        ((android.widget.TextView) row.findViewById(R.id.sa_link_title)).setText(titleRes);
        ((android.widget.TextView) row.findViewById(R.id.sa_link_subtitle)).setText(summaryRes);
        row.findViewById(R.id.sa_link_external).setVisibility(external ? android.view.View.VISIBLE : android.view.View.GONE);
        row.setOnClickListener(v -> {
            if (dialog[0] != null) {
                dialog[0].dismiss();
            }
            com.limelight.ui.SafeRun.run(parent.getContext(), "send log", action);
        });
        parent.addView(row);
    }

    // The bug report form with the version and device already filled in
    static String bugReportUrl(Context context) {
        return Links.BUG_REPORT
                + "&app-version=" + Uri.encode(BuildConfig.VERSION_NAME + " (" + BuildConfig.BUILD_TYPE + ")")
                + "&android=" + Uri.encode(Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")")
                + "&device=" + Uri.encode(Build.MANUFACTURER + " " + Build.MODEL);
    }

    private static void copy(Context context, String report) {
        android.content.ClipboardManager clipboard = (android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Super Artemis log", report));
            Toast.makeText(context, R.string.crash_copied, Toast.LENGTH_LONG).show();
        }
    }

    // Android 10 and later save into Downloads without a permission. Older versions fall back to the share sheet.
    private static void saveToDownloads(Activity activity, String report) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            share(activity, report);
            return;
        }
        String name = "super-artemis-log-" + new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(new Date()) + ".txt";
        android.content.ContentValues values = new android.content.ContentValues();
        values.put(android.provider.MediaStore.Downloads.DISPLAY_NAME, name);
        values.put(android.provider.MediaStore.Downloads.MIME_TYPE, "text/plain");
        try {
            Uri uri = activity.getContentResolver().insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (uri == null) {
                throw new IOException("No Downloads entry");
            }
            try (java.io.OutputStream out = activity.getContentResolver().openOutputStream(uri)) {
                if (out == null) {
                    throw new IOException("Cannot write " + uri);
                }
                out.write(report.getBytes(StandardCharsets.UTF_8));
            }
            Toast.makeText(activity, activity.getString(R.string.crash_saved, name), Toast.LENGTH_LONG).show();
        }
        catch (IOException | RuntimeException e) {
            LimeLog.warning("Unable to save the log: " + e);
            share(activity, report);
        }
    }

    private static void share(Activity activity, String report) {
        try {
            File file = new File(new File(activity.getCacheDir(), "diagnostics-share"), "super-artemis-log.txt");
            write(file, report);
            Uri uri = FileProvider.getUriForFile(activity, activity.getPackageName() + ".fileprovider", file);
            Intent send = new Intent(Intent.ACTION_SEND)
                    .setType("text/plain")
                    .putExtra(Intent.EXTRA_SUBJECT, activity.getString(R.string.crash_share_subject))
                    .putExtra(Intent.EXTRA_STREAM, uri)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            activity.startActivity(Intent.createChooser(send, activity.getString(R.string.crash_share)));
        }
        catch (Throwable e) {
            LimeLog.warning("Unable to share the diagnostic log: " + e);
            Toast.makeText(activity, R.string.crash_share_failed, Toast.LENGTH_LONG).show();
        }
    }

    // NOTE: Reads the system record of how the previous process ended (Android 11+). Only crashes, native crashes and ANRs count.
    // NOTE: An ANR record carries the thread dump, which shows where the app was stuck.
    static String lastAbnormalExit(Context context, boolean onlyNew) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return null;
        }
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (am == null) {
            return null;
        }
        List<ApplicationExitInfo> exits = am.getHistoricalProcessExitReasons(context.getPackageName(), 0, 5);
        long seen = prefs(context).getLong(KEY_LAST_EXIT, 0);
        for (ApplicationExitInfo exit : exits) {
            int reason = exit.getReason();
            if (reason != ApplicationExitInfo.REASON_CRASH_NATIVE && reason != ApplicationExitInfo.REASON_ANR
                    && reason != ApplicationExitInfo.REASON_CRASH) {
                continue;
            }
            if (onlyNew && exit.getTimestamp() <= seen) {
                return null;
            }
            // A Java crash already has its own, richer report
            if (onlyNew && reason == ApplicationExitInfo.REASON_CRASH) {
                return null;
            }
            StringBuilder text = new StringBuilder(header(context, reason == ApplicationExitInfo.REASON_ANR ? "App not responding" : "Native crash"));
            text.append("When: ").append(stamp(exit.getTimestamp())).append("\n")
                    .append("Description: ").append(exit.getDescription()).append("\n")
                    .append("Signal or status: ").append(exit.getStatus()).append("\n");
            if (reason == ApplicationExitInfo.REASON_ANR) {
                text.append("\nThread dump:\n").append(trace(exit));
            }
            return text.toString();
        }
        return null;
    }

    private static void markExitsSeen(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return;
        }
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (am == null) {
            return;
        }
        List<ApplicationExitInfo> exits = am.getHistoricalProcessExitReasons(context.getPackageName(), 0, 1);
        if (!exits.isEmpty()) {
            prefs(context).edit().putLong(KEY_LAST_EXIT, exits.get(0).getTimestamp()).apply();
        }
    }

    private static String trace(ApplicationExitInfo exit) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return "";
        }
        try (InputStream in = exit.getTraceInputStream()) {
            if (in == null) {
                return "(none)\n";
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) > 0 && out.size() < TRACE_LIMIT) {
                out.write(buffer, 0, read);
            }
            return out.toString("UTF-8") + "\n";
        }
        catch (IOException e) {
            return "(unreadable: " + e + ")\n";
        }
    }

    // Any app may read its own log lines without a permission, so this works on every device without root or Shizuku
    static String ownLog() {
        try {
            java.lang.Process logcat = Runtime.getRuntime().exec(new String[] {
                    "logcat", "-d", "-v", "threadtime", "-t", String.valueOf(LOG_LINES), "--pid=" + Process.myPid()});
            StringBuilder out = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(logcat.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    out.append(line).append('\n');
                }
            }
            return out.length() == 0 ? "(empty)\n" : out.toString();
        }
        catch (Throwable e) {
            return "(unavailable: " + e + ")\n";
        }
    }

    private static String header(Context context, String kind) {
        return "Super Artemis " + kind + "\n"
                + "App: " + BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ", " + BuildConfig.BUILD_TYPE + ")\n"
                + "Package: " + context.getPackageName() + "\n"
                + "Device: " + Build.MANUFACTURER + " " + Build.MODEL + " (" + Build.DEVICE + ")\n"
                + "Android: " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + "), build " + Build.DISPLAY + "\n"
                + "Report time: " + stamp(System.currentTimeMillis()) + "\n\n";
    }

    private static String stamp(long timeMs) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.US).format(new Date(timeMs));
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static File dir(Context context) {
        return new File(context.getFilesDir(), DIR);
    }

    private static void write(File file, String text) throws IOException {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Cannot create " + parent);
        }
        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write(text.getBytes(StandardCharsets.UTF_8));
        }
    }

    private static String read(File file) throws IOException {
        try (InputStream in = new java.io.FileInputStream(file)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) > 0) {
                out.write(buffer, 0, read);
            }
            return out.toString("UTF-8");
        }
    }
}
