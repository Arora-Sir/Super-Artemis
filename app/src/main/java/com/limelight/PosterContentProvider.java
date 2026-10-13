package com.limelight;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import com.limelight.grid.assets.DiskAssetLoader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

public class PosterContentProvider extends ContentProvider {


    public static final String AUTHORITY = "poster." + BuildConfig.APPLICATION_ID;
    public static final String PNG_MIME_TYPE = "image/png";
    public static final int APP_ID_PATH_INDEX = 2;
    public static final int COMPUTER_UUID_PATH_INDEX = 1;
    private DiskAssetLoader mDiskAssetLoader;

    private static final UriMatcher sUriMatcher;
    private static final String BOXART_PATH = "boxart";
    private static final int BOXART_URI_ID = 1;

    // "*" matches one path segment, "#" matches a number. Constraining the shape here is
    // the first of three checks, the previous pattern only matched the bare "boxart"
    // path, so it never matched a real request, and openFile() ignored the result anyway.
    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(AUTHORITY, BOXART_PATH + "/*/#", BOXART_URI_ID);
    }

    /** Computer UUIDs as produced by the pairing flow. Anything else is not ours. */
    private static final Pattern UUID_PATTERN =
            Pattern.compile("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        // This provider is exported, so every caller is untrusted. The result of the match
        // is now honoured, it used to fall through to openBoxArtFile() regardless, which
        // made the URI pattern decorative.
        if (sUriMatcher.match(uri) != BOXART_URI_ID) {
            throw new FileNotFoundException();
        }
        return openBoxArtFile(uri, mode);
    }

    public ParcelFileDescriptor openBoxArtFile(Uri uri, String mode) throws FileNotFoundException {
        if (!"r".equals(mode)) {
            throw new UnsupportedOperationException("This provider is only for read mode");
        }

        List<String> segments = uri.getPathSegments();
        if (segments.size() != 3) {
            throw new FileNotFoundException();
        }

        String uuid = segments.get(COMPUTER_UUID_PATH_INDEX);
        String appId = segments.get(APP_ID_PATH_INDEX);

        // Second check: the UUID becomes a directory name, and getPathSegments() decodes
        // percent-escapes, so an encoded "%2F" arrives as a real separator and "../.."
        // walks out of the cache directory. Only accept the shape we actually produce.
        if (!UUID_PATTERN.matcher(uuid).matches()) {
            throw new FileNotFoundException();
        }

        int parsedAppId;
        try {
            parsedAppId = Integer.parseInt(appId);
        } catch (NumberFormatException e) {
            // "#" in the matcher should have caught this; belt and braces, and it stops an
            // exported component from crashing the app on malformed input.
            throw new FileNotFoundException();
        }

        File file = mDiskAssetLoader.getFile(uuid, parsedAppId);

        // Third check: whatever the path construction did, the result must still live
        // inside the box art directory. This is the one that holds even if the two above
        // are bypassed by something we did not anticipate.
        File boxArtRoot = mDiskAssetLoader.getBoxArtDirectory();
        String canonicalFile;
        String canonicalRoot;
        try {
            canonicalFile = file.getCanonicalPath();
            canonicalRoot = boxArtRoot.getCanonicalPath();
        } catch (IOException e) {
            throw new FileNotFoundException();
        }
        if (!canonicalFile.startsWith(canonicalRoot + File.separator)) {
            throw new FileNotFoundException();
        }

        if (file.exists()) {
            return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
        }
        throw new FileNotFoundException();
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("This provider is only for read mode");
    }

    @Override
    public String getType(Uri uri) {
        return PNG_MIME_TYPE;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException("This provider is only for read mode");
    }

    @Override
    public boolean onCreate() {
        mDiskAssetLoader = new DiskAssetLoader(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        throw new UnsupportedOperationException("This provider doesn't support query");
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        throw new UnsupportedOperationException("This provider is support read only");
    }


    public static Uri createBoxArtUri(String uuid, String appId) {
        return new Uri.Builder()
                .scheme(ContentResolver.SCHEME_CONTENT)
                .authority(AUTHORITY)
                .appendPath(BOXART_PATH)
                .appendPath(uuid)
                .appendPath(appId)
                .build();
    }

}
