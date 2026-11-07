package com.limelight.preferences;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.DigitsKeyListener;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;

import com.limelight.R;

import java.util.ArrayList;
import java.util.List;

// NOTE: The Custom Resolution row: opens a dialog with an aspect ratio row, minus, width x height fields, plus, and a bar of
// NOTE: labelled stops of the chosen ratio. OK stays disabled while the size is empty, odd, outside 256..8192 or not
// NOTE: decodable on this device. The result is stored as "WxH" under edit_diy_w_h, the key the picker and the stream read.
public class CustomResolutionPreference extends Preference {

    private static final String DEFAULT_VALUE = "1920x1080";
    // Width of each number field, enough for four digits at 22 sp
    private static final int FIELD_DP = 64;
    private static final int MAX_DIGITS = 4;
    // Colour of an invalid field border and of the reason line
    private static final int ERROR_COLOR = 0xFFF2B8B5;
    private static final int SEGMENT_FILL_ALPHA = 0x33;
    // Below this screen height in dp (a phone in landscape) everything in the dialog is a little smaller, so it fits whole
    private static final int SHORT_SCREEN_DP = 480;
    // Sizes on a short screen: title and field text in sp, the minus and plus buttons, the ratio buttons and rectangles in dp
    private static final int SHORT_TITLE_SP = 18;
    private static final int SHORT_FIELD_SP = 18;
    private static final int SHORT_BUTTON_DP = 34;
    private static final int SHORT_RATIO_BUTTON_DP = 34;
    private static final int SHORT_RATIO_RECT_DP = 22;
    // Space between the ratio row and the minus, fields and plus row, in dp (owner: closer together)
    private static final int ROW_TOP_DP = 16;
    // Space between the fields and the bar, in dp
    private static final int BAR_TOP_DP = 14;
    // Height of the ratio rectangles inside the ratio buttons, in dp; shrinks on a narrow dialog so 21:9 fits its button
    private static final int RATIO_RECT_DP = 26;
    // Height of a ratio button, in dp
    private static final int RATIO_BUTTON_DP = 46;
    private static final String[] DECODER_TYPES = {"video/hevc", "video/avc", "video/av01"};

    public CustomResolutionPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public CustomResolutionPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public CustomResolutionPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomResolutionPreference(Context context) {
        super(context);
    }

    @Override
    protected void onClick() {
        new Dialog(getContext()).show();
    }

    // The screen in the pixels it shows now (real metrics), the longer side first
    private static int[] screenSize(Context context) {
        DisplayMetrics m = new DisplayMetrics();
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        wm.getDefaultDisplay().getRealMetrics(m);
        return new int[]{Math.max(m.widthPixels, m.heightPixels), Math.min(m.widthPixels, m.heightPixels)};
    }

    // Video capabilities of the device's hardware and software decoders for the stream codecs, read once per dialog
    private static List<MediaCodecInfo.VideoCapabilities> decoders() {
        List<MediaCodecInfo.VideoCapabilities> caps = new ArrayList<>();
        try {
            for (MediaCodecInfo info : new MediaCodecList(MediaCodecList.REGULAR_CODECS).getCodecInfos()) {
                if (info.isEncoder()) {
                    continue;
                }
                for (String type : info.getSupportedTypes()) {
                    for (String wanted : DECODER_TYPES) {
                        if (type.equalsIgnoreCase(wanted)) {
                            MediaCodecInfo.VideoCapabilities v = info.getCapabilitiesForType(type).getVideoCapabilities();
                            if (v != null) {
                                caps.add(v);
                            }
                        }
                    }
                }
            }
        } catch (RuntimeException ignored) {
            // A broken codec list must not block the dialog; the size check then relies on the range rules only
        }
        return caps;
    }

    private final class Dialog {
        private final Context context;
        private final CustomResolutionModel model;
        private final List<MediaCodecInfo.VideoCapabilities> caps = decoders();
        private CustomResolutionModel.Ratio ratio;

        private final int text;
        private final int secondary;
        private int accent;

        private LinearLayout segments;
        private EditText widthField;
        private EditText heightField;
        private LinearLayout widthBox;
        private LinearLayout heightBox;

        private ResolutionStopsBar bar;
        private TextView reason;
        private AlertDialog dialog;
        private boolean filling;
        // Compact height: smaller ratio buttons and rectangles
        private boolean compact;

