/**
 * Created by Karim Mreisi.
 */

package com.limelight.binding.input.virtual_controller.keyboard;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.limelight.Game;
import com.limelight.R;
import com.limelight.preferences.PreferenceConfiguration;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class KeyBoardLayoutController {
    private static final Set<Integer> MODIFIER_KEY_CODES = new HashSet<>();
    private static final Set<Integer> SPECIAL_KEY_CODES = new HashSet<>();
    private static final long POPUP_DURATION_MS = 75;
    // NOTE: Tags that are not an Android key code. "fn" switches the function row, and "vk:" carries a Windows virtual key code in hex.
    private static final String TAG_FN = "fn";
    private static final String TAG_VK = "vk:";
    private static final int VK_F1 = 0x70;
    private static final int VK_F13 = 0x7C;
    private static final int FUNCTION_KEY_COUNT = 12;
    // Above the quick-action bars, which sit at an elevation of 9999 dp
    private static final float ON_TOP_ELEVATION_DP = 10000;

    private final long timerLongClickTimeout = 300;
    private final Context context;
    private final PreferenceConfiguration prefConfig;
    private ViewCallbacks viewCallbacks;
    private FrameLayout frame_layout = null;
    private final Handler handler;
    public boolean shown = false;
    private final LinearLayout keyboardView;
    private PopupWindow keyPopup;
    private TextView keyPopupText;
    private Runnable hidePopupRunnable;
    private final List<TextView> functionKeys = new ArrayList<>();
    private TextView fnKey;
    private boolean fnActive;

    static {
        MODIFIER_KEY_CODES.add(KeyEvent.KEYCODE_ALT_LEFT);
        MODIFIER_KEY_CODES.add(KeyEvent.KEYCODE_ALT_RIGHT);
        MODIFIER_KEY_CODES.add(KeyEvent.KEYCODE_CTRL_LEFT);
        MODIFIER_KEY_CODES.add(KeyEvent.KEYCODE_CTRL_RIGHT);
        MODIFIER_KEY_CODES.add(KeyEvent.KEYCODE_SHIFT_LEFT);
        MODIFIER_KEY_CODES.add(KeyEvent.KEYCODE_SHIFT_RIGHT);
        MODIFIER_KEY_CODES.add(KeyEvent.KEYCODE_META_LEFT);
        MODIFIER_KEY_CODES.add(KeyEvent.KEYCODE_META_RIGHT);

        SPECIAL_KEY_CODES.add(KeyEvent.KEYCODE_TAB);
        SPECIAL_KEY_CODES.add(KeyEvent.KEYCODE_ENTER);
        SPECIAL_KEY_CODES.add(KeyEvent.KEYCODE_SPACE);
        SPECIAL_KEY_CODES.add(KeyEvent.KEYCODE_DEL);
        SPECIAL_KEY_CODES.add(KeyEvent.KEYCODE_FORWARD_DEL);
        SPECIAL_KEY_CODES.add(KeyEvent.KEYCODE_ESCAPE);
        SPECIAL_KEY_CODES.add(KeyEvent.KEYCODE_CAPS_LOCK);
        SPECIAL_KEY_CODES.add(KeyEvent.KEYCODE_INSERT);
        SPECIAL_KEY_CODES.add(KeyEvent.KEYCODE_DPAD_UP);
        SPECIAL_KEY_CODES.add(KeyEvent.KEYCODE_DPAD_DOWN);
        SPECIAL_KEY_CODES.add(KeyEvent.KEYCODE_DPAD_LEFT);
        SPECIAL_KEY_CODES.add(KeyEvent.KEYCODE_DPAD_RIGHT);
        SPECIAL_KEY_CODES.add(KeyEvent.KEYCODE_PAGE_UP);
        SPECIAL_KEY_CODES.add(KeyEvent.KEYCODE_PAGE_DOWN);
        SPECIAL_KEY_CODES.add(KeyEvent.KEYCODE_MOVE_HOME);
        SPECIAL_KEY_CODES.add(KeyEvent.KEYCODE_MOVE_END);
    }

    private static final HashMap<Integer, Runnable> longClickRunnables = new HashMap<>();

    private final BitSet modifierKeyStates = new BitSet();
    // Keys whose press already went to the PC and whose release has not, so hiding the keyboard can let go of them
    private final Map<Integer, View> pressedKeys = new HashMap<>();
    // F13 to F24 held on the keyboard, released the same way when the keyboard hides
    private final Map<Integer, View> pressedVirtualKeys = new HashMap<>();

    public boolean isModifierKeyPressed(int keyCode) {
        return modifierKeyStates.get(keyCode);
    }

    private boolean isModifierKey(int keyCode) {
        if (prefConfig.stickyModifierKey) {
            return MODIFIER_KEY_CODES.contains(keyCode);
        }

        return false;
    }

    private boolean isSpecialKey(int keyCode) {
        return SPECIAL_KEY_CODES.contains(keyCode) || MODIFIER_KEY_CODES.contains(keyCode);
    }

    public KeyBoardLayoutController(FrameLayout layout, final Context context, PreferenceConfiguration prefConfig) {
        this.frame_layout = layout;
        this.context = context;
        this.prefConfig = prefConfig;
        this.keyboardView = (LinearLayout) LayoutInflater.from(context).inflate(R.layout.layout_axixi_keyboard, null);
        this.handler = new Handler(Looper.getMainLooper());
        initKeyPopup();
        initKeyboard();
    }

    public void setViewCallbacks(ViewCallbacks viewCallbacks) {
        this.viewCallbacks = viewCallbacks;
    }

    public Handler getHandler() {
        return handler;
    }

    private void initKeyboard() {
        @SuppressLint("ClickableViewAccessibility")
        View.OnTouchListener touchListener = (View v, MotionEvent event) -> {
            int eventAction = event.getAction();
            String tag = (String) v.getTag();
            if (TextUtils.equals("hide", tag)) {
                if (eventAction == MotionEvent.ACTION_UP || eventAction == MotionEvent.ACTION_CANCEL) {
                    hide();
                }
                return true;
            }

            if (TextUtils.equals(TAG_FN, tag)) {
                if (eventAction == MotionEvent.ACTION_UP) {
                    setFnActive(!fnActive);
                }
                return true;
            }
            if (tag.startsWith(TAG_VK)) {
                return onVirtualKeyTouch(v, eventAction, Integer.parseInt(tag.substring(TAG_VK.length()), 16));
            }

            int keyCode = Integer.parseInt(tag);
            int keyAction;
            boolean _isModifierKey = isModifierKey(keyCode);
            boolean _isSpecialKey = isSpecialKey(keyCode);
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    if (_isModifierKey && isModifierKeyPressed(keyCode)) {
                        modifierKeyStates.clear(keyCode);
                        return true;
                    }

                    // Key popup
                    if (!TextUtils.equals("hide", tag) && !_isSpecialKey) {
                        String popupText;
                        KeyEvent tempEvent = new KeyEvent(KeyEvent.ACTION_DOWN, keyCode);
                        int unicodeChar = tempEvent.getUnicodeChar(0);

                        if (unicodeChar != 0) {
                            popupText = String.valueOf((char) unicodeChar);
                        } else {
                            popupText = KeyEvent.keyCodeToString(keyCode).replace("KEYCODE_", "");
                        }

                        keyPopupText.setText(popupText);

                        // Force layout measurement
                        keyPopupText.measure(
                                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                        );

                        int popupWidth = keyPopupText.getMeasuredWidth();

                        // Calculate position using the measured width
                        int[] location = new int[2];
                        v.getLocationInWindow(location);

                        // Center the popup over the key
                        int x = location[0] + (v.getWidth() - popupWidth) / 2;

                        // Show the popup above the key
                        int y = (int) (location[1] - v.getHeight() * 1.5);

                        keyPopup.update(x, y, popupWidth, ViewGroup.LayoutParams.WRAP_CONTENT);

                        if (keyPopup.isShowing()) {
                            keyPopup.update(x, y, popupWidth, ViewGroup.LayoutParams.WRAP_CONTENT);
                        } else {
                            keyPopup.showAtLocation(v, Gravity.NO_GRAVITY, x, y);
                        }
                    }

                    keyAction = KeyEvent.ACTION_DOWN;
                    break;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (_isModifierKey && isModifierKeyPressed(keyCode)) {
                        return true;
                    }

                    // Remove any pending hide operations
                    handler.removeCallbacks(hidePopupRunnable);
                    // Schedule a new hide operation
                    handler.postDelayed(hidePopupRunnable, POPUP_DURATION_MS);

                    keyAction = KeyEvent.ACTION_UP;
                    break;
                default:
                    return false;
            }

            KeyEvent keyEvent = new KeyEvent(keyAction, keyCode);
            keyEvent.setSource(0);
            sendKeyEvent(keyEvent);
            if (keyAction == KeyEvent.ACTION_DOWN) {
                pressedKeys.put(keyCode, v);
            }
            else {
                pressedKeys.remove(keyCode);
            }

            if (_isModifierKey) {
                Runnable longClickRunnable = longClickRunnables.get(keyCode);
                if (longClickRunnable != null) {
                    getHandler().removeCallbacks(longClickRunnable);
                    if (keyAction == KeyEvent.ACTION_DOWN) {
                        getHandler().postDelayed(longClickRunnable, timerLongClickTimeout);
                    }
                }
            }

            if (keyAction == KeyEvent.ACTION_DOWN) {
                if (prefConfig.enableKeyboardVibrate) {
                    keyboardView.performHapticFeedback(
                            HapticFeedbackConstants.VIRTUAL_KEY,
                            HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING |
                                    HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
                    );
                }
                v.setBackgroundResource(R.drawable.bg_ax_keyboard_button_confirm);
            } else {
                if (prefConfig.enableKeyboardVibrate) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                        keyboardView.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY_RELEASE);
                    } else {
                        keyboardView.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                    }
                }
                v.setBackgroundResource(R.drawable.bg_ax_keyboard_button);
            }
            return true;
        };
        addFnKey();
        addExtraFunctionRow();
        for (int i = 0; i < keyboardView.getChildCount(); i++) {
            LinearLayout keyboardRow = (LinearLayout) keyboardView.getChildAt(i);
            for (int j = 0; j < keyboardRow.getChildCount(); j++) {
                View child = keyboardRow.getChildAt(j);
                keyboardRow.getChildAt(j).setOnTouchListener(touchListener);
                String keyTag = (String) child.getTag();
                if (keyTag.equals("hide") || keyTag.equals(TAG_FN) || keyTag.startsWith(TAG_VK)) {
                    continue;
                }
                int keycode = Integer.parseInt((String) child.getTag());
                if (isModifierKey(keycode)) {
                    longClickRunnables.put(keycode, () -> {
                        modifierKeyStates.set(keycode);
                        if (prefConfig.enableKeyboardVibrate) {
                            child.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                        }
                    });
                }
            }
        }
    }

    private TextView newKey(String label, String tag, float weight) {
        TextView key = new TextView(context);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, weight);
        key.setLayoutParams(params);
        key.setBackgroundResource(R.drawable.bg_ax_keyboard_button);
        key.setGravity(Gravity.CENTER);
        key.setText(label);
        key.setTag(tag);
        key.setTextSize(12);
        return key;
    }

    // NOTE: Fn sits at the end of the first row and swaps F1 to F12 for F13 to F24 while it is on, so the on-screen keyboard reaches every function key.
    private void addFnKey() {
        LinearLayout firstRow = (LinearLayout) keyboardView.getChildAt(0);
        for (int i = 0; i < firstRow.getChildCount(); i++) {
            View child = firstRow.getChildAt(i);
            Object tag = child.getTag();
            if (child instanceof TextView && tag instanceof String && ((String) tag).matches("\\d+")) {
                int code = Integer.parseInt((String) tag);
                if (code >= KeyEvent.KEYCODE_F1 && code <= KeyEvent.KEYCODE_F12) {
                    functionKeys.add((TextView) child);
                }
            }
        }
        if (functionKeys.size() != FUNCTION_KEY_COUNT) {
            functionKeys.clear();
            return;
        }
        fnKey = newKey("Fn", TAG_FN, 100);
        firstRow.addView(fnKey);
    }

    private void setFnActive(boolean active) {
        fnActive = active;
        for (int i = 0; i < functionKeys.size(); i++) {
            TextView key = functionKeys.get(i);
            if (active) {
                key.setText("F" + (13 + i));
                key.setTag(TAG_VK + Integer.toHexString(VK_F13 + i));
            }
            else {
                key.setText("F" + (1 + i));
                key.setTag(String.valueOf(KeyEvent.KEYCODE_F1 + i));
            }
        }
        if (fnKey != null) {
            fnKey.setBackgroundResource(active ? R.drawable.bg_ax_keyboard_button_confirm : R.drawable.bg_ax_keyboard_button);
        }
    }

    // NOTE: An extra row with F13 to F24 right under the F1 to F12 row, for macro users who want both at once. The rows share the height, so the keyboard keeps its size.
    private void addExtraFunctionRow() {
        if (!prefConfig.keyboardShowF13F24Row) {
            return;
        }
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, 100));
        for (int i = 0; i < FUNCTION_KEY_COUNT; i++) {
            row.addView(newKey("F" + (13 + i), TAG_VK + Integer.toHexString(VK_F13 + i), 100));
        }
        keyboardView.addView(row, Math.min(1, keyboardView.getChildCount()));
    }

    private boolean onVirtualKeyTouch(View v, int action, int vk) {
        if (action == MotionEvent.ACTION_DOWN) {
            sendVirtualKey(vk, true);
            v.setBackgroundResource(R.drawable.bg_ax_keyboard_button_confirm);
            pressedVirtualKeys.put(vk, v);
        }
        else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            sendVirtualKey(vk, false);
            v.setBackgroundResource(R.drawable.bg_ax_keyboard_button);
            pressedVirtualKeys.remove(vk);
        }
        return true;
    }

    private void sendVirtualKey(int vk, boolean down) {
        if (Game.instance == null || !Game.instance.connected) {
            return;
        }
        Game.instance.sendVirtualKey((short) vk, down);
    }

    private void initKeyPopup() {
        // Create the popup window
        keyPopupText = new TextView(context);
        keyPopupText.setBackgroundResource(R.drawable.key_popup_background);
        keyPopupText.setTextColor(Color.WHITE);
        keyPopupText.setTextSize(32);
        keyPopupText.setGravity(Gravity.CENTER);
        keyPopupText.setPadding(24, 16, 24, 16);

        keyPopup = new PopupWindow(
                keyPopupText,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );

        hidePopupRunnable = () -> keyPopup.dismiss();
    }

    public boolean isKeyboardVisible() {
        return keyboardView.getVisibility() == View.VISIBLE;
    }

    // NOTE: Sends the release of every key still down, including Ctrl, Alt, Shift or Win latched with a long press, and clears their pressed look.
    // NOTE: Without this a key held when the keyboard hides stays pressed on the PC with nothing on screen to show it.
    private void releaseHeldKeys() {
        for (Map.Entry<Integer, View> key : new ArrayList<>(pressedKeys.entrySet())) {
            KeyEvent release = new KeyEvent(KeyEvent.ACTION_UP, key.getKey());
            release.setSource(0);
            sendKeyEvent(release);
            key.getValue().setBackgroundResource(R.drawable.bg_ax_keyboard_button);
        }
        pressedKeys.clear();
        for (Map.Entry<Integer, View> key : new ArrayList<>(pressedVirtualKeys.entrySet())) {
            sendVirtualKey(key.getKey(), false);
            key.getValue().setBackgroundResource(R.drawable.bg_ax_keyboard_button);
        }
        pressedVirtualKeys.clear();
        modifierKeyStates.clear();
    }

    public void hide(boolean temporary) {
        if (prefConfig.releaseKeysOnClose) {
            releaseHeldKeys();
        }
        if (prefConfig.enableKeyboardVibrate) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                keyboardView.performHapticFeedback(HapticFeedbackConstants.REJECT);
            } else {
                keyboardView.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
            }
        }
        keyboardView.setVisibility(View.GONE);
        keyboardView.setElevation(0);
        if (!temporary) {
            shown = false;
        }

        if (viewCallbacks != null) {
            viewCallbacks.onKeyboardControllerVisibilityChange(false);
        }
    }

    public void hide() {
        hide(false);
    }

    public void show() {
        // NOTE: The quick-action bars sit at a very high elevation so they stay above the stream, which also put them over the keys.
        // NOTE: The keyboard goes above them while it is open, and it takes every touch on its own area so none reaches a bar below.
        if (prefConfig.keyboardOnTop) {
            keyboardView.setElevation(ON_TOP_ELEVATION_DP * context.getResources().getDisplayMetrics().density);
            keyboardView.bringToFront();
            keyboardView.setClickable(true);
        }
        else {
            keyboardView.setElevation(0);
        }
        keyboardView.setVisibility(View.VISIBLE);
        shown = true;
        if (viewCallbacks != null) {
            viewCallbacks.onKeyboardControllerVisibilityChange(true);
        }
    }

    public void toggleVisibility() {
        if (keyboardView.getVisibility() == View.VISIBLE) {
            hide();
        } else {
            show();
        }
    }

    public void refreshLayout() {
        frame_layout.removeView(keyboardView);

        int height = 0;
        int width = 0;
        int widthPreference = 0;
        if (prefConfig.onscreenKeyboardAutoFitDisabled) {
            height = dip2px(context,prefConfig.onscreenKeyboardHeight);
            widthPreference = prefConfig.onscreenKeyboardWidth;
            width = widthPreference == 1000 ? ViewGroup.LayoutParams.MATCH_PARENT : dip2px(context, widthPreference);
        } else {
            DisplayMetrics screen = context.getResources().getDisplayMetrics();
            width = screen.widthPixels;
            height = (int) (screen.heightPixels * 0.5);
        }
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(width, height);
        params.gravity = Gravity.BOTTOM;
        switch (prefConfig.onscreenKeyboardAlignMode) {
            case "left": {
                params.gravity |= Gravity.START;
                break;
            }
            case "right": {
                params.gravity |= Gravity.END;
                break;
            }
            case "center":
            default: {
                params.gravity |= Gravity.CENTER_HORIZONTAL;
            }
        }

        // params.leftMargin = 20 + buttonSize;
        // params.topMargin = 15;
        keyboardView.setAlpha(prefConfig.oscKeyboardOpacity / 100f);
        frame_layout.addView(keyboardView, params);
    }

    public int dip2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    public void sendKeyEvent(KeyEvent keyEvent) {
        if (Game.instance == null || !Game.instance.connected) {
            return;
        }
        // 1-Mouse 0-Buttons 2-Stick 3-DPad
        if (keyEvent.getSource() == 1) {
            Game.instance.mouseButtonEvent(keyEvent.getKeyCode(), KeyEvent.ACTION_DOWN == keyEvent.getAction());
        } else {
            Game.instance.onKey(null, keyEvent.getKeyCode(), keyEvent);
        }
    }

    public interface ViewCallbacks {
        void onKeyboardControllerVisibilityChange(boolean visible);
    }
}
