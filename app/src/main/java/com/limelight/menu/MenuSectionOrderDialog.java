package com.limelight.menu;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.limelight.R;
import com.limelight.ui.Dialogs;
import com.limelight.ui.DragReorder;

import java.util.List;

// NOTE: A short list of the three Quick Menu sections that is reordered by dragging a row by its handle or by a long press, like the profiles list.
// NOTE: The new order is saved when a drag ends and applies the next time the Quick Menu opens.
public final class MenuSectionOrderDialog {
    private MenuSectionOrderDialog() {
    }

    public static void show(Context context, SharedPreferences prefs) {
        List<String> order = GameMenuLayout.getSectionOrder(prefs);
        show(context, order, () -> GameMenuLayout.setSectionOrder(prefs, order));
    }

    public static void show(Context context, List<String> order, Runnable onOrderChanged) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        Context dialogContext = builder.getContext();

        RecyclerView list = new RecyclerView(dialogContext);
        list.setLayoutManager(new LinearLayoutManager(dialogContext));
        final ItemTouchHelper[] helper = new ItemTouchHelper[1];
        SectionAdapter adapter = new SectionAdapter(order, holder -> helper[0].startDrag(holder));
        list.setAdapter(adapter);
        helper[0] = DragReorder.attach(list, true, null, (from, to) -> {
            DragReorder.moveItem(order, from, to);
            adapter.notifyItemMoved(from, to);
            if (onOrderChanged != null) {
                onOrderChanged.run();
            }
            return true;
        }, onOrderChanged);

        builder.setCustomTitle(Dialogs.header(dialogContext, R.drawable.ic_sa_menu_open, context.getString(R.string.title_menu_section_order),
                        context.getString(R.string.menu_section_order_hint)))
                .setView(list)
                .setNeutralButton(R.string.game_menu_editor_reset, null)
                .setPositiveButton(android.R.string.ok, null);
        AlertDialog dialog = Dialogs.show(builder, Dialogs.WIDTH_STANDARD_DP);
        // Reset keeps the dialog open so the default order is visible right away
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
            order.clear();
            order.addAll(GameMenuLayout.DEFAULT_SECTION_ORDER);
            adapter.notifyDataSetChanged();
            if (onOrderChanged != null) {
                onOrderChanged.run();
            }
        });
    }

    interface DragStarter {
        void startDrag(RecyclerView.ViewHolder holder);
    }

    static final class SectionAdapter extends RecyclerView.Adapter<SectionAdapter.Holder> {
        private final List<String> order;
        private final DragStarter dragStarter;

        SectionAdapter(List<String> order, DragStarter dragStarter) {
            this.order = order;
            this.dragStarter = dragStarter;
        }

        static final class Holder extends RecyclerView.ViewHolder {
            final ImageView handle;
            final TextView label;

            Holder(View itemView) {
                super(itemView);
                handle = itemView.findViewById(R.id.game_menu_editor_handle);
                label = itemView.findViewById(R.id.game_menu_item_label);
                itemView.findViewById(R.id.game_menu_item_icon).setVisibility(View.GONE);
                itemView.findViewById(R.id.game_menu_editor_edit).setVisibility(View.GONE);
                itemView.findViewById(R.id.game_menu_editor_visibility).setVisibility(View.GONE);
                itemView.findViewById(R.id.game_menu_editor_check).setVisibility(View.GONE);
            }
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.game_menu_editor_item, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            holder.label.setText(GameMenuLayout.sectionTitleRes(order.get(position)));
            holder.handle.setOnTouchListener((v, event) -> {
                if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    dragStarter.startDrag(holder);
                }
                return false;
            });
        }

        @Override
        public int getItemCount() {
            return order.size();
        }
    }
}