        Dialog(Context context) {
            this.context = context;
            int[] screen = screenSize(context);
            this.model = new CustomResolutionModel(screen[0], screen[1]);
            this.text = StepperViews.textColor(context);
            this.secondary = StepperViews.secondaryTextColor(context);
        }

        private int dp(int v) {
            return StepperViews.dp(context, v);
        }

        void show() {
            int[] saved = CustomResolutionModel.parse(getPersistedString(DEFAULT_VALUE));
            if (saved == null) {
                saved = CustomResolutionModel.parse(DEFAULT_VALUE);
            }
            ratio = model.ratioFor(saved[0], saved[1]);

            // A short screen (Android's compact height, < 480 dp: a phone in landscape): the same centred card, its parts a
            // little smaller and the gaps tighter, so the one column fits whole (a full-screen dialog was tried and rejected)
            boolean shortScreen = context.getResources().getConfiguration().screenHeightDp < SHORT_SCREEN_DP;
            compact = shortScreen;

            LinearLayout layout = new LinearLayout(context);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setPadding(6, 6, 6, 6);
            // One column on every screen; the scroll view is a safety net for very small windows
            android.widget.ScrollView scroll = new android.widget.ScrollView(context);
            scroll.addView(layout);

            // No description and no screen line under the title (owner: OK already says the size applies); the ratio row
            // comes right under the title
            segments = new LinearLayout(context);
            segments.setOrientation(LinearLayout.HORIZONTAL);
            segments.setPadding(dp(4), dp(4), dp(4), dp(4));
            LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            sp.setMargins(dp(16), dp(shortScreen ? 2 : 8), dp(16), 0);
            layout.addView(segments, sp);

            LinearLayout row = new LinearLayout(context);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER);
            // Air between the screen line and the fields (owner's remark on the first build)
            row.setPadding(0, dp(shortScreen ? 10 : ROW_TOP_DP), 0, dp(shortScreen ? 0 : 4));
            View minus = StepperViews.roundButton(context, R.drawable.ic_stepper_minus, "-");
            View plus = StepperViews.roundButton(context, R.drawable.ic_stepper_plus, "+");
            widthField = field();
            heightField = field();
            if (shortScreen) {
                for (EditText f : new EditText[]{widthField, heightField}) {
                    f.setTextSize(TypedValue.COMPLEX_UNIT_SP, SHORT_FIELD_SP);
                    f.setPadding(0, dp(1), 0, dp(1));
                }
            }
            widthBox = StepperViews.box(context, widthField, null);
            heightBox = StepperViews.box(context, heightField, null);
            TextView times = new TextView(context);
            times.setText("×");
            times.setTextSize(TypedValue.COMPLEX_UNIT_SP, shortScreen ? 16 : 20);
            times.setTextColor(secondary);
            times.setPadding(dp(8), 0, dp(8), 0);
            LinearLayout.LayoutParams buttonSize = shortScreen
                    ? new LinearLayout.LayoutParams(dp(SHORT_BUTTON_DP), dp(SHORT_BUTTON_DP))
                    : StepperViews.buttonParams(context);
            row.addView(minus, buttonSize);
            row.addView(widthBox, StepperViews.middleParams(context));
            row.addView(times);
            row.addView(heightBox, StepperViews.middleParams(context));
            row.addView(plus, new LinearLayout.LayoutParams(buttonSize));
            layout.addView(row, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

            AlertDialog.Builder b = new AlertDialog.Builder(context);
            if (shortScreen) {
                // A smaller title on a short screen; its left edge stays on the dialog's text indent
                TextView title = new TextView(context);
                title.setText(getTitle());
                title.setTextSize(TypedValue.COMPLEX_UNIT_SP, SHORT_TITLE_SP);
                title.setTextColor(text);
                title.setSingleLine(true);
                title.setPadding(dp(24), dp(14), dp(24), dp(2));
                b.setCustomTitle(title);
            } else {
                b.setTitle(getTitle());
            }
            b.setView(scroll);
            b.setPositiveButton(android.R.string.ok, (d, w) -> save());
            b.setNegativeButton(context.getString(R.string.cancel), (d, w) -> d.dismiss());
            dialog = b.create();
            // create() builds the buttons, so the bar can take their accent colour before the dialog is shown
            dialog.create();
            accent = dialog.getButton(AlertDialog.BUTTON_POSITIVE).getCurrentTextColor();

            bar = new ResolutionStopsBar(context, accent, text, secondary);
            bar.setListener(i -> {
                CustomResolutionModel.Stop s = model.stops(ratio).get(i);
                fill(s.width, s.height);
            });
            LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            // Without the stop chip (removed by the owner) the bar stands right under the fields, its bold label names the stop
            bp.setMargins(0, dp(shortScreen ? 12 : BAR_TOP_DP), 0, 0);
            bar.setCompact(shortScreen);
            layout.addView(bar, bp);

            // The reason line takes no room while there is no reason
            reason = new TextView(context);
            reason.setGravity(Gravity.CENTER);
            reason.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            reason.setTextColor(ERROR_COLOR);
            reason.setPadding(dp(16), dp(6), dp(16), 0);
            reason.setVisibility(View.GONE);
            layout.addView(reason);

            dialog.setOnShowListener(d -> {
                StepperViews.pillButtons(dialog);
                StepperViews.matchTitleIndent(dialog, null);
                update();
            });

            savedWidth = saved[0];
            savedHeight = saved[1];
            fill(savedWidth, savedHeight);

            minus.setOnClickListener(v -> step(false));
            plus.setOnClickListener(v -> step(true));
            TextWatcher watcher = new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    if (!filling) {
                        update();
                    }
                }
            };
            widthField.addTextChangedListener(watcher);
            heightField.addTextChangedListener(watcher);

