package com.limelight.theme;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.widget.TextViewCompat;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.limelight.R;
import com.limelight.ui.Dialogs;

// NOTE: Picks the app style from two preview cards, each drawn with that style's real colors and controls, so the choice is visible before it is made.
// NOTE: Classic shows the neutral greys and checkboxes, Material You shows the chosen color scheme, switches and rounded sections.
// NOTE: Below the cards each color scheme is a swatch in its real primary color. Tapping one picks Material You with that scheme.
public final class AppStylePicker {
    public interface OnPicked {
        void onPicked(String style, String scheme);
    }

    private static final int[] SCHEME_NAMES = {
            R.string.color_scheme_wallpaper, R.string.color_scheme_blue, R.string.color_scheme_teal, R.string.color_scheme_green,
            R.string.color_scheme_purple, R.string.color_scheme_orange, R.string.color_scheme_rose, R.string.color_scheme_mono};

    private AppStylePicker() {
    }

    public static void show(Context context, String current, OnPicked onPicked) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        Context dialogContext = builder.getContext();
        View body = LayoutInflater.from(dialogContext).inflate(R.layout.dialog_app_style, null);
        LinearLayout cards = body.findViewById(R.id.app_style_cards);
        // Phones stack the cards, wider screens put them side by side
        boolean sideBySide = context.getResources().getDisplayMetrics().widthPixels
                / context.getResources().getDisplayMetrics().density >= 600;
        cards.setOrientation(sideBySide ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);

        String scheme = ThemeManager.currentScheme(context);
        AlertDialog[] dialog = new AlertDialog[1];
        View classic = card(context, dialogContext, cards, ThemeManager.CLASSIC, current, scheme, sideBySide, () -> {
            onPicked.onPicked(ThemeManager.CLASSIC, scheme);
            dialog[0].dismiss();
        });
        View materialYou = card(context, dialogContext, cards, ThemeManager.MATERIAL_YOU, current, scheme, sideBySide, () -> {
            onPicked.onPicked(ThemeManager.MATERIAL_YOU, scheme);
            dialog[0].dismiss();
        });
        cards.addView(classic);
        cards.addView(materialYou);

        com.limelight.ui.FlowRow swatches = body.findViewById(R.id.app_style_schemes);
        for (int i = 0; i < ThemeManager.SCHEMES.size(); i++) {
            String option = ThemeManager.SCHEMES.get(i);
            boolean selected = ThemeManager.MATERIAL_YOU.equals(current) && option.equals(scheme);
            swatches.addView(swatch(context, dialogContext, option, SCHEME_NAMES[i], selected, () -> {
                onPicked.onPicked(ThemeManager.MATERIAL_YOU, option);
                dialog[0].dismiss();
            }));
        }

