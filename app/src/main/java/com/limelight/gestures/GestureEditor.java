package com.limelight.gestures;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.limelight.R;
import com.limelight.actions.StreamAction;
import com.limelight.customkeys.CustomKey;
import com.limelight.customkeys.CustomKeyStore;
import com.limelight.menu.GameMenuLayout;

import java.util.ArrayList;
import java.util.List;

// Dialog that lists every multi-finger tap and double tap with the action it runs, and changes an action with one more tap
public final class GestureEditor {
    static final class Option {
        final String binding;
        final String label;

        Option(String binding, String label) {
            this.binding = binding;
            this.label = label;
        }
    }

    private GestureEditor() {
    }

    // Nothing first, then the menu and mouse options, every stream action except session actions, then custom keys
    static List<Option> options(Context context, List<CustomKey> keys) {
        List<Option> options = new ArrayList<>();
        options.add(new Option(GestureBindingStore.NONE, context.getString(R.string.gesture_action_none)));
        options.add(new Option(GestureBindingStore.QUICK_MENU, context.getString(R.string.gesture_action_quick_menu)));
        options.add(new Option(GestureBindingStore.MIDDLE_CLICK, context.getString(R.string.gesture_action_middle_click)));
        for (StreamAction action : GameMenuLayout.configurableActions()) {
            options.add(new Option(action.id, context.getString(action.nameRes)));
        }
        for (CustomKey key : keys) {
            options.add(new Option(GestureBindingStore.customKeyBinding(key.id), context.getString(R.string.quick_bar_custom_key, key.name)));
        }
        return options;
    }

    static String labelFor(List<Option> options, String binding, Context context) {
        for (Option option : options) {
            if (option.binding.equals(binding)) {
                return option.label;
            }
        }
        // A custom key that was deleted after it was bound does nothing until the binding is changed
        return context.getString(R.string.gesture_action_missing_key);
    }

    static String slotTitle(Context context, GestureBindingStore.Slot slot) {
        return context.getString(slot.doubleTap ? R.string.gesture_double_tap : R.string.gesture_tap, slot.fingers);
    }

    public static void show(Context context, SharedPreferences prefs) {
        final List<Option> options = options(context, CustomKeyStore.loadOrEmpty(context));

        LinearLayout list = new LinearLayout(context);
        list.setOrientation(LinearLayout.VERTICAL);
        TextView hint = new TextView(context);
        hint.setText(R.string.gesture_editor_hint);
        float density = context.getResources().getDisplayMetrics().density;
        int padding = (int) (24 * density);
        hint.setPadding(padding, (int) (4 * density), padding, (int) (8 * density));
        list.addView(hint);

        final List<Runnable> refreshers = new ArrayList<>();
        for (GestureBindingStore.Slot slot : GestureBindingStore.SLOTS) {
            View row = LayoutInflater.from(context).inflate(R.layout.gesture_row, list, false);
            TextView title = row.findViewById(R.id.gesture_row_title);
            TextView action = row.findViewById(R.id.gesture_row_action);
            title.setText(slotTitle(context, slot));
            Runnable refresh = () -> action.setText(labelFor(options, GestureBindingStore.get(prefs, slot), context));
            refresh.run();
            refreshers.add(refresh);
            row.setOnClickListener(v -> choose(context, prefs, slot, options, refresh));
            list.addView(row);
        }

        ScrollView scroll = new ScrollView(context);
        scroll.addView(list);
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle(R.string.title_gestures)
                .setView(scroll)
                .setPositiveButton(R.string.custom_keys_done, null)
                .setNeutralButton(R.string.game_menu_editor_reset, null)
                .create();
        dialog.show();
        // Reset keeps the dialog open so the restored defaults are visible right away
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
            GestureBindingStore.reset(prefs);
            for (Runnable refresh : refreshers) {
                refresh.run();
            }
        });
    }

    private static void choose(Context context, SharedPreferences prefs, GestureBindingStore.Slot slot, List<Option> options, Runnable onChosen) {
        String current = GestureBindingStore.get(prefs, slot);
        String[] labels = new String[options.size()];
        int checked = 0;
        for (int i = 0; i < options.size(); i++) {
            labels[i] = options.get(i).label;
            if (options.get(i).binding.equals(current)) {
                checked = i;
            }
        }
        new AlertDialog.Builder(context)
                .setTitle(slotTitle(context, slot))
                .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                    GestureBindingStore.set(prefs, slot, options.get(which).binding);
                    onChosen.run();
                    dialog.dismiss();
                })
                .setNegativeButton(R.string.game_menu_cancel, null)
                .show();
    }
}
