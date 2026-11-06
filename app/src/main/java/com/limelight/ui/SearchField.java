package com.limelight.ui;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;

import com.limelight.R;

import java.util.Locale;
import java.util.function.Consumer;

// NOTE: The one search box of the key and action pickers, so the gesture editor and the quick bar editor filter exactly the same way.
// NOTE: Every word typed must appear somewhere in the label, in any order, and case is ignored.
public final class SearchField {
    private SearchField() {
    }

    public static EditText create(Context context, Consumer<String> onQuery) {
        EditText field = new EditText(context) {
            // NOTE: Back first hides the keyboard, and the field gives up focus then, so anything hidden while typing comes back.
            @Override
            public boolean onKeyPreIme(int keyCode, android.view.KeyEvent event) {
                if (keyCode == android.view.KeyEvent.KEYCODE_BACK && event.getAction() == android.view.KeyEvent.ACTION_UP && hasFocus()) {
                    clearFocus();
                }
                return super.onKeyPreIme(keyCode, event);
            }
        };
        field.setHint(R.string.picker_search_hint);
        field.setSingleLine(true);
        // NOTE: Without these flags a landscape phone shows the keyboard as a fullscreen text box that hides the dialog and its live results.
        // NOTE: The text stays in the field of the dialog, and the list under it filters as each letter is typed.
        field.setImeOptions(EditorInfo.IME_ACTION_SEARCH | EditorInfo.IME_FLAG_NO_EXTRACT_UI | EditorInfo.IME_FLAG_NO_FULLSCREEN);
        field.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_ck_search, 0, 0, 0);
        field.setCompoundDrawablePadding((int) Dialogs.dp(context, 8));
        int pad = (int) Dialogs.dp(context, 12);
        field.setPadding(pad, pad / 2, pad, pad / 2);
        field.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                onQuery.accept(s.toString());
            }
        });
        return field;
    }

    public static boolean matches(CharSequence label, String query) {
        if (query == null || query.trim().isEmpty()) {
            return true;
        }
        String text = String.valueOf(label).toLowerCase(Locale.ROOT);
        for (String word : query.trim().toLowerCase(Locale.ROOT).split("\\s+")) {
            if (!text.contains(word)) {
                return false;
            }
        }
        return true;
    }
}
