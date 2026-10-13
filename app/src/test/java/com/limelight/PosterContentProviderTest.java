package com.limelight;

import android.content.ContentResolver;
import android.net.Uri;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * The provider is exported, so every caller is untrusted.
 *
 * It used to ignore the UriMatcher result entirely and hand the UUID segment straight to
 * the path builder. Because {@link Uri#getPathSegments()} decodes percent-escapes, an
 * encoded separator arrived as a real one and walked out of the cache directory, any app
 * on the device could read files it had no business reading.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = {33})
public class PosterContentProviderTest {

    private static final String VALID_UUID = "3f2b1a9c-4d5e-6f70-8192-a3b4c5d6e7f8";

    private PosterContentProvider provider;
    private File cacheDir;

    @Before
    public void setUp() {
        provider = Robolectric.buildContentProvider(PosterContentProvider.class).create().get();
        cacheDir = org.robolectric.RuntimeEnvironment.getApplication().getCacheDir();
    }

    private Uri boxArtUri(String uuid, String appId) {
        return new Uri.Builder()
                .scheme(ContentResolver.SCHEME_CONTENT)
                .authority(PosterContentProvider.AUTHORITY)
                .appendPath("boxart")
                .appendPath(uuid)
                .appendPath(appId)
                .build();
    }

    /** Writes a real file so a legitimate request has something to return. */
    private File seedBoxArt(String uuid, int appId) throws Exception {
        File dir = new File(new File(cacheDir, "boxart"), uuid);
        assertTrue("could not create " + dir, dir.mkdirs() || dir.isDirectory());
        File f = new File(dir, appId + ".png");
        try (FileOutputStream out = new FileOutputStream(f)) {
            out.write(new byte[] {(byte) 0x89, 'P', 'N', 'G'});
        }
        return f;
    }

    private void assertRejected(Uri uri, String why) {
        try {
            provider.openFile(uri, "r");
            fail("should have rejected " + why + ": " + uri);
        } catch (FileNotFoundException expected) {
            // correct
        } catch (Exception other) {
            fail("expected FileNotFoundException for " + why + ", got " + other);
        }
    }

    @Test
    public void legitimateBoxArtIsServed() throws Exception {
        seedBoxArt(VALID_UUID, 42);
        assertNotNull(provider.openFile(boxArtUri(VALID_UUID, "42"), "r"));
    }

    /**
     * Plants a real file outside the box art directory and tries to reach it by traversal.
     *
     * Planting the file is the whole point. An earlier version of this test only asserted
     * that traversal URIs were rejected, and it passed against the vulnerable code,
     * because the path it walked to happened not to exist. A test that cannot fail on the
     * bug it was written for is worse than no test, since it reads as coverage.
     */
    @Test
    public void traversalCannotReachAFileOutsideTheBoxArtDirectory() throws Exception {
        // cacheDir/boxart/../999.png resolves to cacheDir/999.png
        File secret = new File(cacheDir, "999.png");
        try (FileOutputStream out = new FileOutputStream(secret)) {
            out.write("this must never be served".getBytes("UTF-8"));
        }
        assertTrue("fixture not written", secret.exists());

        assertRejected(boxArtUri("..", "999"), "traversal to a file that really exists");

        // And one level further up, outside the cache directory entirely.
        File parent = cacheDir.getParentFile();
        if (parent != null) {
            File higher = new File(parent, "998.png");
            try (FileOutputStream out = new FileOutputStream(higher)) {
                out.write("also must never be served".getBytes("UTF-8"));
            }
            assertRejected(boxArtUri("../..", "998"), "traversal above the cache directory");
        }
    }

    @Test
    public void traversalShapedUuidsAreRejected() {
        assertRejected(boxArtUri("../../databases", "1"), "encoded traversal");
        assertRejected(boxArtUri("..", "1"), "parent directory");
        assertRejected(boxArtUri("../shared_prefs", "1"), "sibling directory");
    }

    @Test
    public void nonUuidSegmentsAreRejected() {
        assertRejected(boxArtUri("not-a-uuid", "1"), "malformed uuid");
        assertRejected(boxArtUri("", "1"), "empty uuid");
        assertRejected(boxArtUri("3f2b1a9c4d5e6f708192a3b4c5d6e7f8", "1"), "uuid without dashes");
    }

    @Test
    public void nonNumericAppIdIsRejectedWithoutCrashing() {
        // Previously Integer.parseInt() threw NumberFormatException out of an exported
        // component, which is a crash any app could trigger.
        assertRejected(boxArtUri(VALID_UUID, "../../secret"), "non-numeric app id");
        assertRejected(boxArtUri(VALID_UUID, "abc"), "non-numeric app id");
    }

    @Test
    public void unexpectedPathsAreRejected() {
        Uri wrongRoot = new Uri.Builder()
                .scheme(ContentResolver.SCHEME_CONTENT)
                .authority(PosterContentProvider.AUTHORITY)
                .appendPath("secrets")
                .appendPath(VALID_UUID)
                .appendPath("1")
                .build();
        assertRejected(wrongRoot, "unknown path root");

        Uri tooShort = new Uri.Builder()
                .scheme(ContentResolver.SCHEME_CONTENT)
                .authority(PosterContentProvider.AUTHORITY)
                .appendPath("boxart")
                .build();
        assertRejected(tooShort, "missing segments");
    }

    @Test
    public void writeModesAreRefused() throws Exception {
        seedBoxArt(VALID_UUID, 7);
        for (String mode : new String[] {"w", "rw", "wt"}) {
            try {
                provider.openFile(boxArtUri(VALID_UUID, "7"), mode);
                fail("should have refused mode " + mode);
            } catch (UnsupportedOperationException | FileNotFoundException expected) {
                // either is an acceptable refusal
            }
        }
    }

    @Test
    public void missingFileDoesNotLeakExistence() {
        // A UUID that is well-formed but was never cached must look the same as a rejected
        // one, so the provider cannot be used to probe for what is on disk.
        assertRejected(boxArtUri("00000000-0000-0000-0000-000000000000", "999"), "absent file");
    }
}
