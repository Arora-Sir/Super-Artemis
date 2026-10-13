package com.limelight.about;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.widget.Toast;

import com.limelight.R;

// Every external link the app opens for Super Artemis itself, kept in one place so a moved page is fixed once
public final class Links {
    public static final String REPOSITORY = "https://github.com/Arora-Sir/Super-Artemis";
    public static final String RELEASES = REPOSITORY + "/releases";
    public static final String NEW_ISSUE = REPOSITORY + "/issues/new/choose";
    public static final String ISSUES = REPOSITORY + "/issues";
    // The bug report form, whose fields can be filled from the link by their ids in .github/ISSUE_TEMPLATE/bug_report.yml
    public static final String BUG_REPORT = REPOSITORY + "/issues/new?template=bug_report.yml";
    public static final String PRIVACY = REPOSITORY + "/blob/HEAD/PRIVACY.md";
    public static final String LICENSE = REPOSITORY + "/blob/HEAD/LICENSE.txt";

    public static final String PAYPAL = "https://paypal.me/arorasir";
    public static final String UPI_ID = "mohit1998arora@yescred";
    // Same payment as the bundled QR code (res/drawable-nodpi/support_upi_qr.png), so both lead to one place
    public static final String UPI_PAYMENT = "upi://pay?pa=" + UPI_ID + "&pn=Mohit%20Arora&cu=INR&tn=Super%20Artemis%20support";

    public static final String WEBSITE = "https://mohitarora.me";
    public static final String GITHUB_PROFILE = "https://github.com/Arora-Sir";
    public static final String LINKEDIN = "https://www.linkedin.com/in/arora-sir";
    public static final String X_PROFILE = "https://x.com/MohitAr0rA";
    // Short forms shown under each profile link
    public static final String WEBSITE_LABEL = "mohitarora.me";
    public static final String GITHUB_LABEL = "@Arora-Sir";
    public static final String LINKEDIN_LABEL = "in/arora-sir";
    public static final String X_LABEL = "@MohitAr0rA";

    private Links() {
    }

    public static String githubUser(String handle) {
        return "https://github.com/" + handle;
    }

    // Opens a link in the matching app or the browser, and says so instead of crashing when nothing can open it
    public static void open(Context context, String url) {
        try {
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        }
        catch (ActivityNotFoundException e) {
            Toast.makeText(context, context.getString(R.string.about_no_app_for_link, url), Toast.LENGTH_LONG).show();
        }
    }

    // NOTE: Opens a GitHub page in the default browser, skipping the GitHub app that claims every github.com link.
    // NOTE: The GitHub app cannot show the issue form chooser, so it opens its home screen and asks again which app to use.
    // NOTE: A plain https link with no host resolves to the browser the user picked as default, which is the app this page needs.
    public static void openInBrowser(Context context, String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        String browser = defaultBrowser(context);
        if (browser != null) {
            try {
                context.startActivity(new Intent(intent).setPackage(browser));
                return;
            }
            catch (ActivityNotFoundException e) {
                // Fall back to the usual link handling below
            }
        }
        open(context, url);
    }

    // The package of the default browser, or null when there is none or the system would ask which one to use
    static String defaultBrowser(Context context) {
        Intent probe = new Intent(Intent.ACTION_VIEW, Uri.parse("https://"));
        probe.addCategory(Intent.CATEGORY_BROWSABLE);
        ResolveInfo info = context.getPackageManager().resolveActivity(probe, PackageManager.MATCH_DEFAULT_ONLY);
        if (info == null || info.activityInfo == null) {
            return null;
        }
        String pkg = info.activityInfo.packageName;
        // The system resolver means no default is set
        return "android".equals(pkg) || pkg.contains("resolver") ? null : pkg;
    }
}
