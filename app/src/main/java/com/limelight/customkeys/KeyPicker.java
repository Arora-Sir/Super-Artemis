package com.limelight.customkeys;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.widget.Button;
import android.widget.Toast;

import com.limelight.R;
import com.limelight.ui.Dialogs;

// NOTE: One way to create a special key from any picker: the Send special keys menu, the quick-action bar editor and its long-press menu, the gesture editor and the menu editor.
// NOTE: The new key is saved with the other special keys first, so it appears in every picker, then handed back to the picker that asked for it.
public final class KeyPicker {
    public interface OnCreated {
        void onCreated(CustomKey key);
    }

    private KeyPicker() {
    }

    public static void createKey(Context context, OnCreated onCreated) {
        CustomKeyEditor.show(context, null, key -> {
            CustomKey saved;
            try {
                saved = CustomKeyStore.upsert(context, key);
            }
            catch (CustomKeyStore.FormatException e) {
                Toast.makeText(context, R.string.wrong_import_format, Toast.LENGTH_SHORT).show();
                return;
            }
            onCreated.onCreated(saved);
        });
    }

    // NOTE: The "Add key or flow" button that sits under a picker list.
    // NOTE: The button lives in the picker's dialog, but the key editor opens from the activity context, because a dialog theme drops the chip and segment styles of a nested dialog.
    public static Button addButton(Context context, Context editorContext, OnCreated onCreated) {
        Button button = new Button(context, null, 0, R.style.Widget_SuperArtemis_Button_Tonal);
        button.setText(R.string.key_picker_add);
        button.setGravity(Gravity.CENTER);
        Drawable icon = context.getDrawable(R.drawable.ic_gm_add);
        if (icon != null) {
            int size = (int) Dialogs.dp(context, 18);
            icon.setBounds(0, 0, size, size);
            icon.setTint(button.getCurrentTextColor());
            button.setCompoundDrawablesRelative(icon, null, null, null);
            button.setCompoundDrawablePadding((int) Dialogs.dp(context, 8));
        }
        button.setOnClickListener(v -> createKey(editorContext, onCreated));
        return button;
    }
}
