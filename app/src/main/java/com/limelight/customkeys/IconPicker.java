package com.limelight.customkeys;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.limelight.R;
import com.limelight.ui.Dialogs;

// NOTE: The icon grid for special keys. It scrolls on its own and fits as many columns as the dialog is wide, so every icon stays a 48dp target.
// NOTE: Tapping the selected icon again clears it, which means "no icon" for a custom key and "default icon" for a built-in key.
final class IconPicker {
    interface OnPicked {
        void onPicked(String icon);
    }

    private static final int CELL_DP = 52;
    private static final int GRID_HEIGHT_DP = 232;

    private IconPicker() {
    }

    static void attach(RecyclerView grid, String[] selected) {
        Context context = grid.getContext();
        // Columns are worked out before each layout pass, so every row has the same number of cells
        GridLayoutManager layout = new GridLayoutManager(context, 6) {
            @Override
            public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
                int width = getWidth() - getPaddingLeft() - getPaddingRight();
                if (width > 0) {
                    int columns = Math.max(4, (int) (width / Dialogs.dp(context, CELL_DP)));
                    if (getSpanCount() != columns) {
                        setSpanCount(columns);
                    }
                }
                super.onLayoutChildren(recycler, state);
            }
        };
        grid.setLayoutManager(layout);
        grid.getLayoutParams().height = (int) Dialogs.dp(context, GRID_HEIGHT_DP);
        // The grid sits inside a scrolling dialog, so a drag that starts on it scrolls the icons and not the dialog
        grid.addOnItemTouchListener(new RecyclerView.SimpleOnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
                if (e.getActionMasked() == MotionEvent.ACTION_DOWN && rv.getParent() != null) {
                    rv.getParent().requestDisallowInterceptTouchEvent(true);
                }
                return false;
            }
        });
        grid.setAdapter(new IconAdapter(selected));
    }

    // Icon choice for a built-in key, whose keys and name stay fixed
    static void showForBuiltIn(Context context, CustomKey key, OnPicked onPicked) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        Context dialogContext = builder.getContext();
        String[] selected = {key.icon};
        View body = LayoutInflater.from(dialogContext).inflate(R.layout.dialog_icon_picker, null);
        attach(body.findViewById(R.id.icon_picker_grid), selected);
        builder.setCustomTitle(Dialogs.header(dialogContext, IconCatalog.drawableFor(key.icon), key.name,
                        context.getString(R.string.special_key_icon_hint)))
                .setView(body)
                .setPositiveButton(R.string.game_menu_editor_save, (d, which) -> onPicked.onPicked(selected[0]))
                .setNegativeButton(R.string.game_menu_cancel, null);
        Dialogs.show(builder, Dialogs.WIDTH_STANDARD_DP);
    }

    private static final class IconAdapter extends RecyclerView.Adapter<IconAdapter.Holder> {
        private final String[] selected;

        IconAdapter(String[] selected) {
            this.selected = selected;
        }

        static final class Holder extends RecyclerView.ViewHolder {
            final ImageView image;

            Holder(View itemView) {
                super(itemView);
                image = itemView.findViewById(R.id.custom_key_icon);
            }
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.custom_key_icon_item, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            IconCatalog.Icon icon = IconCatalog.ICONS.get(position);
            holder.image.setImageResource(icon.drawableRes);
            holder.image.setContentDescription(icon.id.replace('_', ' '));
            holder.image.setSelected(icon.id.equals(selected[0]));
            holder.image.setOnClickListener(v -> {
                selected[0] = icon.id.equals(selected[0]) ? null : icon.id;
                notifyDataSetChanged();
            });
        }

        @Override
        public int getItemCount() {
            return IconCatalog.ICONS.size();
        }
    }
}
