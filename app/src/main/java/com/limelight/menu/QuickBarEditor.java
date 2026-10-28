package com.limelight.menu;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.widget.TextViewCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.limelight.R;
import com.limelight.actions.StreamAction;
import com.limelight.customkeys.CustomKey;
import com.limelight.customkeys.IconCatalog;
import com.limelight.customkeys.KeyEditActions;
import com.limelight.customkeys.KeyPicker;
import com.limelight.customkeys.SpecialKeys;
import com.limelight.ui.Dialogs;
import com.limelight.ui.DragReorder;
import com.limelight.ui.FlowRow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// NOTE: Editor for the quick-action bars of every set. A screen preview draws the bars of the shown set to scale, and a chip or a tap selects one.
// NOTE: The selected bar gets its anchor, rows, columns, offset and items. The set menu switches, adds, renames, duplicates and deletes sets and copies a bar to another set.
// NOTE: Nothing is stored until Save, and Save also makes the shown set the active one, so the bars over the stream match what was edited.
public final class QuickBarEditor {
    // Screens at least this wide show the controls beside the list instead of above it
    private static final int TWO_PANE_MIN_WIDTH_DP = 720;
    private static final int TWO_PANE_MIN_HEIGHT_DP = 480;
    // Offset sliders move in steps of this many dp, which keeps 200 slider steps across the full range
    private static final int OFFSET_STEP_DP = BarSets.OFFSET_MAX_DP * 2 / 200;
    private static final int[] ANCHOR_LABELS = {
            R.string.quick_bar_anchor_top_left, R.string.quick_bar_anchor_top_center, R.string.quick_bar_anchor_top_right,
            R.string.quick_bar_anchor_center_left, R.string.quick_bar_anchor_center, R.string.quick_bar_anchor_center_right,
            R.string.quick_bar_anchor_bottom_left, R.string.quick_bar_anchor_bottom_center, R.string.quick_bar_anchor_bottom_right};

    static final class Row {
        final String item;
        // Label and icon change when the key behind a row is edited, the item never does
        int iconRes;
        String label;
        boolean pinned;

        Row(String item, int iconRes, String label, boolean pinned) {
            this.item = item;
            this.iconRes = iconRes;
            this.label = label;
            this.pinned = pinned;
        }
    }

    private QuickBarEditor() {
    }

    // Pinned items first in their saved order, then every other action and special key. Items that no longer exist are dropped.
    static List<Row> buildRows(Context context, List<String> pinned, List<CustomKey> keys) {
        Map<String, Row> available = new LinkedHashMap<>();
        for (StreamAction action : GameMenuLayout.configurableActions()) {
            available.put(action.id, new Row(action.id, action.iconRes, context.getString(action.nameRes), false));
        }
        for (CustomKey key : keys) {
            String item = QuickBarLayout.customKeyItem(key.id);
            available.put(item, new Row(item, IconCatalog.drawableFor(key.icon),
                    com.limelight.customkeys.KeyLabels.pickerLabel(context, key), false));
        }

        List<Row> rows = new ArrayList<>();
        for (String item : pinned) {
            Row row = available.remove(item);
            if (row != null) {
                row.pinned = true;
                rows.add(row);
            }
        }
        rows.addAll(available.values());
        return rows;
    }

    static List<String> pinnedItems(List<Row> rows) {
        List<String> items = new ArrayList<>();
        for (Row row : rows) {
            if (row.pinned) {
                items.add(row.item);
            }
        }
        return items;
    }

    public static int anchorNameFor(String anchor) {
        int index = BarSets.ANCHORS.indexOf(anchor);
        return ANCHOR_LABELS[index >= 0 ? index : 0];
    }

    // A new bar goes to the first anchor no bar of the set uses yet, so it never lands on top of another bar
    static String freeAnchor(BarSets.Set set) {
        for (String anchor : BarSets.ANCHORS) {
            boolean used = false;
            for (BarSets.Bar bar : set.bars) {
                used |= bar.anchor.equals(anchor);
            }
            if (!used) {
                return anchor;
            }
        }
        return BarSets.TOP_LEFT;
    }

    public static void show(Context context, SharedPreferences prefs, Runnable onChanged) {
        show(context, prefs, onChanged, -1);
    }

    // Opens with this bar of the active set selected, as when the editor starts from a long press on that bar
    public static void show(Context context, SharedPreferences prefs, Runnable onChanged, int startBar) {
        new Session(context, prefs, onChanged, startBar).open();
    }

