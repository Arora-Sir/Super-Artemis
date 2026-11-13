package com.limelight.ui;

import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.limelight.R;
import com.limelight.customkeys.KeyNames;
import com.limelight.customkeys.RecorderKeyboard;

import java.util.ArrayList;
import java.util.List;

// NOTE: One full-screen search layer for the key and action pickers, shared by the quick bar editor and the gesture picker so both behave the same.
// NOTE: It lives in its own window with its own keyboard handling, so the editor behind it is never squeezed. On a phone in landscape the keyboard takes more than half of a dialog.
// NOTE: Type mode filters by the words typed. Press mode shows the in-app keyboard and lists the keys, flows and shortcuts that use every key pressed.
public final class FindPanel {
    public static final class Item {
        public final String id;
        public final CharSequence label;
        public final int iconRes;
        // A short line under the label, for example the group the item belongs to
        public final CharSequence subtitle;
        // Windows virtual key codes the item sends, or null for an item without keys such as a stream action
        public final List<Integer> keys;
        public boolean checked;

        public Item(String id, CharSequence label, int iconRes, CharSequence subtitle, List<Integer> keys, boolean checked) {
            this.id = id;
            this.label = label;
            this.iconRes = iconRes;
            this.subtitle = subtitle;
            this.keys = keys;
            this.checked = checked;
        }
    }

    public interface Listener {
        // Returns whether the item is checked after the pick. A panel that stays open shows that state on the row.
        boolean onPick(Item item);
    }

    // Left and right modifiers count as the same key, so a search for Ctrl finds a shortcut that uses either side
    static int normalize(int vk) {
        switch (vk) {
            case 0xA0:
            case 0xA1:
                return 0x10;
            case 0xA2:
            case 0xA3:
                return 0x11;
            case 0xA4:
            case 0xA5:
                return 0x12;
            case 0x5C:
                return 0x5B;
            default:
                return vk;
        }
    }

    // NOTE: The button that opens the panel, drawn like the Add key or flow button so every action button of the editors feels the same.
    public static android.widget.Button openButton(Context context, int labelRes, Runnable onClick) {
        return openButton(context, labelRes, R.drawable.ic_ck_search, onClick);
    }

    public static android.widget.Button openButton(Context context, int labelRes, int iconRes, Runnable onClick) {
        android.widget.Button button = new android.widget.Button(context, null, 0, R.style.Widget_SuperArtemis_Button_Tonal);
        button.setText(labelRes);
        button.setGravity(android.view.Gravity.CENTER);
        android.graphics.drawable.Drawable icon = context.getDrawable(iconRes);
        if (icon != null) {
            int size = (int) Dialogs.dp(context, 18);
            icon.setBounds(0, 0, size, size);
            icon.setTint(button.getCurrentTextColor());
            button.setCompoundDrawablesRelative(icon, null, null, null);
            button.setCompoundDrawablePadding((int) Dialogs.dp(context, 8));
        }
        button.setOnClickListener(v -> onClick.run());
        return button;
    }

    // NOTE: The key labels of the built-in layout show two characters on a symbol key, the key and its shifted twin, so one tap would read as two keys.
    // NOTE: A symbol key is named by the one character it types without Shift, and every other key keeps its label from the layout.
    static String symbolName(int vk) {
        switch (vk) {
            case 0xBA:
                return ";";
            case 0xBB:
                return "=";
            case 0xBC:
                return ",";
            case 0xBD:
                return "-";
            case 0xBE:
                return ".";
            case 0xBF:
                return "/";
            case 0xC0:
                return "`";
            case 0xDB:
                return "[";
            case 0xDC:
                return "\\";
            case 0xDD:
                return "]";
            case 0xDE:
                return "'";
            default:
                return null;
        }
    }

    private static String keyName(int vk, KeyNames names) {
        String symbol = symbolName(vk);
        if (symbol != null) {
            return symbol;
        }
        return names != null ? names.label(vk) : String.valueOf(vk);
    }

    // True when the item sends every key that was pressed
    static boolean usesAll(Item item, List<Integer> pressed) {
        if (item.keys == null) {
            return false;
        }
        List<Integer> own = new ArrayList<>();
        for (int vk : item.keys) {
            own.add(normalize(vk));
        }
        return own.containsAll(pressed);
    }

