package com.limelight.binding.video;

public interface CrashListener {
    void notifyCrash(Exception e);

    // The platform took the hardware decoder away (BUG-077), so the stream must end without a crash report
    default void onDecoderReclaimed() {
    }
}
