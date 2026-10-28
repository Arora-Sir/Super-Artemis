package com.limelight.gestures;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.limelight.R;
import com.limelight.actions.StreamAction;
import com.limelight.customkeys.CustomKey;
import com.limelight.customkeys.IconCatalog;
import com.limelight.customkeys.KeyNames;
import com.limelight.customkeys.SpecialKeys;
import com.limelight.menu.GameMenuLayout;
import com.limelight.ui.ChoiceList;
import com.limelight.ui.Dialogs;

import java.util.ArrayList;
import java.util.List;

// NOTE: Lists every multi-finger gesture in sections (taps, double taps, holds, swipes) with the action each one runs, and changes an action in a picker.
// NOTE: Every choice is saved at once. Wide screens show taps and swipes side by side.
public final class GestureEditor {
    static final class Option {
        final String binding;
        final String label;
        final int iconRes;
        // Options of one group share a heading in the picker
        final int groupRes;

        Option(String binding, String label, int iconRes, int groupRes) {
            this.binding = binding;
            this.label = label;
            this.iconRes = iconRes;
            this.groupRes = groupRes;
        }
    }

    // Upstream setting key, kept so the saved choice carries over
    // Master switch for every multi-finger gesture in every touch mode, shown on the Settings row
    public static final String ENABLED_PREF = "checkbox_gestures_enabled";
    // Wide enough for two columns of gesture rows
    private static final int TWO_COLUMN_MIN_WIDTH_DP = 720;

    private GestureEditor() {
    }

    // Nothing first, then the menu and mouse options, every stream action except session actions, then special keys
    static List<Option> options(Context context, List<CustomKey> keys) {
        List<Option> options = new ArrayList<>();
        options.add(new Option(GestureBindingStore.NONE, context.getString(R.string.gesture_action_none),
                R.drawable.ic_sa_touch_app, 0));
        options.add(new Option(GestureBindingStore.QUICK_MENU, context.getString(R.string.gesture_action_quick_menu),
                R.drawable.ic_sa_menu_open, R.string.gesture_group_menu));
        options.add(new Option(GestureBindingStore.MIDDLE_CLICK, context.getString(R.string.gesture_action_middle_click),
                R.drawable.ic_sa_mouse, R.string.gesture_group_menu));
        for (StreamAction action : GameMenuLayout.configurableActions()) {
            options.add(new Option(action.id, context.getString(action.nameRes), action.iconRes, R.string.gesture_group_actions));
        }
        for (CustomKey key : keys) {
            options.add(new Option(GestureBindingStore.customKeyBinding(key.id), com.limelight.customkeys.KeyLabels.pickerLabel(context, key),
                    IconCatalog.drawableFor(key.icon), R.string.gesture_group_keys));
        }
        return options;
    }

    static Option optionFor(List<Option> options, String binding, Context context) {
        for (Option option : options) {
            if (option.binding.equals(binding)) {
                return option;
            }
        }
        List<Integer> combo = GestureBindingStore.comboKeys(binding);
        if (combo != null) {
            return new Option(binding, new KeyNames(context).describe(combo), R.drawable.ic_sa_keyboard, R.string.gesture_group_keys);
        }
        // A special key that was deleted or hidden after it was bound does nothing until the binding is changed
        return new Option(binding, context.getString(R.string.gesture_action_missing_key), R.drawable.ic_sa_keyboard, 0);
    }

    static String slotTitle(Context context, GestureBindingStore.Slot slot) {
        switch (slot.kind) {
            case DOUBLE_TAP:
                return context.getString(R.string.gesture_double_tap, slot.fingers);
            case HOLD:
                return context.getString(R.string.gesture_hold, slot.fingers);
            case SWIPE_UP:
                return context.getString(R.string.gesture_swipe_up, slot.fingers);
            case SWIPE_DOWN:
                return context.getString(R.string.gesture_swipe_down, slot.fingers);
            case SWIPE_LEFT:
                return context.getString(R.string.gesture_swipe_left, slot.fingers);
            case SWIPE_RIGHT:
                return context.getString(R.string.gesture_swipe_right, slot.fingers);
            default:
                return context.getString(R.string.gesture_tap, slot.fingers);
        }
    }

    static int slotIcon(GestureBindingStore.Slot slot) {
        switch (slot.kind) {
            case SWIPE_UP:
                return R.drawable.ic_sa_swipe_up;
            case SWIPE_DOWN:
                return R.drawable.ic_sa_swipe_down;
            case SWIPE_LEFT:
                return R.drawable.ic_sa_swipe_left;
            case SWIPE_RIGHT:
                return R.drawable.ic_sa_swipe_right;
            case DOUBLE_TAP:
                return R.drawable.ic_sa_gesture;
            case HOLD:
                return R.drawable.ic_ck_timer;
            default:
                return R.drawable.ic_sa_touch_app;
        }
    }

    public static void show(Context context, SharedPreferences prefs) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        Context dialogContext = builder.getContext();
        final List<Option> options = options(context, SpecialKeys.visible(context));
        final List<Runnable> refreshers = new ArrayList<>();
        final boolean twoColumns = Dialogs.isWide(context)
                || context.getResources().getDisplayMetrics().widthPixels / context.getResources().getDisplayMetrics().density >= TWO_COLUMN_MIN_WIDTH_DP;

