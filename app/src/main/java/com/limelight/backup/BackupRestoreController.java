package com.limelight.backup;

import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.widget.TextViewCompat;
import androidx.fragment.app.Fragment;

import com.limelight.LimeLog;
import com.limelight.R;
import com.limelight.ui.Dialogs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// NOTE: Runs the backup and restore dialogs for the settings screen and keeps all file and crypto work off the main thread.
// NOTE: A backup is saved through the system file picker or handed to the share sheet. A restore reads a picked or opened file,
// NOTE: asks for the password of an encrypted one, then lets the user tick which parts to apply before anything is written.
// NOTE: Create it while the fragment is being constructed, because Activity Result launchers must be registered before the fragment starts.
public final class BackupRestoreController {
    private static final long RESTART_DELAY_MS = 800;
    // Long enough to resist guessing with PBKDF2, short enough to type on a phone
    static final int MIN_PASSWORD_LENGTH = 8;

    private final Fragment fragment;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ActivityResultLauncher<String> backupLauncher;
    private final ActivityResultLauncher<String[]> restoreLauncher;
    private boolean pendingIncludePairingKeys;
    private char[] pendingPassword;

    public BackupRestoreController(Fragment fragment) {
        this.fragment = fragment;
        backupLauncher = fragment.registerForActivityResult(
                new ActivityResultContracts.CreateDocument(BackupRestoreManager.MIME_TYPE), this::onBackupDestinationChosen);
        restoreLauncher = fragment.registerForActivityResult(
                new ActivityResultContracts.OpenDocument(), this::startRestore);
    }

