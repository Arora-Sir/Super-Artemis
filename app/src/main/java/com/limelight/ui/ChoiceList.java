package com.limelight.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.core.text.BidiFormatter;
import androidx.core.widget.TextViewCompat;

import android.util.TypedValue;

import com.limelight.R;

import java.util.ArrayList;
import java.util.List;

// NOTE: One choice-list dialog for every Super Artemis picker: an icon header, one row per choice with its icon, and a check on the current one.
// NOTE: Picking a row closes the dialog first and then runs the row action, so an action that opens another dialog never stacks on top of this one.
public final class ChoiceList {
    private final AlertDialog.Builder builder;
    private final Context context;
    private final LinearLayout list;
    private final LinearLayout root;
    private final List<View> rows = new ArrayList<>();
    private final List<Entry> entries = new ArrayList<>();
    private boolean searchEnabled;

    // What one choice is, kept so the Find panel can list it without reading the views
    private static final class Entry {
        final int iconRes;
        final CharSequence label;
        final CharSequence subtitle;
        final boolean checked;
        final List<Integer> keys;
        final Runnable onPick;

        Entry(int iconRes, CharSequence label, CharSequence subtitle, boolean checked, List<Integer> keys, Runnable onPick) {
            this.iconRes = iconRes;
            this.label = label;
            this.subtitle = subtitle;
            this.checked = checked;
            this.keys = keys;
            this.onPick = onPick;
        }
    }
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
        root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        builder.setCustomTitle(Dialogs.header(context, iconRes, title, null))
                .setView(root)
                .setNegativeButton(cancelRes, null);
    }

    // Themed context of the dialog, for views added below the rows such as section headings or an add button
    public Context context() {
        return context;
    }

    public LinearLayout list() {
        return list;
    }

    // NOTE: Adds an uppercase section label with a subtle divider line before it, grouping choices into distinct panels.
    public ChoiceList addSectionHeader(CharSequence title) {
        if (list.getChildCount() > 0) {
            View divider = new View(context);
            int dividerHeight = Math.max(1, (int) Dialogs.dp(context, 1));
            LinearLayout.LayoutParams divParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dividerHeight);
            int marginVertical = (int) Dialogs.dp(context, 8);
            int marginHorizontal = (int) Dialogs.dp(context, 8);
            divParams.setMargins(marginHorizontal, marginVertical, marginHorizontal, marginVertical);
            divider.setLayoutParams(divParams);

            TypedValue tv = new TypedValue();
            if (context.getTheme().resolveAttribute(R.attr.saOutlineVariant, tv, true)) {
                divider.setBackgroundColor(tv.data);
            } else {
                divider.setBackgroundColor(0x33888888);
            }
            list.addView(divider);
        }

        TextView header = new TextView(context);
        header.setText(title);
        TextViewCompat.setTextAppearance(header, R.style.TextAppearance_SuperArtemis_SectionLabel);
        int padX = (int) Dialogs.dp(context, 16);
        int padTop = (int) Dialogs.dp(context, 6);
        int padBottom = (int) Dialogs.dp(context, 4);
        header.setPadding(padX, padTop, padX, padBottom);
        list.addView(header);
        return this;
    }

    public ChoiceList add(int iconRes, CharSequence label, boolean checked, Runnable onPick) {
        return add(iconRes, label, null, checked, onPick);
    }

    // A choice that sends these Windows virtual keys, so the Find panel can also find it by pressing them
    public ChoiceList add(int iconRes, CharSequence label, boolean checked, List<Integer> keys, Runnable onPick) {
        return add(iconRes, label, null, checked, keys, onPick);
    }

    public ChoiceList add(int iconRes, CharSequence label, CharSequence subtitle, boolean checked, Runnable onPick) {
        return add(iconRes, label, subtitle, checked, null, onPick);
    }

    // A choice with a short line under its label, for choices that need a hint before they are opened
    public ChoiceList add(int iconRes, CharSequence label, CharSequence subtitle, boolean checked, List<Integer> keys, Runnable onPick) {
        View row = LayoutInflater.from(context).inflate(R.layout.sa_choice_row, list, false);
        ImageView iconView = row.findViewById(R.id.sa_choice_icon);
        if (iconRes != 0) {
            iconView.setImageResource(iconRes);
            iconView.setVisibility(View.VISIBLE);
        } else {
            iconView.setVisibility(View.GONE);
        }
        TextView labelView = row.findViewById(R.id.sa_choice_label);
        // NOTE: A row whose text is in another script, such as a Hebrew language name in an English list, still lines up with every other row.
        // NOTE: The bidi wrapper pins the paragraph to the direction of the app language, so an English app keeps every row on the left and a Hebrew app mirrors the whole list.
        labelView.setText(BidiFormatter.getInstance().unicodeWrap(label.toString()));
        labelView.setTextDirection(View.TEXT_DIRECTION_LOCALE);
        labelView.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
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
        row.setTag(label);
        rows.add(row);
        entries.add(new Entry(iconRes, label, subtitle, checked, keys, onPick));
        list.addView(row);
        return this;
    }

    // NOTE: Adds a Search row at the top of the list that opens the Find panel with every choice, found by name or by the keys pressed.
    // NOTE: Picking a result closes both and runs that choice. The search never happens inside this dialog, where the keyboard would squeeze the list.
    public ChoiceList enableSearch() {
        searchEnabled = true;
        return this;
    }

    private void addSearchRow() {
        android.widget.Button search = FindPanel.openButton(context, R.string.find_search_row, this::openFind);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins((int) Dialogs.dp(context, 8), (int) Dialogs.dp(context, 4), (int) Dialogs.dp(context, 8), (int) Dialogs.dp(context, 8));
        list.addView(search, 0, params);
    }

    private void openFind() {
        List<FindPanel.Item> found = new ArrayList<>();
        List<Entry> source = new ArrayList<>();
        CharSequence group = null;
        for (int i = 0; i < list.getChildCount(); i++) {
            View child = list.getChildAt(i);
            int at = rows.indexOf(child);
            if (at >= 0) {
                Entry entry = entries.get(at);
                found.add(new FindPanel.Item(String.valueOf(source.size()), entry.label, entry.iconRes, group != null ? group : entry.subtitle, entry.keys, entry.checked));
                source.add(entry);
            }
            else if (child instanceof TextView) {
                group = ((TextView) child).getText();
            }
        }
        FindPanel.show(context, found, false, item -> {
            Entry entry = source.get(Integer.parseInt(item.id));
            dismiss();
            entry.onPick.run();
            return true;
        });
    }

    public void dismiss() {
        if (dialog != null) {
            dialog.dismiss();
        }
    }

    public AlertDialog show() {
        if (searchEnabled) {
            addSearchRow();
        }
        dialog = Dialogs.show(builder, Dialogs.WIDTH_STANDARD_DP);
        return dialog;
    }
}
