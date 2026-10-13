package com.limelight.customkeys;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.limelight.R;

import java.util.Collections;
import java.util.List;

// NOTE: Lists the user's custom keys with add, edit, reorder and delete. Every change is saved right away to the shared custom keys JSON.
// NOTE: Used from Settings and from the in-stream Send Special Keys menu, so keys can be made without leaving a stream.
public final class CustomKeysManager {
    private CustomKeysManager() {
    }

    public static void show(Context context, Runnable onClosed) {
        final List<CustomKey> keys;
        try {
            keys = CustomKeyStore.load(context);
        }
        catch (CustomKeyStore.FormatException e) {
            showUnreadable(context, onClosed);
            return;
        }

        final KeyNames names = new KeyNames(context);
        final float density = context.getResources().getDisplayMetrics().density;

        LinearLayout body = new LinearLayout(context);
        body.setOrientation(LinearLayout.VERTICAL);
        TextView empty = new TextView(context);
        empty.setText(R.string.custom_keys_empty);
        int padding = (int) (24 * density);
        empty.setPadding(padding, (int) (8 * density), padding, (int) (8 * density));
        body.addView(empty);

        RecyclerView list = new RecyclerView(context);
        list.setLayoutManager(new LinearLayoutManager(context));
        int listHeight = (int) (context.getResources().getDisplayMetrics().heightPixels * 0.5f);
        body.addView(list, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, listHeight));

        final Runnable save = () -> CustomKeyStore.save(context, keys);
        final Runnable[] updateEmpty = new Runnable[1];
        final Adapter adapter = new Adapter(context, keys, names, save, () -> updateEmpty[0].run());
        updateEmpty[0] = () -> {
            empty.setVisibility(keys.isEmpty() ? View.VISIBLE : View.GONE);
            list.setVisibility(keys.isEmpty() ? View.GONE : View.VISIBLE);
        };
        updateEmpty[0].run();
        list.setAdapter(adapter);

        final ItemTouchHelper[] touchHelper = new ItemTouchHelper[1];
        adapter.dragStarter = holder -> touchHelper[0].startDrag(holder);
        touchHelper[0] = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder from, @NonNull RecyclerView.ViewHolder to) {
                int fromPos = from.getAdapterPosition();
                int toPos = to.getAdapterPosition();
                if (fromPos == RecyclerView.NO_POSITION || toPos == RecyclerView.NO_POSITION) {
                    return false;
                }
                Collections.swap(keys, fromPos, toPos);
                adapter.notifyItemMoved(fromPos, toPos);
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            }

            // Saves once per drag instead of on every step
            @Override
            public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);
                save.run();
            }
        });
        touchHelper[0].attachToRecyclerView(list);

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle(R.string.custom_keys_title)
                .setView(body)
                .setPositiveButton(R.string.custom_keys_done, null)
                .setNeutralButton(R.string.custom_key_add, null)
                .setOnDismissListener(d -> {
                    if (onClosed != null) {
                        onClosed.run();
                    }
                })
                .create();
        dialog.show();
        // Adding opens the editor on top, so the manager stays open instead of closing like a normal dialog button
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> CustomKeyEditor.show(context, null, key -> {
            CustomKeyStore.upsert(keys, key);
            save.run();
            adapter.notifyDataSetChanged();
            updateEmpty[0].run();
        }));
    }

    private static void showUnreadable(Context context, Runnable onClosed) {
        new AlertDialog.Builder(context)
                .setTitle(R.string.custom_keys_title)
                .setMessage(R.string.custom_keys_unreadable)
                .setPositiveButton(R.string.custom_keys_start_over, (d, which) -> {
                    CustomKeyStore.save(context, Collections.emptyList());
                    show(context, onClosed);
                })
                .setNegativeButton(R.string.game_menu_cancel, (d, which) -> {
                    if (onClosed != null) {
                        onClosed.run();
                    }
                })
                .show();
    }

    private interface DragStarter {
        void startDrag(RecyclerView.ViewHolder holder);
    }

    private static final class Adapter extends RecyclerView.Adapter<Adapter.Holder> {
        private final Context context;
        private final List<CustomKey> keys;
        private final KeyNames names;
        private final Runnable save;
        private final Runnable onCountChanged;
        DragStarter dragStarter;

        Adapter(Context context, List<CustomKey> keys, KeyNames names, Runnable save, Runnable onCountChanged) {
            this.context = context;
            this.keys = keys;
            this.names = names;
            this.save = save;
            this.onCountChanged = onCountChanged;
        }

        static final class Holder extends RecyclerView.ViewHolder {
            final ImageView handle;
            final ImageView icon;
            final TextView name;
            final TextView combo;
            final ImageButton delete;

            Holder(View itemView) {
                super(itemView);
                handle = itemView.findViewById(R.id.custom_key_row_handle);
                icon = itemView.findViewById(R.id.custom_key_row_icon);
                name = itemView.findViewById(R.id.custom_key_row_name);
                combo = itemView.findViewById(R.id.custom_key_row_keys);
                delete = itemView.findViewById(R.id.custom_key_row_delete);
            }
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.custom_key_row, parent, false));
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            CustomKey key = keys.get(position);
            holder.icon.setImageResource(IconCatalog.drawableFor(key.icon));
            holder.name.setText(key.name);
            holder.combo.setText(names.describe(key.keys));
            holder.itemView.setOnClickListener(v -> CustomKeyEditor.show(context, key, edited -> {
                CustomKeyStore.upsert(keys, edited);
                save.run();
                notifyDataSetChanged();
            }));
            holder.delete.setOnClickListener(v -> new AlertDialog.Builder(context)
                    .setMessage(context.getString(R.string.custom_key_delete_confirm, key.name))
                    .setPositiveButton(R.string.custom_key_delete, (d, which) -> {
                        keys.remove(key);
                        save.run();
                        notifyDataSetChanged();
                        onCountChanged.run();
                    })
                    .setNegativeButton(R.string.game_menu_cancel, null)
                    .show());
            holder.handle.setOnTouchListener((v, event) -> {
                if (event.getActionMasked() == MotionEvent.ACTION_DOWN && dragStarter != null) {
                    dragStarter.startDrag(holder);
                }
                return false;
            });
        }

        @Override
        public int getItemCount() {
            return keys.size();
        }
    }
}