    // All state of one open editor. Rows per bar hold the item choices until Save writes them back into the bars.
    static final class Session {
        final Context context;
        final SharedPreferences prefs;
        final Runnable onChanged;
        final BarSets.State state;
        // The special keys the rows are built from. A key made or edited here is added or replaced, so switching sets never loses it.
        final List<CustomKey> keys;
        RecyclerView listView;
        final List<List<Row>> rowsByBar = new ArrayList<>();
        int shownSet;
        int selected;

        View body;
        Context dialogContext;
        Adapter adapter;
        boolean binding;

        Session(Context context, SharedPreferences prefs, Runnable onChanged, int startBar) {
            this.context = context;
            this.prefs = prefs;
            this.onChanged = onChanged;
            this.state = BarSets.load(context);
            this.keys = new ArrayList<>(SpecialKeys.visible(context));
            this.shownSet = state.active;
            loadRows();
            List<BarSets.Bar> bars = set().bars;
            selected = startBar >= 0 && startBar < bars.size() ? startBar : bars.isEmpty() ? -1 : 0;
        }

        BarSets.Set set() {
            return state.sets.get(shownSet);
        }

        BarSets.Bar bar() {
            return selected >= 0 ? set().bars.get(selected) : null;
        }

        void loadRows() {
            rowsByBar.clear();
            for (BarSets.Bar bar : set().bars) {
                rowsByBar.add(buildRows(context, bar.items, keys));
            }
        }

        // Writes the item choices of the shown set back into its bars, before switching sets, copying a bar or saving
        void storeRows() {
            List<BarSets.Bar> bars = set().bars;
            for (int i = 0; i < bars.size(); i++) {
                bars.get(i).items.clear();
                bars.get(i).items.addAll(pinnedItems(rowsByBar.get(i)));
            }
        }

        void showSet(int index) {
            storeRows();
            shownSet = index;
            loadRows();
            selected = set().bars.isEmpty() ? -1 : 0;
            refresh();
        }

        void open() {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            dialogContext = builder.getContext();
            body = LayoutInflater.from(dialogContext).inflate(R.layout.dialog_quick_bar_editor, null);
            DisplayMetrics metrics = context.getResources().getDisplayMetrics();
            // A phone in landscape is wide enough for two panes but too short for them, so it keeps the single scrolling page
            boolean twoPane = metrics.widthPixels / metrics.density >= TWO_PANE_MIN_WIDTH_DP
                    && metrics.heightPixels / metrics.density >= TWO_PANE_MIN_HEIGHT_DP;
            arrangePanes(twoPane, metrics);
            // NOTE: A phone in landscape is about 380dp tall, so the 168dp preview filled the whole visible page and hid every control.
            // NOTE: Short screens get a smaller preview and a list of fixed height, so the bar settings start on the first page.
            boolean shortScreen = metrics.heightPixels / metrics.density < TWO_PANE_MIN_HEIGHT_DP;
            if (shortScreen) {
                body.findViewById(R.id.quick_bar_preview).getLayoutParams().height = (int) (112 * metrics.density);
            }

            RecyclerView list = new RecyclerView(dialogContext);
            listView = list;
            list.setLayoutManager(new LinearLayoutManager(dialogContext));
            LinearLayout listColumn = new LinearLayout(dialogContext);
            listColumn.setOrientation(LinearLayout.VERTICAL);
            // Two panes: the list takes whatever height the right pane has left. One page: the list has a bounded height inside the page scroll.
            listColumn.addView(list, twoPane
                    ? new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
                    : new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                            shortScreen ? (int) (220 * metrics.density) : (int) (metrics.heightPixels * 0.4f)));
            ((FrameLayout) body.findViewById(R.id.quick_bar_list)).addView(listColumn,
                    new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                            twoPane ? ViewGroup.LayoutParams.MATCH_PARENT : ViewGroup.LayoutParams.WRAP_CONTENT));

            adapter = new Adapter(this::drawPreview);
            adapter.editor = this::editKey;
            list.setAdapter(adapter);
            final ItemTouchHelper[] touchHelper = new ItemTouchHelper[1];
            adapter.dragStarter = holder -> touchHelper[0].startDrag(holder);
            touchHelper[0] = DragReorder.attach(list, true, null, (from, to) -> {
                DragReorder.moveItem(adapter.rows, from, to);
                adapter.notifyItemMoved(from, to);
                drawPreview();
                return true;
            }, null);

