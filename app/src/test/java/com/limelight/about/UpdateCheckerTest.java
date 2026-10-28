package com.limelight.about;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.app.Dialog;

import androidx.appcompat.app.AppCompatActivity;

import com.limelight.R;
import com.limelight.shadows.ShadowMoonBridge;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowDialog;

import java.io.IOException;

// NOTE: The update check is one manual request to GitHub. These tests never go online, they feed the parser the answers GitHub gives.
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, shadows = {ShadowMoonBridge.class})
public class UpdateCheckerTest {
    private static String release(String tag, String url, String extra) {
        return "{\"tag_name\":\"" + tag + "\",\"name\":\"Super Artemis " + tag + "\",\"html_url\":\"" + url + "\",\"body\":\"Fixes\nand more\"" + extra + "}";
    }

    @Test
    public void versionsCompareByNumberAndIgnoreTheVPrefixAndSuffixes() {
        assertTrue(UpdateChecker.isNewer("v1.0.1", "1.0.0"));
        assertTrue(UpdateChecker.isNewer("v1.1.0", "1.0.9"));
        assertTrue(UpdateChecker.isNewer("2.0", "1.9.9"));
        assertTrue(UpdateChecker.isNewer("v1.10.0", "1.9.0"));
        assertFalse(UpdateChecker.isNewer("v1.0.0", "1.0.0"));
        assertFalse(UpdateChecker.isNewer("v1.0", "1.0.0"));
        assertFalse(UpdateChecker.isNewer("v0.9.9", "1.0.0"));
        assertFalse(UpdateChecker.isNewer("v1.0.0-beta", "1.0.0"));
        assertFalse(UpdateChecker.isNewer("garbage", "1.0.0"));
    }

    @Test
    public void aNewerReleaseIsAnUpdate() throws Exception {
        UpdateChecker.Result result = UpdateChecker.evaluate(release("v1.1.0", Links.RELEASES + "/tag/v1.1.0", ""), "1.0.0");
        assertEquals(UpdateChecker.Status.UPDATE_AVAILABLE, result.status);
        assertEquals("v1.1.0", result.release.tag);
        assertEquals(Links.RELEASES + "/tag/v1.1.0", result.release.pageUrl);
    }

    @Test
    public void theSameReleaseIsUpToDate() throws Exception {
        assertEquals(UpdateChecker.Status.UP_TO_DATE,
                UpdateChecker.evaluate(release("v1.0.0", Links.RELEASES + "/tag/v1.0.0", ""), "1.0.0").status);
    }

    @Test
    public void draftsAndPrereleasesAreNotOffered() throws Exception {
        assertEquals(UpdateChecker.Status.NO_RELEASE,
                UpdateChecker.evaluate(release("v9.0.0", Links.RELEASES, ",\"draft\":true"), "1.0.0").status);
        assertEquals(UpdateChecker.Status.NO_RELEASE,
                UpdateChecker.evaluate(release("v9.0.0", Links.RELEASES, ",\"prerelease\":true"), "1.0.0").status);
        assertEquals(UpdateChecker.Status.NO_RELEASE, UpdateChecker.evaluate("{}", "1.0.0").status);
    }

    @Test
    public void aLinkOutsideThisProjectIsNeverOpened() throws Exception {
        UpdateChecker.Result result = UpdateChecker.evaluate(release("v2.0.0", "https://evil.example/download", ""), "1.0.0");
        assertEquals(Links.RELEASES, result.release.pageUrl);
    }

    @Test
    public void anUnreadableAnswerIsAnError() {
        try {
            UpdateChecker.evaluate("<html>not json</html>", "1.0.0");
            fail("Expected an IOException");
        }
        catch (IOException expected) {
            // The caller turns this into the "could not check" dialog
        }
    }

    @Test
    public void longReleaseNotesAreCutAtALineEnd() {
        StringBuilder body = new StringBuilder();
        for (int i = 0; i < 400; i++) {
            body.append("Line number ").append(i).append("\n");
        }
        String shown = UpdateDialogs.notes(body.toString());
        assertTrue(shown.length() <= UpdateDialogs.NOTES_MAX_CHARS + 8);
        assertTrue(shown.endsWith("..."));
        assertEquals("short", UpdateDialogs.notes("short\r\n"));
    }

    @Test
    public void everyResultHasADialog() throws Exception {
        for (UpdateChecker.Status status : UpdateChecker.Status.values()) {
            ActivityController<AppCompatActivity> controller = Robolectric.buildActivity(AppCompatActivity.class);
            controller.get().setTheme(R.style.AppTheme_MaterialYou);
            AppCompatActivity activity = controller.setup().get();
            UpdateChecker.Release release = status == UpdateChecker.Status.UPDATE_AVAILABLE
                    ? UpdateChecker.evaluate(release("v1.1.0", Links.RELEASES, ""), "1.0.0").release : null;
            UpdateDialogs.showResult(activity, resultOf(status, release));
            Dialog dialog = ShadowDialog.getLatestDialog();
            assertNotNull("No dialog for " + status, dialog);
            assertTrue(dialog.isShowing());
            dialog.dismiss();
        }
    }

    // The result constructor is package private, so a result is made the way the checker makes one
    private static UpdateChecker.Result resultOf(UpdateChecker.Status status, UpdateChecker.Release release) throws IOException {
        switch (status) {
            case UPDATE_AVAILABLE:
                return UpdateChecker.evaluate(release("v1.1.0", Links.RELEASES, ""), "1.0.0");
            case UP_TO_DATE:
                return UpdateChecker.evaluate(release("v1.0.0", Links.RELEASES, ""), "1.0.0");
            case NO_RELEASE:
                return UpdateChecker.evaluate("{}", "1.0.0");
            default:
                return new UpdateChecker.Result(UpdateChecker.Status.FAILED, null);
        }
    }
}
