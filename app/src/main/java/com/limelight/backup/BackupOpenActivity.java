package com.limelight.backup;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.limelight.preferences.StreamSettings;

// NOTE: Receives a backup opened from a file manager, a download notification or the share sheet, and hands it to Settings,
// NOTE: which runs the usual password prompt and restore preview. Nothing is restored without that preview.
public class BackupOpenActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        android.os.Parcelable shared = Intent.ACTION_SEND.equals(intent.getAction()) ? intent.getParcelableExtra(Intent.EXTRA_STREAM) : intent.getData();
        Uri uri = shared instanceof Uri ? (Uri) shared : null;
        if (uri != null) {
            Intent settings = new Intent(this, StreamSettings.class)
                    .setData(uri)
                    .putExtra(StreamSettings.EXTRA_RESTORE_BACKUP, true)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(settings);
        }
        finish();
    }
}
