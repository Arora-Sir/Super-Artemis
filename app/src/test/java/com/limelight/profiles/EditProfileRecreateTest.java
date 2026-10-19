package com.limelight.profiles;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.limelight.EditProfileActivity;
import com.limelight.R;
import com.limelight.shadows.ShadowMoonBridge;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;

// NOTE: The system rebuilds the profile editor after a theme switch, a rotation or process death, and rebuilds its settings fragment by class name.
// NOTE: The fragment once had no constructor without arguments, and the editor crashed with "could not find Fragment constructor" (seen on the S24).
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, shadows = {ShadowMoonBridge.class})
public class EditProfileRecreateTest {
    @Test
    public void profileEditorSurvivesBeingRebuiltBySystem() {
        ActivityController<EditProfileActivity> controller = Robolectric.buildActivity(EditProfileActivity.class).setup();
        controller.recreate();
        EditProfileActivity activity = controller.get();
        assertNotNull(activity.getSupportFragmentManager().findFragmentById(R.id.preferences_container));
        assertTrue(activity.getSupportFragmentManager().findFragmentById(R.id.preferences_container)
                instanceof EditProfileActivity.ProfilePreferenceFragment);
        assertNotNull(activity.getInMemoryPrefs());
    }
}
