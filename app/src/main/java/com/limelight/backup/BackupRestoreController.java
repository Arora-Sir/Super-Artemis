package com.limelight.backup;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.limelight.LimeLog;
import com.limelight.R;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// NOTE: Runs the backup and restore dialogs for the settings screen and keeps all file work off the main thread.
// NOTE: Create it while the fragment is being constructed, because Activity Result launchers must be registered before the fragment starts.
public final class BackupRestoreController {
    private static final long RESTART_DELAY_MS = 800;
    private static final String[] RESTORE_MIME_TYPES = {BackupRestoreManager.MIME_TYPE, "text/plain", "application/octet-stream"};

    private final Fragment fragment;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ActivityResultLauncher<String> backupLauncher;
    private final ActivityResultLauncher<String[]> restoreLauncher;
    private boolean pendingIncludePairingKeys;

    public BackupRestoreController(Fragment fragment) {
        this.fragment = fragment;
        backupLauncher = fragment.registerForActivityResult(
                new ActivityResultContracts.CreateDocument(BackupRestoreManager.MIME_TYPE), this::onBackupDestinationChosen);
        restoreLauncher = fragment.registerForActivityResult(
                new ActivityResultContracts.OpenDocument(), this::onRestoreFileChosen);
    }

    // Settings only is the safe default, pairing keys are an explicit opt-in
    public void showBackupOptions() {
        Context context = fragment.requireContext();
        View body = LayoutInflater.from(context).inflate(R.layout.dialog_backup_options, null);
        CheckBox includeKeys = body.findViewById(R.id.backup_include_keys);
        includeKeys.setChecked(false);
        body.findViewById(R.id.backup_keys_card).setOnClickListener(v -> includeKeys.toggle());

        new AlertDialog.Builder(context)
                .setTitle(R.string.title_backup_settings)
                .setView(body)
                .setPositiveButton(R.string.backup_choose_location, (dialog, which) -> {
                    pendingIncludePairingKeys = includeKeys.isChecked();
                    backupLauncher.launch(BackupRestoreManager.defaultFileName());
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    // Nothing is written until the user confirms the preview of the chosen file
    public void chooseRestoreFile() {
        restoreLauncher.launch(RESTORE_MIME_TYPES);
    }

    public void shutdown() {
        executor.shutdown();
    }

    private void onBackupDestinationChosen(Uri uri) {
        if (uri == null) {
            return;
        }
        Context appContext = fragment.requireContext().getApplicationContext();
        boolean includePairingKeys = pendingIncludePairingKeys;
        executor.execute(() -> {
            try {
                BackupRestoreManager.writeBackup(appContext, uri, includePairingKeys);
                toast(appContext, appContext.getString(R.string.backup_success));
            }
            catch (Exception e) {
                LimeLog.warning("Backup failed: " + e);
                toast(appContext, appContext.getString(R.string.backup_failed, describe(e)));
            }
        });
    }

    private void onRestoreFileChosen(Uri uri) {
        if (uri == null) {
            return;
        }
        Context appContext = fragment.requireContext().getApplicationContext();
        executor.execute(() -> {
            try {
                BackupFile backup = BackupRestoreManager.readBackup(appContext, uri);
                RestorePreview preview = RestorePreview.of(appContext, backup);
                mainHandler.post(() -> showRestorePreview(backup, preview));
            }
            catch (Exception e) {
                LimeLog.warning("Restore failed: " + e);
                toast(appContext, appContext.getString(R.string.restore_failed, describe(e)));
            }
        });
    }

    private void showRestorePreview(BackupFile backup, RestorePreview preview) {
        if (!fragment.isAdded()) {
            return;
        }
        Context context = fragment.requireContext();
        Resources res = context.getResources();
        View body = LayoutInflater.from(context).inflate(R.layout.dialog_restore_preview, null);

        String unknown = context.getString(R.string.restore_unknown);
        ((TextView) body.findViewById(R.id.restore_created)).setText(preview.createdAt == null ? unknown
                : DateUtils.formatDateTime(context, preview.createdAt.getTime(), DateUtils.FORMAT_SHOW_DATE
                        | DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_SHOW_YEAR | DateUtils.FORMAT_ABBREV_MONTH));
        ((TextView) body.findViewById(R.id.restore_version)).setText(preview.appVersion.isEmpty() ? unknown : preview.appVersion);
        ((TextView) body.findViewById(R.id.restore_contents)).setText(context.getString(R.string.restore_contents_value,
                res.getQuantityString(R.plurals.restore_settings_count, preview.settingsCount, preview.settingsCount),
                res.getQuantityString(R.plurals.restore_pcs_count, preview.backupPcCount, preview.backupPcCount)));

        // Replacing pairing keys is the only outcome that can lock this device out of its PCs, so it is the only red one
        View outcome = body.findViewById(R.id.restore_outcome);
        ImageView outcomeIcon = body.findViewById(R.id.restore_outcome_icon);
        TextView outcomeText = body.findViewById(R.id.restore_outcome_text);
        outcomeText.setText(describeOutcome(res, preview));
        if (preview.replacesPairingKeys) {
            outcome.setBackgroundResource(R.drawable.bg_caution_callout);
            outcomeIcon.setImageResource(R.drawable.ic_gm_warning);
            outcomeText.setTextColor(ContextCompat.getColor(context, R.color.backup_caution));
        }

        new AlertDialog.Builder(context)
                .setTitle(R.string.restore_preview_title)
                .setView(body)
                .setPositiveButton(R.string.restore_apply, (dialog, which) -> applyBackup(backup))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    static String describeOutcome(Resources res, RestorePreview preview) {
        if (preview.replacesPairingKeys) {
            return res.getString(R.string.restore_outcome_replace_keys);
        }
        List<String> lines = new ArrayList<>();
        if (preview.localPcCount > 0) {
            lines.add(res.getQuantityString(R.plurals.restore_outcome_keep_pcs, preview.localPcCount, preview.localPcCount));
        }
        if (preview.newPcCount > 0) {
            int plural = preview.newPcsNeedPairing ? R.plurals.restore_outcome_new_pcs_pair : R.plurals.restore_outcome_new_pcs;
            lines.add(res.getQuantityString(plural, preview.newPcCount, preview.newPcCount));
        }
        if (lines.isEmpty()) {
            lines.add(res.getString(R.string.restore_outcome_no_pcs));
        }
        return TextUtils.join("\n", lines);
    }

    private void applyBackup(BackupFile backup) {
        Activity activity = fragment.requireActivity();
        Context appContext = activity.getApplicationContext();
        executor.execute(() -> {
            try {
                BackupRestoreManager.applyBackup(appContext, backup);
                mainHandler.post(() -> {
                    Toast.makeText(appContext, R.string.restore_success, Toast.LENGTH_LONG).show();
                    mainHandler.postDelayed(() -> BackupRestoreManager.restartApp(activity), RESTART_DELAY_MS);
                });
            }
            catch (Exception e) {
                LimeLog.warning("Restore failed: " + e);
                toast(appContext, appContext.getString(R.string.restore_failed, describe(e)));
            }
        });
    }

    private void toast(Context context, String message) {
        mainHandler.post(() -> Toast.makeText(context, message, Toast.LENGTH_LONG).show());
    }

    private static String describe(Exception e) {
        return e.getMessage() != null ? e.getMessage() : e.toString();
    }
}