            dialog.show();
        }

        private int savedWidth;
        private int savedHeight;

        // A field for digits only, with the number keyboard; the key listener is set before the raw input type, because a
        // key listener alone switches the keyboard to letters
        private EditText field() {
            EditText f = StepperViews.numberField(context, FIELD_DP, MAX_DIGITS);
            f.setKeyListener(DigitsKeyListener.getInstance("0123456789"));
            f.setRawInputType(InputType.TYPE_CLASS_NUMBER);
            return f;
        }

        private Integer value(EditText f) {
            try {
                return Integer.parseInt(f.getText().toString().trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }

        private void step(boolean up) {
            Integer w = value(widthField);
            Integer h = value(heightField);
            int ww = w != null ? w : 0;
            int hh = h != null ? h : 0;
            int i = up ? model.plus(ratio, ww, hh) : model.minus(ratio, ww, hh);
            CustomResolutionModel.Stop s = model.stops(ratio).get(i);
            fill(s.width, s.height);
        }

        private void fill(int w, int h) {
            filling = true;
            widthField.setText(String.valueOf(w));
            heightField.setText(String.valueOf(h));
            filling = false;
            update();
        }

        private void pickRatio(CustomResolutionModel.Ratio r) {
            ratio = r;
            Integer h = value(heightField);
            int i = model.closestByHeight(r, h != null ? h : 1080);
            CustomResolutionModel.Stop s = model.stops(r).get(i);
            fill(s.width, s.height);
        }

        private String ratioName(CustomResolutionModel.Ratio r) {
            return r == CustomResolutionModel.Ratio.NATIVE ? context.getString(R.string.resolution_prefix_native) : CustomResolutionModel.ratioLabel(r);
        }

        // The ratio row (owner's accepted mockup): five equal buttons; inside each, a rectangle of the ratio's own shape with
        // the ratio written in it. The chosen button is tinted and its rectangle filled with the accent. All rectangles share
        // one height, lowered on a narrow dialog so the widest ratio still fits its button.
        private void drawSegments() {
            segments.removeAllViews();
            segments.setBackground(rounded(withAlpha(text, 0x14), dp(14)));
            CustomResolutionModel.Ratio[] all = CustomResolutionModel.Ratio.values();

            float widest = 0;
            for (CustomResolutionModel.Ratio r : all) {
                widest = Math.max(widest, model.ratioValue(r));
            }
            int rectH = dp(compact ? SHORT_RATIO_RECT_DP : RATIO_RECT_DP);
            int rowW = segments.getWidth() - segments.getPaddingLeft() - segments.getPaddingRight();
            if (rowW > 0) {
                int buttonW = rowW / all.length;
                rectH = Math.min(rectH, Math.round((buttonW - dp(8)) / widest));
            } else {
                // The row has no width before its first layout; draw again once it has one
                segments.post(this::drawSegments);
            }

            for (CustomResolutionModel.Ratio r : all) {
                boolean on = r == ratio;
                LinearLayout button = new LinearLayout(context);
                button.setGravity(Gravity.CENTER);
                button.setClickable(true);
                button.setFocusable(true);
                button.setContentDescription(ratioName(r));
                if (on) {
                    button.setBackground(rounded(withAlpha(accent, SEGMENT_FILL_ALPHA), dp(10)));
                }

                TextView rect = new TextView(context);
                rect.setText(ratioName(r));
                rect.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
                rect.setSingleLine(true);
                rect.setGravity(Gravity.CENTER);
                rect.setIncludeFontPadding(false);
                rect.setTextColor(on ? (0xFF000000 | darker(accent)) : secondary);
                if (on) {
                    rect.setTypeface(Typeface.DEFAULT_BOLD);
                }
                GradientDrawable shape = new GradientDrawable();
                shape.setCornerRadius(dp(4));
                shape.setStroke(Math.max(1, Math.round(dp(3) / 2f)), on ? accent : secondary);
                shape.setColor(on ? accent : Color.TRANSPARENT);
                rect.setBackground(shape);
                // Width from the ratio; never narrower than its own text (a near-square Native of a foldable)
                int textW = Math.round(rect.getPaint().measureText(ratioName(r))) + dp(8);
                int rectW = Math.max(Math.round(rectH * model.ratioValue(r)), textW);
                button.addView(rect, new LinearLayout.LayoutParams(rectW, rectH));

                button.setOnClickListener(v -> pickRatio(r));
                segments.addView(button, new LinearLayout.LayoutParams(0, dp(compact ? SHORT_RATIO_BUTTON_DP : RATIO_BUTTON_DP), 1f));
            }
        }

        // A dark tone of the accent for the text on a filled rectangle
        private int darker(int color) {
            int r = ((color >> 16) & 0xFF) * 3 / 10;
            int g = ((color >> 8) & 0xFF) * 3 / 10;
            int b = (color & 0xFF) * 3 / 10;
            return (r << 16) | (g << 8) | b;
        }

        private Drawable rounded(int color, int radius) {
            GradientDrawable d = new GradientDrawable();
            d.setCornerRadius(radius);
            d.setColor(color);
            return d;
        }

        private int withAlpha(int color, int alpha) {
            return (color & 0x00FFFFFF) | (alpha << 24);
        }

        // Redraws everything from the two fields: ratio row, screen line, bar, reason line, field borders and OK
        private void update() {
            Integer w = value(widthField);
            Integer h = value(heightField);
            int ww = w != null ? w : 0;
            int hh = h != null ? h : 0;
            List<CustomResolutionModel.Stop> stops = model.stops(ratio);
            int idx = model.indexOf(ratio, ww, hh);

            drawSegments();

            if (bar != null) {
                List<String> labels = new ArrayList<>();
                for (CustomResolutionModel.Stop s : stops) {
                    labels.add(s.label);
                }
                bar.setStops(labels, model.position(ratio, ww, hh), idx);
            }

            String why = problem(w, h);
            boolean bad = why != null && (w != null || h != null);
            if (reason != null) {
                reason.setText(bad ? why : "");
                reason.setVisibility(bad ? View.VISIBLE : View.GONE);
            }
            border(widthBox, bad);
            border(heightBox, bad);
            View ok = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            if (ok != null) {
                ok.setEnabled(why == null);
                ok.setAlpha(why == null ? 1f : 0.38f);
            }
        }

        private void border(LinearLayout box, boolean bad) {
            Drawable bg = box.getBackground();
            if (bg instanceof GradientDrawable) {
                ((GradientDrawable) bg.mutate()).setStroke(dp(bad ? 2 : 1), bad ? ERROR_COLOR : withAlpha(text, 0x33));
            }
        }

        // The reason OK is disabled, or null when the size can be saved
        private String problem(Integer w, Integer h) {
            switch (CustomResolutionModel.check(w, h)) {
                case EMPTY:
                    return context.getString(R.string.custom_resolution_error_empty);
                case OUT_OF_RANGE:
                    return context.getString(R.string.custom_resolution_error_range, CustomResolutionModel.MIN_SIDE, CustomResolutionModel.MAX_SIDE);
                case ODD:
                    return context.getString(R.string.custom_resolution_error_odd);
                default:
                    break;
            }
            if (!caps.isEmpty()) {
                for (MediaCodecInfo.VideoCapabilities c : caps) {
                    if (c.isSizeSupported(w, h)) {
                        return null;
                    }
                }
                return context.getString(R.string.custom_resolution_error_decoder, w, h);
            }
            return null;
        }

        private void save() {
            Integer w = value(widthField);
            Integer h = value(heightField);
            if (problem(w, h) != null) {
                return;
            }
            String v = w + "x" + h;
            if (callChangeListener(v)) {
                persistString(v);
                notifyChanged();
            }
        }
    }
}
