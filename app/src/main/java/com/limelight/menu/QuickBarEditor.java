package com.limelight.menu;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.limelight.R;
import com.limelight.actions.StreamAction;
import com.limelight.customkeys.CustomKey;
import com.limelight.customkeys.CustomKeyStore;
import com.limelight.customkeys.IconCatalog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Dialog for choosing the corner of the quick-action bar and which actions and custom keys it shows, in what order
public final class QuickBarEditor {
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

    // Pinned items first in their saved order, then every other action and custom key. Items that no longer exist are dropped.
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

    static int radioIdFor(String position) {
        switch (position) {
            case QuickBarLayout.POSITION_TOP_RIGHT:
                return R.id.quick_bar_top_right;
            case QuickBarLayout.POSITION_BOTTOM_LEFT:
                return R.id.quick_bar_bottom_left;
            case QuickBarLayout.POSITION_BOTTOM_RIGHT:
                return R.id.quick_bar_bottom_right;
            default:
                return R.id.quick_bar_top_left;
        }
    }

    static String positionFor(int radioId) {
        if (radioId == R.id.quick_bar_top_right) {
            return QuickBarLayout.POSITION_TOP_RIGHT;
        }
        if (radioId == R.id.quick_bar_bottom_left) {
            return QuickBarLayout.POSITION_BOTTOM_LEFT;
        }
        if (radioId == R.id.quick_bar_bottom_right) {
            return QuickBarLayout.POSITION_BOTTOM_RIGHT;
        }
        return QuickBarLayout.POSITION_TOP_LEFT;
    }

    public static void show(Context context, SharedPreferences prefs, Runnable onChanged) {
        final List<Row> rows = buildRows(context, QuickBarLayout.getItems(prefs), CustomKeyStore.loadOrEmpty(context));

        View body = LayoutInflater.from(context).inflate(R.layout.dialog_quick_bar_editor, null);
        RadioGroup positionGroup = body.findViewById(R.id.quick_bar_position);
        positionGroup.check(radioIdFor(QuickBarLayout.getPosition(prefs)));

        RecyclerView list = new RecyclerView(context);
        list.setLayoutManager(new LinearLayoutManager(context));
        int listHeight = (int) (context.getResources().getDisplayMetrics().heightPixels * 0.45f);
        ((FrameLayout) body.findViewById(R.id.quick_bar_list)).addView(list,
                new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, listHeight));

        final ItemTouchHelper[] touchHelper = new ItemTouchHelper[1];
        final Adapter adapter = new Adapter(rows, holder -> touchHelper[0].startDrag(holder));
        list.setAdapter(adapter);
        touchHelper[0] = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder from, @NonNull RecyclerView.ViewHolder to) {
                int fromPos = from.getAdapterPosition();
                int toPos = to.getAdapterPosition();
                if (fromPos == RecyclerView.NO_POSITION || toPos == RecyclerView.NO_POSITION) {
                    return false;
                }
                Collections.swap(rows, fromPos, toPos);
                adapter.notifyItemMoved(fromPos, toPos);
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            }
        });
        touchHelper[0].attachToRecyclerView(list);

        new AlertDialog.Builder(context)
                .setTitle(R.string.title_quick_bar)
                .setView(body)
                .setPositiveButton(R.string.game_menu_editor_save, (dialog, which) -> {
                    List<String> items = new ArrayList<>();
                    for (Row row : rows) {
                        if (row.pinned) {
                            items.add(row.item);
                        }
                    }
                    QuickBarLayout.setItems(prefs, items);
                    QuickBarLayout.setPosition(prefs, positionFor(positionGroup.getCheckedRadioButtonId()));
                    if (onChanged != null) {
                        onChanged.run();
                    }
                })
                .setNegativeButton(R.string.game_menu_cancel, null)
                .show();
    }

    private interface DragStarter {
        void startDrag(RecyclerView.ViewHolder holder);
    }

    private static final class Adapter extends RecyclerView.Adapter<Adapter.Holder> {
        private final List<Row> rows;
        private final DragStarter dragStarter;

        Adapter(List<Row> rows, DragStarter dragStarter) {
            this.rows = rows;
            this.dragStarter = dragStarter;
        }

        static final class Holder extends RecyclerView.ViewHolder {
            final ImageView handle;
            final ImageView icon;
            final TextView label;
            final CheckBox check;

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
            holder.check.setOnCheckedChangeListener((button, checked) -> row.pinned = checked);
            holder.itemView.setOnClickListener(v -> holder.check.toggle());
            holder.handle.setOnTouchListener((v, event) -> {
                if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
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
