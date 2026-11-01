Regarding the data structure, storing an optional `UUID defaultTemplateProfileId` in `ProfilesData` keeps profile creation clean and predictable.

When this template ID is set, newly created profiles inherit their baseline settings from that specified profile. When null, profile creation continues copying the active profile or default preferences as it does today.

This approach keeps existing saved profiles decoupled and untouched. It also requires no breaking schema migrations in `profiles.json`. Now that pull requests #8, #10, and #11 are up for review, we will revisit the template selector once they are merged.
