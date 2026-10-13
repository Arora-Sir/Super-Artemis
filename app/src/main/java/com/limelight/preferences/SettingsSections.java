package com.limelight.preferences;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceGroupAdapter;
import androidx.preference.PreferenceScreen;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.limelight.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// NOTE: On wide screens a list of the settings sections sits left of the settings, and tapping a section jumps straight to it.
// NOTE: The list follows scrolling, so the section at the top of the settings stays highlighted. Phones show only the settings.
final class SettingsSections {
    // Material 3 "expanded" window width, where a side list leaves enough room for the settings themselves
    static final int MIN_WIDTH_DP = 840;

    private static final Map<String, Integer> ICONS = new HashMap<>();

    static {
        ICONS.put("category_video_settings", R.drawable.ic_sa_videocam);
        ICONS.put("category_audio_settings", R.drawable.ic_sa_volume_up);
        ICONS.put("category_stream_controls", R.drawable.ic_sa_dashboard_customize);
        ICONS.put("category_session", R.drawable.ic_sa_exit_to_app);
        ICONS.put("category_ui_settings", R.drawable.ic_sa_palette);
        ICONS.put("category_input_settings", R.drawable.ic_sa_mouse);
        ICONS.put("category_virtual_trackpad_settings", R.drawable.ic_sa_touch_app);
        ICONS.put("category_gamepad_settings", R.drawable.ic_sa_sports_esports);
        ICONS.put("category_onscreen_controls", R.drawable.ic_sa_gamepad);
        ICONS.put("category_full_keyboard", R.drawable.ic_sa_keyboard);
        ICONS.put("category_special_key_layout", R.drawable.ic_sa_keyboard_command_key);
        ICONS.put("category_clipboard", R.drawable.ic_sa_content_paste);
        ICONS.put("category_host_settings", R.drawable.ic_sa_computer);
        ICONS.put("category_perf_monitor_settings", R.drawable.ic_sa_speed);
        ICONS.put("category_backup_restore", R.drawable.ic_sa_settings_backup_restore);
        ICONS.put("category_advanced_settings", R.drawable.ic_sa_tune);
        ICONS.put("category_settings_misc", R.drawable.ic_sa_more_horiz);
        ICONS.put("category_about", R.drawable.ic_sa_info);
    }

    private final Activity activity;
    private final RecyclerView rail;
    private final boolean visible;
    private final List<PreferenceCategory> sections = new ArrayList<>();
    private RecyclerView list;
    private int selected = -1;

    SettingsSections(Activity activity, RecyclerView rail) {
        this.activity = activity;
        this.rail = rail;
        DisplayMetrics metrics = activity.getResources().getDisplayMetrics();
        visible = metrics.widthPixels / metrics.density >= MIN_WIDTH_DP;
        rail.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    // Called for every new settings list, since search results rebuild the settings fragment
    void attach(PreferenceFragmentCompat fragment) {
        if (!visible) {
            return;
        }
        sections.clear();
        PreferenceScreen screen = fragment.getPreferenceScreen();
        for (int i = 0; i < screen.getPreferenceCount(); i++) {
            Preference preference = screen.getPreference(i);
            if (preference instanceof PreferenceCategory && preference.isVisible() && preference.getKey() != null) {
                sections.add((PreferenceCategory) preference);
            }
        }
        list = fragment.getListView();
        rail.setLayoutManager(new LinearLayoutManager(activity));
        rail.setAdapter(new Adapter());
        list.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                followScroll();
            }
        });
        list.post(this::followScroll);
    }

    private void jumpTo(int index) {
        RecyclerView.Adapter<?> adapter = list.getAdapter();
        if (adapter instanceof PreferenceGroup.PreferencePositionCallback && list.getLayoutManager() instanceof LinearLayoutManager) {
            int position = ((PreferenceGroup.PreferencePositionCallback) adapter).getPreferenceAdapterPosition(sections.get(index));
            if (position >= 0) {
                ((LinearLayoutManager) list.getLayoutManager()).scrollToPositionWithOffset(position, 0);
            }
        }
        select(index);
    }

    // Highlights the section owning the first visible row. Near the end of the list, a tapped section that cannot reach the top keeps its highlight.
    @SuppressLint("RestrictedApi")
    private void followScroll() {
        if (!(list.getLayoutManager() instanceof LinearLayoutManager) || !(list.getAdapter() instanceof PreferenceGroupAdapter)) {
            return;
        }
        int first = ((LinearLayoutManager) list.getLayoutManager()).findFirstVisibleItemPosition();
        PreferenceGroupAdapter adapter = (PreferenceGroupAdapter) list.getAdapter();
        for (int position = first; position >= 0 && position < adapter.getItemCount(); position--) {
            int index = sections.indexOf(topLevelSection(adapter.getItem(position)));
            if (index >= 0) {
                if (!list.canScrollVertically(1) && selected > index) {
                    return;
                }
                select(index);
                return;
            }
        }
    }

    // The "show more" row of a collapsed section has no parent, so the search moves on to the row above it
    private static PreferenceCategory topLevelSection(Preference preference) {
        Preference current = preference;
        while (current != null) {
            PreferenceGroup parent = current.getParent();
            if (parent instanceof PreferenceScreen && current instanceof PreferenceCategory) {
                return (PreferenceCategory) current;
            }
            current = parent;
        }
        return null;
    }

    private void select(int index) {
        if (index == selected) {
            return;
        }
        int previous = selected;
        selected = index;
        RecyclerView.Adapter<?> adapter = rail.getAdapter();
        if (adapter != null) {
            if (previous >= 0) {
                adapter.notifyItemChanged(previous);
            }
            adapter.notifyItemChanged(index);
        }
        rail.scrollToPosition(index);
    }

    private final class Adapter extends RecyclerView.Adapter<Adapter.Holder> {
        final class Holder extends RecyclerView.ViewHolder {
            final ImageView icon;
            final TextView title;

            Holder(View itemView) {
                super(itemView);
                icon = itemView.findViewById(R.id.settings_section_icon);
                title = itemView.findViewById(R.id.settings_section_title);
            }
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.settings_section_item, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            PreferenceCategory section = sections.get(position);
            Integer icon = ICONS.get(section.getKey());
            holder.icon.setImageResource(icon != null ? icon : R.drawable.ic_sa_tune);
            holder.title.setText(section.getTitle());
            boolean isSelected = position == selected;
            holder.itemView.setSelected(isSelected);
            int color = SettingsListStyle.resolveColor(activity, isSelected ? R.attr.saOnSecondaryContainer : R.attr.saOnSurfaceVariant);
            holder.icon.setImageTintList(android.content.res.ColorStateList.valueOf(color));
            holder.title.setTextColor(color);
            holder.itemView.setOnClickListener(v -> {
                int index = holder.getBindingAdapterPosition();
                if (index != RecyclerView.NO_POSITION) {
                    jumpTo(index);
                }
            });
        }

        @Override
        public int getItemCount() {
            return sections.size();
        }
    }
}
