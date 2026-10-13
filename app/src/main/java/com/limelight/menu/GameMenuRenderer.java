package com.limelight.menu;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.limelight.R;

import java.util.ArrayList;
import java.util.List;

// NOTE: Renders in-stream menus in the list, compact or grid style with one RecyclerView, including full-width section headers.
// NOTE: The same renderer draws the live preview in the menu editor, so the preview always matches the real menu.
public final class GameMenuRenderer {
    public interface OnEntryClick {
        void onClick(MenuEntry entry);
    }

    private static final int GRID_COLUMN_WIDTH_DP = 96;

    private GameMenuRenderer() {
    }

    // A null click listener renders a read-only menu, which the editor preview uses
    public static RecyclerView create(Context context, String style, OnEntryClick onClick) {
        float density = context.getResources().getDisplayMetrics().density;
        RecyclerView view = new RecyclerView(context);
        Adapter adapter = new Adapter(style, onClick);
        view.setAdapter(adapter);
        view.setItemAnimator(null);
        view.setClipToPadding(false);

        if (GameMenuLayout.STYLE_GRID.equals(style)) {
            AutoFitGridLayoutManager layoutManager = new AutoFitGridLayoutManager(context, (int) (GRID_COLUMN_WIDTH_DP * density));
            layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                @Override
                public int getSpanSize(int position) {
                    return adapter.isHeader(position) ? layoutManager.getSpanCount() : 1;
                }
            });
            view.setLayoutManager(layoutManager);
            int padding = (int) (8 * density);
            view.setPadding(padding, padding, padding, padding);
        }
        else {
            view.setLayoutManager(new LinearLayoutManager(context));
            int padding = (int) (8 * density);
            view.setPadding(0, padding, 0, padding);
        }
        return view;
    }

    public static void setEntries(RecyclerView view, List<MenuEntry> entries) {
        ((Adapter) view.getAdapter()).setEntries(entries);
    }

    // Focuses the first tappable item, so D-pad and controller users can navigate right away
    public static void focusFirstItem(RecyclerView view) {
        Adapter adapter = (Adapter) view.getAdapter();
        for (int i = 0; i < adapter.getItemCount(); i++) {
            if (!adapter.isHeader(i)) {
                final int position = i;
                view.post(() -> {
                    RecyclerView.ViewHolder holder = view.findViewHolderForAdapterPosition(position);
                    if (holder != null) {
                        holder.itemView.requestFocus();
                    }
                });
                return;
            }
        }
    }

    static int layoutFor(String style) {
        if (GameMenuLayout.STYLE_GRID.equals(style)) {
            return R.layout.game_menu_grid_item;
        }
        if (GameMenuLayout.STYLE_COMPACT.equals(style)) {
            return R.layout.game_menu_compact_item;
        }
        return R.layout.game_menu_list_item;
    }

    // Picks as many columns of at least the given width as fit, recomputed whenever the menu is laid out
    private static final class AutoFitGridLayoutManager extends GridLayoutManager {
        private final int columnWidth;

        AutoFitGridLayoutManager(Context context, int columnWidth) {
            super(context, 1);
            this.columnWidth = columnWidth;
        }

        @Override
        public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
            int width = getWidth() - getPaddingLeft() - getPaddingRight();
            if (width > 0) {
                int spans = Math.max(1, width / columnWidth);
                if (spans != getSpanCount()) {
                    setSpanCount(spans);
                }
            }
            super.onLayoutChildren(recycler, state);
        }
    }

    private static final class Adapter extends RecyclerView.Adapter<Adapter.Holder> {
        private static final int TYPE_HEADER = 0;
        private static final int TYPE_ITEM = 1;
        private static final int TYPE_DESTRUCTIVE = 2;

        private final String style;
        private final OnEntryClick onClick;
        private final List<MenuEntry> entries = new ArrayList<>();

        Adapter(String style, OnEntryClick onClick) {
            this.style = style;
            this.onClick = onClick;
        }

        void setEntries(List<MenuEntry> newEntries) {
            entries.clear();
            entries.addAll(newEntries);
            notifyDataSetChanged();
        }

        boolean isHeader(int position) {
            return position >= 0 && position < entries.size() && entries.get(position).header;
        }

        static final class Holder extends RecyclerView.ViewHolder {
            final ImageView icon;
            final TextView label;

            Holder(View itemView) {
                super(itemView);
                icon = itemView.findViewById(R.id.game_menu_item_icon);
                label = itemView.findViewById(R.id.game_menu_item_label);
            }
        }

        @Override
        public int getItemViewType(int position) {
            MenuEntry entry = entries.get(position);
            if (entry.header) {
                return TYPE_HEADER;
            }
            return entry.destructive ? TYPE_DESTRUCTIVE : TYPE_ITEM;
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            if (viewType == TYPE_HEADER) {
                View view = inflater.inflate(R.layout.game_menu_section_header, parent, false);
                ViewCompat.setAccessibilityHeading(view, true);
                return new Holder(view);
            }

            Holder holder = new Holder(inflater.inflate(layoutFor(style), parent, false));
            // Session actions end the stream, so they are drawn in red in every style
            if (viewType == TYPE_DESTRUCTIVE) {
                ColorStateList red = ColorStateList.valueOf(ContextCompat.getColor(parent.getContext(), R.color.menu_destructive));
                holder.label.setTextColor(red);
                if (holder.icon != null) {
                    ImageViewCompat.setImageTintList(holder.icon, red);
                }
            }
            boolean interactive = onClick != null;
            holder.itemView.setClickable(interactive);
            holder.itemView.setFocusable(interactive);
            return holder;
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            MenuEntry entry = entries.get(position);
            holder.label.setText(entry.label);
            if (entry.header) {
                return;
            }

            if (holder.icon != null) {
                boolean showIcon = entry.iconRes != 0 && !GameMenuLayout.STYLE_LIST.equals(style);
                holder.icon.setVisibility(showIcon ? View.VISIBLE : View.GONE);
                if (showIcon) {
                    holder.icon.setImageResource(entry.iconRes);
                }
            }
            if (onClick != null) {
                holder.itemView.setOnClickListener(v -> onClick.onClick(entry));
            }
        }

        @Override
        public int getItemCount() {
            return entries.size();
        }
    }
}
