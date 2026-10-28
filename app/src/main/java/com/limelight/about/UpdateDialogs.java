package com.limelight.about;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Toast;

import androidx.preference.PreferenceManager;

import com.limelight.BuildConfig;
import com.limelight.R;
import com.limelight.ui.Dialogs;

// NOTE: The dialogs of Settings > About > Check for updates: a one-time explanation of the single request, then the answer.
// NOTE: The check only ever runs from this tap. Nothing is checked at start, in the background or during a stream, and nothing is installed by the app.
public final class UpdateDialogs {
    // Release notes are shown as plain text, shortened so a long changelog stays one dialog
    static final int NOTES_MAX_CHARS = 1500;

    private UpdateDialogs() {
    }

    public static void start(Activity activity) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        if (prefs.getBoolean(UpdateChecker.PREF_CONSENT, false)) {
            run(activity);
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setCustomTitle(Dialogs.header(builder.getContext(), R.drawable.ic_sa_info,
                        activity.getString(R.string.update_consent_title), null))
                .setMessage(R.string.update_consent_message)
                .setPositiveButton(R.string.update_consent_check, (d, which) -> {
                    prefs.edit().putBoolean(UpdateChecker.PREF_CONSENT, true).apply();
                    run(activity);
                })
                .setNegativeButton(R.string.game_menu_cancel, null);
        Dialogs.show(builder, Dialogs.WIDTH_STANDARD_DP);
    }

    private static void run(Activity activity) {
        Toast.makeText(activity, R.string.update_checking, Toast.LENGTH_SHORT).show();
        UpdateChecker.check(BuildConfig.VERSION_NAME, result -> {
            if (activity.isFinishing() || activity.isDestroyed()) {
                return;
            }
            showResult(activity, result);
        });
    }

    static void showResult(Context context, UpdateChecker.Result result) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        Context dialogContext = builder.getContext();
        switch (result.status) {
            case UPDATE_AVAILABLE:
                builder.setCustomTitle(Dialogs.header(dialogContext, R.drawable.ic_sa_info,
                                context.getString(R.string.update_available_title, result.release.tag),
                                context.getString(R.string.update_available_summary, BuildConfig.VERSION_NAME)))
                        .setMessage(notes(result.release.notes))
                        .setPositiveButton(R.string.update_open_release, (d, which) -> Links.openInBrowser(context, result.release.pageUrl))
                        .setNegativeButton(R.string.update_later, null);
                break;
            case UP_TO_DATE:
                builder.setCustomTitle(Dialogs.header(dialogContext, R.drawable.ic_sa_check,
                                context.getString(R.string.update_current_title), context.getString(R.string.update_current_summary, BuildConfig.VERSION_NAME)))
                        .setPositiveButton(android.R.string.ok, null);
                break;
            case NO_RELEASE:
                builder.setCustomTitle(Dialogs.header(dialogContext, R.drawable.ic_sa_info,
                                context.getString(R.string.update_none_title), context.getString(R.string.update_none_summary)))
                        .setPositiveButton(android.R.string.ok, null);
                break;
            default:
                builder.setCustomTitle(Dialogs.header(dialogContext, R.drawable.ic_sa_warning,
                                context.getString(R.string.update_failed_title), context.getString(R.string.update_failed_summary)))
                        .setPositiveButton(R.string.update_open_releases, (d, which) -> Links.openInBrowser(context, Links.RELEASES))
                        .setNegativeButton(android.R.string.cancel, null);
                break;
        }
        Dialogs.show(builder, Dialogs.WIDTH_STANDARD_DP);
    }

    // Plain text of the release notes, cut at a line end where possible
    static String notes(String body) {
        String text = body == null ? "" : body.replace("\r", "").trim();
        if (text.length() <= NOTES_MAX_CHARS) {
            return text;
        }
        int cut = text.lastIndexOf('\n', NOTES_MAX_CHARS);
        return text.substring(0, cut > NOTES_MAX_CHARS / 2 ? cut : NOTES_MAX_CHARS).trim() + "\n...";
    }
}
