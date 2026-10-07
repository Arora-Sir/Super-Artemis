package com.limelight;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import com.limelight.binding.input.GameInputDevice;
import com.limelight.binding.input.KeyboardTranslator;
import com.limelight.preferences.PreferenceConfiguration;
import com.limelight.utils.KeyConfigHelper;
import com.limelight.utils.KeyMapper;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Provide options for ongoing Game Stream.
 * <p>
 * Shown on back action in game activity.
 */
public class GameMenu implements Game.GameMenuCallbacks {

    public static final long KEY_UP_DELAY = 25;
    private static final long TEST_GAME_FOCUS_DELAY = 10;

    public static final String PREF_NAME = "specialPrefs"; // SharedPreferences的名称

    public static final String KEY_NAME = "special_key"; // 要保存的键名称

    public static class MenuOption {
        private final String label;
        // Icon for compact and grid styles, 0 for dynamic items such as server commands
        private final int iconRes;
        private final boolean withGameFocus;
        private final Runnable runnable;

        public MenuOption(String label, int iconRes, boolean withGameFocus, Runnable runnable) {
            this.label = label;
            this.iconRes = iconRes;
            this.withGameFocus = withGameFocus;
            this.runnable = runnable;
        }

        public MenuOption(String label, boolean withGameFocus, Runnable runnable) {
            this(label, 0, withGameFocus, runnable);
        }

        public MenuOption(String label, Runnable runnable) {
            this(label, false, runnable);
        }
    }

    // Renders menu options as plain rows (list), rows with small icons (compact) or icon tiles (grid)
    private static final class MenuOptionAdapter extends ArrayAdapter<MenuOption> {
        private final String style;
        private final LayoutInflater inflater;

        MenuOptionAdapter(Context context, String style) {
            super(context, 0);
            this.style = style;
            this.inflater = LayoutInflater.from(context);
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            MenuOption option = getItem(position);
            String label = option != null ? option.label : "";

            if (GameMenuLayout.STYLE_LIST.equals(style)) {
                View view = convertView != null ? convertView : inflater.inflate(android.R.layout.simple_list_item_1, parent, false);
                ((TextView) view.findViewById(android.R.id.text1)).setText(label);
                return view;
            }

            int layout = GameMenuLayout.STYLE_GRID.equals(style) ? R.layout.game_menu_grid_item : R.layout.game_menu_compact_item;
            View view = convertView != null ? convertView : inflater.inflate(layout, parent, false);
            ImageView icon = view.findViewById(R.id.game_menu_item_icon);
            if (option != null && option.iconRes != 0) {
                icon.setImageResource(option.iconRes);
                icon.setVisibility(View.VISIBLE);
            }
            else {
                icon.setVisibility(View.GONE);
            }
            ((TextView) view.findViewById(R.id.game_menu_item_label)).setText(label);
            return view;
        }
    }

    private final Game game;
    private final Context dialogScreenContext;

    private AlertDialog currentDialog;

    public GameMenu(Game game, Context dialogScreenContext) {
        this.game = game;
        this.dialogScreenContext = dialogScreenContext;
    }

    public GameMenu(Game game) {
        this.game = game;
        this.dialogScreenContext = game;
    }

    private String getString(int id) {
        return game.getResources().getString(id);
    }


    private void sendKeys(short[] keys) {
        game.sendKeys(keys);
    }

    private void runWithGameFocus(Runnable runnable) {
        // Ensure that the Game activity is still active (not finished)
        if (game.isFinishing()) {
            return;
        }
        // Check if the game window has focus again, if not try again after delay
        if (!game.hasWindowFocus() && dialogScreenContext instanceof Game) {
            new Handler().postDelayed(() -> runWithGameFocus(runnable), TEST_GAME_FOCUS_DELAY);
            return;
        }
        // Game Activity has focus, run runnable
        runnable.run();
    }

    private void run(MenuOption option) {
        if (option.runnable == null) {
            return;
        }

        if (option.withGameFocus) {
            runWithGameFocus(option.runnable);
        } else {
            option.runnable.run();
        }
    }

