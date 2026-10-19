package com.limelight.ui;

import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.widget.EditText;

import androidx.test.core.app.ApplicationProvider;

import com.limelight.R;
import com.limelight.shadows.ShadowMoonBridge;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

// NOTE: Dialogs that build their text field in code (restore password, key step values, set names) pass the text field style as the default style.
// NOTE: That drops the platform editTextStyle, so the style itself must make the field focusable in touch mode, or a tap never opens the keyboard.
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, shadows = {ShadowMoonBridge.class})
public class TextFieldsTest {
    @Test
    public void codeBuiltFieldsTakeFocusFromATap() {
        Context base = ApplicationProvider.getApplicationContext();
        Context classic = new android.view.ContextThemeWrapper(new android.view.ContextThemeWrapper(base, R.style.AppTheme), R.style.ThemeOverlay_SuperArtemis_Classic);
        for (Context context : new Context[] {new android.view.ContextThemeWrapper(base, R.style.AppTheme_MaterialYou), classic}) {
            EditText field = new EditText(context, null, 0, R.style.Widget_SuperArtemis_TextField);
            assertTrue(field.isFocusableInTouchMode());
            assertTrue(field.isClickable());
            assertTrue(field.isLongClickable());
            assertTrue(field.onCheckIsTextEditor());
        }
    }
}
