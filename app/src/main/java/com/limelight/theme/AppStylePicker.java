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
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.widget.TextViewCompat;

import java.util.ArrayList;
import java.util.List;

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
            R.string.color_scheme_purple, R.string.color_scheme_orange, R.string.color_scheme_rose, R.string.color_scheme_amber, R.string.color_scheme_mono,
            R.string.color_scheme_custom};

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

        // NOTE: Taps only change what is shown. The two cards and the dots redraw with the pending choice, and Apply is what changes the app, so a mis-tap never recreates the screen.
        String[] pendingStyle = {current};
        String[] pendingScheme = {ThemeManager.currentScheme(context)};
        // NOTE: The ten tiles (Wallpaper, eight schemes and the custom tile) sit on one line in a wide dialog and in two rows of five on a phone.
        // NOTE: Every tile has the same weight, so both layouts stay symmetric on any screen and nothing scrolls sideways.
        boolean twoRows = Dialogs.isCompact(dialogContext);
        LinearLayout dots = body.findViewById(R.id.app_style_schemes);
        dots.setOrientation(twoRows ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
        TextView schemeLabel = body.findViewById(R.id.app_style_schemes_label);
        Runnable[] render = new Runnable[1];
        Runnable openPicker = () -> ColorPickerDialog.show(context, ThemeManager.customSeed(context), color -> {
            ThemeManager.chooseCustomColor(context, color);
            pendingStyle[0] = ThemeManager.MATERIAL_YOU;
            pendingScheme[0] = ThemeManager.SCHEME_CUSTOM;
            render[0].run();
        });
        render[0] = () -> {
            cards.removeAllViews();
            cards.addView(card(context, dialogContext, cards, ThemeManager.CLASSIC, pendingStyle[0], pendingScheme[0], sideBySide, () -> {
                pendingStyle[0] = ThemeManager.CLASSIC;
                render[0].run();
            }));
            cards.addView(card(context, dialogContext, cards, ThemeManager.MATERIAL_YOU, pendingStyle[0], pendingScheme[0], sideBySide, () -> {
                pendingStyle[0] = ThemeManager.MATERIAL_YOU;
                render[0].run();
            }));

            // NOTE: The tiles follow the accent row of DualClone. The last tile is the plus, and it wears the last custom color once there is one.
            // NOTE: The tiles carry no names, because ten names do not fit. The chosen name sits in the section label, and every tile has its name for screen readers.
            dots.removeAllViews();
            boolean inUse = ThemeManager.MATERIAL_YOU.equals(pendingStyle[0]);
            List<View> tiles = new ArrayList<>();
            for (int i = 0; i < ThemeManager.SCHEMES.size(); i++) {
                String option = ThemeManager.SCHEMES.get(i);
                if (ThemeManager.SCHEME_CUSTOM.equals(option)) {
                    continue;
                }
                boolean selected = inUse && option.equals(pendingScheme[0]);
                tiles.add(swatch(context, dialogContext, option, SCHEME_NAMES[i], selected, false, () -> {
                    pendingStyle[0] = ThemeManager.MATERIAL_YOU;
                    pendingScheme[0] = option;
                    render[0].run();
                }));
            }
            boolean customSelected = inUse && ThemeManager.SCHEME_CUSTOM.equals(pendingScheme[0]);
            tiles.add(swatch(context, dialogContext, ThemeManager.SCHEME_CUSTOM, R.string.color_scheme_add, customSelected, true, openPicker));
            if (twoRows) {
                int perRow = (tiles.size() + 1) / 2;
                for (int start = 0; start < tiles.size(); start += perRow) {
                    LinearLayout row = new LinearLayout(dialogContext);
                    row.setOrientation(LinearLayout.HORIZONTAL);
                    for (int k = 0; k < perRow; k++) {
                        // A short last row is padded with empty tiles of the same weight, so the columns line up
                        row.addView(start + k < tiles.size() ? tiles.get(start + k) : emptyTile(dialogContext));
                    }
                    dots.addView(row, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                }
            }
            else {
                for (View tile : tiles) {
                    dots.addView(tile);
                }
            }
            int shownName = SCHEME_NAMES[Math.max(0, ThemeManager.SCHEMES.indexOf(pendingScheme[0]))];
            schemeLabel.setText(inUse
                    ? context.getString(R.string.app_style_scheme_named, context.getString(shownName))
                    : context.getString(R.string.app_style_scheme));
        };
        render[0].run();

        builder.setCustomTitle(Dialogs.header(dialogContext, R.drawable.ic_sa_palette,
                        context.getString(R.string.title_app_style), context.getString(R.string.app_style_picker_hint)))
                .setView(body)
                .setPositiveButton(R.string.app_style_apply, (d, which) -> onPicked.onPicked(pendingStyle[0], pendingScheme[0]))
                .setNegativeButton(R.string.game_menu_cancel, null);
        Dialogs.show(builder, Dialogs.WIDTH_STANDARD_DP + 120);
    }

    private static View emptyTile(Context context) {
        View space = new View(context);
        space.setLayoutParams(new LinearLayout.LayoutParams(0, (int) Dialogs.dp(context, 48), 1f));
        return space;
    }

    // A round dot in the scheme's primary color, ringed and checked when it is the scheme in use.
    // The plus tile is a plus inside a rainbow ring, filled with the last custom color once one exists.
    private static View swatch(Context context, Context dialogContext, String scheme, int nameRes, boolean selected, boolean plus, Runnable onTap) {
        float density = context.getResources().getDisplayMetrics().density;
        FrameLayout item = new FrameLayout(dialogContext);
        item.setBackgroundResource(R.drawable.bg_button_text);
        android.widget.ImageView dot = new android.widget.ImageView(dialogContext);
        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        int inset = (int) (6 * density);
        if (plus) {
            boolean hasColor = ThemeManager.hasCustomColor(context);
            int fill = hasColor ? ThemeManager.customSeed(context) : Dialogs.color(dialogContext, R.attr.saSurfaceContainerHigh);
            circle.setColor(fill);
            if (selected) {
                circle.setStroke((int) (3 * density), Dialogs.color(dialogContext, R.attr.saOnSurface));
            }
            dot.setImageResource(R.drawable.ic_sa_add);
            int glyph = hasColor ? AccentColors.onColor(fill) : Dialogs.color(dialogContext, R.attr.saOnSurface);
            dot.setImageTintList(android.content.res.ColorStateList.valueOf(glyph));
            dot.setPadding(inset, inset, inset, inset);
        }
        else {
            Context schemeContext = ThemeManager.materialYouContext(context, scheme);
            circle.setColor(Dialogs.color(schemeContext, R.attr.saPrimary));
            if (selected) {
                // A ring alone is hard to see on light swatches, so the swatch in use also carries a check in its own on-primary color
                circle.setStroke((int) (3 * density), Dialogs.color(dialogContext, R.attr.saOnSurface));
                dot.setImageResource(R.drawable.ic_sa_check);
                dot.setImageTintList(android.content.res.ColorStateList.valueOf(Dialogs.color(schemeContext, R.attr.saOnPrimary)));
                dot.setPadding(inset, inset, inset, inset);
            }
        }
        if (plus) {
            // A rainbow ring around the plus says any color can be picked, whether or not a custom color is saved yet
            GradientDrawable rainbow = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                    new int[] {0xFFFF5252, 0xFFFFB300, 0xFF66BB6A, 0xFF29B6F6, 0xFF7E57C2, 0xFFEC407A, 0xFFFF5252});
            rainbow.setGradientType(GradientDrawable.SWEEP_GRADIENT);
            rainbow.setShape(GradientDrawable.OVAL);
            android.graphics.drawable.LayerDrawable ring = new android.graphics.drawable.LayerDrawable(new android.graphics.drawable.Drawable[] {rainbow, circle});
            int rim = (int) (3 * density);
            ring.setLayerInset(1, rim, rim, rim, rim);
            dot.setBackground(ring);
        }
        else {
            dot.setBackground(circle);
        }
        int size = (int) (32 * density);
        item.addView(dot, new FrameLayout.LayoutParams(size, size, Gravity.CENTER));
        item.setContentDescription(context.getString(nameRes) + (selected ? ", " + context.getString(R.string.color_scheme_in_use) : ""));
        item.setOnClickListener(v -> onTap.run());
        item.setLayoutParams(new LinearLayout.LayoutParams(0, (int) (48 * density), 1f));
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