    // The items to list for what was typed or pressed
    static List<Item> filter(List<Item> items, boolean pressMode, String query, List<Integer> pressed) {
        List<Item> out = new ArrayList<>();
        for (Item item : items) {
            boolean match = pressMode
                    ? item.keys != null && (pressed.isEmpty() || usesAll(item, pressed))
                    : SearchField.matches(item.label, query) || SearchField.matches(String.valueOf(item.subtitle), query) && !TextUtils.isEmpty(item.subtitle);
            if (match) {
                out.add(item);
            }
        }
        return out;
    }

    private final Context context;
    private final List<Item> items;
    private final boolean stayOpen;
    private final Listener listener;
    private final List<Integer> pressed = new ArrayList<>();
    private final List<String> pressedNames = new ArrayList<>();
    private Dialog dialog;
    private View root;
    private LinearLayout results;
    private TextView empty;
    private EditText field;
    private ImageView modeButton;
    private View pressedBar;
    private TextView pressedText;
    private ScrollView keyboardHost;
    private View keyboard;
    private boolean pressMode;
    private String query = "";
    private KeyNames names;

    private FindPanel(Context context, List<Item> items, boolean stayOpen, Listener listener) {
        this.context = context;
        this.items = items;
        this.stayOpen = stayOpen;
        this.listener = listener;
    }

    // Opens the panel. With stayOpen it is a multi choice list that closes with the back arrow, otherwise the first pick closes it.
    public static Dialog show(Context base, List<Item> items, boolean stayOpen, Listener listener) {
        FindPanel panel = new FindPanel(ThemedDialogs.context(base), items, stayOpen, listener);
        return panel.open();
    }