            // A key or flow made here is pinned to the selected bar and offered to the other bars
            ((LinearLayout) body.findViewById(R.id.quick_bar_items_header)).addView(KeyPicker.addButton(dialogContext, context, this::keyCreated),
                    new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            buildAnchorPicker();
            wireSliders();
            body.findViewById(R.id.quick_bar_reset_offset).setOnClickListener(v -> {
                if (bar() != null) {
                    bar().offsetXDp = 0;
                    bar().offsetYDp = 0;
                    refresh();
                }
            });
            body.findViewById(R.id.quick_bar_remove_bar).setOnClickListener(v -> {
                if (selected >= 0) {
                    storeRows();
                    set().bars.remove(selected);
                    rowsByBar.remove(selected);
                    selected = Math.min(selected, set().bars.size() - 1);
                    refresh();
                }
            });
            body.findViewById(R.id.quick_bar_set).setOnClickListener(this::showSetMenu);
            body.findViewById(R.id.quick_bar_set_menu).setOnClickListener(this::showSetMenu);
            // The preview is measured after layout, and bars are drawn to the scale of its width
            body.findViewById(R.id.quick_bar_preview).addOnLayoutChangeListener(
                    (v, l, t, r, b, ol, ot, or, ob) -> {
                        if (r - l != or - ol) {
                            v.post(this::drawPreview);
                        }
                    });
            refresh();

            // With a profile active the bars belong to it, so the title names the profile being edited
            String profile = com.limelight.profiles.ProfilesManager.getInstance().getActiveName();
            builder.setCustomTitle(Dialogs.compactTitle(dialogContext, R.drawable.ic_sa_dashboard_customize, profile.isEmpty()
                    ? context.getString(R.string.title_quick_bar) : context.getString(R.string.title_quick_bar_profile, profile)))
                    .setView(body)
                    .setPositiveButton(R.string.game_menu_editor_save, (dialog, which) -> {
                        storeRows();
                        state.active = shownSet;
                        BarSets.save(context, state);
                        if (onChanged != null) {
                            onChanged.run();
                        }
                    })
                    .setNegativeButton(R.string.game_menu_cancel, null);
            Dialogs.show(builder, twoPane ? Dialogs.WIDTH_WIDE_DP : Dialogs.WIDTH_STANDARD_DP);
        }

        // A key or flow made here is pinned to the selected bar and offered to the other bars
        void keyCreated(CustomKey key) {
            if (selected < 0) {
                addBar();
            }
            // The key joins the editor's own list, because rebuilding the rows after a set switch would otherwise drop it and Save would unpin it
            keys.removeIf(known -> known.id.equals(key.id));
            keys.add(key);
            String item = QuickBarLayout.customKeyItem(key.id);
            for (int i = 0; i < rowsByBar.size(); i++) {
                List<Row> rows = rowsByBar.get(i);
                boolean here = i == selected;
                int at = rows.size();
                if (here) {
                    at = 0;
                    while (at < rows.size() && rows.get(at).pinned) {
                        at++;
                    }
                }
                rows.add(at, new Row(item, IconCatalog.drawableFor(key.icon), com.limelight.customkeys.KeyLabels.pickerLabel(context, key), here));
            }
            refresh();
            scrollToItem(item);
        }

        // A key made on a phone sits below the visible part of the list, so the list scrolls to it
        void scrollToItem(String item) {
            if (listView == null || selected < 0) {
                return;
            }
            List<Row> rows = rowsByBar.get(selected);
            for (int i = 0; i < rows.size(); i++) {
                if (rows.get(i).item.equals(item)) {
                    listView.scrollToPosition(i);
                    return;
                }
            }
        }

        // NOTE: The pencil of a key row edits that key in place, so a forgotten name or icon is fixed without leaving the editor.
        // NOTE: Every row that points at the key takes the new label and icon, in every bar of every shown set.
        void editKey(Row row) {
            KeyEditActions.editItem(context, row.item, () -> {
                keys.clear();
                keys.addAll(SpecialKeys.visible(context));
                for (List<Row> rows : rowsByBar) {
                    for (Row other : rows) {
                        CustomKey changed = findKey(other.item);
                        if (changed != null) {
                            other.iconRes = IconCatalog.drawableFor(changed.icon);
                            other.label = com.limelight.customkeys.KeyLabels.pickerLabel(context, changed);
                        }
                    }
                }
                adapter.notifyDataSetChanged();
                drawPreview();
            });
        }

        CustomKey findKey(String item) {
            String id = QuickBarLayout.customKeyId(item);
            for (CustomKey key : keys) {
                if (key.id.equals(id)) {
                    return key;
                }
            }
            return null;
        }

