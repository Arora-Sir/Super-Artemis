package com.limelight;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// Dialog for choosing which actions appear in the in-stream Quick Menu and in what order
public final class GameMenuEditor {
    private static final class Row {
        final GameMenuLayout.Action action;
        boolean inQuickMenu;

        Row(GameMenuLayout.Action action, boolean inQuickMenu) {
            this.action = action;
            this.inQuickMenu = inQuickMenu;
        }
    }

    private GameMenuEditor() {
    }

    // Checked rows come first in their saved order, the rest follow in catalog order
    static List<Row> buildRows(List<String> quickItems) {
        List<Row> rows = new ArrayList<>();
        for (String id : quickItems) {
            GameMenuLayout.Action action = GameMenuLayout.find(id);
            if (action != null) {
                rows.add(new Row(action, true));
            }
        }
        for (GameMenuLayout.Action action : GameMenuLayout.ACTIONS) {
            if (!quickItems.contains(action.id)) {
                rows.add(new Row(action, false));
            }
        }
        return rows;
    }

    public static void show(Context context, SharedPreferences prefs, Runnable onChanged) {
        final List<Row> rows = buildRows(GameMenuLayout.getQuickItems(prefs));
        final float density = context.getResources().getDisplayMetrics().density;

        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);

        TextView hint = new TextView(context);
        hint.setText(R.string.game_menu_editor_hint);
        int padding = (int) (20 * density);
        hint.setPadding(padding, (int) (8 * density), padding, (int) (8 * density));
        container.addView(hint);

        RecyclerView list = new RecyclerView(context);
        list.setLayoutManager(new LinearLayoutManager(context));
        container.addView(list, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        final ItemTouchHelper[] touchHelper = new ItemTouchHelper[1];
        final RowAdapter adapter = new RowAdapter(rows, holder -> touchHelper[0].startDrag(holder));
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

            @Override
            public boolean isLongPressDragEnabled() {
                return true;
            }
        });
        touchHelper[0].attachToRecyclerView(list);

        new AlertDialog.Builder(context)
                .setTitle(R.string.title_customize_game_menu)
                .setView(container)
                .setPositiveButton(R.string.game_menu_editor_save, (dialog, which) -> {
                    List<String> ids = new ArrayList<>();
                    for (Row row : rows) {
                        if (row.inQuickMenu) {
                            ids.add(row.action.id);
                        }
                    }
                    GameMenuLayout.setQuickItems(prefs, ids);
                    if (onChanged != null) {
                        onChanged.run();
                    }
                })
                .setNeutralButton(R.string.game_menu_editor_reset, (dialog, which) -> {
                    GameMenuLayout.reset(prefs);
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

    private static final class RowAdapter extends RecyclerView.Adapter<RowAdapter.Holder> {
        private final List<Row> rows;
        private final DragStarter dragStarter;

        RowAdapter(List<Row> rows, DragStarter dragStarter) {
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
            holder.icon.setImageResource(row.action.iconRes);
            holder.label.setText(row.action.editorLabelRes);
            holder.check.setOnCheckedChangeListener(null);
            holder.check.setChecked(row.inQuickMenu);
            holder.check.setOnCheckedChangeListener((button, checked) -> row.inQuickMenu = checked);
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