    // NOTE: Settings only is the safe default. Pairing keys are an explicit opt-in and always need a password,
    // NOTE: because anyone holding an unprotected copy could connect to the user's PCs as this device.
    public void showBackupOptions() {
        Context context = fragment.requireContext();
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        Context dialogContext = builder.getContext();
        View body = LayoutInflater.from(dialogContext).inflate(R.layout.dialog_backup_options, null);
        CompoundButton includeKeys = body.findViewById(R.id.backup_include_keys);
        CompoundButton usePassword = body.findViewById(R.id.backup_use_password);
        View passwordToggle = body.findViewById(R.id.backup_password_toggle);
        View passwordFields = body.findViewById(R.id.backup_password_fields);
        TextView passwordSummary = body.findViewById(R.id.backup_password_summary);
        EditText password = body.findViewById(R.id.backup_password);
        EditText confirm = body.findViewById(R.id.backup_password_confirm);
        TextView error = body.findViewById(R.id.backup_password_error);

        Runnable refresh = () -> {
            boolean keys = includeKeys.isChecked();
            if (keys) {
                usePassword.setChecked(true);
            }
            usePassword.setEnabled(!keys);
            passwordToggle.setEnabled(!keys);
            passwordSummary.setText(keys ? R.string.backup_password_required : R.string.backup_password_summary);
            passwordFields.setVisibility(usePassword.isChecked() ? View.VISIBLE : View.GONE);
            error.setVisibility(View.GONE);
        };
        body.findViewById(R.id.backup_keys_card).setOnClickListener(v -> {
            includeKeys.toggle();
            refresh.run();
        });
        includeKeys.setOnClickListener(v -> refresh.run());
        passwordToggle.setOnClickListener(v -> {
            usePassword.toggle();
            refresh.run();
        });
        usePassword.setOnClickListener(v -> refresh.run());
        refresh.run();

        builder.setCustomTitle(Dialogs.header(dialogContext, R.drawable.ic_sa_backup,
                        context.getString(R.string.title_backup_settings), context.getString(R.string.backup_dialog_summary)))
                .setView(body)
                .setPositiveButton(R.string.backup_save_to_device, null)
                .setNeutralButton(R.string.backup_share, null)
                .setNegativeButton(android.R.string.cancel, null);
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            // The dialog shrinks and scrolls above the keyboard, so the second password field and the buttons stay reachable
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
        dialog.show();
        Dialogs.fitWidth(dialog, Dialogs.WIDTH_STANDARD_DP);
        Dialogs.setButtonIcon(dialog.getButton(AlertDialog.BUTTON_NEUTRAL), R.drawable.ic_sa_share);
        confirm.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
                return true;
            }
            return false;
        });
        // The buttons check the password first and keep the dialog open when it is not acceptable
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            char[] chosen = checkedPassword(context, usePassword.isChecked(), password, confirm, error);
            if (chosen == null && usePassword.isChecked()) {
                return;
            }
            dialog.dismiss();
            pendingIncludePairingKeys = includeKeys.isChecked();
            pendingPassword = chosen;
            backupLauncher.launch(BackupRestoreManager.defaultFileName());
        });
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
            char[] chosen = checkedPassword(context, usePassword.isChecked(), password, confirm, error);
            if (chosen == null && usePassword.isChecked()) {
                return;
            }
            dialog.dismiss();
            shareBackup(includeKeys.isChecked(), chosen);
        });
    }

    // Returns the password, or null with the reason shown when it is too short or the two entries differ
    private static char[] checkedPassword(Context context, boolean wanted, EditText password, EditText confirm, TextView error) {
        if (!wanted) {
            return null;
        }
        String first = password.getText().toString();
        String second = confirm.getText().toString();
        String problem = null;
        if (first.length() < MIN_PASSWORD_LENGTH) {
            problem = context.getString(R.string.backup_password_too_short, MIN_PASSWORD_LENGTH);
        }
        else if (!first.equals(second)) {
            problem = context.getString(R.string.backup_password_mismatch);
        }
        if (problem != null) {
            error.setText(problem);
            error.setVisibility(View.VISIBLE);
            return null;
        }
        return first.toCharArray();
    }

    // Nothing is written until the user confirms the preview of the chosen file
    public void chooseRestoreFile() {
        restoreLauncher.launch(BackupRestoreManager.READABLE_MIME_TYPES);
    }

    public void shutdown() {
        executor.shutdown();
    }

    private void onBackupDestinationChosen(Uri uri) {
        char[] password = pendingPassword;
        pendingPassword = null;
        if (uri == null) {
            clear(password);
            return;
        }
        Context appContext = fragment.requireContext().getApplicationContext();
        boolean includePairingKeys = pendingIncludePairingKeys;
        executor.execute(() -> {
            try {
                BackupRestoreManager.writeBackup(appContext, uri, includePairingKeys, password);
                toast(appContext, appContext.getString(R.string.backup_success));
            }
            catch (Exception e) {
                LimeLog.warning("Backup failed: " + e);
                toast(appContext, appContext.getString(R.string.backup_failed, describe(e)));
            }
            finally {
                clear(password);
            }
        });
    }

    // The share sheet gets a temporary copy, so the user can send it with Quick Share, a chat app or a cloud drive without saving it first
    private void shareBackup(boolean includePairingKeys, char[] password) {
        Context context = fragment.requireContext();
        Context appContext = context.getApplicationContext();
        executor.execute(() -> {
            try {
                Uri uri = BackupRestoreManager.createShareableBackup(appContext, includePairingKeys, password);
                mainHandler.post(() -> {
                    if (!fragment.isAdded()) {
                        return;
                    }
                    Intent send = new Intent(Intent.ACTION_SEND)
                            .setType(BackupRestoreManager.MIME_TYPE)
                            .putExtra(Intent.EXTRA_STREAM, uri)
                            .putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.backup_share_subject))
                            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    send.setClipData(ClipData.newRawUri("", uri));
                    fragment.startActivity(Intent.createChooser(send, context.getString(R.string.backup_share)));
                });
            }
            catch (Exception e) {
                LimeLog.warning("Backup share failed: " + e);
                toast(appContext, appContext.getString(R.string.backup_failed, describe(e)));
            }
            finally {
                clear(password);
            }
        });
    }

    // Entry point for a picked file and for a backup opened from a file manager or shared into the app
    public void startRestore(Uri uri) {
        if (uri == null) {
            return;
        }
        Context appContext = fragment.requireContext().getApplicationContext();
        executor.execute(() -> {
            try {
                byte[] data = BackupRestoreManager.readBytes(appContext, uri);
                openBackup(appContext, data, null);
            }
            catch (Exception e) {
                LimeLog.warning("Restore failed: " + e);
                toast(appContext, appContext.getString(R.string.restore_failed, describe(e)));
            }
        });
    }

    // Runs on the worker thread: parses (and decrypts) the file, then shows the password prompt or the preview
    private void openBackup(Context appContext, byte[] data, char[] password) {
        try {
            BackupFile backup = BackupRestoreManager.parseBackup(data, password);
            RestorePreview preview = RestorePreview.of(appContext, backup);
            List<DeviceCompatibility.Issue> issues = DeviceCompatibility.check(appContext, backup);
            mainHandler.post(() -> showRestorePreview(backup, preview, issues));
        }
        catch (BackupRestoreManager.PasswordRequiredException e) {
            mainHandler.post(() -> askPassword(appContext, data, false));
        }
        catch (BackupCrypto.WrongPasswordException e) {
            mainHandler.post(() -> askPassword(appContext, data, true));
        }
        catch (Exception e) {
            LimeLog.warning("Restore failed: " + e);
            toast(appContext, appContext.getString(R.string.restore_failed, describe(e)));
        }
        finally {
            clear(password);
        }
    }

    private void askPassword(Context appContext, byte[] data, boolean wrong) {
        if (!fragment.isAdded()) {
            return;
        }
        Context context = fragment.requireContext();
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        Context dialogContext = builder.getContext();
        EditText field = new EditText(dialogContext, null, 0, R.style.Widget_SuperArtemis_TextField);
        field.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        field.setImeOptions(EditorInfo.IME_ACTION_DONE);
        field.setSingleLine(true);
        field.setHint(R.string.backup_password_hint);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            field.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);
        }
        LinearLayout box = new LinearLayout(dialogContext);
        int side = (int) Dialogs.dp(dialogContext, 24);
        box.setPadding(side, 0, side, 0);
        box.setOrientation(LinearLayout.VERTICAL);
        box.addView(field, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        if (wrong) {
            // A wrong password is said next to the field, where the eye already is, and not only in the subtitle
            TextView error = new TextView(dialogContext);
            error.setText(R.string.restore_password_wrong_inline);
            TextViewCompat.setTextAppearance(error, R.style.TextAppearance_SuperArtemis_BodySmall);
            error.setTextColor(Dialogs.color(dialogContext, R.attr.saError));
            error.setPadding((int) Dialogs.dp(dialogContext, 16), (int) Dialogs.dp(dialogContext, 6), 0, 0);
            box.addView(error);
        }

        builder.setCustomTitle(Dialogs.header(dialogContext, R.drawable.ic_sa_privacy_tip,
                        context.getString(R.string.restore_password_title),
                        context.getString(wrong ? R.string.restore_password_wrong : R.string.restore_password_message)))
                .setView(box)
                .setPositiveButton(R.string.restore_unlock, null)
                .setNegativeButton(android.R.string.cancel, null);
        AlertDialog dialog = builder.create();
        dialog.show();
        Dialogs.fitWidth(dialog, Dialogs.WIDTH_STANDARD_DP);
        Button unlock = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        unlock.setEnabled(false);
        field.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                unlock.setEnabled(s.length() > 0);
            }
        });
        field.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE && unlock.isEnabled()) {
                unlock.performClick();
                return true;
            }
            return false;
        });
        field.requestFocus();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
        unlock.setOnClickListener(v -> {
            char[] password = field.getText().toString().toCharArray();
            field.setText("");
            dialog.dismiss();
            Toast.makeText(context, R.string.restore_unlocking, Toast.LENGTH_SHORT).show();
            executor.execute(() -> openBackup(appContext, data, password));
        });
    }

    // One tick per part of the backup and one per setting this device cannot use, so the user decides exactly what changes
    private void showRestorePreview(BackupFile backup, RestorePreview preview, List<DeviceCompatibility.Issue> issues) {
        if (!fragment.isAdded()) {
            return;
        }
        Context context = fragment.requireContext();
        Resources res = context.getResources();
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        Context dialogContext = builder.getContext();
        View body = LayoutInflater.from(dialogContext).inflate(R.layout.dialog_restore_preview, null);

        String unknown = context.getString(R.string.restore_unknown);
        String created = preview.createdAt == null ? unknown : DateUtils.formatDateTime(context, preview.createdAt.getTime(),
                DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_SHOW_YEAR | DateUtils.FORMAT_ABBREV_MONTH);
        String device = backup.deviceModel().isEmpty() ? unknown : backup.deviceModel();
        ((TextView) body.findViewById(R.id.restore_details)).setText(context.getString(R.string.restore_details_value,
                created, device, preview.appVersion.isEmpty() ? unknown : preview.appVersion));

        LinearLayout parts = body.findViewById(R.id.restore_parts);
        CheckBox settings = addChoice(parts, context.getString(R.string.restore_part_settings),
                res.getQuantityString(R.plurals.restore_settings_count, preview.settingsCount, preview.settingsCount), true);
        int layoutFiles = backup.layoutFileCount();
        CheckBox layouts = layoutFiles == 0 ? null : addChoice(parts, context.getString(R.string.restore_part_layouts),
                context.getString(R.string.restore_part_layouts_detail), true);
        CheckBox profiles = !backup.hasProfiles() ? null : addChoice(parts, context.getString(R.string.restore_part_profiles),
                context.getString(R.string.restore_part_profiles_detail), true);
        CheckBox pcs = addChoice(parts, context.getString(R.string.restore_part_pcs),
                res.getQuantityString(R.plurals.restore_pcs_count, preview.backupPcCount, preview.backupPcCount), true);
        CheckBox keys = !backup.hasPairingKeys() ? null : addChoice(parts, context.getString(R.string.restore_part_keys),
                context.getString(R.string.restore_part_keys_detail), true);

        // A setting this device cannot use stays unticked, which keeps this device's own value for it
        List<CheckBox> issueChecks = new ArrayList<>();
        if (!issues.isEmpty()) {
            body.findViewById(R.id.restore_issues_label).setVisibility(View.VISIBLE);
            body.findViewById(R.id.restore_issues_hint).setVisibility(View.VISIBLE);
            LinearLayout issueList = body.findViewById(R.id.restore_issues);
            issueList.setVisibility(View.VISIBLE);
            for (DeviceCompatibility.Issue issue : issues) {
                CheckBox check = addChoice(issueList, issue.message, context.getString(R.string.restore_issue_keep_local), false);
                check.setTag(issue.key);
                issueChecks.add(check);
            }
        }

        View outcome = body.findViewById(R.id.restore_outcome);
        ImageView outcomeIcon = body.findViewById(R.id.restore_outcome_icon);
        TextView outcomeText = body.findViewById(R.id.restore_outcome_text);
        AlertDialog[] dialog = new AlertDialog[1];
        Runnable refresh = () -> {
            boolean replaceKeys = keys != null && keys.isChecked();
            if (replaceKeys) {
                pcs.setChecked(true);
            }
            pcs.setEnabled(!replaceKeys);
            ((View) pcs.getParent()).setEnabled(!replaceKeys);
            for (CheckBox check : issueChecks) {
                check.setEnabled(settings.isChecked());
                ((View) check.getParent()).setEnabled(settings.isChecked());
            }
            outcomeText.setText(describeOutcome(res, preview, replaceKeys, pcs.isChecked()));
            outcome.setBackgroundResource(replaceKeys ? R.drawable.bg_caution_callout : R.drawable.bg_safe_callout);
            outcomeIcon.setImageResource(replaceKeys ? R.drawable.ic_gm_warning : R.drawable.ic_gm_check_circle);
            outcomeText.setTextColor(replaceKeys ? ContextCompat.getColor(context, R.color.backup_caution)
                    : Dialogs.color(dialogContext, android.R.attr.textColorPrimary));
            if (dialog[0] != null) {
                dialog[0].getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(selection(settings, layouts, profiles, pcs, keys, issueChecks) != null);
            }
        };
        for (CheckBox check : Arrays.asList(settings, layouts, profiles, pcs, keys)) {
            if (check != null) {
                ((View) check.getParent()).setOnClickListener(v -> {
                    check.toggle();
                    refresh.run();
                });
            }
        }
        for (CheckBox check : issueChecks) {
            ((View) check.getParent()).setOnClickListener(v -> check.toggle());
        }

        builder.setCustomTitle(Dialogs.header(dialogContext, R.drawable.ic_sa_settings_backup_restore,
                        context.getString(R.string.restore_preview_title), null))
                .setView(body)
                .setPositiveButton(R.string.restore_apply, (d, which) -> {
                    RestoreSelection chosen = selection(settings, layouts, profiles, pcs, keys, issueChecks);
                    if (chosen != null) {
                        applyBackup(backup, chosen);
                    }
                })
                .setNegativeButton(android.R.string.cancel, null);
        dialog[0] = builder.create();
        dialog[0].show();
        Dialogs.fitWidth(dialog[0], Dialogs.WIDTH_STANDARD_DP);
        refresh.run();
    }

    private static CheckBox addChoice(LinearLayout parent, String title, String subtitle, boolean checked) {
        View row = LayoutInflater.from(parent.getContext()).inflate(R.layout.restore_choice_row, parent, false);
        ((TextView) row.findViewById(R.id.restore_choice_title)).setText(title);
        TextView sub = row.findViewById(R.id.restore_choice_subtitle);
        if (TextUtils.isEmpty(subtitle)) {
            sub.setVisibility(View.GONE);
        }
        else {
            sub.setText(subtitle);
        }
        CheckBox check = row.findViewById(R.id.restore_choice_check);
        check.setChecked(checked);
        parent.addView(row);
        return check;
    }

    // Null when nothing is ticked, which disables Restore
    private static RestoreSelection selection(CheckBox settings, CheckBox layouts, CheckBox profiles, CheckBox pcs, CheckBox keys,
                                              List<CheckBox> issueChecks) {
        Set<String> keepLocal = new HashSet<>();
        for (CheckBox check : issueChecks) {
            if (!check.isChecked()) {
                keepLocal.add((String) check.getTag());
            }
        }
        RestoreSelection chosen = new RestoreSelection(settings.isChecked(), isChecked(layouts), isChecked(profiles),
                pcs.isChecked(), isChecked(keys), keepLocal);
        return chosen.isEmpty() ? null : chosen;
    }

    private static boolean isChecked(CheckBox check) {
        return check != null && check.isChecked();
    }

    static String describeOutcome(Resources res, RestorePreview preview) {
        return describeOutcome(res, preview, preview.replacesPairingKeys, true);
    }

    static String describeOutcome(Resources res, RestorePreview preview, boolean replaceKeys, boolean restorePcs) {
        if (replaceKeys) {
            return res.getString(R.string.restore_outcome_replace_keys);
        }
        List<String> lines = new ArrayList<>();
        if (preview.localPcCount > 0) {
            lines.add(res.getQuantityString(R.plurals.restore_outcome_keep_pcs, preview.localPcCount, preview.localPcCount));
        }
        if (restorePcs && preview.newPcCount > 0) {
            // Without the backup's pairing keys, PCs from another device are added but must be paired again
            boolean needPairing = preview.newPcsNeedPairing || preview.replacesPairingKeys;
            int plural = needPairing ? R.plurals.restore_outcome_new_pcs_pair : R.plurals.restore_outcome_new_pcs;
            lines.add(res.getQuantityString(plural, preview.newPcCount, preview.newPcCount));
        }
        if (lines.isEmpty()) {
            lines.add(res.getString(R.string.restore_outcome_no_pcs));
        }
        return TextUtils.join("\n", lines);
    }

    private void applyBackup(BackupFile backup, RestoreSelection selection) {
        Activity activity = fragment.requireActivity();
        Context appContext = activity.getApplicationContext();
        executor.execute(() -> {
            try {
                BackupRestoreManager.applyBackup(appContext, backup, selection);
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

    private static void clear(char[] password) {
        if (password != null) {
            Arrays.fill(password, '\0');
        }
    }

    private void toast(Context context, String message) {
        mainHandler.post(() -> Toast.makeText(context, message, Toast.LENGTH_LONG).show());
    }

    private static String describe(Exception e) {
        return e.getMessage() != null ? e.getMessage() : e.toString();
    }
}
