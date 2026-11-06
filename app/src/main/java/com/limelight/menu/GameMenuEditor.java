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
import com.limelight.customkeys.KeyEditActions;
import com.limelight.customkeys.KeyPicker;
import com.limelight.customkeys.SpecialKeys;
import com.limelight.ui.Dialogs;
import com.limelight.ui.ScaledPreview;
import com.limelight.ui.DragReorder;

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
    private static final int SHORT_SCREEN_MAX_DP = 480;
    // Room kept under the list for the Add key or flow button on a short screen
    private static final int ADD_BUTTON_DP = 64;
    private static final int[] STYLE_IDS = {
            R.id.game_menu_editor_style_list, R.id.game_menu_editor_style_grid, R.id.game_menu_editor_style_compact};

    static final class Row {
        // The stored item: an action ID or "key:<special key ID>"
        final String item;
        // Null for a special key
        final StreamAction action;
        // Label, menu label and icon change when the key behind a row is edited
        int iconRes;
        String label;
        // What the real Quick Menu shows: the action name, or the key name without the "Key:" prefix of the list
        String menuLabel;
        boolean inQuickMenu;
        // Left out of the Advanced Menu. Only matters for a row that is not in the Quick Menu.
        boolean hidden;

        Row(StreamAction action, String label, boolean inQuickMenu) {
            this(action.id, action, action.iconRes, label, label, inQuickMenu);
        }

        Row(String item, StreamAction action, int iconRes, String label, String menuLabel, boolean inQuickMenu) {
            this.item = item;
            this.action = action;
            this.iconRes = iconRes;
            this.label = label;
            this.menuLabel = menuLabel;
            this.inQuickMenu = inQuickMenu;
        }

        static Row forKey(Context context, CustomKey key, boolean inQuickMenu) {
            return new Row(GameMenuLayout.keyItem(key.id), null, IconCatalog.drawableFor(key.icon),
                    com.limelight.customkeys.KeyLabels.pickerLabel(context, key), com.limelight.customkeys.KeyLabels.displayLabel(context, key), inQuickMenu);
        }
    }

    private GameMenuEditor() {
    }

    // Checked rows come first in their saved order, then the other actions in catalog order, then the other special keys
    static List<Row> buildRows(Context context, List<String> quickItems, List<CustomKey> keys) {
        return buildRows(context, quickItems, java.util.Collections.emptySet(), keys);
    }

    static List<Row> buildRows(Context context, List<String> quickItems, java.util.Set<String> hiddenItems, List<CustomKey> keys) {
        return buildRows(context, quickItems, hiddenItems, java.util.Collections.emptyList(), keys);
    }

    // NOTE: A custom key outside the Quick Menu is hidden from the Advanced Menu until its eye is switched on, an action is shown until hidden.
    static List<Row> buildRows(Context context, List<String> quickItems, java.util.Set<String> hiddenItems, List<String> advancedKeys, List<CustomKey> keys) {
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
            if (action != null && !action.destructive && !GameMenuLayout.isMenuNavigation(action.id)) {
                rows.add(new Row(action, res.getString(action.nameRes), true));
            }
        }
        for (StreamAction action : GameMenuLayout.menuActions()) {
            if (!quickItems.contains(action.id)) {
                Row row = new Row(action, res.getString(action.nameRes), false);
                row.hidden = hiddenItems.contains(action.id);
                rows.add(row);
            }
        }
        for (CustomKey key : keys) {
            String item = GameMenuLayout.keyItem(key.id);
            if (!quickItems.contains(item)) {
                Row row = Row.forKey(context, key, false);
                row.hidden = !advancedKeys.contains(item);
                rows.add(row);
            }
        }
        return rows;
    }

    // The Quick Menu as it will look with these rows: pinned actions, the fixed Session section, then navigation
    private static final float PREVIEW_SCALE = 0.62f;

    static List<MenuEntry> buildPreview(Resources res, List<Row> rows) {
        return buildPreview(res, rows, GameMenuLayout.DEFAULT_SECTION_ORDER);
    }

    static List<MenuEntry> buildPreview(Resources res, List<Row> rows, List<String> sectionOrder) {
        List<MenuEntry> actions = new ArrayList<>();
        for (Row row : rows) {
            if (row.inQuickMenu) {
                actions.add(new MenuEntry(row.action != null ? res.getString(row.action.nameRes) : row.menuLabel, row.iconRes, false, false, null));
            }
        }
        List<MenuEntry> session = new ArrayList<>();
        for (StreamAction action : StreamActionRegistry.sessionActions()) {
            session.add(new MenuEntry(res.getString(action.nameRes), action.iconRes, true, false, null));
        }
        List<MenuEntry> navigation = new ArrayList<>();
        navigation.add(new MenuEntry(res.getString(R.string.game_menu_advanced), R.drawable.ic_gm_more, false, false, null));
        navigation.add(new MenuEntry(res.getString(R.string.game_menu_customize), R.drawable.ic_gm_customize_menu, false, false, null));
        navigation.add(new MenuEntry(res.getString(R.string.game_menu_settings), R.drawable.ic_ck_settings, false, false, null));
        navigation.add(new MenuEntry(res.getString(R.string.game_menu_customize_quick_bar), R.drawable.ic_gm_toggle_floating_button, false, false, null));
        navigation.add(new MenuEntry(res.getString(R.string.game_menu_cancel), R.drawable.ic_gm_cancel, false, false, null));

        java.util.Map<String, List<MenuEntry>> sections = new java.util.HashMap<>();
        sections.put(GameMenuLayout.SECTION_ACTIONS, actions);
        sections.put(GameMenuLayout.SECTION_SESSION, session);
        sections.put(GameMenuLayout.SECTION_MENU, navigation);
        MenuSections ordered = new MenuSections(res);
        for (String section : sectionOrder) {
            ordered.add(GameMenuLayout.sectionTitleRes(section), sections.get(section));
        }
        return ordered.build();
    }

    // NOTE: The Advanced Menu as it will look with these rows: the actions and keys that are switched off in the Quick Menu and not hidden, then navigation.
    static List<MenuEntry> buildAdvancedPreview(Resources res, List<Row> rows) {
        List<MenuEntry> actions = new ArrayList<>();
        for (Row row : rows) {
            if (!row.inQuickMenu && !row.hidden) {
                actions.add(new MenuEntry(row.action != null ? res.getString(row.action.nameRes) : row.menuLabel, row.iconRes, false, false, null));
            }
        }
        List<MenuEntry> navigation = new ArrayList<>();
        navigation.add(new MenuEntry(res.getString(R.string.game_menu_customize), R.drawable.ic_gm_customize_menu, false, false, null));
        navigation.add(new MenuEntry(res.getString(R.string.game_menu_customize_quick_bar), R.drawable.ic_gm_toggle_floating_button, false, false, null));
        navigation.add(new MenuEntry(res.getString(R.string.game_menu_back_to_quick_menu), R.drawable.ic_gm_back, false, false, null));
        navigation.add(new MenuEntry(res.getString(R.string.game_menu_cancel), R.drawable.ic_gm_cancel, false, false, null));
        return new MenuSections(res)
                .add(R.string.game_menu_section_actions, actions)
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
        final List<Row> rows = buildRows(context, GameMenuLayout.getQuickItems(prefs), GameMenuLayout.getHiddenItems(prefs), GameMenuLayout.getAdvancedKeys(prefs), SpecialKeys.visible(context));
        final Resources res = context.getResources();
        final DisplayMetrics metrics = res.getDisplayMetrics();
        final boolean twoPane = metrics.widthPixels / metrics.density >= TWO_PANE_MIN_WIDTH_DP;

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        Context dialogContext = builder.getContext();
        View body = LayoutInflater.from(dialogContext).inflate(R.layout.dialog_game_menu_editor, null);
        body.setTag(Dialogs.NO_LIST_EDGES);
        View listPane = body.findViewById(R.id.game_menu_editor_list_pane);
        FrameLayout listContainer = body.findViewById(R.id.game_menu_editor_list);
        FrameLayout previewContainer = body.findViewById(R.id.game_menu_editor_preview);
        LinearLayout panes = body.findViewById(R.id.game_menu_editor_panes);

        // NOTE: Dialogs only give custom views the space they ask for, so both panes get explicit heights from the screen size.
        // NOTE: Narrow screens stack the preview under the list. The list gets the same 40 percent of the screen as the bar editor list, so a phone shows about seven rows.
        // NOTE: A phone in landscape (under 480dp tall) has no room for the style row and the hint above two panes, so they move into the right pane, which scrolls by itself.
        // NOTE: The left pane then holds only the list and Add key or flow, so a drag on the list scrolls the list and nothing else.
        final boolean shortScreen = metrics.heightPixels / metrics.density < SHORT_SCREEN_MAX_DP;
        final int tabHeight = (int) (40 * metrics.density);
        int paneHeight = shortScreen ? Math.max((int) (180 * metrics.density), metrics.heightPixels - (int) (150 * metrics.density)) : (int) (metrics.heightPixels * (twoPane ? 0.55f : 0.3f));
        int listHeight = shortScreen ? paneHeight - (int) (ADD_BUTTON_DP * metrics.density) - tabHeight : twoPane ? paneHeight : (int) (metrics.heightPixels * 0.4f);
        panes.setOrientation(twoPane ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
        View previewPane = body.findViewById(R.id.game_menu_editor_preview_pane);
        if (!twoPane) {
            listPane.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            LinearLayout.LayoutParams previewParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            previewParams.topMargin = (int) (12 * metrics.density);
            previewPane.setLayoutParams(previewParams);
            previewPane.setPadding(0, 0, 0, 0);
        }

        RecyclerView list = new RecyclerView(dialogContext);
        list.setLayoutManager(new LinearLayoutManager(dialogContext));
        LinearLayout listColumn = new LinearLayout(dialogContext);
        listColumn.setOrientation(LinearLayout.VERTICAL);
        listColumn.addView(list, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, listHeight));
        listContainer.addView(listColumn, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        final String[] style = {GameMenuLayout.getStyle(prefs)};
        final RecyclerView[] preview = new RecyclerView[1];
        final List<String> sectionOrder = new ArrayList<>(GameMenuLayout.getSectionOrder(prefs));
        // NOTE: The preview shows the Quick Menu or the Advanced Menu, whichever segment is selected.
        final boolean[] showAdvanced = {false};
        final java.util.function.Supplier<List<MenuEntry>> previewEntries = () -> showAdvanced[0]
                ? buildAdvancedPreview(res, rows)
                : buildPreview(res, rows, sectionOrder);
        // NOTE: The preview is drawn at 62 percent and sits in a taller box, so most of the menu is visible at once instead of its first section only.
        final int previewHeight = shortScreen ? (int) (200 * metrics.density) : twoPane ? paneHeight : (int) (metrics.heightPixels * 0.34f);
        // The renderer fixes its style when created, so a style change rebuilds the preview view
        final Runnable rebuildPreview = () -> {
            previewContainer.removeAllViews();
            preview[0] = GameMenuRenderer.create(dialogContext, style[0], null);
            ScaledPreview.show(previewContainer, preview[0], previewHeight, PREVIEW_SCALE);
            GameMenuRenderer.setEntries(preview[0], previewEntries.get());
        };
        final Runnable refreshPreview = () -> GameMenuRenderer.setEntries(preview[0], previewEntries.get());
        rebuildPreview.run();

        LinearLayout sectionsColumn = new LinearLayout(dialogContext);
        sectionsColumn.setOrientation(LinearLayout.VERTICAL);
        sectionsColumn.setVisibility(View.GONE);

        RecyclerView sectionsList = new RecyclerView(dialogContext);
        sectionsList.setLayoutManager(new LinearLayoutManager(dialogContext));
        final ItemTouchHelper[] sectionsTouchHelper = new ItemTouchHelper[1];
        final MenuSectionOrderDialog.SectionAdapter sectionsAdapter = new MenuSectionOrderDialog.SectionAdapter(
                sectionOrder, holder -> sectionsTouchHelper[0].startDrag(holder));
        sectionsList.setAdapter(sectionsAdapter);
        sectionsTouchHelper[0] = DragReorder.attach(sectionsList, true, null, (from, to) -> {
            DragReorder.moveItem(sectionOrder, from, to);
            sectionsAdapter.notifyItemMoved(from, to);
            refreshPreview.run();
            return true;
        }, refreshPreview);
        sectionsColumn.addView(sectionsList, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView sectionsHint = new TextView(dialogContext);
        sectionsHint.setText(R.string.menu_section_order_hint);
        sectionsHint.setTextAppearance(dialogContext, R.style.TextAppearance_SuperArtemis_BodySmall);
        LinearLayout.LayoutParams hintParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hintParams.topMargin = (int) (8 * metrics.density);
        sectionsColumn.addView(sectionsHint, hintParams);

        listContainer.addView(sectionsColumn, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

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

        // Quick Menu and Advanced Menu work as a second segmented button above the preview
        final View previewQuick = body.findViewById(R.id.game_menu_editor_preview_quick);
        final View previewAdvanced = body.findViewById(R.id.game_menu_editor_preview_advanced);
        final Runnable markPreview = () -> {
            previewQuick.setSelected(!showAdvanced[0]);
            previewAdvanced.setSelected(showAdvanced[0]);
        };
        previewQuick.setOnClickListener(v -> {
            showAdvanced[0] = false;
            markPreview.run();
            refreshPreview.run();
        });
        previewAdvanced.setOnClickListener(v -> {
            showAdvanced[0] = true;
            markPreview.run();
            refreshPreview.run();
        });
        markPreview.run();

        final View tabActions = body.findViewById(R.id.game_menu_editor_tab_actions);
        final View tabSections = body.findViewById(R.id.game_menu_editor_tab_sections);
        final boolean[] showSections = {false};
        final Runnable markTabs = () -> {
            tabActions.setSelected(!showSections[0]);
            tabSections.setSelected(showSections[0]);
            listColumn.setVisibility(showSections[0] ? View.GONE : View.VISIBLE);
            sectionsColumn.setVisibility(showSections[0] ? View.VISIBLE : View.GONE);
        };
        tabActions.setOnClickListener(v -> {
            showSections[0] = false;
            markTabs.run();
        });
        tabSections.setOnClickListener(v -> {
            showSections[0] = true;
            markTabs.run();
            if (showAdvanced[0]) {
                showAdvanced[0] = false;
                markPreview.run();
                refreshPreview.run();
            }
        });
        markTabs.run();

        final ItemTouchHelper[] touchHelper = new ItemTouchHelper[1];
        final RowAdapter adapter = new RowAdapter(rows, holder -> touchHelper[0].startDrag(holder), refreshPreview);
        list.setAdapter(adapter);
        // NOTE: The pencil of a key row edits that key in place, so a forgotten name or icon is fixed without leaving the editor.
        adapter.editor = row -> KeyEditActions.editItem(context, row.item, () -> {
            for (Row other : rows) {
                String keyId = GameMenuLayout.keyId(other.item);
                CustomKey changed = keyId != null ? SpecialKeys.find(context, keyId) : null;
                if (changed != null) {
                    other.iconRes = IconCatalog.drawableFor(changed.icon);
                    other.label = com.limelight.customkeys.KeyLabels.pickerLabel(context, changed);
                    other.menuLabel = com.limelight.customkeys.KeyLabels.displayLabel(context, changed);
                }
            }
            adapter.notifyDataSetChanged();
            refreshPreview.run();
        });
        // A key or flow made here is pinned straight away, after the rows already in the Quick Menu
        listColumn.addView(KeyPicker.addButton(dialogContext, context, key -> {
            int at = 0;
            while (at < rows.size() && rows.get(at).inQuickMenu) {
                at++;
            }
            rows.add(at, Row.forKey(context, key, true));
            adapter.notifyItemInserted(at);
            list.scrollToPosition(at);
            refreshPreview.run();
        }), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        touchHelper[0] = DragReorder.attach(list, true, null, (from, to) -> {
            DragReorder.moveItem(rows, from, to);
            adapter.notifyItemMoved(from, to);
            refreshPreview.run();
            return true;
        }, null);

        if (shortScreen) {
            // The right pane takes the style row, the preview and the hint, and scrolls by itself
            ViewGroup bodyGroup = (ViewGroup) body;
            View styleLabel = body.findViewById(R.id.game_menu_editor_style_label);
            View styleRow = body.findViewById(R.id.game_menu_editor_style);
            View hint = body.findViewById(R.id.game_menu_editor_hint);
            bodyGroup.removeView(styleLabel);
            bodyGroup.removeView(styleRow);
            bodyGroup.removeView(hint);
            LinearLayout pane = (LinearLayout) previewPane;
            pane.addView(styleLabel, 0);
            pane.addView(styleRow, 1);
            LinearLayout.LayoutParams rowParams = (LinearLayout.LayoutParams) styleRow.getLayoutParams();
            rowParams.bottomMargin = (int) (12 * metrics.density);
            pane.addView(hint);
            panes.removeView(previewPane);
            android.widget.ScrollView scroll = new android.widget.ScrollView(dialogContext);
            scroll.setTag(Dialogs.NO_SCROLL_EDGES);
            scroll.addView(previewPane, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            panes.addView(scroll, new LinearLayout.LayoutParams(0, paneHeight, 1f));
        }
        View content = body;
        if (!twoPane && !shortScreen) {
            // NOTE: A tall narrow screen cannot show the style row, the list and the preview at once, so the page scrolls, and a drag on the list scrolls the list first.
            androidx.core.widget.NestedScrollView page = new androidx.core.widget.NestedScrollView(dialogContext);
            page.addView(body);
            content = page;
        }
        builder.setCustomTitle(Dialogs.compactTitle(dialogContext, R.drawable.ic_gm_customize_menu, context.getString(R.string.title_customize_game_menu)))
                .setView(content)
                .setPositiveButton(R.string.game_menu_editor_save, (dialog, which) -> {
                    List<String> ids = new ArrayList<>();
                    List<String> hiddenIds = new ArrayList<>();
                    List<String> advancedKeys = new ArrayList<>();
                    for (Row row : rows) {
                        if (row.inQuickMenu) {
                            ids.add(row.item);
                        }
                        else if (row.action == null && !row.hidden) {
                            advancedKeys.add(row.item);
                        }
                        else if (row.hidden && row.action != null) {
                            hiddenIds.add(row.item);
                        }
                    }
                    GameMenuLayout.setQuickItems(prefs, ids);
                    GameMenuLayout.setHiddenItems(prefs, hiddenIds);
                    GameMenuLayout.setAdvancedKeys(prefs, advancedKeys);
                    GameMenuLayout.setStyle(prefs, style[0]);
                    GameMenuLayout.setSectionOrder(prefs, sectionOrder);
                    if (onChanged != null) {
                        onChanged.run();
                    }
                })
                .setNeutralButton(R.string.game_menu_editor_reset, (dialog, which) -> {
                    sectionOrder.clear();
                    sectionOrder.addAll(GameMenuLayout.DEFAULT_SECTION_ORDER);
                    GameMenuLayout.reset(prefs);
                    if (onChanged != null) {
                        onChanged.run();
                    }
                })
                .setNegativeButton(R.string.game_menu_cancel, null);
        Dialogs.reshowOnRotation(Dialogs.show(builder, twoPane ? Dialogs.WIDTH_WIDE_DP : Dialogs.WIDTH_STANDARD_DP), () -> show(context, prefs, onChanged));
    }

    private interface DragStarter {
        void startDrag(RecyclerView.ViewHolder holder);
    }

    private interface RowEditor {
        void edit(Row row);
    }

    private static final class RowAdapter extends RecyclerView.Adapter<RowAdapter.Holder> {
        private final List<Row> rows;
        private final DragStarter dragStarter;
        private final Runnable onRowsChanged;
        RowEditor editor;

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
            final ImageView visibility;
            final ImageView edit;

            Holder(View itemView) {
                super(itemView);
                edit = itemView.findViewById(R.id.game_menu_editor_edit);
                handle = itemView.findViewById(R.id.game_menu_editor_handle);
                icon = itemView.findViewById(R.id.game_menu_item_icon);
                label = itemView.findViewById(R.id.game_menu_item_label);
                check = itemView.findViewById(R.id.game_menu_editor_check);
                visibility = itemView.findViewById(R.id.game_menu_editor_visibility);
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
            boolean editable = editor != null && GameMenuLayout.keyId(row.item) != null;
            holder.edit.setVisibility(editable ? View.VISIBLE : View.GONE);
            holder.edit.setOnClickListener(editable ? v -> editor.edit(row) : null);
            holder.check.setOnCheckedChangeListener(null);
            holder.check.setChecked(row.inQuickMenu);
            bindVisibility(holder, row);
            holder.check.setOnCheckedChangeListener((button, checked) -> {
                row.inQuickMenu = checked;
                // Pinning an action shows it again, so unpinning it later puts it back in the Advanced Menu
                // A key goes back to hidden when unpinned, because keys are listed in the Advanced Menu only on request
                row.hidden = row.action == null && !checked;
                bindVisibility(holder, row);
                onRowsChanged.run();
            });
            holder.visibility.setOnClickListener(v -> {
                row.hidden = !row.hidden;
                bindVisibility(holder, row);
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

        // The eye only shows for an action outside the Quick Menu, because that is the only place hiding changes anything
        private static void bindVisibility(Holder holder, Row row) {
            boolean applies = !row.inQuickMenu;
            holder.visibility.setVisibility(applies ? View.VISIBLE : View.GONE);
            holder.visibility.setImageResource(row.hidden ? R.drawable.ic_sa_visibility_off : R.drawable.ic_sa_visibility);
            holder.visibility.setContentDescription(holder.itemView.getContext().getString(
                    row.hidden ? R.string.game_menu_editor_show_in_advanced : R.string.game_menu_editor_hide_from_advanced));
            float alpha = applies && row.hidden ? 0.45f : 1f;
            holder.icon.setAlpha(alpha);
            holder.label.setAlpha(alpha);
        }

        @Override
        public int getItemCount() {
            return rows.size();
        }
    }
}
