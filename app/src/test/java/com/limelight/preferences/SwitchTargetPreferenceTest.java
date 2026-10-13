package com.limelight.preferences;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.preference.PreferenceViewHolder;
import androidx.test.core.app.ApplicationProvider;

import com.limelight.R;
import com.limelight.shadows.ShadowMoonBridge;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, shadows = {ShadowMoonBridge.class})
public class SwitchTargetPreferenceTest {
    @Test
    public void rowOpensTheEditorAndOnlyTheSwitchSideToggles() {
        Context context = new android.view.ContextThemeWrapper(ApplicationProvider.getApplicationContext(), R.style.AppTheme_MaterialYou);
        PreferenceManager manager = new PreferenceManager(context);
        PreferenceScreen screen = manager.createPreferenceScreen(context);
        SwitchTargetPreference preference = new SwitchTargetPreference(context, null);
        preference.setKey("switch_target_test");
        preference.setTitle("Gestures");
        preference.setPersistent(false);
        screen.addPreference(preference);
        preference.setChecked(true);

        boolean[] opened = {false};
        preference.setOnPreferenceClickListener(p -> {
            opened[0] = true;
            return true;
        });
        preference.performClick();
        assertTrue(opened[0]);
        assertTrue(preference.isChecked());

        FrameLayout parent = new FrameLayout(context);
        View row = LayoutInflater.from(context).inflate(preference.getLayoutResource(), parent, false);
        ViewGroup widgetFrame = row.findViewById(android.R.id.widget_frame);
        assertNotNull(widgetFrame);
        LayoutInflater.from(context).inflate(preference.getWidgetLayoutResource(), widgetFrame);
        preference.onBindViewHolder(PreferenceViewHolder.createInstanceForTests(row));

        widgetFrame.performClick();
        assertFalse(preference.isChecked());
    }
}
