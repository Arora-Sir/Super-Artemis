package com.limelight.hosthelp;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.limelight.R;
import com.limelight.about.Links;
import com.limelight.ui.Dialogs;
import com.limelight.ui.SafeRun;

// NOTE: Windows ignores a Win+L that a remote streaming session injects, so the Lock key reaches the PC and does nothing there.
// NOTE: A two line AutoHotkey script on the PC catches the key and locks the session, and this screen hands the user that script.
public final class LockKeyHelp {
    static final String KEY = "pref_lock_key_help";
    // The script a user saves on the PC, one hotkey that calls the API Windows itself uses to lock
    public static final String SCRIPT = "#Requires AutoHotkey v2.0\n#l::DllCall(\"user32\\LockWorkStation\")";

    private LockKeyHelp() {
    }

    public static void bind(PreferenceFragmentCompat fragment) {
        Preference row = fragment.findPreference(KEY);
        if (row != null) {
            Context context = fragment.requireContext();
            row.setOnPreferenceClickListener(p -> {
                SafeRun.run(context, KEY, () -> show(context));
                return true;
            });
        }
    }

    public static void show(Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        Context dialogContext = builder.getContext();
        View body = LayoutInflater.from(dialogContext).inflate(R.layout.dialog_lock_help, null);
        TextView script = body.findViewById(R.id.lock_help_script);
        script.setText(SCRIPT);
        script.setTypeface(Typeface.MONOSPACE);
        builder.setCustomTitle(Dialogs.header(dialogContext, R.drawable.ic_ck_lock, context.getString(R.string.title_lock_help), context.getString(R.string.lock_help_intro)))
                .setView(body)
                .setPositiveButton(R.string.lock_help_copy, (d, which) -> copy(context))
                .setNeutralButton(R.string.lock_help_guide, (d, which) -> Links.open(context, Links.LOCK_GUIDE))
                .setNegativeButton(R.string.game_menu_cancel, null);
        Dialogs.show(builder, Dialogs.WIDTH_STANDARD_DP);
    }

    private static void copy(Context context) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText("AutoHotkey script", SCRIPT));
        }
        Toast.makeText(context, R.string.lock_help_copied, Toast.LENGTH_SHORT).show();
    }
}
