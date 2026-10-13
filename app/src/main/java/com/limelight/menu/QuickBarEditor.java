package com.limelight.menu;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.widget.TextViewCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.limelight.R;
import com.limelight.actions.StreamAction;
import com.limelight.customkeys.CustomKey;
import com.limelight.customkeys.IconCatalog;
import com.limelight.customkeys.SpecialKeys;
import com.limelight.ui.Dialogs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// NOTE: Editor for the quick-action bars. A small screen preview shows all four corners with their buttons, and tapping a corner selects it.
// NOTE: The selected corner gets its direction (row or column) and its list of actions and keys, so several corners can carry their own bars.
public final class QuickBarEditor {
    // Screens at least this wide show the preview beside the list instead of above it
    private static final int TWO_PANE_MIN_WIDTH_DP = 720;
    // The preview draws at most this many buttons per corner and counts the rest
    private static final int PREVIEW_MAX_BUTTONS = 5;

    static final class Row {
        final String item;
        final int iconRes;
        final String label;
        boolean pinned;

        Row(String item, int iconRes, String label, boolean pinned) {
            this.item = item;
            this.iconRes = iconRes;
            this.label = label;
            this.pinned = pinned;
        }
    }

    private QuickBarEditor() {
    }

    // Pinned items first in their saved order, then every other action and special key. Items that no longer exist are dropped.
    static List<Row> buildRows(Context context, List<String> pinned, List<CustomKey> keys) {
        Map<String, Row> available = new LinkedHashMap<>();
        for (StreamAction action : GameMenuLayout.configurableActions()) {
            available.put(action.id, new Row(action.id, action.iconRes, context.getString(action.nameRes), false));
        }
        for (CustomKey key : keys) {
            String item = QuickBarLayout.customKeyItem(key.id);
            available.put(item, new Row(item, IconCatalog.drawableFor(key.icon),
                    context.getString(R.string.quick_bar_custom_key, key.name), false));
        }

        List<Row> rows = new ArrayList<>();
        for (String item : pinned) {
            Row row = available.remove(item);
            if (row != null) {
                row.pinned = true;
                rows.add(row);
            }
        }
        rows.addAll(available.values());
        return rows;
    }

    static List<String> pinnedItems(List<Row> rows) {
        List<String> items = new ArrayList<>();
        for (Row row : rows) {
            if (row.pinned) {
                items.add(row.item);
            }
        }
        return items;
    }

    static int slotIdFor(String corner) {
        switch (corner) {
            case QuickBarLayout.POSITION_TOP_RIGHT:
                return R.id.quick_bar_slot_top_right;
            case QuickBarLayout.POSITION_BOTTOM_LEFT:
                return R.id.quick_bar_slot_bottom_left;
            case QuickBarLayout.POSITION_BOTTOM_RIGHT:
                return R.id.quick_bar_slot_bottom_right;
            default:
                return R.id.quick_bar_slot_top_left;
        }
    }

    public static int cornerNameFor(String corner) {
        switch (corner) {
            case QuickBarLayout.POSITION_TOP_RIGHT:
                return R.string.quick_bar_corner_top_right;
            case QuickBarLayout.POSITION_BOTTOM_LEFT:
                return R.string.quick_bar_corner_bottom_left;
            case QuickBarLayout.POSITION_BOTTOM_RIGHT:
                return R.string.quick_bar_corner_bottom_right;
            default:
                return R.string.quick_bar_corner_top_left;
        }
    }

    public static void show(Context context, SharedPreferences prefs, Runnable onChanged) {
        show(context, prefs, onChanged, null);
    }

