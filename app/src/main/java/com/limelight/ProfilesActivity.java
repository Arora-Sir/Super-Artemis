package com.limelight;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.widget.ImageButton;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.limelight.profiles.ProfilesAdapter;
import com.limelight.profiles.ProfilesManager;
import com.limelight.theme.ThemeManager;
import com.limelight.ui.DragReorder;
import com.limelight.utils.UiHelper;

public class ProfilesActivity extends AppCompatActivity implements ProfilesManager.ProfileChangeListener {
    private String appliedStyle;
    private ProfilesAdapter adapter;
    private RecyclerView recyclerView;
    private View emptyState;

    @Override
    protected void onResume() {
        super.onResume();
        // The app style changed in settings while this screen was in the background
        if (ThemeManager.needsRecreate(this, appliedStyle)) {
            recreate();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Classic or Material You, applied before any view is inflated
        appliedStyle = ThemeManager.apply(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profiles);

        // Setup RecyclerView
        recyclerView = findViewById(R.id.profilesRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new ProfilesAdapter(this);
        recyclerView.setAdapter(adapter);

        // Drag by the handle or a long press moves a profile. The order is saved once per drag, not on every step.
        ItemTouchHelper touchHelper = DragReorder.attach(recyclerView, true, null, (from, to) -> {
            if (!ProfilesManager.getInstance().move(from, to)) {
                return false;
            }
            adapter.notifyItemMoved(from, to);
            return true;
        }, () -> ProfilesManager.getInstance().save(this));
        adapter.setDragStarter(touchHelper::startDrag);

        // Setup empty state
        emptyState = findViewById(R.id.emptyState);

        findViewById(R.id.profilesBack).setOnClickListener(v -> finish());

        // Setup FloatingActionButton
        FloatingActionButton fab = findViewById(R.id.addProfileFab);
        fab.setOnClickListener(v -> {
            Intent intent = new Intent(this, EditProfileActivity.class);
            startActivity(intent);
        });

        // Register for profile changes
        ProfilesManager.getInstance().addListener(this);

        updateUI();

        UiHelper.notifyNewRootView(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ProfilesManager.getInstance().removeListener(this);
    }

    @Override
    public void onProfilesChanged() {
        runOnUiThread(this::updateUI);
    }

    private void updateUI() {
        int profileCount = ProfilesManager.getInstance().getProfiles().size();
        if (profileCount == 0) {
            recyclerView.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyState.setVisibility(View.GONE);
        }
        adapter.notifyDataSetChanged();
    }
}