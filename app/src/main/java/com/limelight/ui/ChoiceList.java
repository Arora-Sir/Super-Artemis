package com.limelight.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.limelight.R;

// NOTE: One choice-list dialog for every Super Artemis picker: an icon header, one row per choice with its icon, and a check on the current one.
// NOTE: Picking a row closes the dialog first and then runs the row action, so an action that opens another dialog never stacks on top of this one.
public final class ChoiceList {
    private final AlertDialog.Builder builder;
    private final Context context;
    private final LinearLayout list;
    private AlertDialog dialog;

    public ChoiceList(AlertDialog.Builder builder, int iconRes, CharSequence title, int cancelRes) {
        this.builder = builder;
        this.context = builder.getContext();
        list = new LinearLayout(context);
        list.setOrientation(LinearLayout.VERTICAL);
        int side = (int) Dialogs.dp(context, 8);
        list.setPadding(side, 0, side, side);
        ScrollView scroll = new ScrollView(context);
        scroll.addView(list);
        builder.setCustomTitle(Dialogs.header(context, iconRes, title, null))
                .setView(scroll)
                .setNegativeButton(cancelRes, null);
    }

    // Themed context of the dialog, for views added below the rows such as section headings or an add button
    public Context context() {
        return context;
    }

    public LinearLayout list() {
        return list;
    }

    public ChoiceList add(int iconRes, CharSequence label, boolean checked, Runnable onPick) {
        return add(iconRes, label, null, checked, onPick);
    }

    // A choice with a short line under its label, for choices that need a hint before they are opened
    public ChoiceList add(int iconRes, CharSequence label, CharSequence subtitle, boolean checked, Runnable onPick) {
        View row = LayoutInflater.from(context).inflate(R.layout.sa_choice_row, list, false);
        ((ImageView) row.findViewById(R.id.sa_choice_icon)).setImageResource(iconRes);
        ((TextView) row.findViewById(R.id.sa_choice_label)).setText(label);
        TextView hint = row.findViewById(R.id.sa_choice_subtitle);
        if (subtitle != null) {
            hint.setText(subtitle);
            hint.setVisibility(View.VISIBLE);
        }
        row.findViewById(R.id.sa_choice_check).setVisibility(checked ? View.VISIBLE : View.INVISIBLE);
        row.setOnClickListener(v -> {
            dismiss();
            onPick.run();
        });
        list.addView(row);
        return this;
    }

    public void dismiss() {
        if (dialog != null) {
            dialog.dismiss();
        }
    }

    public AlertDialog show() {
        dialog = Dialogs.show(builder, Dialogs.WIDTH_STANDARD_DP);
        return dialog;
    }
}