    // Opens with this corner selected, as when the editor starts from a long press on that corner's bar
    public static void show(Context context, SharedPreferences prefs, Runnable onChanged, String startCorner) {
        final List<CustomKey> keys = SpecialKeys.visible(context);
        final Map<String, List<Row>> rowsByCorner = new HashMap<>();
        final Map<String, String> directions = new HashMap<>();
        String firstUsed = null;
        for (String corner : QuickBarLayout.POSITIONS) {
            List<String> items = QuickBarLayout.getItems(prefs, corner);
            rowsByCorner.put(corner, buildRows(context, items, keys));
            directions.put(corner, QuickBarLayout.getDirection(prefs, corner));
            if (firstUsed == null && !items.isEmpty()) {
                firstUsed = corner;
            }
        }
        final String[] selected = {startCorner != null ? startCorner : firstUsed != null ? firstUsed : QuickBarLayout.POSITION_TOP_LEFT};

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        Context dialogContext = builder.getContext();
        View body = LayoutInflater.from(dialogContext).inflate(R.layout.dialog_quick_bar_editor, null);
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        boolean twoPane = metrics.widthPixels / metrics.density >= TWO_PANE_MIN_WIDTH_DP;
        arrangePanes(body, twoPane, dialogContext);

        RecyclerView list = new RecyclerView(dialogContext);
        list.setLayoutManager(new LinearLayoutManager(dialogContext));
        int listHeight = (int) (metrics.heightPixels * (twoPane ? 0.5f : 0.4f));
        ((FrameLayout) body.findViewById(R.id.quick_bar_list)).addView(list,
                new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, listHeight));
        ((TextView) body.findViewById(R.id.quick_bar_preview_label)).setText(R.string.quick_bar_preview_label);

        final Runnable[] refresh = new Runnable[1];
        final Adapter adapter = new Adapter(() -> refresh[0].run());
        refresh[0] = () -> {
            for (String corner : QuickBarLayout.POSITIONS) {
                drawCorner(dialogContext, body.findViewById(slotIdFor(corner)), corner, rowsByCorner.get(corner),
                        directions.get(corner), corner.equals(selected[0]));
            }
            boolean column = QuickBarLayout.DIRECTION_COLUMN.equals(directions.get(selected[0]));
            body.findViewById(R.id.quick_bar_direction_row).setSelected(!column);
            body.findViewById(R.id.quick_bar_direction_column).setSelected(column);
            ((TextView) body.findViewById(R.id.quick_bar_items_label)).setText(
                    context.getString(R.string.quick_bar_corner_items, context.getString(cornerNameFor(selected[0]))));
        };
        final Runnable selectCorner = () -> {
            adapter.setRows(rowsByCorner.get(selected[0]));
            refresh[0].run();
        };
        for (String corner : QuickBarLayout.POSITIONS) {
            body.findViewById(slotIdFor(corner)).setOnClickListener(v -> {
                selected[0] = corner;
                selectCorner.run();
            });
        }
        body.findViewById(R.id.quick_bar_direction_row).setOnClickListener(v -> {
            directions.put(selected[0], QuickBarLayout.DIRECTION_ROW);
            refresh[0].run();
        });
        body.findViewById(R.id.quick_bar_direction_column).setOnClickListener(v -> {
            directions.put(selected[0], QuickBarLayout.DIRECTION_COLUMN);
            refresh[0].run();
        });

