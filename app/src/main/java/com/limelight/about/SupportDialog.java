package com.limelight.about;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.limelight.R;

// NOTE: Support development, reached only from Settings > About. A UPI app opens through the system chooser, the QR works from another phone,
// NOTE: and when no UPI app is installed the UPI ID is copied instead, so every path ends somewhere useful.
public final class SupportDialog {
    private SupportDialog() {
    }

    public static void show(Context context) {
        View body = LayoutInflater.from(context).inflate(R.layout.dialog_support, null);
        ((TextView) body.findViewById(R.id.support_upi_id)).setText(Links.UPI_ID);
        body.findViewById(R.id.support_upi_app).setOnClickListener(v -> payWithUpiApp(context));
        body.findViewById(R.id.support_copy_upi).setOnClickListener(v -> copyUpiId(context, R.string.support_upi_copied));
        body.findViewById(R.id.support_paypal).setOnClickListener(v -> Links.open(context, Links.PAYPAL));
        body.findViewById(R.id.support_star).setOnClickListener(v -> Links.open(context, Links.REPOSITORY));

        new AlertDialog.Builder(context)
                .setTitle(R.string.title_support)
                .setView(body)
                .setPositiveButton(R.string.custom_keys_done, null)
                .show();
    }

    static Intent upiIntent() {
        return new Intent(Intent.ACTION_VIEW, Uri.parse(Links.UPI_PAYMENT));
    }

    private static void payWithUpiApp(Context context) {
        Intent intent = upiIntent();
        boolean hasUpiApp = !context.getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY).isEmpty();
        if (!hasUpiApp) {
            copyUpiId(context, R.string.support_no_upi_app);
            return;
        }
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.support_pay_upi_app))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }

    private static void copyUpiId(Context context, int messageRes) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText("UPI ID", Links.UPI_ID));
        }
        Toast.makeText(context, messageRes, Toast.LENGTH_LONG).show();
    }
}
