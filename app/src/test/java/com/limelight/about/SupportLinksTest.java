package com.limelight.about;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Dialog;

import androidx.appcompat.app.AppCompatActivity;

import com.limelight.R;
import com.limelight.shadows.ShadowMoonBridge;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowDialog;

// NOTE: The Support dialog lists every payment path of the blueprint: UPI, PayPal and GitHub Sponsors, all reachable only from Settings.
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, shadows = {ShadowMoonBridge.class})
public class SupportLinksTest {
    @Test
    public void sponsorsLinkPointsAtTheDeveloperProfile() {
        assertEquals("https://github.com/sponsors/Arora-Sir", Links.SPONSORS);
        assertTrue(Links.SPONSORS.startsWith("https://"));
    }

    @Test
    public void supportDialogShowsUpiPayPalAndSponsors() {
        org.robolectric.android.controller.ActivityController<AppCompatActivity> controller = Robolectric.buildActivity(AppCompatActivity.class);
        controller.get().setTheme(R.style.AppTheme_MaterialYou);
        AppCompatActivity activity = controller.setup().get();
        SupportDialog.show(activity);
        Dialog dialog = ShadowDialog.getLatestDialog();
        assertNotNull(dialog.findViewById(R.id.support_upi_app));
        assertNotNull(dialog.findViewById(R.id.support_paypal));
        assertNotNull(dialog.findViewById(R.id.support_sponsors));
        dialog.dismiss();
    }
}
