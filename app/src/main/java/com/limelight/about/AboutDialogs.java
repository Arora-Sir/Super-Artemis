package com.limelight.about;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.URLSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.limelight.LimeLog;
import com.limelight.R;
import com.limelight.ui.Dialogs;
import com.limelight.ui.FlowRow;

import java.util.List;

// The developer profiles and the in-app credits, both opened from Settings > About
public final class AboutDialogs {
    private AboutDialogs() {
    }

    public static void showProfiles(Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        Context dialogContext = builder.getContext();
        LinearLayout rows = new LinearLayout(dialogContext);
        rows.setOrientation(LinearLayout.VERTICAL);
        int side = (int) Dialogs.dp(dialogContext, 12);
        rows.setPadding(side, 0, side, (int) Dialogs.dp(dialogContext, 16));
        addLinkRow(rows, R.drawable.ic_sa_public, R.string.profile_website, Links.WEBSITE_LABEL, Links.WEBSITE);
        addLinkRow(rows, R.drawable.ic_brand_github, R.string.profile_github, Links.GITHUB_LABEL, Links.GITHUB_PROFILE);
        addLinkRow(rows, R.drawable.ic_brand_linkedin, R.string.profile_linkedin, Links.LINKEDIN_LABEL, Links.LINKEDIN);
        addLinkRow(rows, R.drawable.ic_brand_x, R.string.profile_x, Links.X_LABEL, Links.X_PROFILE);

        ScrollView scroll = new ScrollView(dialogContext);
        scroll.addView(rows);
        builder.setCustomTitle(Dialogs.header(dialogContext, R.drawable.ic_sa_person,
                        context.getString(R.string.developer_name), context.getString(R.string.developer_role)))
                .setView(scroll);
        Dialogs.show(builder, Dialogs.WIDTH_STANDARD_DP);
    }

    private static void addLinkRow(LinearLayout parent, int iconRes, int titleRes, String address, String url) {
        View row = LayoutInflater.from(parent.getContext()).inflate(R.layout.sa_link_row, parent, false);
        ((ImageView) row.findViewById(R.id.sa_link_icon)).setImageResource(iconRes);
        ((TextView) row.findViewById(R.id.sa_link_title)).setText(titleRes);
        ((TextView) row.findViewById(R.id.sa_link_subtitle)).setText(address);
        row.setOnClickListener(v -> Links.open(parent.getContext(), url));
        parent.addView(row);
    }

    // "Code ported by @logabell", with each handle linking to the person's GitHub profile
    static CharSequence creditLine(Context context, CreditsRepository.Entry entry) {
        SpannableStringBuilder text = new SpannableStringBuilder(entry.role);
        if (!entry.handles.isEmpty()) {
            text.append(' ').append(context.getString(R.string.credits_by)).append(' ');
            for (int i = 0; i < entry.handles.size(); i++) {
                if (i > 0) {
                    text.append(", ");
                }
                String handle = entry.handles.get(i);
                int start = text.length();
                text.append('@').append(handle);
                text.setSpan(new URLSpan(Links.githubUser(handle)), start, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        return text;
    }

    // NOTE: One card per credit: the feature, the people with links to their profiles, and a chip for every PR, commit or issue.
    // NOTE: Wide screens show two columns of cards, so the list stays short and every line stays readable.
    public static void showCredits(Context context) {
        List<CreditsRepository.Entry> entries;
        try {
            entries = CreditsRepository.load(context);
        }
        catch (Exception e) {
            LimeLog.warning("Unable to read credits: " + e);
            Links.open(context, Links.REPOSITORY + "#credits-and-thanks");
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        Context dialogContext = builder.getContext();
        boolean twoColumns = Dialogs.isWide(context);
        RecyclerView list = new RecyclerView(dialogContext);
        list.setLayoutManager(twoColumns
                ? new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
                : new LinearLayoutManager(dialogContext));
        int side = (int) Dialogs.dp(dialogContext, 18);
        list.setPadding(side, 0, side, side);
        list.setClipToPadding(false);
        list.setAdapter(new CreditsAdapter(entries));

        builder.setCustomTitle(Dialogs.header(dialogContext, R.drawable.ic_sa_groups,
                        context.getString(R.string.title_credits), context.getString(R.string.credits_intro)))
                .setView(list);
        Dialogs.show(builder, twoColumns ? Dialogs.WIDTH_WIDE_DP : Dialogs.WIDTH_STANDARD_DP);
    }

    private static final class CreditsAdapter extends RecyclerView.Adapter<CreditsAdapter.Holder> {
        private final List<CreditsRepository.Entry> entries;

        CreditsAdapter(List<CreditsRepository.Entry> entries) {
            this.entries = entries;
        }

        static final class Holder extends RecyclerView.ViewHolder {
            final TextView feature;
            final TextView people;
            final FlowRow links;

            Holder(View itemView) {
                super(itemView);
                feature = itemView.findViewById(R.id.credit_feature);
                people = itemView.findViewById(R.id.credit_people);
                links = itemView.findViewById(R.id.credit_links);
                people.setMovementMethod(LinkMovementMethod.getInstance());
            }
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.credit_card, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            Context context = holder.itemView.getContext();
            CreditsRepository.Entry entry = entries.get(position);
            holder.feature.setText(entry.feature);
            holder.people.setText(creditLine(context, entry));
            holder.links.removeAllViews();
            holder.links.setVisibility(entry.links.isEmpty() ? View.GONE : View.VISIBLE);
            for (CreditsRepository.Link link : entry.links) {
                TextView chip = (TextView) LayoutInflater.from(context).inflate(R.layout.sa_chip, holder.links, false);
                chip.setText(link.label);
                Drawable icon = context.getDrawable(R.drawable.ic_sa_link);
                if (icon != null) {
                    int size = (int) Dialogs.dp(context, 16);
                    icon.setBounds(0, 0, size, size);
                    chip.setCompoundDrawablesRelative(icon, null, null, null);
                }
                chip.setOnClickListener(v -> Links.open(context, link.url));
                holder.links.addView(chip);
            }
        }

        @Override
        public int getItemCount() {
            return entries.size();
        }
    }
}
