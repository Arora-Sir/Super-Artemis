package com.limelight.ui;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.util.TypedValue;

import androidx.test.core.app.ApplicationProvider;

import com.limelight.R;
import com.limelight.shadows.ShadowMoonBridge;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// NOTE: Dialogs over the stream must carry the app style, and a plain AlertDialog.Builder on the activity silently drops it.
// NOTE: This test reads the sources of the stream screens and the shared dialog helpers and fails on any dialog built without the themed context.
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, shadows = {ShadowMoonBridge.class})
public class StreamDialogsTest {
    private static final String[] SOURCES = {
            "Game.java", "GameMenu.java", "utils/Dialog.java", "utils/SpinnerDialog.java"};
    // A builder or progress dialog whose context argument is anything other than a themed context
    private static final Pattern RAW_DIALOG = Pattern.compile("new (?:android\\.app\\.)?(AlertDialog\\.Builder|ProgressDialog)\\(([^)]*)\\)");

    private static File source(String path) {
        // Gradle runs unit tests from the module folder, an IDE may run them from the project root
        File module = new File("src/main/java/com/limelight/" + path);
        return module.exists() ? module : new File("app/src/main/java/com/limelight/" + path);
    }

    @Test
    public void streamDialogsUseTheThemedContext() throws Exception {
        List<String> problems = new ArrayList<>();
        for (String path : SOURCES) {
            File file = source(path);
            assertTrue("Missing source " + file.getAbsolutePath(), file.exists());
            String code = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            Matcher matcher = RAW_DIALOG.matcher(code);
            while (matcher.find()) {
                String argument = matcher.group(2);
                if (!argument.contains("themedContext") && !argument.contains("ThemedDialogs")) {
                    problems.add(path + ": " + matcher.group());
                }
            }
        }
        assertTrue("Dialogs without the app style: " + problems, problems.isEmpty());
    }

    @Test
    public void themedContextResolvesTheColorRoles() {
        Context context = ThemedDialogs.context(ApplicationProvider.getApplicationContext());
        TypedValue value = new TypedValue();
        assertTrue(context.getTheme().resolveAttribute(R.attr.saPrimary, value, true));
        assertNotEquals(0, Dialogs.color(context, R.attr.saPrimary));
    }
}
