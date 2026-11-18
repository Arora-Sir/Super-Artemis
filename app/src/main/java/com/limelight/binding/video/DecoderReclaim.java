package com.limelight.binding.video;

import android.media.MediaCodec;
import android.os.Build;

// NOTE: Android takes a hardware decoder away from a background or picture-in-picture app when another app asks for the same decoder slot (MediaCodec error 1101, ERROR_RECLAIMED).
// NOTE: That is a platform decision, not a decoder fault, so it ends the stream cleanly and offers a reconnect instead of counting as a crash (BUG-077).
public final class DecoderReclaim {
    // NOTE: Value of MediaCodec.CodecException.ERROR_RECLAIMED, kept as a literal so the check also compiles and runs below API 23.
    public static final int ERROR_RECLAIMED = 1101;

    private DecoderReclaim() {
    }

    public static boolean isReclaimed(int errorCode) {
        return errorCode == ERROR_RECLAIMED;
    }

    // Reads the error code only where the platform provides it, and treats anything else as not reclaimed
    public static boolean isReclaimed(IllegalStateException e) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || !(e instanceof MediaCodec.CodecException)) {
            return false;
        }
        return isReclaimed(((MediaCodec.CodecException) e).getErrorCode());
    }
}
