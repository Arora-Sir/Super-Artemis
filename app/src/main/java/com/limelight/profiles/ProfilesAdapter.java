package com.limelight.profiles;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.limelight.EditProfileActivity;
import com.limelight.R;

import java.util.List;
import java.util.UUID;

public class ProfilesAdapter extends RecyclerView.Adapter<ProfilesAdapter.ProfileViewHolder> {
    private final Context context;
    private final ProfilesManager profilesManager;

    // Starts a drag of the row whose handle was touched, set by the screen that attaches DragReorder
    public interface DragStarter {
        void startDrag(RecyclerView.ViewHolder holder);
    }

    DragStarter dragStarter;

    public void setDragStarter(DragStarter dragStarter) {
        this.dragStarter = dragStarter;
    }

    public ProfilesAdapter(Context context) {
        this.context = context;
        this.profilesManager = ProfilesManager.getInstance();
    }

    @NonNull
    @Override
    public ProfileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.row_profile, parent, false);
        return new ProfileViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProfileViewHolder holder, int position) {
        List<SettingsProfile> profiles = profilesManager.getProfiles();
        SettingsProfile profile = profiles.get(position);
        SettingsProfile activeProfile = profilesManager.getActive();

        // NOTE: The profile that new profiles start from carries a star after its name. A text note was cut off by the narrow name column in portrait.
        holder.profileName.setText(profile.getName());
        boolean isTemplate = profilesManager.isDefaultTemplate(profile.getUuid());
        android.graphics.drawable.Drawable star = isTemplate ? context.getDrawable(R.drawable.ic_sa_star) : null;
        if (star != null) {
            int size = (int) (18 * context.getResources().getDisplayMetrics().density);
            star = star.mutate();
            star.setBounds(0, 0, size, size);
            star.setTint(com.limelight.ui.Dialogs.color(holder.itemView.getContext(), R.attr.saPrimary));
        }
        holder.profileName.setCompoundDrawablesRelative(null, null, star, null);
        holder.profileName.setCompoundDrawablePadding((int) (6 * context.getResources().getDisplayMetrics().density));
        holder.profileName.setContentDescription(isTemplate ? context.getString(R.string.profile_manager_template_name, profile.getName()) : profile.getName());
        holder.profileTimestamp.setText(DateUtils.getRelativeTimeSpanString(
            profile.getModifiedUtc(), System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS));

        boolean isActive = activeProfile != null && activeProfile.getUuid().equals(profile.getUuid());
        holder.profileActive.setChecked(isActive);

        // Set click listeners
        holder.profileActive.setOnClickListener(v -> {
            if (isActive) {
                profilesManager.setActive(null);
                Toast.makeText(context, R.string.profile_manager_deactivated_profile, Toast.LENGTH_SHORT).show();
            } else {
                profilesManager.setActive(profile.getUuid());
                Toast.makeText(context, context.getString(R.string.profile_manager_activated_profile, profile.getName()), Toast.LENGTH_SHORT).show();
            }
            profilesManager.save(context);
        });

        holder.editProfile.setOnClickListener(v -> {
            Intent intent = new Intent(context, EditProfileActivity.class);
            intent.putExtra("profileUuid", profile.getUuid().toString());
            context.startActivity(intent);
        });

        holder.deleteProfile.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setCustomTitle(com.limelight.ui.Dialogs.header(builder.getContext(), R.drawable.ic_sa_delete,
                    context.getString(R.string.profile_manager_delete_profile),
                    context.getString(R.string.profile_manager_confirm_profile_deleteion, profile.getName())))
                .setPositiveButton(R.string.profile_manager_delete, (dialog, which) -> {
                    profilesManager.delete(profile.getUuid());
                    profilesManager.save(context);
                    Toast.makeText(context, context.getString(R.string.profile_manager_profile_deleted, profile.getName()), Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(context.getString(R.string.cancel), null)
                .show();
        });

        holder.copyProfile.setOnClickListener(v -> showCopyMenu(v, profile));

        // Also make the whole row clickable to edit
        holder.itemView.setOnClickListener(v -> holder.editProfile.performClick());

        // Touching the handle starts the drag at once, a long press anywhere on the row does the same
        holder.dragHandle.setOnTouchListener((v, event) -> {
            if (event.getActionMasked() == android.view.MotionEvent.ACTION_DOWN && dragStarter != null) {
                dragStarter.startDrag(holder);
            }
            return false;
        });
    }

    // NOTE: The Copy profile menu is the app's own choice list, not a system popup, so it has its own surface and stays readable in Pure black, where a popup merged with the black screen behind it.
    // NOTE: Duplicate makes a new profile, the template item marks the profile that new profiles start from, and Copy settings into overwrites another profile after a confirmation.
    private void showCopyMenu(View anchor, SettingsProfile profile) {
        com.limelight.ui.ChoiceList choices = new com.limelight.ui.ChoiceList(new AlertDialog.Builder(context), R.drawable.ic_sa_content_copy,
                profile.getName(), R.string.cancel);
        choices.add(R.drawable.ic_sa_content_copy, context.getString(R.string.profile_manager_duplicate), false, () -> {
            SettingsProfile copy = profilesManager.duplicate(profile.getUuid(),
                    context.getString(R.string.profile_manager_copy_name, profile.getName()));
            if (copy != null) {
                profilesManager.save(context);
                Toast.makeText(context, context.getString(R.string.profile_manager_duplicated, copy.getName()), Toast.LENGTH_SHORT).show();
            }
        });
        boolean isTemplate = profilesManager.isDefaultTemplate(profile.getUuid());
        choices.add(R.drawable.ic_sa_star, context.getString(isTemplate ? R.string.profile_manager_template_clear : R.string.profile_manager_template_set), isTemplate, () -> {
            profilesManager.setDefaultTemplate(isTemplate ? null : profile.getUuid());
            profilesManager.save(context);
            Toast.makeText(context, isTemplate ? context.getString(R.string.profile_manager_template_cleared)
                    : context.getString(R.string.profile_manager_template_is_set, profile.getName()), Toast.LENGTH_SHORT).show();
        });
        List<SettingsProfile> others = new java.util.ArrayList<>();
        for (SettingsProfile other : profilesManager.getProfiles()) {
            if (!other.getUuid().equals(profile.getUuid())) {
                others.add(other);
            }
        }
        if (!others.isEmpty()) {
            choices.add(R.drawable.ic_sa_content_paste, context.getString(R.string.profile_manager_copy_into), false, () -> showCopyIntoPicker(profile, others));
        }
        choices.show();
    }

    // Same picker look as every other Super Artemis choice list: header with icon, one row per profile
    private void showCopyIntoPicker(SettingsProfile profile, List<SettingsProfile> others) {
        com.limelight.ui.ChoiceList choices = new com.limelight.ui.ChoiceList(new AlertDialog.Builder(context), R.drawable.ic_sa_content_copy,
                context.getString(R.string.profile_manager_copy_into_title, profile.getName()), R.string.cancel);
        for (SettingsProfile other : others) {
            choices.add(R.drawable.ic_sa_person, other.getName(), false, () -> confirmCopyInto(profile, other));
        }
        choices.show();
    }

    private void confirmCopyInto(SettingsProfile from, SettingsProfile to) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setCustomTitle(com.limelight.ui.Dialogs.header(builder.getContext(), R.drawable.ic_sa_content_copy,
                        context.getString(R.string.profile_manager_copy_into),
                        context.getString(R.string.profile_manager_copy_into_confirm, from.getName(), to.getName())))
                .setPositiveButton(R.string.profile_manager_copy_into_action, (dialog, which) -> {
                    if (profilesManager.copyOptionsInto(from.getUuid(), to.getUuid())) {
                        profilesManager.save(context);
                        Toast.makeText(context, context.getString(R.string.profile_manager_copied_into, to.getName()), Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    @Override
    public int getItemCount() {
        return profilesManager.getProfiles().size();
    }

    static class ProfileViewHolder extends RecyclerView.ViewHolder {
        android.widget.ImageView dragHandle;
        TextView profileName;
        TextView profileTimestamp;
        RadioButton profileActive;
        ImageButton copyProfile;
        ImageButton editProfile;
        ImageButton deleteProfile;

        ProfileViewHolder(@NonNull View itemView) {
            super(itemView);
            dragHandle = itemView.findViewById(R.id.profileDragHandle);
            profileName = itemView.findViewById(R.id.profileName);
            profileTimestamp = itemView.findViewById(R.id.profileTimestamp);
            profileActive = itemView.findViewById(R.id.profileActive);
            copyProfile = itemView.findViewById(R.id.copyProfile);
            editProfile = itemView.findViewById(R.id.editProfile);
            deleteProfile = itemView.findViewById(R.id.deleteProfile);
        }
    }
}