        LinearLayout columns = new LinearLayout(dialogContext);
        columns.setOrientation(twoColumns ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
        int side = (int) Dialogs.dp(dialogContext, 12);
        columns.setPadding(side, 0, side, (int) Dialogs.dp(dialogContext, 8));
        LinearLayout taps = column(dialogContext, twoColumns);
        LinearLayout swipes = column(dialogContext, twoColumns);
        columns.addView(taps);
        columns.addView(swipes);

        addSection(taps, R.string.gesture_section_taps);
        for (GestureBindingStore.Slot slot : GestureBindingStore.SLOTS) {
            if (slot.kind == GestureBindingStore.Kind.TAP) {
                addRow(context, taps, prefs, slot, options, refreshers);
            }
        }
        addSection(taps, R.string.gesture_section_double_taps);
        for (GestureBindingStore.Slot slot : GestureBindingStore.SLOTS) {
            if (slot.kind == GestureBindingStore.Kind.DOUBLE_TAP) {
                addRow(context, taps, prefs, slot, options, refreshers);
            }
        }
        addSection(taps, R.string.gesture_section_holds);
        for (GestureBindingStore.Slot slot : GestureBindingStore.SLOTS) {
            if (slot.kind == GestureBindingStore.Kind.HOLD) {
                addRow(context, taps, prefs, slot, options, refreshers);
            }
        }
        addHoldDurationRow(context, taps, prefs, refreshers);
        for (int fingers : GestureBindingStore.SWIPE_FINGER_COUNTS) {
            TextView header = addSection(swipes, 0);
            header.setText(context.getString(R.string.gesture_section_swipes, fingers));
            for (GestureBindingStore.Slot slot : GestureBindingStore.SLOTS) {
                if (slot.fingers == fingers && slot.kind.isSwipe()) {
                    addRow(context, swipes, prefs, slot, options, refreshers);
                }
            }
        }

        // One tap sets the swipes Windows uses on a touchpad
        View preset = LayoutInflater.from(dialogContext).inflate(R.layout.gesture_row, swipes, false);
        ((ImageView) preset.findViewById(R.id.gesture_row_icon)).setImageResource(R.drawable.ic_ck_window);
        ((TextView) preset.findViewById(R.id.gesture_row_title)).setText(R.string.gesture_windows_preset);
        ((TextView) preset.findViewById(R.id.gesture_row_action)).setText(R.string.gesture_windows_preset_summary);
        preset.setOnClickListener(v -> {
            GestureBindingStore.applyWindowsPreset(prefs);
            refreshAll(refreshers);
            Toast.makeText(context, R.string.gesture_windows_preset_applied, Toast.LENGTH_SHORT).show();
        });
        swipes.addView(preset);

        ScrollView scroll = new ScrollView(dialogContext);
        scroll.addView(columns);
        builder.setCustomTitle(Dialogs.header(dialogContext, R.drawable.ic_sa_gesture,
                        context.getString(R.string.title_gestures), context.getString(R.string.gesture_editor_hint)))
                .setView(scroll)
                .setNeutralButton(R.string.game_menu_editor_reset, null);
        AlertDialog dialog = Dialogs.show(builder, twoColumns ? Dialogs.WIDTH_WIDE_DP : Dialogs.WIDTH_STANDARD_DP);
        // Reset keeps the dialog open so the restored defaults are visible right away
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
            GestureBindingStore.reset(prefs);
            refreshAll(refreshers);
        });
    }

    private static LinearLayout column(Context context, boolean twoColumns) {
        LinearLayout column = new LinearLayout(context);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setLayoutParams(twoColumns
                ? new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                : new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return column;
    }

    private static TextView addSection(LinearLayout parent, int titleRes) {
        TextView header = (TextView) LayoutInflater.from(parent.getContext()).inflate(R.layout.special_keys_header, parent, false);
        if (titleRes != 0) {
            header.setText(titleRes);
        }
        parent.addView(header);
        return header;
    }

    private static void addRow(Context context, LinearLayout parent, SharedPreferences prefs, GestureBindingStore.Slot slot,
                               List<Option> options, List<Runnable> refreshers) {
        View row = LayoutInflater.from(parent.getContext()).inflate(R.layout.gesture_row, parent, false);
        ((ImageView) row.findViewById(R.id.gesture_row_icon)).setImageResource(slotIcon(slot));
        ((TextView) row.findViewById(R.id.gesture_row_title)).setText(slotTitle(context, slot));
        TextView action = row.findViewById(R.id.gesture_row_action);
        Runnable refresh = () -> {
            Option option = optionFor(options, GestureBindingStore.get(prefs, slot), context);
            boolean none = GestureBindingStore.NONE.equals(option.binding);
            action.setText(option.label);
            action.setTextColor(Dialogs.color(context, none ? R.attr.saOnSurfaceVariant : R.attr.saPrimary));
            Drawable icon = none ? null : context.getDrawable(option.iconRes);
            if (icon != null) {
                int size = (int) Dialogs.dp(context, 16);
                icon = icon.mutate();
                icon.setBounds(0, 0, size, size);
                icon.setTint(Dialogs.color(context, R.attr.saPrimary));
            }
            action.setCompoundDrawablesRelative(icon, null, null, null);
        };
        refresh.run();
        refreshers.add(refresh);
        row.setOnClickListener(v -> choose(context, prefs, slot, options, refresh));
        // NOTE: Holding a row that runs a key or flow edits that key in place, so a forgotten name or icon is fixed without leaving the editor.
        row.setOnLongClickListener(v -> {
            String keyId = GestureBindingStore.customKeyId(GestureBindingStore.get(prefs, slot));
            if (keyId == null || com.limelight.customkeys.KeyEditActions.kindOf(context, keyId) == com.limelight.customkeys.KeyEditActions.Kind.NONE) {
                return false;
            }
            com.limelight.customkeys.KeyEditActions.edit(context, keyId, () -> {
                CustomKey changed = SpecialKeys.find(context, keyId);
                for (int i = 0; i < options.size() && changed != null; i++) {
                    if (options.get(i).binding.equals(GestureBindingStore.customKeyBinding(keyId))) {
                        options.set(i, new Option(options.get(i).binding, com.limelight.customkeys.KeyLabels.pickerLabel(context, changed),
                                IconCatalog.drawableFor(changed.icon), R.string.gesture_group_keys));
                    }
                }
                refreshAll(refreshers);
            });
            return true;
        });
        parent.addView(row);
    }

    // How long a hold takes, picked from three choices like any other gesture setting
    private static void addHoldDurationRow(Context context, LinearLayout parent, SharedPreferences prefs, List<Runnable> refreshers) {
        View row = LayoutInflater.from(parent.getContext()).inflate(R.layout.gesture_row, parent, false);
        ((ImageView) row.findViewById(R.id.gesture_row_icon)).setImageResource(R.drawable.ic_ck_timer);
        ((TextView) row.findViewById(R.id.gesture_row_title)).setText(R.string.gesture_hold_duration);
        TextView value = row.findViewById(R.id.gesture_row_action);
        Runnable refresh = () -> {
            value.setText(holdDurationLabel(context, GestureBindingStore.holdDurationMs(prefs)));
            value.setTextColor(Dialogs.color(context, R.attr.saPrimary));
        };
        refresh.run();
        refreshers.add(refresh);
        row.setOnClickListener(v -> chooseHoldDuration(context, prefs, refresh));
        parent.addView(row);
    }

    static String holdDurationLabel(Context context, long durationMs) {
        return context.getString(R.string.gesture_hold_seconds, String.valueOf(durationMs / 1000f).replace(".0", ""));
    }

    private static void chooseHoldDuration(Context context, SharedPreferences prefs, Runnable onChosen) {
        long current = GestureBindingStore.holdDurationMs(prefs);
        ChoiceList choices = new ChoiceList(new AlertDialog.Builder(context), R.drawable.ic_ck_timer,
                context.getString(R.string.gesture_hold_duration), R.string.game_menu_cancel);
        for (long duration : GestureBindingStore.HOLD_DURATIONS_MS) {
            choices.add(R.drawable.ic_ck_timer, holdDurationLabel(context, duration), duration == current, () -> {
                GestureBindingStore.setHoldDurationMs(prefs, duration);
                onChosen.run();
            });
        }
        choices.show();
    }

    private static void refreshAll(List<Runnable> refreshers) {
        for (Runnable refresh : refreshers) {
            refresh.run();
        }
    }

    // Picker with the options grouped under headings, each with its icon, and a check on the current choice
    private static void choose(Context context, SharedPreferences prefs, GestureBindingStore.Slot slot, List<Option> options, Runnable onChosen) {
        String current = GestureBindingStore.get(prefs, slot);
        ChoiceList choices = new ChoiceList(new AlertDialog.Builder(context), slotIcon(slot), slotTitle(context, slot), R.string.game_menu_cancel);

        int group = -1;
        for (Option option : options) {
            if (option.groupRes != group) {
                group = option.groupRes;
                if (group != 0) {
                    addSection(choices.list(), group);
                }
            }
            choices.add(option.iconRes, option.label, option.binding.equals(current), () -> {
                GestureBindingStore.set(prefs, slot, option.binding);
                onChosen.run();
            });
        }
        // A key or flow made here is bound to this gesture straight away
        choices.list().addView(com.limelight.customkeys.KeyPicker.addButton(choices.context(), context, key -> {
            // The new key joins the editor's own options, or its row would read "Deleted custom key" until the editor is reopened
            options.add(new Option(GestureBindingStore.customKeyBinding(key.id), com.limelight.customkeys.KeyLabels.pickerLabel(context, key),
                    IconCatalog.drawableFor(key.icon), R.string.gesture_group_keys));
            GestureBindingStore.set(prefs, slot, GestureBindingStore.customKeyBinding(key.id));
            onChosen.run();
            choices.dismiss();
        }));
        choices.show();
    }
}
