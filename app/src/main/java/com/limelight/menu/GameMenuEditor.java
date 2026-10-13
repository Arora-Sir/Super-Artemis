package com.limelight.menu;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.util.DisplayMetrics;
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
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.limelight.R;
import com.limelight.actions.StreamAction;
import com.limelight.actions.StreamActionRegistry;
import com.limelight.customkeys.CustomKey;
import com.limelight.customkeys.IconCatalog;
import com.limelight.customkeys.KeyPicker;
import com.limelight.customkeys.SpecialKeys;
import com.limelight.ui.Dialogs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// NOTE: Dialog for choosing the menu style and which actions appear in the in-stream Quick Menu, in what order.
// NOTE: A live preview drawn by GameMenuRenderer updates on every change, so the user sees the real result before saving.
public final class GameMenuEditor {
    // Screens at least this wide show the list and the preview side by side
    private static final int TWO_PANE_MIN_WIDTH_DP = 600;
    private static final int[] STYLE_IDS = {
            R.id.game_menu_editor_style_list, R.id.game_menu_editor_style_grid, R.id.game_menu_editor_style_compact};

    static final class Row {
        // The stored item: an action ID or "key:<special key ID>"
        final String item;
        // Null for a special key
        final StreamAction action;
        final int iconRes;
        final String label;
        boolean inQuickMenu;

        Row(StreamAction action, String label, boolean inQuickMenu) {
            this(action.id, action, action.iconRes, label, inQuickMenu);
        }

        Row(String item, StreamAction action, int iconRes, String label, boolean inQuickMenu) {
            this.item = item;
            this.action = action;
            this.iconRes = iconRes;
            this.label = label;
            this.inQuickMenu = inQuickMenu;
        }

        static Row forKey(Context context, CustomKey key, boolean inQuickMenu) {
            return new Row(GameMenuLayout.keyItem(key.id), null, IconCatalog.drawableFor(key.icon),
                    context.getString(R.string.quick_bar_custom_key, key.name), inQuickMenu);
        }
    }

    private GameMenuEditor() {
    }

    // Checked rows come first in their saved order, then the other actions in catalog order, then the other special keys
    static List<Row> buildRows(Context context, List<String> quickItems, List<CustomKey> keys) {
        Resources res = context.getResources();
        Map<String, CustomKey> keysById = new LinkedHashMap<>();
        for (CustomKey key : keys) {
            keysById.put(key.id, key);
        }
        List<Row> rows = new ArrayList<>();
        for (String id : quickItems) {
            String keyId = GameMenuLayout.keyId(id);
            if (keyId != null) {
                CustomKey key = keysById.get(keyId);
                if (key != null) {
                    rows.add(Row.forKey(context, key, true));
                }
                continue;
            }
            StreamAction action = StreamActionRegistry.find(id);
            if (action != null && !action.destructive) {
                rows.add(new Row(action, res.getString(action.nameRes), true));
            }
        }
        for (StreamAction action : GameMenuLayout.configurableActions()) {
            if (!quickItems.contains(action.id)) {
                rows.add(new Row(action, res.getString(action.nameRes), false));
            }
        }
        for (CustomKey key : keys) {
            if (!quickItems.contains(GameMenuLayout.keyItem(key.id))) {
                rows.add(Row.forKey(context, key, false));
            }
        }
        return rows;
    }

    // The Quick Menu as it will look with these rows: pinned actions, the fixed Session section, then navigation
    static List<MenuEntry> buildPreview(Resources res, List<Row> rows) {
        List<MenuEntry> actions = new ArrayList<>();
        for (Row row : rows) {
            if (row.inQuickMenu) {
                actions.add(new MenuEntry(row.action != null ? res.getString(row.action.nameRes) : row.label, row.iconRes, false, false, null));
            }
        }
        List<MenuEntry> session = new ArrayList<>();
        for (StreamAction action : StreamActionRegistry.sessionActions()) {
            session.add(new MenuEntry(res.getString(action.nameRes), action.iconRes, true, false, null));
        }
        List<MenuEntry> navigation = new ArrayList<>();
        navigation.add(new MenuEntry(res.getString(R.string.game_menu_advanced), R.drawable.ic_gm_more, false, false, null));
        navigation.add(new MenuEntry(res.getString(R.string.game_menu_customize), R.drawable.ic_gm_customize_menu, false, false, null));
        navigation.add(new MenuEntry(res.getString(R.string.game_menu_customize_quick_bar), R.drawable.ic_gm_toggle_floating_button, false, false, null));
        navigation.add(new MenuEntry(res.getString(R.string.game_menu_cancel), R.drawable.ic_gm_cancel, false, false, null));

        return new MenuSections(res)
                .add(R.string.game_menu_section_actions, actions)
                .add(R.string.game_menu_section_session, session)
                .add(R.string.game_menu_section_menu, navigation)
                .build();
    }

    static int radioIdFor(String style) {
        if (GameMenuLayout.STYLE_GRID.equals(style)) {
            return R.id.game_menu_editor_style_grid;
        }
        if (GameMenuLayout.STYLE_COMPACT.equals(style)) {
            return R.id.game_menu_editor_style_compact;
        }
        return R.id.game_menu_editor_style_list;
    }

    static String styleFor(int radioId) {
        if (radioId == R.id.game_menu_editor_style_grid) {
            return GameMenuLayout.STYLE_GRID;
        }
        if (radioId == R.id.game_menu_editor_style_compact) {
            return GameMenuLayout.STYLE_COMPACT;
        }
        return GameMenuLayout.STYLE_LIST;
    }

