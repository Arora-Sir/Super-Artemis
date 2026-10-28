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
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.limelight.R;
import com.limelight.ui.Dialogs;
import com.limelight.ui.DragReorder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// NOTE: Manages every special key in one list: the built-in keys (hide, change icon) and the user's own keys (edit, delete), all reorderable.
// NOTE: Hidden built-in keys wait in their own section to be shown again. Every change is saved at once, so back simply closes the dialog.
// NOTE: Used from Settings and from the in-stream Send special keys menu, so keys can be changed without leaving a stream.
public final class SpecialKeysManager {
    private SpecialKeysManager() {
    }

    public static void show(Context context, Runnable onClosed) {
        try {
            CustomKeyStore.load(context);
        }
        catch (CustomKeyStore.FormatException e) {
            showUnreadable(context, onClosed);
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        Context dialogContext = builder.getContext();
        final KeyNames names = new KeyNames(context);
        final List<CustomKey> visible = new ArrayList<>();
        final List<CustomKey> hidden = new ArrayList<>();

        RecyclerView list = new RecyclerView(dialogContext);
        list.setLayoutManager(new LinearLayoutManager(dialogContext));
        int side = (int) Dialogs.dp(dialogContext, 12);
        list.setPadding(side, 0, side, side);
        list.setClipToPadding(false);
        list.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                (int) (context.getResources().getDisplayMetrics().heightPixels * 0.5f)));

        final Adapter adapter = new Adapter(context, names, visible, hidden);
        final Runnable reload = () -> {
            visible.clear();
            visible.addAll(SpecialKeys.visible(context));
            hidden.clear();
            hidden.addAll(SpecialKeys.hiddenBuiltIns(context));
            adapter.notifyDataSetChanged();
        };
        adapter.reload = reload;
        reload.run();
        list.setAdapter(adapter);

        final ItemTouchHelper[] touchHelper = new ItemTouchHelper[1];
        adapter.dragStarter = holder -> touchHelper[0].startDrag(holder);
        // Only shown keys can be reordered, the hidden section stays put. The order is saved once per drag instead of on every step.
        touchHelper[0] = DragReorder.attach(list, true, position -> position < visible.size(), (from, to) -> {
            DragReorder.moveItem(visible, from, to);
            adapter.notifyItemMoved(from, to);
            return true;
        }, () -> SpecialKeys.saveOrder(context, visible));

