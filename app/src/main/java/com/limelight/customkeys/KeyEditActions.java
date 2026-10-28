package com.limelight.customkeys;

import android.content.Context;
import android.widget.Toast;

import com.limelight.R;
import com.limelight.menu.QuickBarLayout;

// NOTE: The one way to change an existing special key from any screen: Settings, the bar and menu editors, the gesture editor and the long-press menu of a bar button.
// NOTE: A custom key opens the same editor Settings uses and is saved under its own ID, so every bar, menu and gesture that points at it keeps working. A built-in key only changes its icon.
public final class KeyEditActions {
    public enum Kind {
        CUSTOM, BUILT_IN, NONE
    }

    private KeyEditActions() {
    }

    public static Kind kindOf(Context context, String keyId) {
        if (keyId == null || SpecialKeys.find(context, keyId) == null) {
            return Kind.NONE;
        }
        return BuiltInKeys.isBuiltIn(keyId) ? Kind.BUILT_IN : Kind.CUSTOM;
    }

    // Accepts the item of a bar or menu ("key:<id>") and returns what can be edited about it
    public static Kind kindOfItem(Context context, String item) {
        return kindOf(context, QuickBarLayout.customKeyId(item));
    }

    public static void editItem(Context context, String item, Runnable onChanged) {
        edit(context, QuickBarLayout.customKeyId(item), onChanged);
    }

    // Does nothing for a key that was deleted while the editor was open. onChanged runs after a save, so the caller refreshes its labels and icons.
    public static void edit(Context context, String keyId, Runnable onChanged) {
        CustomKey key = keyId != null ? SpecialKeys.find(context, keyId) : null;
        if (key == null) {
            return;
        }
        if (BuiltInKeys.isBuiltIn(keyId)) {
            IconPicker.showForBuiltIn(context, key, icon -> {
                SpecialKeys.setBuiltInIcon(context, keyId, icon);
                onChanged.run();
            });
            return;
        }
        CustomKeyEditor.show(context, key, edited -> {
            try {
                CustomKeyStore.upsert(context, edited);
            }
            catch (CustomKeyStore.FormatException e) {
                Toast.makeText(context, R.string.key_edit_failed, Toast.LENGTH_SHORT).show();
                return;
            }
            onChanged.run();
        });
    }
}