        void addBar() {
            storeRows();
            BarSets.Bar bar = new BarSets.Bar(freeAnchor(set()), 4, false, 0, 0, new ArrayList<>());
            set().bars.add(bar);
            rowsByBar.add(buildRows(context, bar.items, keys));
            selected = set().bars.size() - 1;
        }

        // NOTE: Wide screens get two panes of one fixed height instead of one page that scrolls as a whole.
        // NOTE: The left pane (preview, bars, position, grid and offset) scrolls on its own, and the right pane is the header, the item list filling the rest and Add key or flow at the bottom.
        // NOTE: One page made the list a fixed half of the screen, so on a tablet it ended halfway down next to a much taller left column.
        void arrangePanes(boolean twoPane, DisplayMetrics metrics) {
            if (!twoPane) {
                return;
            }
            LinearLayout panes = body.findViewById(R.id.quick_bar_panes);
            View controls = body.findViewById(R.id.quick_bar_controls);
            LinearLayout items = body.findViewById(R.id.quick_bar_items_pane);
            panes.removeAllViews();

            int side = (int) Dialogs.dp(context, 24);
            // Leaves room for the dialog title, the buttons and the window insets
            int height = Math.max((int) Dialogs.dp(context, 360), metrics.heightPixels - (int) Dialogs.dp(context, 210));
            LinearLayout row = new LinearLayout(dialogContext);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(side, 0, side, 0);
            row.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height));

            androidx.core.widget.NestedScrollView left = new androidx.core.widget.NestedScrollView(dialogContext);
            left.setVerticalScrollBarEnabled(true);
            left.addView(controls, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            LinearLayout.LayoutParams leftParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
            leftParams.rightMargin = side;
            row.addView(left, leftParams);

            items.findViewById(R.id.quick_bar_list).setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
            row.addView(items, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));
            body = row;
        }

        // Nine segments in three rows, laid out like the screen, so picking an anchor reads as picking a spot
        void buildAnchorPicker() {
            LinearLayout picker = body.findViewById(R.id.quick_bar_anchor);
            int gap = (int) Dialogs.dp(context, 6);
            int sidePad = (int) Dialogs.dp(context, 4);
            List<TextView> cells = new ArrayList<>();
            for (int r = 0; r < 3; r++) {
                LinearLayout row = new LinearLayout(dialogContext);
                row.setOrientation(LinearLayout.HORIZONTAL);
                for (int c = 0; c < 3; c++) {
                    String anchor = BarSets.ANCHORS.get(r * 3 + c);
                    TextView cell = new TextView(dialogContext, null, 0, R.style.SuperArtemis_Segment);
                    cell.setText(ANCHOR_LABELS[r * 3 + c]);
                    cell.setTag(anchor);
                    cell.setMaxLines(1);
                    // Narrow side padding lets "Bottom right" fit a phone-width cell at full size
                    cell.setPadding(sidePad, cell.getPaddingTop(), sidePad, cell.getPaddingBottom());
                    TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(cell, 9, 14, 1, android.util.TypedValue.COMPLEX_UNIT_SP);
                    cells.add(cell);
                    cell.setOnClickListener(v -> {
                        if (bar() != null) {
                            bar().anchor = anchor;
                            refresh();
                        }
                    });
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, (int) Dialogs.dp(context, 40), 1f);
                    if (c > 0) {
                        params.leftMargin = gap;
                    }
                    row.addView(cell, params);
                }
                LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                if (r > 0) {
                    rowParams.topMargin = gap;
                }
                picker.addView(row, rowParams);
            }
            // NOTE: Each label shrinks on its own to fit its cell, which left "Bottom right" smaller than its neighbours.
            // NOTE: After the first layout every label takes the smallest size, so the nine segments read as one set.
            picker.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
                @Override
                public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                    picker.removeOnLayoutChangeListener(this);
                    float smallest = Float.MAX_VALUE;
                    for (TextView cell : cells) {
                        smallest = Math.min(smallest, cell.getTextSize());
                    }
                    float size = smallest;
                    picker.post(() -> {
                        for (TextView cell : cells) {
                            TextViewCompat.setAutoSizeTextTypeWithDefaults(cell, TextViewCompat.AUTO_SIZE_TEXT_TYPE_NONE);
                            cell.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, size);
                        }
                    });
                }
            });
        }

        void wireSliders() {
            ColorStateList tint = ColorStateList.valueOf(Dialogs.color(dialogContext, R.attr.saPrimary));
            // Fill across or down works as one segmented button next to the "per line" slider
            for (int fillId : new int[] {R.id.quick_bar_fill_across, R.id.quick_bar_fill_down}) {
                body.findViewById(fillId).setOnClickListener(v -> {
                    BarSets.Bar bar = bar();
                    if (bar != null) {
                        bar.down = fillId == R.id.quick_bar_fill_down;
                        refresh();
                    }
                });
            }
            int[] ids = {R.id.quick_bar_rows, R.id.quick_bar_offset_x, R.id.quick_bar_offset_y};
            for (int id : ids) {
                SeekBar slider = body.findViewById(id);
                slider.setProgressTintList(tint);
                slider.setThumbTintList(tint);
                slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        BarSets.Bar bar = bar();
                        if (binding || bar == null) {
                            return;
                        }
                        if (id == R.id.quick_bar_rows) {
                            bar.perLine = progress + 1;
                        }
                        else if (id == R.id.quick_bar_offset_x) {
                            bar.offsetXDp = (progress - 100) * OFFSET_STEP_DP;
                        }
                        else {
                            bar.offsetYDp = (progress - 100) * OFFSET_STEP_DP;
                        }
                        bindLabels();
                        drawPreview();
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                    }
                });
            }
        }

        // The stream runs in landscape, so what fits is worked out for the long side as width, however the editor is held
        float[] screenDp() {
            DisplayMetrics metrics = context.getResources().getDisplayMetrics();
            float width = metrics.widthPixels / metrics.density;
            float height = metrics.heightPixels / metrics.density;
            return new float[] {Math.max(width, height), Math.min(width, height)};
        }

        int fitFor(BarSets.Bar bar) {
            float[] screen = screenDp();
            return BarFit.maxPerLine(BarFit.spanDp(bar.down, screen[0], screen[1], 0));
        }

        // One tap choices next to the slider: a few common line lengths and the longest line that fits this screen
        void buildFitPresets() {
            FlowRow presets = body.findViewById(R.id.quick_bar_fit_presets);
            presets.removeAllViews();
            BarSets.Bar bar = bar();
            if (bar == null) {
                return;
            }
            int fit = fitFor(bar);
            List<Integer> values = new ArrayList<>();
            for (int value : new int[] {4, 6, 10}) {
                if (value < fit) {
                    values.add(value);
                }
            }
            for (int value : values) {
                TextView chip = (TextView) LayoutInflater.from(dialogContext).inflate(R.layout.sa_action_chip, presets, false);
                chip.setText(context.getString(R.string.quick_bar_fit_preset, value));
                chip.setOnClickListener(v -> setPerLine(value));
                presets.addView(chip);
            }
            TextView best = (TextView) LayoutInflater.from(dialogContext).inflate(R.layout.sa_action_chip, presets, false);
            best.setText(R.string.quick_bar_fit_best);
            best.setOnClickListener(v -> setPerLine(fit));
            presets.addView(best);
        }

        void setPerLine(int value) {
            if (bar() != null) {
                bar().perLine = Math.max(1, Math.min(BarSets.GRID_MAX, value));
                refresh();
            }
        }

        void bindLabels() {
            BarSets.Bar bar = bar();
            if (bar == null) {
                return;
            }
            int fit = fitFor(bar);
            ((TextView) body.findViewById(R.id.quick_bar_fit_note)).setText(bar.perLine > fit
                    ? context.getString(R.string.quick_bar_fit_over, fit, bar.perLine)
                    : context.getString(R.string.quick_bar_fit_ok, fit));
            ((TextView) body.findViewById(R.id.quick_bar_rows_label)).setText(context.getString(R.string.quick_bar_per_line, bar.perLine));
            ((TextView) body.findViewById(R.id.quick_bar_offset_x_label)).setText(context.getString(R.string.quick_bar_offset_x, bar.offsetXDp));
            ((TextView) body.findViewById(R.id.quick_bar_offset_y_label)).setText(context.getString(R.string.quick_bar_offset_y, bar.offsetYDp));
        }

        void refresh() {
            ((TextView) body.findViewById(R.id.quick_bar_set)).setText(set().name);
            BarSets.Bar bar = bar();
            body.findViewById(R.id.quick_bar_bar_controls).setVisibility(bar != null ? View.VISIBLE : View.GONE);
            body.findViewById(R.id.quick_bar_items_pane).setVisibility(bar != null ? View.VISIBLE : View.GONE);
            buildBarChips();
            buildFitPresets();
            if (bar != null) {
                LinearLayout picker = body.findViewById(R.id.quick_bar_anchor);
                for (int r = 0; r < picker.getChildCount(); r++) {
                    LinearLayout row = (LinearLayout) picker.getChildAt(r);
                    for (int c = 0; c < row.getChildCount(); c++) {
                        View cell = row.getChildAt(c);
                        cell.setSelected(bar.anchor.equals(cell.getTag()));
                    }
                }
                binding = true;
                ((SeekBar) body.findViewById(R.id.quick_bar_rows)).setProgress(bar.perLine - 1);
                body.findViewById(R.id.quick_bar_fill_across).setSelected(!bar.down);
                body.findViewById(R.id.quick_bar_fill_down).setSelected(bar.down);
                ((SeekBar) body.findViewById(R.id.quick_bar_offset_x)).setProgress(bar.offsetXDp / OFFSET_STEP_DP + 100);
                ((SeekBar) body.findViewById(R.id.quick_bar_offset_y)).setProgress(bar.offsetYDp / OFFSET_STEP_DP + 100);
                binding = false;
                bindLabels();
                ((TextView) body.findViewById(R.id.quick_bar_items_label)).setText(
                        context.getString(R.string.quick_bar_bar_items, context.getString(anchorNameFor(bar.anchor))));
                adapter.setRows(rowsByBar.get(selected));
            }
            drawPreview();
        }

        // One chip per bar of the shown set, named by its anchor, then "Add bar"
        void buildBarChips() {
            FlowRow chips = body.findViewById(R.id.quick_bar_bars);
            chips.removeAllViews();
            List<BarSets.Bar> bars = set().bars;
            for (int i = 0; i < bars.size(); i++) {
                int index = i;
                TextView chip = (TextView) LayoutInflater.from(dialogContext).inflate(R.layout.sa_action_chip, chips, false);
                chip.setText(context.getString(anchorNameFor(bars.get(i).anchor)));
                chip.setSelected(i == selected);
                if (i == selected) {
                    android.graphics.drawable.Drawable check = dialogContext.getDrawable(R.drawable.ic_sa_check);
                    if (check != null) {
                        int size = (int) Dialogs.dp(context, 20);
                        check.setBounds(0, 0, size, size);
                        check.setTint(Dialogs.color(dialogContext, R.attr.saPrimary));
                        chip.setCompoundDrawablesRelative(check, null, null, null);
                    }
                    chip.setTextColor(Dialogs.color(dialogContext, R.attr.saPrimary));
                }
                chip.setOnClickListener(v -> {
                    selected = index;
                    refresh();
                });
                chips.addView(chip);
            }
            TextView add = (TextView) LayoutInflater.from(dialogContext).inflate(R.layout.sa_action_chip, chips, false);
            add.setText(R.string.quick_bar_add_bar);
            android.graphics.drawable.Drawable icon = dialogContext.getDrawable(R.drawable.ic_sa_add);
            if (icon != null) {
                int size = (int) Dialogs.dp(context, 20);
                icon.setBounds(0, 0, size, size);
                icon.setTint(add.getCurrentTextColor());
                add.setCompoundDrawablesRelative(icon, null, null, null);
            }
            add.setOnClickListener(v -> {
                addBar();
                refresh();
            });
            chips.addView(add);
        }

        // NOTE: Draws every bar of the shown set at its anchor and offset, scaled from the real screen to the preview width.
        // NOTE: Buttons use the same grid as the stream (BarSets.grid), so rows, columns and the fill direction look exactly as they will.
        void drawPreview() {
            FrameLayout preview = body.findViewById(R.id.quick_bar_preview);
            preview.removeAllViews();
            int width = preview.getWidth() - preview.getPaddingLeft() - preview.getPaddingRight();
            if (width <= 0) {
                return;
            }
            DisplayMetrics metrics = context.getResources().getDisplayMetrics();
            // Streams run in landscape, so the long side of the screen maps to the preview width
            float scale = width / (float) Math.max(metrics.widthPixels, metrics.heightPixels);
            float density = metrics.density;
            // True scale makes icons a few pixels wide on a tablet preview, so they never shrink below a readable size
            int button = Math.max((int) Dialogs.dp(context, 12), Math.round(BarFit.BUTTON_DP * density * scale));
            int gap = Math.max(1, Math.round(BarFit.GAP_DP * density * scale));
            List<BarSets.Bar> bars = set().bars;
            float[] screen = screenDp();
            for (int i = 0; i < bars.size(); i++) {
                // The preview shows the bar as the stream draws it on this screen, with a line that is too long wrapped where it fits
                BarSets.Bar bar = BarFit.fitted(bars.get(i), screen[0], screen[1], 0);
                List<String> items = pinnedItems(rowsByBar.get(i));
                LinearLayout grid = new LinearLayout(dialogContext);
                grid.setOrientation(LinearLayout.VERTICAL);
                grid.setGravity(bar.isRight() ? Gravity.END : bar.isLeft() ? Gravity.START : Gravity.CENTER_HORIZONTAL);
                int pad = (int) Dialogs.dp(context, 3);
                grid.setPadding(pad, pad, pad, pad);
                grid.setBackgroundResource(R.drawable.bg_corner_slot);
                grid.setSelected(i == selected);
                grid.setMinimumWidth((int) Dialogs.dp(context, 24));
                grid.setMinimumHeight((int) Dialogs.dp(context, 16));
                List<List<String>> cells = BarSets.grid(bar, items);
                Map<String, Integer> icons = new LinkedHashMap<>();
                for (Row row : rowsByBar.get(i)) {
                    icons.put(row.item, row.iconRes);
                }
                for (int r = 0; r < cells.size(); r++) {
                    LinearLayout line = new LinearLayout(dialogContext);
                    line.setOrientation(LinearLayout.HORIZONTAL);
                    for (int c = 0; c < cells.get(r).size(); c++) {
                        String item = cells.get(r).get(c);
                        ImageView cell = new ImageView(dialogContext);
                        if (item != null) {
                            cell.setImageResource(icons.containsKey(item) ? icons.get(item) : R.drawable.ic_sa_keyboard);
                            cell.setBackgroundResource(R.drawable.bg_mini_button);
                            int cellPad = Math.max(1, button / 6);
                            cell.setPadding(cellPad, cellPad, cellPad, cellPad);
                        }
                        cell.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(button, button);
                        if (c > 0) {
                            params.leftMargin = gap;
                        }
                        line.addView(cell, params);
                    }
                    LinearLayout.LayoutParams lineParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    if (r > 0) {
                        lineParams.topMargin = gap;
                    }
                    grid.addView(line, lineParams);
                }
                int index = i;
                grid.setOnClickListener(v -> {
                    selected = index;
                    refresh();
                });
                grid.setContentDescription(context.getString(R.string.quick_bar_bar_items, context.getString(anchorNameFor(bar.anchor))) + ", " + items.size());
                FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                int vertical = bar.isTop() ? Gravity.TOP : bar.isBottom() ? Gravity.BOTTOM : Gravity.CENTER_VERTICAL;
                int horizontal = bar.isLeft() ? Gravity.START : bar.isRight() ? Gravity.END : Gravity.CENTER_HORIZONTAL;
                params.gravity = vertical | horizontal;
                preview.addView(grid, params);
                QuickActionBar.placeInside(grid, bar.offsetXDp * density * scale, bar.offsetYDp * density * scale);
            }
            if (bars.isEmpty()) {
                TextView empty = new TextView(dialogContext);
                empty.setText(R.string.quick_bar_no_bars);
                empty.setGravity(Gravity.CENTER);
                TextViewCompat.setTextAppearance(empty, R.style.TextAppearance_SuperArtemis_BodySmall);
                preview.addView(empty, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
            }
        }

        void showSetMenu(View anchor) {
            PopupMenu popup = new PopupMenu(dialogContext, anchor);
            for (int i = 0; i < state.sets.size(); i++) {
                String name = state.sets.get(i).name;
                popup.getMenu().add(0, 1000 + i, i, i == shownSet ? context.getString(R.string.quick_bar_set_active, name) : name);
            }
            popup.getMenu().add(1, 1, 100, R.string.quick_bar_set_new);
            popup.getMenu().add(1, 2, 101, R.string.quick_bar_set_rename);
            popup.getMenu().add(1, 3, 102, R.string.quick_bar_set_duplicate);
            if (bar() != null && state.sets.size() > 1) {
                popup.getMenu().add(1, 4, 103, R.string.quick_bar_copy_bar);
            }
            if (state.sets.size() > 1) {
                popup.getMenu().add(1, 5, 104, R.string.quick_bar_set_delete);
            }
            popup.setOnMenuItemClickListener(item -> {
                int id = item.getItemId();
                if (id >= 1000) {
                    showSet(id - 1000);
                }
                else if (id == 1) {
                    askName(R.string.quick_bar_set_new, context.getString(R.string.quick_bar_set_default_name, state.sets.size() + 1), name -> {
                        state.sets.add(new BarSets.Set(name, new ArrayList<>()));
                        showSet(state.sets.size() - 1);
                    });
                }
                else if (id == 2) {
                    askName(R.string.quick_bar_set_rename, set().name, name -> {
                        set().name = name;
                        refresh();
                    });
                }
                else if (id == 3) {
                    storeRows();
                    state.sets.add(set().copy(context.getString(R.string.quick_bar_set_copy_name, set().name)));
                    showSet(state.sets.size() - 1);
                }
                else if (id == 4) {
                    copyBarToSet();
                }
                else if (id == 5) {
                    state.sets.remove(shownSet);
                    if (state.active >= state.sets.size() || state.active == shownSet) {
                        state.active = 0;
                    }
                    else if (state.active > shownSet) {
                        state.active--;
                    }
                    shownSet = Math.min(shownSet, state.sets.size() - 1);
                    loadRows();
                    selected = set().bars.isEmpty() ? -1 : 0;
                    refresh();
                }
                return true;
            });
            popup.show();
        }

        void copyBarToSet() {
            storeRows();
            PopupMenu popup = new PopupMenu(dialogContext, body.findViewById(R.id.quick_bar_set_menu));
            for (int i = 0; i < state.sets.size(); i++) {
                if (i != shownSet) {
                    popup.getMenu().add(0, i, i, state.sets.get(i).name);
                }
            }
            popup.setOnMenuItemClickListener(item -> {
                state.sets.get(item.getItemId()).bars.add(bar().copy());
                android.widget.Toast.makeText(context, context.getString(R.string.quick_bar_bar_copied, item.getTitle()), android.widget.Toast.LENGTH_SHORT).show();
                return true;
            });
            popup.show();
        }

        private interface NameResult {
            void accept(String name);
        }

        void askName(int titleRes, String initial, NameResult result) {
            AlertDialog.Builder builder = new AlertDialog.Builder(dialogContext);
            Context nameContext = builder.getContext();
            EditText field = new EditText(nameContext, null, 0, R.style.Widget_SuperArtemis_TextField);
            field.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
            field.setSingleLine(true);
            field.setText(initial);
            field.setSelectAllOnFocus(true);
            LinearLayout box = new LinearLayout(nameContext);
            int side = (int) Dialogs.dp(nameContext, 24);
            box.setPadding(side, 0, side, 0);
            box.addView(field, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            builder.setTitle(titleRes)
                    .setView(box)
                    .setPositiveButton(R.string.game_menu_editor_save, (d, w) -> {
                        String name = field.getText().toString().trim();
                        if (!name.isEmpty()) {
                            result.accept(name);
                        }
                    })
                    .setNegativeButton(R.string.game_menu_cancel, null);
            Dialogs.show(builder, Dialogs.WIDTH_STANDARD_DP);
            Dialogs.openKeyboard(field);
        }
    }

    private interface DragStarter {
        void startDrag(RecyclerView.ViewHolder holder);
    }

    private interface RowEditor {
        void edit(Row row);
    }

    private static final class Adapter extends RecyclerView.Adapter<Adapter.Holder> {
        private final Runnable onChanged;
        List<Row> rows = new ArrayList<>();
        DragStarter dragStarter;
        RowEditor editor;

        Adapter(Runnable onChanged) {
            this.onChanged = onChanged;
        }

        @SuppressLint("NotifyDataSetChanged")
        void setRows(List<Row> rows) {
            this.rows = rows;
            notifyDataSetChanged();
        }

        static final class Holder extends RecyclerView.ViewHolder {
            final ImageView handle;
            final ImageView icon;
            final TextView label;
            final CompoundButton check;
            final ImageView edit;

            Holder(View itemView) {
                super(itemView);
                edit = itemView.findViewById(R.id.game_menu_editor_edit);
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
            // Only keys made or built in can be edited, an action row has no pencil
            boolean editable = editor != null && QuickBarLayout.customKeyId(row.item) != null;
            holder.edit.setVisibility(editable ? View.VISIBLE : View.GONE);
            holder.edit.setOnClickListener(editable ? v -> editor.edit(row) : null);
            holder.check.setOnCheckedChangeListener(null);
            holder.check.setChecked(row.pinned);
            holder.check.setOnCheckedChangeListener((button, checked) -> {
                row.pinned = checked;
                onChanged.run();
            });
            holder.itemView.setOnClickListener(v -> holder.check.toggle());
            holder.handle.setOnTouchListener((v, event) -> {
                if (event.getActionMasked() == MotionEvent.ACTION_DOWN && dragStarter != null) {
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
