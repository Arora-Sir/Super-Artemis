package com.limelight.preferences;

import static org.junit.Assert.assertEquals;

import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.limelight.shadows.ShadowMoonBridge;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

// NOTE: A settings row title that was one line with dots at the end now wraps to two lines on a narrow phone.
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, shadows = {ShadowMoonBridge.class})
public class SettingsListStyleTitleTest {
    @Test
    public void aSingleLineTitleBecomesATwoLineTitleThatEndsWithDots() {
        AppCompatActivity activity = Robolectric.buildActivity(AppCompatActivity.class).setup().get();
        LinearLayout row = new LinearLayout(activity);
        TextView title = new TextView(activity);
        title.setId(android.R.id.title);
        title.setSingleLine(true);
        row.addView(title);

        SettingsListStyle.allowTwoLineTitle(row);

        assertEquals(2, title.getMaxLines());
        assertEquals(android.text.TextUtils.TruncateAt.END, title.getEllipsize());
    }

    @Test
    public void aRowWithoutATitleIsLeftAlone() {
        AppCompatActivity activity = Robolectric.buildActivity(AppCompatActivity.class).setup().get();
        View plain = new View(activity);
        SettingsListStyle.allowTwoLineTitle(plain);
    }
}