    private void showMenuDialog(String title, MenuOption[] options) {
        int themeResId = game.getApplicationInfo().theme;

        Context themedContext = new ContextThemeWrapper(dialogScreenContext, themeResId);
        AlertDialog.Builder builder = new AlertDialog.Builder(themedContext);
        builder.setTitle(title);

        String style = GameMenuLayout.getStyle(prefs());
        final MenuOptionAdapter actions = new MenuOptionAdapter(themedContext, style);

        // Items run by position, so two options with the same label can no longer trigger each other
        if (GameMenuLayout.STYLE_GRID.equals(style)) {
            float density = themedContext.getResources().getDisplayMetrics().density;
            GridView grid = new GridView(themedContext);
            grid.setNumColumns(GridView.AUTO_FIT);
            grid.setColumnWidth((int) (96 * density));
            grid.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
            int padding = (int) (8 * density);
            grid.setPadding(padding, padding, padding, padding);
            grid.setAdapter(actions);
            grid.setOnItemClickListener((parent, view, position, id) -> {
                MenuOption option = actions.getItem(position);
                if (currentDialog != null) {
                    currentDialog.dismiss();
                }
                if (option != null) {
                    run(option);
                }
            });
            builder.setView(grid);
        }
        else {
            builder.setAdapter(actions, (dialog, which) -> {
                MenuOption option = actions.getItem(which);
                if (option != null) {
                    run(option);
                }
            });
        }

        builder.setOnCancelListener(dialog -> hideMenu());

        if (currentDialog != null) {
            currentDialog.dismiss();
        }
        currentDialog = builder.show();

        Window window = currentDialog.getWindow();

        if (window != null) {
            View decorView = window.getDecorView();
            decorView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {

                    decorView.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                    new Handler(Looper.getMainLooper()).post(() -> {
                        for (MenuOption option : options) {
                            actions.add(option);
                        }
                        actions.notifyDataSetChanged();
                    });
                }
            });
        }
    }

    private void showSpecialKeysMenu() {
        List<MenuOption> options = new ArrayList<>();

        if(!PreferenceConfiguration.readPreferences(game).disableDefaultExtraKeys){
            options.add(new MenuOption(getString(R.string.game_menu_send_keys_esc),
                    () -> sendKeys(new short[]{KeyboardTranslator.VK_ESCAPE})));

            options.add(new MenuOption(getString(R.string.game_menu_send_keys_f11),
                    () -> sendKeys(new short[]{KeyboardTranslator.VK_F11})));

            options.add(new MenuOption(getString(R.string.game_menu_send_keys_alt_f4),
                    () -> sendKeys(new short[]{KeyboardTranslator.VK_LMENU, KeyboardTranslator.VK_F4})));

            options.add(new MenuOption(getString(R.string.game_menu_send_keys_alt_enter),
                    () -> sendKeys(new short[]{KeyboardTranslator.VK_LMENU, KeyboardTranslator.VK_RETURN})));

            options.add(new MenuOption(getString(R.string.game_menu_send_keys_ctrl_v),
                    () -> sendKeys(new short[]{KeyboardTranslator.VK_LCONTROL, KeyboardTranslator.VK_V})));

            options.add(new MenuOption(getString(R.string.game_menu_send_keys_win),
                    () -> sendKeys(new short[]{KeyboardTranslator.VK_LWIN})));

            options.add(new MenuOption(getString(R.string.game_menu_send_keys_win_d),
                    () -> sendKeys(new short[]{KeyboardTranslator.VK_LWIN, KeyboardTranslator.VK_D})));

            options.add(new MenuOption(getString(R.string.game_menu_send_keys_win_g),
                    () -> sendKeys(new short[]{KeyboardTranslator.VK_LWIN, KeyboardTranslator.VK_G})));

            options.add(new MenuOption(getString(R.string.game_menu_send_keys_ctrl_alt_tab),
                    () -> sendKeys(new short[]{KeyboardTranslator.VK_LCONTROL, KeyboardTranslator.VK_LMENU, KeyboardTranslator.VK_TAB})));

            options.add(new MenuOption(getString(R.string.game_menu_send_keys_shift_tab),
                    () -> sendKeys(new short[]{KeyboardTranslator.VK_LSHIFT, KeyboardTranslator.VK_TAB})));

            options.add(new MenuOption(getString(R.string.game_menu_send_keys_win_shift_left),
                    () -> sendKeys(new short[]{KeyboardTranslator.VK_LWIN, KeyboardTranslator.VK_LSHIFT, KeyboardTranslator.VK_LEFT})));

            options.add(new MenuOption(getString(R.string.game_menu_send_keys_ctrl_alt_shift_f1),
                    () -> sendKeys(new short[]{KeyboardTranslator.VK_LCONTROL,KeyboardTranslator.VK_LMENU, KeyboardTranslator.VK_LSHIFT, KeyboardTranslator.VK_F1})));

            options.add(new MenuOption(getString(R.string.game_menu_send_keys_ctrl_alt_shift_f12),
                    () -> sendKeys(new short[]{KeyboardTranslator.VK_LCONTROL,KeyboardTranslator.VK_LMENU, KeyboardTranslator.VK_LSHIFT, KeyboardTranslator.VK_F12})));

            options.add(new MenuOption(getString(R.string.game_menu_send_keys_alt_b),
                    () -> sendKeys(new short[]{KeyboardTranslator.VK_LWIN, KeyboardTranslator.VK_LMENU, KeyboardTranslator.VK_B})));
        }

        // Import custom shortcuts
        SharedPreferences preferences = game.getSharedPreferences(PREF_NAME, Activity.MODE_PRIVATE);
        String value = preferences.getString(KEY_NAME,"");

        if(!TextUtils.isEmpty(value)){
            try {
                KeyConfigHelper.ShortcutFile shortcutFile = KeyConfigHelper.parseShortcutFile(value);
                if (shortcutFile != null && shortcutFile.data != null && !shortcutFile.data.isEmpty()) {
                    List<KeyConfigHelper.Shortcut> data = shortcutFile.data;
                    for (KeyConfigHelper.Shortcut sc : data) {
                        List<String> keys = sc.keys;
                        short[] keyCodes = new short[keys.size()];

                        for (int i = 0; i < keys.size(); i++) {
                            String code = keys.get(i);
                            int keycode;

                            if (code.startsWith("0x")) {               // literal hex value
                                keycode = Integer.parseInt(code.substring(2), 16);
                            } else if (code.startsWith("VK_")) {       // symbolic constant in KeyMapper
                                Field field = KeyMapper.class.getDeclaredField(code);
                                keycode = field.getInt(null);
                            } else {                                   // unsupported
                                throw new IllegalArgumentException("Unknown key code: " + code);
                            }
                            keyCodes[i] = (short) keycode;
                        }

                        // Whatever MenuOption looks like in your project
                        MenuOption option = new MenuOption(sc.name, () -> sendKeys(keyCodes));
                        options.add(option);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(game,getString(R.string.wrong_import_format),Toast.LENGTH_SHORT).show();
            }
        }
        options.add(new MenuOption(getString(R.string.game_menu_cancel), null));

        showMenuDialog(getString(R.string.game_menu_send_keys), options.toArray(new MenuOption[options.size()]));
    }

    private SharedPreferences prefs() {
        return PreferenceManager.getDefaultSharedPreferences(game);
    }

    private MenuOption action(String id, String label, boolean withGameFocus, Runnable runnable) {
        GameMenuLayout.Action info = GameMenuLayout.find(id);
        return new MenuOption(label, info != null ? info.iconRes : 0, withGameFocus, runnable);
    }

    private MenuOption cancelOption() {
        return new MenuOption(getString(R.string.game_menu_cancel), R.drawable.ic_gm_cancel, false, null);
    }

    // Configurable actions that apply right now, keyed by ID in GameMenuLayout.ACTIONS order
    private Map<String, MenuOption> buildActions() {
        Map<String, MenuOption> actions = new LinkedHashMap<>();

        actions.put(GameMenuLayout.ID_DISCONNECT, action(GameMenuLayout.ID_DISCONNECT,
                getString(R.string.game_menu_disconnect), false, game::disconnect));
        actions.put(GameMenuLayout.ID_QUIT, action(GameMenuLayout.ID_QUIT,
                getString(R.string.game_menu_quit_session), false, game::quit));
        actions.put(GameMenuLayout.ID_UPLOAD_CLIPBOARD, action(GameMenuLayout.ID_UPLOAD_CLIPBOARD,
                getString(R.string.game_menu_upload_clipboard), true, () -> game.sendClipboard(true)));
        actions.put(GameMenuLayout.ID_FETCH_CLIPBOARD, action(GameMenuLayout.ID_FETCH_CLIPBOARD,
                getString(R.string.game_menu_fetch_clipboard), true, () -> game.getClipboard(0)));
        actions.put(GameMenuLayout.ID_SERVER_CMD, action(GameMenuLayout.ID_SERVER_CMD,
                getString(R.string.game_menu_server_cmd), true, () -> {
                    ArrayList<String> serverCmds = game.getServerCmds();
                    if (serverCmds.isEmpty()) {
                        int themeResId = game.getApplicationInfo().theme;
                        Context themedContext = new ContextThemeWrapper(dialogScreenContext, themeResId);
                        new AlertDialog.Builder(themedContext)
                                .setTitle(R.string.game_dialog_title_server_cmd_empty)
                                .setMessage(R.string.game_dialog_message_server_cmd_empty)
                                .show();
                    } else {
                        hideMenu();
                        this.showServerCmd(serverCmds);
                    }
                }));
        actions.put(GameMenuLayout.ID_TOGGLE_KEYBOARD, action(GameMenuLayout.ID_TOGGLE_KEYBOARD,
                getString(R.string.game_menu_toggle_keyboard), true, game::toggleKeyboard));
        actions.put(GameMenuLayout.ID_ZOOM, action(GameMenuLayout.ID_ZOOM,
                getString(game.isZoomModeEnabled() ? R.string.game_menu_disable_zoom_mode : R.string.game_menu_enable_zoom_mode),
                true, game::toggleZoomMode));
        if (dialogScreenContext == game) {
            actions.put(GameMenuLayout.ID_ROTATE_SCREEN, action(GameMenuLayout.ID_ROTATE_SCREEN,
                    getString(R.string.game_menu_rotate_screen), true, game::rotateScreen));
        }
        // Only offered when the host negotiated the mic stream, so toggling just pauses or resumes local capture
        if (game.isMicrophoneAvailable()) {
            actions.put(GameMenuLayout.ID_MICROPHONE, action(GameMenuLayout.ID_MICROPHONE,
                    getString(game.isMicrophoneStreaming() ? R.string.game_menu_mute_microphone : R.string.game_menu_unmute_microphone),
                    true, game::toggleMicrophoneCapture));
        }
        if (game.allowChangeMouseMode) {
            actions.put(GameMenuLayout.ID_MOUSE_MODE, action(GameMenuLayout.ID_MOUSE_MODE,
                    getString(R.string.game_menu_select_mouse_mode), true, () -> game.selectMouseMode(dialogScreenContext)));
        }
        actions.put(GameMenuLayout.ID_TOGGLE_HUD, action(GameMenuLayout.ID_TOGGLE_HUD,
                getString(R.string.game_menu_toggle_hud), true, game::toggleHUD));
        actions.put(GameMenuLayout.ID_TOGGLE_FLOATING_BUTTON, action(GameMenuLayout.ID_TOGGLE_FLOATING_BUTTON,
                getString(R.string.game_menu_toggle_floating_button), true, game::toggleFloatingButtonVisibility));
        actions.put(GameMenuLayout.ID_TOGGLE_KEYBOARD_MODEL, action(GameMenuLayout.ID_TOGGLE_KEYBOARD_MODEL,
                getString(R.string.game_menu_toggle_keyboard_model), true, game::toggleKeyboardController));
        if (!game.isOnExternalDisplay()) {
            actions.put(GameMenuLayout.ID_TOGGLE_VIRTUAL_CONTROLLER, action(GameMenuLayout.ID_TOGGLE_VIRTUAL_CONTROLLER,
                    getString(R.string.game_menu_toggle_virtual_model), true, game::toggleVirtualController));
        }
        actions.put(GameMenuLayout.ID_TOGGLE_FULL_KEYBOARD, action(GameMenuLayout.ID_TOGGLE_FULL_KEYBOARD,
                getString(R.string.game_menu_toggle_virtual_keyboard_model), true, game::toggleFullKeyboard));
        actions.put(GameMenuLayout.ID_TASK_MANAGER, action(GameMenuLayout.ID_TASK_MANAGER,
                getString(R.string.game_menu_task_manager), true,
                () -> sendKeys(new short[]{KeyboardTranslator.VK_LCONTROL, KeyboardTranslator.VK_LSHIFT, KeyboardTranslator.VK_ESCAPE})));
        // UI navigation action, so it does not wait for game focus
        actions.put(GameMenuLayout.ID_SEND_KEYS, action(GameMenuLayout.ID_SEND_KEYS,
                getString(R.string.game_menu_send_keys), false, () -> {
                    hideMenu();
                    showSpecialKeysMenu();
                }));
        actions.put(GameMenuLayout.ID_TOUCH_SENSITIVITY, action(GameMenuLayout.ID_TOUCH_SENSITIVITY,
                getString(R.string.game_menu_switch_touch_sensitivity_model), true, game::switchTouchSensitivity));

        return actions;
    }

    // Everything not pinned to the Quick Menu lives here, so no action can become unreachable
    private void showAdvancedMenu(GameInputDevice device) {
        List<String> quickItems = GameMenuLayout.getQuickItems(prefs());
        List<MenuOption> options = new ArrayList<>();
        for (Map.Entry<String, MenuOption> entry : buildActions().entrySet()) {
            if (!quickItems.contains(entry.getKey())) {
                options.add(entry.getValue());
            }
        }
        options.add(new MenuOption(getString(R.string.game_menu_customize), R.drawable.ic_gm_customize_menu, false,
                () -> showMenuEditor(device)));
        if (device != null) {
            options.addAll(device.getGameMenuOptions());
        }
        options.add(cancelOption());
        showMenuDialog(getString(R.string.game_menu_advanced), options.toArray(new MenuOption[0]));
    }

    // The editor is an in-stream dialog, so customizing never leaves the Game activity or ends the stream
    private void showMenuEditor(GameInputDevice device) {
        hideMenu();
        Context themedContext = new ContextThemeWrapper(dialogScreenContext, game.getApplicationInfo().theme);
        GameMenuEditor.show(themedContext, prefs(), () -> showMenu(device));
    }

    private void showServerCmd(ArrayList<String> serverCmds) {
        List<MenuOption> options = new ArrayList<>();

        AtomicInteger index = new AtomicInteger(0);
        for (String str : serverCmds) {
            final int finalI = index.getAndIncrement();
            options.add(new MenuOption("> " + str, true, () -> game.sendExecServerCmd(finalI)));
        };

        options.add(new MenuOption(getString(R.string.game_menu_cancel), null));

        showMenuDialog(getString(R.string.game_menu_server_cmd), options.toArray(new MenuOption[options.size()]));
    }

    public void showMenu(GameInputDevice device) {
        Map<String, MenuOption> actions = buildActions();
        List<MenuOption> options = new ArrayList<>();
        for (String id : GameMenuLayout.getQuickItems(prefs())) {
            MenuOption option = actions.get(id);
            if (option != null) {
                options.add(option);
            }
        }

        options.add(new MenuOption(getString(R.string.game_menu_advanced), R.drawable.ic_gm_more, true,
                () -> showAdvancedMenu(device)));
        options.add(cancelOption());

        showMenuDialog(getString(R.string.quick_menu_title), options.toArray(new MenuOption[0]));
    }

    @Override
    public void hideMenu() {
        if (currentDialog != null && currentDialog.isShowing()) {
            currentDialog.dismiss();
        }
        currentDialog = null;
    }

    @Override
    public boolean isMenuOpen() {
        return currentDialog != null && currentDialog.isShowing();
    }
}
