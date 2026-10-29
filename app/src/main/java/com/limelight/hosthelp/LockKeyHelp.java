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
// NOTE: This screen explains two fixes in plain steps: an Apollo server command named Lock (nothing to install) or a two line AutoHotkey script for any host, each with its own Copy button.
public final class LockKeyHelp {
    static final String KEY = "pref_lock_key_help";
    // The script a user saves on the PC, one hotkey that calls the API Windows itself uses to lock
    public static final String SCRIPT = "#Requires AutoHotkey v2.0\n#l::DllCall(\"user32\\LockWorkStation\")";
    // The command line of the Apollo server command named Lock
    public static final String COMMAND = "rundll32.exe user32.dll,LockWorkStation";

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
        fill(body, R.id.lock_help_command, R.id.lock_help_copy_command, COMMAND, "Apollo command", context);
        fill(body, R.id.lock_help_script, R.id.lock_help_copy_script, SCRIPT, "AutoHotkey script", context);
        builder.setCustomTitle(Dialogs.header(dialogContext, R.drawable.ic_ck_lock, context.getString(R.string.title_lock_help), context.getString(R.string.lock_help_intro)))
                .setView(body)
                .setPositiveButton(R.string.lock_help_done, null)
                .setNeutralButton(R.string.lock_help_guide, (d, which) -> Links.open(context, Links.LOCK_GUIDE));
        Dialogs.show(builder, Dialogs.WIDTH_STANDARD_DP);
    }

    // Fills one code box and wires its Copy button
    private static void fill(View body, int textId, int buttonId, String text, String label, Context context) {
        TextView view = body.findViewById(textId);
        view.setText(text);
        view.setTypeface(Typeface.MONOSPACE);
        body.findViewById(buttonId).setOnClickListener(v -> copy(context, label, text));
    }

    private static void copy(Context context, String label, String text) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText(label, text));
        }
        Toast.makeText(context, R.string.lock_help_copied, Toast.LENGTH_SHORT).show();
    }
}