    public static void show(Context context, SharedPreferences prefs, Runnable onChanged) {
        final List<Row> rows = buildRows(context, GameMenuLayout.getQuickItems(prefs), SpecialKeys.visible(context));
        final Resources res = context.getResources();
        final DisplayMetrics metrics = res.getDisplayMetrics();
        final boolean twoPane = metrics.widthPixels / metrics.density >= TWO_PANE_MIN_WIDTH_DP;

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        Context dialogContext = builder.getContext();
        View body = LayoutInflater.from(dialogContext).inflate(R.layout.dialog_game_menu_editor, null);
        FrameLayout listContainer = body.findViewById(R.id.game_menu_editor_list);
        FrameLayout previewContainer = body.findViewById(R.id.game_menu_editor_preview);
        LinearLayout panes = body.findViewById(R.id.game_menu_editor_panes);

        // NOTE: Dialogs only give custom views the space they ask for, so both panes get explicit heights from the screen size.
        // NOTE: Narrow screens stack the preview under the list and give it a little less room.
        int paneHeight = (int) (metrics.heightPixels * (twoPane ? 0.55f : 0.3f));
        panes.setOrientation(twoPane ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
        View previewPane = body.findViewById(R.id.game_menu_editor_preview_pane);
        if (!twoPane) {
            listContainer.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            LinearLayout.LayoutParams previewParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            previewParams.topMargin = (int) (12 * metrics.density);
            previewPane.setLayoutParams(previewParams);
            previewPane.setPadding(0, 0, 0, 0);
        }

        RecyclerView list = new RecyclerView(dialogContext);
        list.setLayoutManager(new LinearLayoutManager(dialogContext));
        LinearLayout listColumn = new LinearLayout(dialogContext);
        listColumn.setOrientation(LinearLayout.VERTICAL);
        listColumn.addView(list, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, paneHeight));
        listContainer.addView(listColumn, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        final String[] style = {GameMenuLayout.getStyle(prefs)};
        final RecyclerView[] preview = new RecyclerView[1];
        final int previewHeight = twoPane ? paneHeight : (int) (paneHeight * 0.85f);
        // The renderer fixes its style when created, so a style change rebuilds the preview view
        final Runnable rebuildPreview = () -> {
            previewContainer.removeAllViews();
            preview[0] = GameMenuRenderer.create(dialogContext, style[0], null);
            previewContainer.addView(preview[0], new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, previewHeight));
            GameMenuRenderer.setEntries(preview[0], buildPreview(res, rows));
        };
        final Runnable refreshPreview = () -> GameMenuRenderer.setEntries(preview[0], buildPreview(res, rows));
        rebuildPreview.run();

        // List, Grid and Compact work as one segmented button, and every choice redraws the preview
        final Runnable markStyle = () -> {
            for (int id : STYLE_IDS) {
                body.findViewById(id).setSelected(id == radioIdFor(style[0]));
            }
        };
        for (int id : STYLE_IDS) {
            body.findViewById(id).setOnClickListener(v -> {
                style[0] = styleFor(id);
                markStyle.run();
                rebuildPreview.run();
            });
        }
        markStyle.run();

        final ItemTouchHelper[] touchHelper = new ItemTouchHelper[1];
        final RowAdapter adapter = new RowAdapter(rows, holder -> touchHelper[0].startDrag(holder), refreshPreview);
        list.setAdapter(adapter);
        // A key or flow made here is pinned straight away, after the rows already in the Quick Menu
        listColumn.addView(KeyPicker.addButton(dialogContext, key -> {
            int at = 0;
            while (at < rows.size() && rows.get(at).inQuickMenu) {
                at++;
            }
            rows.add(at, Row.forKey(context, key, true));
            adapter.notifyItemInserted(at);
            list.scrollToPosition(at);
            refreshPreview.run();
        }), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

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
                refreshPreview.run();
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

        builder.setTitle(R.string.title_customize_game_menu)
                .setView(body)
                .setPositiveButton(R.string.game_menu_editor_save, (dialog, which) -> {
                    List<String> ids = new ArrayList<>();
                    for (Row row : rows) {
                        if (row.inQuickMenu) {
                            ids.add(row.item);
                        }
                    }
                    GameMenuLayout.setQuickItems(prefs, ids);
                    GameMenuLayout.setStyle(prefs, style[0]);
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
                .setNegativeButton(R.string.game_menu_cancel, null);
        Dialogs.show(builder, twoPane ? Dialogs.WIDTH_WIDE_DP : Dialogs.WIDTH_STANDARD_DP);
    }

    private interface DragStarter {
        void startDrag(RecyclerView.ViewHolder holder);
    }

    private static final class RowAdapter extends RecyclerView.Adapter<RowAdapter.Holder> {
        private final List<Row> rows;
        private final DragStarter dragStarter;
        private final Runnable onRowsChanged;

        RowAdapter(List<Row> rows, DragStarter dragStarter, Runnable onRowsChanged) {
            this.rows = rows;
            this.dragStarter = dragStarter;
            this.onRowsChanged = onRowsChanged;
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
            holder.check.setChecked(row.inQuickMenu);
            holder.check.setOnCheckedChangeListener((button, checked) -> {
                row.inQuickMenu = checked;
                onRowsChanged.run();
            });
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
