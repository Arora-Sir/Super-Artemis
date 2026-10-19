package com.limelight;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.content.res.XmlResourceParser;

import androidx.test.core.app.ApplicationProvider;

import com.limelight.shadows.ShadowMoonBridge;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.xmlpull.v1.XmlPullParser;

// NOTE: Apps check for enabled services with spoken feedback to decide whether a screen reader is on.
// NOTE: The keyboard service only filters key events, so it must never declare spoken feedback (upstream #609, YouTube read its UI aloud).
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, shadows = {ShadowMoonBridge.class})
public class KeyboardServiceFeedbackTest {
    @Test
    public void keyboardServiceDeclaresGenericFeedbackOnly() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        XmlResourceParser parser = context.getResources().getXml(R.xml.keyboard_accessibility_service);
        int feedback = -1;
        for (int event = parser.getEventType(); event != XmlPullParser.END_DOCUMENT; event = parser.next()) {
            if (event == XmlPullParser.START_TAG && "accessibility-service".equals(parser.getName())) {
                feedback = parser.getAttributeIntValue("http://schemas.android.com/apk/res/android", "accessibilityFeedbackType", -1);
            }
        }
        assertEquals(AccessibilityServiceInfo.FEEDBACK_GENERIC, feedback);
        assertTrue((feedback & AccessibilityServiceInfo.FEEDBACK_SPOKEN) == 0);
    }
}
