package com.limelight.about;

import android.app.AlertDialog;
import android.content.Context;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.URLSpan;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.limelight.LimeLog;
import com.limelight.R;

import java.util.List;

// The developer profile list and the in-app credits, both opened from Settings > About
public final class AboutDialogs {
    private AboutDialogs() {
    }

    public static void showProfiles(Context context) {
        String[] labels = {
                context.getString(R.string.profile_website),
                context.getString(R.string.profile_github),
                context.getString(R.string.profile_linkedin),
                context.getString(R.string.profile_x)
        };
        String[] urls = {Links.WEBSITE, Links.GITHUB_PROFILE, Links.LINKEDIN, Links.X_PROFILE};
        new AlertDialog.Builder(context)
                .setTitle(R.string.title_developer_profiles)
                .setItems(labels, (dialog, which) -> Links.open(context, urls[which]))
                .setNegativeButton(R.string.game_menu_cancel, null)
                .show();
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

    static CharSequence linkLine(CreditsRepository.Entry entry) {
        SpannableStringBuilder text = new SpannableStringBuilder();
        for (CreditsRepository.Link link : entry.links) {
            if (text.length() > 0) {
                text.append("   ");
            }
            int start = text.length();
            text.append(link.label);
            text.setSpan(new URLSpan(link.url), start, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return text;
    }

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

        float density = context.getResources().getDisplayMetrics().density;
        int padding = (int) (24 * density);
        LinearLayout list = new LinearLayout(context);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(padding, (int) (4 * density), padding, (int) (8 * density));

        TextView intro = new TextView(context);
        intro.setText(R.string.credits_intro);
        intro.setTextSize(14);
        list.addView(intro);

        for (CreditsRepository.Entry entry : entries) {
            TextView feature = new TextView(context);
            feature.setText(entry.feature);
            feature.setTextSize(16);
            feature.setPadding(0, (int) (16 * density), 0, 0);
            list.addView(feature);

            TextView credit = new TextView(context);
            credit.setText(creditLine(context, entry));
            credit.setTextSize(14);
            credit.setMovementMethod(LinkMovementMethod.getInstance());
            list.addView(credit);

            CharSequence links = linkLine(entry);
            if (!TextUtils.isEmpty(links)) {
                TextView linkView = new TextView(context);
                linkView.setText(links);
                linkView.setTextSize(13);
                linkView.setMovementMethod(LinkMovementMethod.getInstance());
                list.addView(linkView);
            }
        }

        ScrollView scroll = new ScrollView(context);
        scroll.addView(list);
        new AlertDialog.Builder(context)
                .setTitle(R.string.title_credits)
                .setView(scroll)
                .setPositiveButton(R.string.custom_keys_done, null)
                .show();
    }
}