        builder.setCustomTitle(Dialogs.header(dialogContext, R.drawable.ic_sa_palette,
                        context.getString(R.string.title_app_style), context.getString(R.string.app_style_picker_hint)))
                .setView(body)
                .setNegativeButton(R.string.game_menu_cancel, null);
        dialog[0] = Dialogs.show(builder, Dialogs.WIDTH_STANDARD_DP + 120);
    }

    // A round swatch in the scheme's primary color with its name below, ringed when it is the scheme in use
    private static View swatch(Context context, Context dialogContext, String scheme, int nameRes, boolean selected, Runnable onTap) {
        float density = context.getResources().getDisplayMetrics().density;
        LinearLayout item = new LinearLayout(dialogContext);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(Gravity.CENTER_HORIZONTAL);
        int pad = (int) (4 * density);
        item.setPadding(pad, pad, pad, pad);
        item.setMinimumWidth((int) (64 * density));
        item.setBackgroundResource(R.drawable.bg_button_text);
        View dot = new View(dialogContext);
        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        circle.setColor(Dialogs.color(ThemeManager.materialYouContext(context, scheme), R.attr.saPrimary));
        if (selected) {
            circle.setStroke((int) (3 * density), Dialogs.color(dialogContext, R.attr.saOnSurface));
        }
        dot.setBackground(circle);
        int size = (int) (36 * density);
        item.addView(dot, new LinearLayout.LayoutParams(size, size));
        TextView name = new TextView(dialogContext);
        name.setText(nameRes);
        TextViewCompat.setTextAppearance(name, R.style.TextAppearance_SuperArtemis_BodySmall);
        name.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        nameParams.topMargin = (int) (4 * density);
        item.addView(name, nameParams);
        item.setContentDescription(context.getString(nameRes) + (selected ? ", " + context.getString(R.string.color_scheme_in_use) : ""));
        item.setOnClickListener(v -> onTap.run());
        return item;
    }

    private static View card(Context context, Context dialogContext, ViewGroup parent, String style, String current, String scheme,
                             boolean sideBySide, Runnable onTap) {
        boolean materialYou = ThemeManager.MATERIAL_YOU.equals(style);
        View card = LayoutInflater.from(dialogContext).inflate(R.layout.app_style_card, parent, false);
        LinearLayout.LayoutParams params = sideBySide
                // Side by side, both cards stretch to the taller one so they line up
                ? new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
                : new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        int gap = (int) Dialogs.dp(context, 6);
        params.setMargins(gap, gap, gap, gap);
        card.setLayoutParams(params);
        card.setSelected(style.equals(current));
        ((TextView) card.findViewById(R.id.app_style_title)).setText(materialYou ? R.string.app_style_material_you : R.string.app_style_classic);
        ((TextView) card.findViewById(R.id.app_style_description)).setText(
                materialYou ? R.string.app_style_material_you_description : R.string.app_style_classic_description);
        ((ImageView) card.findViewById(R.id.app_style_check)).setVisibility(style.equals(current) ? View.VISIBLE : View.INVISIBLE);
        ((LinearLayout) card.findViewById(R.id.app_style_preview)).addView(preview(context, materialYou, scheme));
        card.setOnClickListener(v -> onTap.run());
        return card;
    }

    // A miniature settings section in the style's own theme: the real checkbox or switch on its real colors
    private static View preview(Context base, boolean materialYou, String scheme) {
        Context themed = materialYou
                ? ThemeManager.materialYouContext(base, scheme)
                : new android.view.ContextThemeWrapper(new android.view.ContextThemeWrapper(base, R.style.AppTheme), R.style.ThemeOverlay_SuperArtemis_Classic);
        float density = base.getResources().getDisplayMetrics().density;
        LinearLayout screen = new LinearLayout(themed);
        screen.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (12 * density);
        screen.setPadding(pad, pad, pad, pad);
        GradientDrawable background = new GradientDrawable();
        background.setColor(Dialogs.color(themed, R.attr.saBackground));
        background.setCornerRadius(12 * density);
        screen.setBackground(background);

        TextView section = new TextView(themed);
        section.setText(R.string.category_audio_settings);
        TextViewCompat.setTextAppearance(section, R.style.TextAppearance_SuperArtemis_SectionLabel);
        section.setTextColor(Dialogs.color(themed, R.attr.saPrimary));
        screen.addView(section);

        LinearLayout group = new LinearLayout(themed);
        group.setOrientation(LinearLayout.VERTICAL);
        if (materialYou) {
            GradientDrawable container = new GradientDrawable();
            container.setColor(Dialogs.color(themed, R.attr.saSurfaceContainer));
            container.setCornerRadius(16 * density);
            group.setBackground(container);
        }
        group.addView(row(themed, R.string.preview_setting_microphone, true, materialYou, density));
        group.addView(row(themed, R.string.preview_setting_equalizer, false, materialYou, density));
        LinearLayout.LayoutParams groupParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        groupParams.topMargin = (int) (6 * density);
        screen.addView(group, groupParams);
        return screen;
    }

    private static View row(Context themed, int labelRes, boolean checked, boolean materialYou, float density) {
        LinearLayout row = new LinearLayout(themed);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        // Same row height for a checkbox and a switch, so both previews have the same size
        row.setMinimumHeight((int) (48 * density));
        int pad = (int) ((materialYou ? 12 : 4) * density);
        row.setPadding(pad, 0, pad, 0);
        TextView label = new TextView(themed);
        label.setText(labelRes);
        TextViewCompat.setTextAppearance(label, R.style.TextAppearance_SuperArtemis_BodyMedium);
        label.setTextColor(Dialogs.color(themed, R.attr.saOnSurface));
        row.addView(label, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        CompoundButton toggle = materialYou ? new MaterialSwitch(themed) : new CheckBox(themed);
        toggle.setChecked(checked);
        // A preview, not a setting: taps go to the card around it
        toggle.setClickable(false);
        toggle.setFocusable(false);
        toggle.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        row.addView(toggle);
        return row;
    }
}
