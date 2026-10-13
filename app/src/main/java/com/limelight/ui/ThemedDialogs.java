package com.limelight.ui;

import android.app.AlertDialog;
import android.content.Context;

import com.limelight.theme.ThemeManager;

// NOTE: The one way to build a dialog over the stream or the PC and app lists, so every dialog carries the color roles of the chosen app style.
// NOTE: A plain AlertDialog.Builder on an activity context uses the activity's base theme, which ignored Material You and looked unlike every other dialog.
// NOTE: StreamDialogsTest fails when Game.java builds a dialog without this class, so a new dialog cannot slip back to the old look.
public final class ThemedDialogs {
    private ThemedDialogs() {
    }

    public static Context context(Context base) {
        return ThemeManager.dialogContext(base, base.getApplicationInfo().theme);
    }

    public static AlertDialog.Builder builder(Context base) {
        return new AlertDialog.Builder(context(base));
    }

    // Shows the dialog at the standard dialog width used by every Super Artemis dialog
    public static AlertDialog show(AlertDialog.Builder builder) {
        return Dialogs.show(builder, Dialogs.WIDTH_STANDARD_DP);
    }
}
