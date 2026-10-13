package com.limelight.about;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.Intent;
import android.text.Spanned;
import android.text.style.URLSpan;

import androidx.test.core.app.ApplicationProvider;

import com.limelight.shadows.ShadowMoonBridge;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, shadows = {ShadowMoonBridge.class})
public class AboutTest {
    private final Context context = ApplicationProvider.getApplicationContext();

    @Test
    public void bundledCreditsAreValidAndEveryEntryIsComplete() throws Exception {
        List<CreditsRepository.Entry> entries = CreditsRepository.load(context);
        assertFalse(entries.isEmpty());
        for (CreditsRepository.Entry entry : entries) {
            assertFalse(entry.feature.isEmpty());
            assertFalse("role id was not mapped for " + entry.feature, entry.role.contains("-") && entry.role.equals(entry.role.toLowerCase()));
            for (CreditsRepository.Link link : entry.links) {
                assertTrue(link.url, link.url.startsWith("https://"));
            }
            for (String handle : entry.handles) {
                assertFalse(handle, handle.startsWith("@") || handle.contains(" "));
            }
        }
    }

    @Test
    public void creditLineLinksEachHandleToItsGithubProfile() throws Exception {
        List<CreditsRepository.Entry> entries = CreditsRepository.parse("{\"roles\":{\"code\":\"Code ported\"},\"entries\":["
                + "{\"feature\":\"Mic\",\"role\":\"code\",\"people\":[{\"handle\":\"logabell\"},{\"handle\":\"Yundi339\"}],"
                + "\"links\":[{\"label\":\"owner/repo#1\",\"url\":\"https://github.com/owner/repo/pull/1\"}]}]}");
        CharSequence line = AboutDialogs.creditLine(context, entries.get(0));
        assertEquals("Code ported by @logabell, @Yundi339", line.toString());
        URLSpan[] spans = ((Spanned) line).getSpans(0, line.length(), URLSpan.class);
        assertEquals(2, spans.length);
        assertEquals("https://github.com/logabell", spans[0].getURL());
        // Each link becomes one chip with this label
        assertEquals("owner/repo#1", entries.get(0).links.get(0).label);
        assertEquals("https://github.com/owner/repo/pull/1", entries.get(0).links.get(0).url);
    }

    @Test
    public void upiIntentCarriesTheSamePaymentAsTheQrCode() {
        Intent intent = SupportDialog.upiIntent();
        assertEquals(Intent.ACTION_VIEW, intent.getAction());
        assertEquals("upi", intent.getData().getScheme());
        assertEquals(Links.UPI_ID, intent.getData().getQueryParameter("pa"));
        assertEquals("INR", intent.getData().getQueryParameter("cu"));
    }

    @Test
    public void linksPointAtThisFork() {
        assertTrue(Links.RELEASES.startsWith(Links.REPOSITORY));
        assertTrue(Links.PRIVACY.endsWith("/PRIVACY.md"));
        assertTrue(Links.REPOSITORY.endsWith("/Arora-Sir/Super-Artemis"));
    }
}
