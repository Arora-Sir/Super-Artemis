package com.limelight.ui;

import android.content.Context;
import android.widget.Toast;

import com.limelight.LimeLog;
import com.limelight.R;
import com.limelight.diagnostics.CrashReporter;

// NOTE: Runs a user action (a menu entry, a quick-action bar button, a gesture, an editor opened from Settings) so a bug in it
// NOTE: shows a short message and lands in the diagnostic log instead of closing the app and ending the stream.
// NOTE: It only covers code that runs during the call. Work that runs later, such as a dialog list laid out on the next frame,
// NOTE: is covered by EditorsSmokeTest and scripts/check_view_casts.py instead.
public final class SafeRun {
    private SafeRun() {
    }

    public static void run(Context context, String what, Runnable action) {
        try {
            action.run();
        }
        catch (RuntimeException e) {
            LimeLog.severe("Action failed: " + what + ": " + e);
            CrashReporter.recordHandled(context, what, e);
            if (context != null) {
                Toast.makeText(context.getApplicationContext(), context.getString(R.string.safe_run_failed), Toast.LENGTH_LONG).show();
            }
        }
    }
}