    private Dialog open() {
        dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        root = LayoutInflater.from(context).inflate(R.layout.view_find_panel, null);
        dialog.setContentView(root);

        results = root.findViewById(R.id.find_results);
        empty = root.findViewById(R.id.find_empty);
        modeButton = root.findViewById(R.id.find_mode);
        pressedBar = root.findViewById(R.id.find_pressed_bar);
        pressedText = root.findViewById(R.id.find_pressed_text);
        keyboardHost = root.findViewById(R.id.find_keyboard_host);
        field = SearchField.create(context, text -> {
            query = text;
            refresh();
        });
        field.setBackground(null);
        ((ViewGroup) root.findViewById(R.id.find_field_host)).addView(field,
                new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        // The search key hides the system keyboard and keeps the results
        field.setOnEditorActionListener((v, actionId, event) -> {
            hideSystemKeyboard();
            return true;
        });

        root.findViewById(R.id.find_close).setOnClickListener(v -> dialog.dismiss());
        modeButton.setOnClickListener(v -> setPressMode(!pressMode));
        root.findViewById(R.id.find_clear).setOnClickListener(v -> {
            pressed.clear();
            pressedNames.clear();
            refresh();
        });

        sizeWindow();
        applyInsets();
        refresh();
        dialog.show();
        Dialogs.track(dialog);
        Dialogs.openKeyboard(field);
        return dialog;
    }

    // NOTE: A phone gets the whole screen. A tablet gets a centered panel, because a search that fills a 14 inch screen is hard to read.
    private void sizeWindow() {
        Window window = dialog.getWindow();
        if (window == null) {
            return;
        }
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        float widthDp = metrics.widthPixels / metrics.density;
        float heightDp = metrics.heightPixels / metrics.density;
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE | WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        if (widthDp >= 600 && heightDp >= 480) {
            window.setBackgroundDrawableResource(R.drawable.bg_dialog);
            window.setLayout((int) (Math.min(widthDp, 720) * metrics.density), (int) (Math.min(heightDp * 0.85f, 720) * metrics.density));
        }
        else {
            window.setBackgroundDrawable(new ColorDrawable(Dialogs.color(context, R.attr.saSurfaceContainerHigh)));
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
        }
    }

    // Keeps the bar and the results clear of the status bar, the navigation bar and the keyboard
    private void applyInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            view.setPadding(bars.left, bars.top, bars.right, Math.max(bars.bottom, ime.bottom));
            return insets;
        });
        ViewCompat.requestApplyInsets(root);
    }

    private void hideSystemKeyboard() {
        InputMethodManager input = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (input != null) {
            input.hideSoftInputFromWindow(field.getWindowToken(), 0);
        }
    }

    // NOTE: Press mode swaps the system keyboard for the in-app one. On a wide short screen the keyboard goes beside the results, otherwise under them.
    private void setPressMode(boolean press) {
        pressMode = press;
        pressedBar.setVisibility(press ? View.VISIBLE : View.GONE);
        keyboardHost.setVisibility(press ? View.VISIBLE : View.GONE);
        field.setEnabled(!press);
        field.setAlpha(press ? 0.5f : 1f);
        modeButton.setImageResource(press ? R.drawable.ic_ck_search : R.drawable.ic_ck_keyboard);
        modeButton.setContentDescription(context.getString(press ? R.string.find_mode_type : R.string.find_mode_press));
        if (press) {
            hideSystemKeyboard();
            if (keyboard == null) {
                names = new KeyNames(context);
                keyboard = RecorderKeyboard.build(context, names, this::keyPressed);
                keyboardHost.addView(keyboard);
            }
            LinearLayout body = root.findViewById(R.id.find_body);
            DisplayMetrics metrics = context.getResources().getDisplayMetrics();
            boolean sideBySide = metrics.widthPixels / metrics.density >= 600 && metrics.heightPixels / metrics.density < 480;
            body.setOrientation(sideBySide ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
            View resultsColumn = ((ViewGroup) results.getParent().getParent());
            LinearLayout.LayoutParams resultsParams = (LinearLayout.LayoutParams) resultsColumn.getLayoutParams();
            LinearLayout.LayoutParams keyboardParams = (LinearLayout.LayoutParams) keyboardHost.getLayoutParams();
            if (sideBySide) {
                resultsParams.width = 0;
                resultsParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
                resultsParams.weight = 1;
                keyboardParams.width = 0;
                keyboardParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
                keyboardParams.weight = 1.2f;
            }
            else {
                resultsParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
                resultsParams.height = 0;
                resultsParams.weight = 1;
                keyboardParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
                // Under the results the keyboard takes at most 45 percent of the screen and scrolls inside that
                keyboardParams.height = (int) (metrics.heightPixels * 0.45f);
                keyboardParams.weight = 0;
            }
            resultsColumn.setLayoutParams(resultsParams);
            keyboardHost.setLayoutParams(keyboardParams);
        }
        else {
            field.requestFocus();
            Dialogs.openKeyboard(field);
        }
        refresh();
    }

    private void keyPressed(int vk) {
        int key = normalize(vk);
        if (!pressed.contains(key)) {
            pressed.add(key);
            pressedNames.add(keyName(vk, names));
        }
        refresh();
    }

    private void refresh() {
        if (pressMode) {
            pressedText.setText(pressed.isEmpty() ? context.getString(R.string.find_press_hint) : TextUtils.join(" + ", pressedNames));
        }
        List<Item> shown = filter(items, pressMode, query, pressed);
        results.removeAllViews();
        for (Item item : shown) {
            results.addView(row(item));
        }
        empty.setVisibility(shown.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private View row(Item item) {
        View row = LayoutInflater.from(context).inflate(R.layout.sa_choice_row, results, false);
        ((ImageView) row.findViewById(R.id.sa_choice_icon)).setImageResource(item.iconRes);
        TextView label = row.findViewById(R.id.sa_choice_label);
        label.setText(android.text.TextUtils.isEmpty(item.label) ? "" : item.label);
        label.setTextDirection(View.TEXT_DIRECTION_LOCALE);
        label.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
        TextView subtitle = row.findViewById(R.id.sa_choice_subtitle);
        if (!TextUtils.isEmpty(item.subtitle)) {
            subtitle.setText(item.subtitle);
            subtitle.setVisibility(View.VISIBLE);
        }
        View check = row.findViewById(R.id.sa_choice_check);
        check.setVisibility(item.checked ? View.VISIBLE : View.INVISIBLE);
        if (stayOpen) {
            ViewCompat.setStateDescription(row, context.getString(item.checked ? R.string.find_state_added : R.string.find_state_not_added));
        }
        row.setOnClickListener(v -> {
            boolean now = listener.onPick(item);
            if (stayOpen) {
                item.checked = now;
                check.setVisibility(now ? View.VISIBLE : View.INVISIBLE);
                ViewCompat.setStateDescription(row, context.getString(now ? R.string.find_state_added : R.string.find_state_not_added));
            }
            else {
                dialog.dismiss();
            }
        });
        return row;
    }
}
