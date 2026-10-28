package com.limelight.about;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.limelight.R;
import com.limelight.ui.Dialogs;

// NOTE: Support development, reached only from Settings > About. A UPI app opens through the system chooser, the QR works from another phone,
// NOTE: and when no UPI app is installed the UPI ID is copied instead, so every path ends somewhere useful.
// NOTE: The dialog has no buttons of its own: back or a tap outside closes it, like any other sheet of options.
public final class SupportDialog {
    private SupportDialog() {
    }

    public static void show(Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        Context dialogContext = builder.getContext();
        View body = LayoutInflater.from(dialogContext).inflate(R.layout.dialog_support, null);
        ((TextView) body.findViewById(R.id.support_upi_id)).setText(Links.UPI_ID);
        body.findViewById(R.id.support_upi_app).setOnClickListener(v -> payWithUpiApp(context));
        body.findViewById(R.id.support_copy_upi).setOnClickListener(v -> copyUpiId(context, R.string.support_upi_copied));
        body.findViewById(R.id.support_paypal).setOnClickListener(v -> Links.open(context, Links.PAYPAL));
        // The GitHub app cannot show a sponsors page, so it opens in the default browser
        body.findViewById(R.id.support_sponsors).setOnClickListener(v -> Links.openInBrowser(context, Links.SPONSORS));
        if (Dialogs.isCompact(dialogContext)) {
            stackUpiCard(body);
        }

        builder.setCustomTitle(Dialogs.header(dialogContext, R.drawable.ic_sa_favorite_red,
                        context.getString(R.string.title_support), context.getString(R.string.support_intro)))
                .setView(body);
        Dialogs.show(builder, Dialogs.WIDTH_STANDARD_DP);
    }

    // On a phone the QR sits above the UPI details, so the ID and the pay button get the full card width
    private static void stackUpiCard(View body) {
        LinearLayout card = body.findViewById(R.id.support_upi_card);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        View info = body.findViewById(R.id.support_upi_info);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = (int) Dialogs.dp(body.getContext(), 16);
        info.setLayoutParams(params);
        View pay = body.findViewById(R.id.support_upi_app);
        LinearLayout.LayoutParams payParams = (LinearLayout.LayoutParams) pay.getLayoutParams();
        payParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
        pay.setLayoutParams(payParams);
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
