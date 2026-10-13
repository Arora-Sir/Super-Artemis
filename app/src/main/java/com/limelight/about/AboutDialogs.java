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

    // NOTE: Two cards in the credits style: the developer's profiles, then the projects Super Artemis is built on.
    // NOTE: The second card keeps the upstream authors next to the developer's name, with a shortcut to every credit.
    public static void showProfiles(Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        Context dialogContext = builder.getContext();
        LinearLayout content = new LinearLayout(dialogContext);
        content.setOrientation(LinearLayout.VERTICAL);
        int side = (int) Dialogs.dp(dialogContext, 18);
        content.setPadding(side, 0, side, side);

        LinearLayout profiles = card(dialogContext);
        TextView label = new TextView(dialogContext);
        label.setText(R.string.developer_find_me);
        androidx.core.widget.TextViewCompat.setTextAppearance(label, R.style.TextAppearance_SuperArtemis_SectionLabel);
        int pad = (int) Dialogs.dp(dialogContext, 12);
        label.setPadding(pad, pad, pad, (int) Dialogs.dp(dialogContext, 4));
        profiles.addView(label);
        addLinkRow(profiles, R.drawable.ic_sa_public, R.string.profile_website, Links.WEBSITE_LABEL, Links.WEBSITE);
        addLinkRow(profiles, R.drawable.ic_brand_github, R.string.profile_github, Links.GITHUB_LABEL, Links.GITHUB_PROFILE);
        addLinkRow(profiles, R.drawable.ic_brand_linkedin, R.string.profile_linkedin, Links.LINKEDIN_LABEL, Links.LINKEDIN);
        addLinkRow(profiles, R.drawable.ic_brand_x, R.string.profile_x, Links.X_LABEL, Links.X_PROFILE);
        content.addView(profiles);

        View builtOn = LayoutInflater.from(dialogContext).inflate(R.layout.credit_card, content, false);
        ((TextView) builtOn.findViewById(R.id.credit_feature)).setText(R.string.developer_built_on_title);
        TextView people = builtOn.findViewById(R.id.credit_people);
        people.setText(builtOnLine(dialogContext));
        people.setMovementMethod(LinkMovementMethod.getInstance());
        FlowRow chips = builtOn.findViewById(R.id.credit_links);
        addChip(chips, R.drawable.ic_sa_link, "ClassicOldSong/moonlight-android", () -> Links.open(context, "https://github.com/ClassicOldSong/moonlight-android"));
        addChip(chips, R.drawable.ic_sa_link, "moonlight-stream/moonlight-android", () -> Links.open(context, "https://github.com/moonlight-stream/moonlight-android"));
        addChip(chips, R.drawable.ic_sa_groups, context.getString(R.string.developer_all_credits), () -> showCredits(context));
        LinearLayout.LayoutParams builtOnParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        builtOnParams.topMargin = (int) Dialogs.dp(dialogContext, 12);
        builtOn.setLayoutParams(builtOnParams);
        content.addView(builtOn);

        ScrollView scroll = new ScrollView(dialogContext);
        scroll.addView(content);
        builder.setCustomTitle(Dialogs.header(dialogContext, R.drawable.ic_sa_person,
                        context.getString(R.string.developer_name), context.getString(R.string.developer_role)))
                .setView(scroll);
        Dialogs.show(builder, Dialogs.WIDTH_STANDARD_DP);
    }

    private static LinearLayout card(Context context) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_card);
        int pad = (int) Dialogs.dp(context, 4);
        card.setPadding(pad, pad, pad, pad);
        return card;
    }

    // "a fork of Artemis by @ClassicOldSong, which builds on Moonlight by @cgutman", with both handles linking to GitHub
    private static CharSequence builtOnLine(Context context) {
        String classic = "@ClassicOldSong";
        String moonlight = "@cgutman";
        String text = context.getString(R.string.developer_built_on_body, classic, moonlight);
        SpannableStringBuilder line = new SpannableStringBuilder(text);
        int classicAt = text.indexOf(classic);
        if (classicAt >= 0) {
            line.setSpan(new URLSpan(Links.githubUser("ClassicOldSong")), classicAt, classicAt + classic.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        int moonlightAt = text.indexOf(moonlight);
        if (moonlightAt >= 0) {
            line.setSpan(new URLSpan(Links.githubUser("cgutman")), moonlightAt, moonlightAt + moonlight.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return line;
    }

    private static void addChip(FlowRow row, int iconRes, CharSequence text, Runnable action) {
        Context context = row.getContext();
        TextView chip = (TextView) LayoutInflater.from(context).inflate(R.layout.sa_chip, row, false);
        chip.setText(text);
        Drawable icon = context.getDrawable(iconRes);
        if (icon != null) {
            int size = (int) Dialogs.dp(context, 16);
            icon.setBounds(0, 0, size, size);
            chip.setCompoundDrawablesRelative(icon, null, null, null);
        }
        chip.setOnClickListener(v -> action.run());
        row.addView(chip);
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