        builder.setCustomTitle(Dialogs.header(dialogContext, R.drawable.ic_sa_keyboard,
                        context.getString(R.string.custom_keys_title), context.getString(R.string.special_keys_hint)))
                .setView(list)
                .setPositiveButton(R.string.custom_key_add, null)
                .setNeutralButton(R.string.special_keys_reset, null)
                .setOnDismissListener(d -> {
                    if (onClosed != null) {
                        onClosed.run();
                    }
                });
        AlertDialog dialog = Dialogs.show(builder, Dialogs.WIDTH_STANDARD_DP);
        // Adding and resetting open on top, so the manager stays open instead of closing like a normal dialog button
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> CustomKeyEditor.show(context, null, key -> {
            CustomKey saved;
            try {
                saved = CustomKeyStore.upsert(context, key);
            }
            catch (CustomKeyStore.FormatException e) {
                return;
            }
            reload.run();
            // The new key is the last one, which is far below the visible part of a long list
            for (int i = 0; i < visible.size(); i++) {
                if (visible.get(i).id.equals(saved.id)) {
                    list.scrollToPosition(i);
                    break;
                }
            }
        }));
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> new AlertDialog.Builder(context)
                .setMessage(R.string.special_keys_reset_confirm)
                .setPositiveButton(R.string.special_keys_reset, (d, which) -> {
                    SpecialKeys.resetBuiltIns(context);
                    reload.run();
                })
                .setNegativeButton(R.string.game_menu_cancel, null)
                .show());
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

    // Shown keys first, then a "Hidden built-in keys" label and the hidden keys
    private static final class Adapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private static final int TYPE_KEY = 0;
        private static final int TYPE_HEADER = 1;

        private final Context context;
        private final KeyNames names;
        private final List<CustomKey> visible;
        private final List<CustomKey> hidden;
        DragStarter dragStarter;
        Runnable reload;

        Adapter(Context context, KeyNames names, List<CustomKey> visible, List<CustomKey> hidden) {
            this.context = context;
            this.names = names;
            this.visible = visible;
            this.hidden = hidden;
        }

        static final class KeyHolder extends RecyclerView.ViewHolder {
            final ImageView handle;
            final ImageView icon;
            final TextView name;
            final TextView combo;
            final ImageButton action;

            KeyHolder(View itemView) {
                super(itemView);
                handle = itemView.findViewById(R.id.custom_key_row_handle);
                icon = itemView.findViewById(R.id.custom_key_row_icon);
                name = itemView.findViewById(R.id.custom_key_row_name);
                combo = itemView.findViewById(R.id.custom_key_row_keys);
                action = itemView.findViewById(R.id.custom_key_row_action);
            }
        }

        @Override
        public int getItemViewType(int position) {
            return position == visible.size() && !hidden.isEmpty() ? TYPE_HEADER : TYPE_KEY;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            if (viewType == TYPE_HEADER) {
                View header = inflater.inflate(R.layout.special_keys_header, parent, false);
                ((TextView) header).setText(R.string.special_keys_hidden_header);
                return new RecyclerView.ViewHolder(header) {
                };
            }
            return new KeyHolder(inflater.inflate(R.layout.custom_key_row, parent, false));
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
            if (!(viewHolder instanceof KeyHolder)) {
                return;
            }
            KeyHolder holder = (KeyHolder) viewHolder;
            boolean isHidden = position > visible.size();
            CustomKey key = isHidden ? hidden.get(position - visible.size() - 1) : visible.get(position);
            boolean builtIn = BuiltInKeys.isBuiltIn(key.id);

            holder.icon.setImageResource(IconCatalog.drawableFor(key.icon));
            holder.name.setText(key.name);
            // A flow shows its steps, a shortcut shows its keys
            String combo = key.isSingleCombo() ? names.describe(key.keys) : CustomKeyEditor.summary(context, names, key);
            holder.combo.setText(builtIn ? context.getString(R.string.special_key_built_in, combo) : combo);
            holder.itemView.setAlpha(isHidden ? 0.6f : 1f);
            holder.handle.setVisibility(isHidden ? View.INVISIBLE : View.VISIBLE);

            // The eye shows the key's state like the menu editor: open while it is shown, crossed while it is hidden
            if (isHidden) {
                holder.action.setImageResource(R.drawable.ic_sa_visibility_off);
                holder.action.setContentDescription(context.getString(R.string.special_key_show));
                holder.action.setOnClickListener(v -> {
                    SpecialKeys.setHidden(context, key.id, false);
                    reload.run();
                });
                holder.itemView.setOnClickListener(null);
                holder.itemView.setClickable(false);
                return;
            }

            if (builtIn) {
                holder.action.setImageResource(R.drawable.ic_sa_visibility);
                holder.action.setContentDescription(context.getString(R.string.special_key_hide));
                holder.action.setOnClickListener(v -> {
                    SpecialKeys.setHidden(context, key.id, true);
                    reload.run();
                });
                holder.itemView.setOnClickListener(v -> KeyEditActions.edit(context, key.id, reload));
            }
            else {
                holder.action.setImageResource(R.drawable.ic_sa_delete);
                holder.action.setContentDescription(context.getString(R.string.custom_key_delete));
                holder.action.setOnClickListener(v -> new AlertDialog.Builder(context)
                        .setCustomTitle(Dialogs.header(context, R.drawable.ic_sa_delete,
                                context.getString(R.string.custom_key_delete_confirm, key.name), null))
                        .setPositiveButton(R.string.custom_key_delete, (d, which) -> {
                            List<CustomKey> custom = CustomKeyStore.loadOrEmpty(context);
                            for (int i = 0; i < custom.size(); i++) {
                                if (custom.get(i).id.equals(key.id)) {
                                    custom.remove(i);
                                    break;
                                }
                            }
                            CustomKeyStore.save(context, custom);
                            reload.run();
                        })
                        .setNegativeButton(R.string.game_menu_cancel, null)
                        .show());
                holder.itemView.setOnClickListener(v -> KeyEditActions.edit(context, key.id, reload));
            }
            holder.handle.setOnTouchListener((v, event) -> {
                if (event.getActionMasked() == MotionEvent.ACTION_DOWN && dragStarter != null) {
                    dragStarter.startDrag(holder);
                }
                return false;
            });
        }

        @Override
        public int getItemCount() {
            return visible.size() + (hidden.isEmpty() ? 0 : hidden.size() + 1);
        }
    }
}
