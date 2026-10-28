package com.limelight.customkeys;

import android.app.AlertDialog;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.limelight.R;
import com.limelight.ui.Dialogs;

import java.util.ArrayList;
import java.util.List;

// NOTE: The icon grid for special keys. It scrolls on its own and fits as many columns as the dialog is wide, so every icon stays a 48dp target.
// NOTE: Icons sit in sections (Mic and voice, Media, Windows and so on), and the search field narrows them by name or by words such as "mute" or "voice".
// NOTE: Tapping the selected icon again clears it, which means "no icon" for a custom key and "default icon" for a built-in key.
final class IconPicker {
    interface OnPicked {
        void onPicked(String icon);
    }

    private static final int CELL_DP = 52;
    private static final int GRID_HEIGHT_DP = 280;

    private IconPicker() {
    }

    static void attach(RecyclerView grid, String[] selected) {
        attach(grid, null, selected);
    }

    static void attach(RecyclerView grid, EditText search, String[] selected) {
        Context context = grid.getContext();
        IconAdapter adapter = new IconAdapter(selected);
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
        // A section title or the empty message takes a whole row
        layout.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return adapter.isIcon(position) ? 1 : layout.getSpanCount();
            }
        });
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
        grid.setAdapter(adapter);
        if (search != null) {
            search.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    adapter.show(s.toString());
                    grid.scrollToPosition(0);
                }
            });
        }
    }

    // Icon choice for a built-in key, whose keys and name stay fixed
    static void showForBuiltIn(Context context, CustomKey key, OnPicked onPicked) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        Context dialogContext = builder.getContext();
        String[] selected = {key.icon};
        View body = LayoutInflater.from(dialogContext).inflate(R.layout.dialog_icon_picker, null);
        attach(body.findViewById(R.id.icon_picker_grid), body.findViewById(R.id.icon_picker_search), selected);
        builder.setCustomTitle(Dialogs.header(dialogContext, IconCatalog.drawableFor(key.icon), key.name,
                        context.getString(R.string.special_key_icon_hint)))
                .setView(body)
                .setPositiveButton(R.string.game_menu_editor_save, (d, which) -> onPicked.onPicked(selected[0]))
                .setNegativeButton(R.string.game_menu_cancel, null);
        Dialogs.show(builder, Dialogs.WIDTH_STANDARD_DP);
    }

    // The rows of the grid for a search: a section title before the icons of each group, or one message when nothing matches
    static List<Object> rowsFor(List<IconCatalog.Icon> icons, String emptyMessage) {
        List<Object> rows = new ArrayList<>();
        if (icons.isEmpty()) {
            rows.add(emptyMessage);
            return rows;
        }
        IconCatalog.Group group = null;
        for (IconCatalog.Icon icon : icons) {
            if (icon.group != group) {
                group = icon.group;
                rows.add(group);
            }
            rows.add(icon);
        }
        return rows;
    }

    private static final class IconAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private static final int TYPE_ICON = 0;
        private static final int TYPE_TITLE = 1;

        private final String[] selected;
        private List<Object> rows;

        IconAdapter(String[] selected) {
            this.selected = selected;
            this.rows = rowsFor(IconCatalog.ICONS, "");
        }

        boolean isIcon(int position) {
            return rows.get(position) instanceof IconCatalog.Icon;
        }

        @android.annotation.SuppressLint("NotifyDataSetChanged")
        void show(String query) {
            rows = rowsFor(IconCatalog.search(query), "");
            notifyDataSetChanged();
        }

        static final class Holder extends RecyclerView.ViewHolder {
            final ImageView image;

            Holder(View itemView) {
                super(itemView);
                image = itemView.findViewById(R.id.custom_key_icon);
            }
        }

        static final class TitleHolder extends RecyclerView.ViewHolder {
            final TextView title;

            TitleHolder(View itemView) {
                super(itemView);
                title = (TextView) itemView;
            }
        }

        @Override
        public int getItemViewType(int position) {
            return isIcon(position) ? TYPE_ICON : TYPE_TITLE;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            if (viewType == TYPE_ICON) {
                return new Holder(inflater.inflate(R.layout.custom_key_icon_item, parent, false));
            }
            return new TitleHolder(inflater.inflate(R.layout.special_keys_header, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
            Object row = rows.get(position);
            if (viewHolder instanceof TitleHolder) {
                TextView title = ((TitleHolder) viewHolder).title;
                title.setPadding(title.getPaddingLeft(), title.getPaddingTop() / 2, title.getPaddingRight(), title.getPaddingBottom());
                title.setText(row instanceof IconCatalog.Group
                        ? title.getContext().getString(((IconCatalog.Group) row).labelRes)
                        : title.getContext().getString(R.string.icon_search_empty));
                return;
            }
            Holder holder = (Holder) viewHolder;
            IconCatalog.Icon icon = (IconCatalog.Icon) row;
            holder.image.setImageResource(icon.drawableRes);
            holder.image.setContentDescription(icon.id.replace('_', ' '));
            holder.image.setSelected(icon.id.equals(selected[0]));
            holder.image.setOnClickListener(v -> {
                selected[0] = icon.id.equals(selected[0]) ? null : icon.id;
                notifyItemRangeChanged(0, getItemCount());
            });
        }

        @Override
        public int getItemCount() {
            return rows.size();
        }
    }
}
