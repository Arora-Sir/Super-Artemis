package com.limelight.binding.video;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.media.MediaCodec;

import org.junit.Test;

// NOTE: Covers BUG-077, where the system reclaimed the hardware decoder from a picture-in-picture stream.
public class DecoderReclaimTest {
    @Test
    public void errorCode1101IsReclaimed() {
        assertTrue(DecoderReclaim.isReclaimed(1101));
    }

    @Test
    public void otherCodesAreNotReclaimed() {
        assertFalse(DecoderReclaim.isReclaimed(0));
        assertFalse(DecoderReclaim.isReclaimed(1100));
        assertFalse(DecoderReclaim.isReclaimed(1102));
        assertFalse(DecoderReclaim.isReclaimed(-1));
    }

    @Test
    public void literalMatchesThePlatformConstant() {
        assertEquals(MediaCodec.CodecException.ERROR_RECLAIMED, DecoderReclaim.ERROR_RECLAIMED);
    }

    @Test
    public void aPlainIllegalStateIsNotReclaimed() {
        assertFalse(DecoderReclaim.isReclaimed(new IllegalStateException("x")));
    }
}
