package com.limelight.ui;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

// NOTE: Drag to reorder for every editor list (special keys, Quick Menu, quick-action bars), with an edge scroll that keeps moving while a row is held at the top or bottom.
// NOTE: ItemTouchHelper's default edge scroll ramps up with the fifth power of time over two seconds, so a second into the drag it scrolls at about 3% speed and the list looks stuck.
// NOTE: Here the speed starts at a third of the maximum, grows with how far the row is past the edge, and reaches full speed after 400 ms.
// NOTE: A fast drag can jump over several rows in one step, so the data must move the item (remove, then insert) exactly like notifyItemMoved draws it.
// NOTE: Collections.swap matches the screen only for neighbouring rows, so a fast drag with it saves an order different from the one shown.
public final class DragReorder {
    public interface Mover {
        // Moves the item in the data and notifies the adapter, or returns false to refuse the move
        boolean move(int from, int to);
    }

    public interface Filter {
        boolean canDrag(int position);
    }

    static final long RAMP_MS = 400;

    private DragReorder() {
    }

    // Moves one item to a new position, the same change notifyItemMoved shows on screen
    public static <T> void moveItem(java.util.List<T> items, int from, int to) {
        items.add(to, items.remove(from));
    }

    // Pixels to scroll this frame. The sign follows the side the row left the list on, and the result is never zero while out of bounds.
    static int edgeScroll(int maxScroll, int viewSize, int viewSizeOutOfBounds, long msSinceStartScroll) {
        if (viewSizeOutOfBounds == 0) {
            return 0;
        }
        int direction = (int) Math.signum(viewSizeOutOfBounds);
        float distance = Math.min(1f, Math.abs(viewSizeOutOfBounds) / (float) Math.max(1, viewSize));
        float time = Math.min(1f, msSinceStartScroll / (float) RAMP_MS);
        int speed = Math.round(maxScroll * (0.35f + 0.65f * distance) * (0.4f + 0.6f * time));
        return direction * Math.max(1, speed);
    }

    // Vertical drag for the whole list. A filter of null lets every row move. A long press anywhere on a row also starts a drag when longPress is true.
    public static ItemTouchHelper attach(RecyclerView list, boolean longPress, Filter filter, Mover mover, Runnable onDragEnd) {
        ItemTouchHelper helper = new ItemTouchHelper(new ItemTouchHelper.Callback() {
            @Override
            public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder holder) {
                int position = holder.getBindingAdapterPosition();
                boolean allowed = position != RecyclerView.NO_POSITION && (filter == null || filter.canDrag(position));
                return allowed ? makeMovementFlags(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) : 0;
            }

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder from, @NonNull RecyclerView.ViewHolder to) {
                int fromPos = from.getBindingAdapterPosition();
                int toPos = to.getBindingAdapterPosition();
                if (fromPos == RecyclerView.NO_POSITION || toPos == RecyclerView.NO_POSITION) {
                    return false;
                }
                if (filter != null && (!filter.canDrag(fromPos) || !filter.canDrag(toPos))) {
                    return false;
                }
                return mover.move(fromPos, toPos);
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            }

            @Override
            public boolean isLongPressDragEnabled() {
                return longPress;
            }

            @Override
            public int interpolateOutOfBoundsScroll(@NonNull RecyclerView recyclerView, int viewSize, int viewSizeOutOfBounds,
                                                    int totalSize, long msSinceStartScroll) {
                int maxScroll = (int) (20 * recyclerView.getResources().getDisplayMetrics().density);
                return edgeScroll(maxScroll, viewSize, viewSizeOutOfBounds, msSinceStartScroll);
            }

            @Override
            public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);
                if (onDragEnd != null) {
                    onDragEnd.run();
                }
            }
        });
        helper.attachToRecyclerView(list);
        return helper;
    }
}