        list.setAdapter(adapter);
        final ItemTouchHelper[] touchHelper = new ItemTouchHelper[1];
        adapter.dragStarter = holder -> touchHelper[0].startDrag(holder);
        touchHelper[0] = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder from, @NonNull RecyclerView.ViewHolder to) {
                int fromPos = from.getBindingAdapterPosition();
                int toPos = to.getBindingAdapterPosition();
                if (fromPos == RecyclerView.NO_POSITION || toPos == RecyclerView.NO_POSITION) {
                    return false;
                }
                Collections.swap(adapter.rows, fromPos, toPos);
                adapter.notifyItemMoved(fromPos, toPos);
                refresh[0].run();
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            }
        });
        touchHelper[0].attachToRecyclerView(list);
        selectCorner.run();

        builder.setTitle(R.string.title_quick_bar)
                .setView(body)
                .setPositiveButton(R.string.game_menu_editor_save, (dialog, which) -> {
                    for (String corner : QuickBarLayout.POSITIONS) {
                        QuickBarLayout.setItems(prefs, corner, pinnedItems(rowsByCorner.get(corner)));
                        QuickBarLayout.setDirection(prefs, corner, directions.get(corner));
                    }
                    if (onChanged != null) {
                        onChanged.run();
                    }
                })
                .setNegativeButton(R.string.game_menu_cancel, null);
        Dialogs.show(builder, twoPane ? Dialogs.WIDTH_WIDE_DP : Dialogs.WIDTH_STANDARD_DP);
    }

    // Wide screens put the preview and direction on the left and the item list on the right
    private static void arrangePanes(View body, boolean twoPane, Context context) {
        if (!twoPane) {
            return;
        }
        LinearLayout panes = body.findViewById(R.id.quick_bar_panes);
        panes.setOrientation(LinearLayout.HORIZONTAL);
        View controls = body.findViewById(R.id.quick_bar_controls);
        LinearLayout.LayoutParams controlsParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        controlsParams.rightMargin = (int) Dialogs.dp(context, 24);
        controls.setLayoutParams(controlsParams);
        body.findViewById(R.id.quick_bar_items_pane).setLayoutParams(
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
    }

    // Draws one corner of the preview: its pinned icons in their direction, starting in the corner, and a count for any extra ones
    private static void drawCorner(Context context, LinearLayout slot, String corner, List<Row> rows, String direction, boolean isSelected) {
        slot.removeAllViews();
        slot.setSelected(isSelected);
        boolean column = QuickBarLayout.DIRECTION_COLUMN.equals(direction);
        slot.setOrientation(column ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
        List<Row> pinned = new ArrayList<>();
        for (Row row : rows) {
            if (row.pinned) {
                pinned.add(row);
            }
        }
        int size = (int) Dialogs.dp(context, 22);
        int gap = (int) Dialogs.dp(context, 4);
        int shown = Math.min(pinned.size(), PREVIEW_MAX_BUTTONS);
        List<View> views = new ArrayList<>();
        for (int i = 0; i < shown; i++) {
            ImageView icon = new ImageView(context);
            icon.setImageResource(pinned.get(i).iconRes);
            icon.setBackgroundResource(R.drawable.bg_mini_button);
            int padding = (int) Dialogs.dp(context, 4);
            icon.setPadding(padding, padding, padding, padding);
            icon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            views.add(icon);
        }
        if (pinned.size() > shown) {
            TextView more = new TextView(context);
            more.setText("+" + (pinned.size() - shown));
            TextViewCompat.setTextAppearance(more, R.style.TextAppearance_SuperArtemis_BodySmall);
            more.setGravity(Gravity.CENTER);
            views.add(more);
        }
        // Bars on the right or bottom grow away from their corner, like the real bars over the stream
        boolean reverse = column ? !QuickBarLayout.isTop(corner) : !QuickBarLayout.isLeft(corner);
        if (reverse) {
            Collections.reverse(views);
        }
        for (int i = 0; i < views.size(); i++) {
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
            if (i > 0) {
                if (column) {
                    params.topMargin = gap;
                }
                else {
                    params.leftMargin = gap;
                }
            }
            slot.addView(views.get(i), params);
        }
        slot.setContentDescription(context.getString(R.string.quick_bar_corner_items,
                context.getString(cornerNameFor(corner))) + ", " + pinned.size());
    }

    private interface DragStarter {
        void startDrag(RecyclerView.ViewHolder holder);
    }

    private static final class Adapter extends RecyclerView.Adapter<Adapter.Holder> {
        private final Runnable onChanged;
        List<Row> rows = new ArrayList<>();
        DragStarter dragStarter;

        Adapter(Runnable onChanged) {
            this.onChanged = onChanged;
        }

        @SuppressLint("NotifyDataSetChanged")
        void setRows(List<Row> rows) {
            this.rows = rows;
            notifyDataSetChanged();
        }

        static final class Holder extends RecyclerView.ViewHolder {
            final ImageView handle;
            final ImageView icon;
            final TextView label;
            final CompoundButton check;

            Holder(View itemView) {
                super(itemView);
                handle = itemView.findViewById(R.id.game_menu_editor_handle);
                icon = itemView.findViewById(R.id.game_menu_item_icon);
                label = itemView.findViewById(R.id.game_menu_item_label);
                check = itemView.findViewById(R.id.game_menu_editor_check);
            }
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.game_menu_editor_item, parent, false));
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            Row row = rows.get(position);
            holder.icon.setImageResource(row.iconRes);
            holder.label.setText(row.label);
            holder.check.setOnCheckedChangeListener(null);
            holder.check.setChecked(row.pinned);
            holder.check.setOnCheckedChangeListener((button, checked) -> {
                row.pinned = checked;
                onChanged.run();
            });
            holder.itemView.setOnClickListener(v -> holder.check.toggle());
            holder.handle.setOnTouchListener((v, event) -> {
                if (event.getActionMasked() == MotionEvent.ACTION_DOWN && dragStarter != null) {
                    dragStarter.startDrag(holder);
                }
                return false;
            });
        }

        @Override
        public int getItemCount() {
            return rows.size();
        }
    }
}
