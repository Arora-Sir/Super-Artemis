package com.limelight;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.widget.Toast;

import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;

import com.limelight.actions.StreamAction;
import com.limelight.actions.StreamActionHost;
import com.limelight.actions.StreamActionRegistry;
import com.limelight.binding.input.GameInputDevice;
import com.limelight.customkeys.CustomKey;
import com.limelight.customkeys.CustomKeyEditor;
import com.limelight.customkeys.CustomKeyStore;
import com.limelight.customkeys.SpecialKeys;
import com.limelight.customkeys.SpecialKeysManager;
import com.limelight.customkeys.IconCatalog;
import com.limelight.menu.GameMenuEditor;
import com.limelight.menu.GameMenuLayout;
import com.limelight.menu.GameMenuRenderer;
import com.limelight.menu.MenuEntry;
import com.limelight.menu.MenuSections;
import com.limelight.menu.QuickBarEditor;
import com.limelight.menu.QuickBarLayout;
import com.limelight.theme.ThemeManager;

import java.util.ArrayList;
import java.util.List;
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

    // Kept as the public type for menu items that input devices contribute to the Advanced Menu
    public static class MenuOption extends MenuEntry {
        public MenuOption(String label, int iconRes, boolean withGameFocus, Runnable runnable) {
            super(label, iconRes, false, withGameFocus, runnable);
        }

        public MenuOption(String label, boolean withGameFocus, Runnable runnable) {
            this(label, 0, withGameFocus, runnable);
        }

        public MenuOption(String label, Runnable runnable) {
            this(label, false, runnable);
        }
    }

    private final Game game;
    private final Context dialogScreenContext;
    private final GameActionHost actionHost;

    private AlertDialog currentDialog;

    public GameMenu(Game game, Context dialogScreenContext) {
        this.game = game;
        this.dialogScreenContext = dialogScreenContext;
        this.actionHost = new GameActionHost(game, this, dialogScreenContext);
    }

    public GameMenu(Game game) {
        this(game, game);
    }

    // The quick-action bar and gestures run actions through the same host as the menu
    public StreamActionHost getActionHost() {
        return actionHost;
    }

    private String getString(int id) {
        return game.getResources().getString(id);
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

    private void run(MenuEntry entry) {
        if (entry.runnable == null) {
            return;
        }

        Runnable safe = () -> com.limelight.ui.SafeRun.run(game, entry.label, entry.runnable);
        if (entry.withGameFocus) {
            runWithGameFocus(safe);
        } else {
            safe.run();
        }
        // Toggles such as the microphone also show their state on the quick-action bar
        game.refreshQuickActionBar();
    }

    // Runs an action exactly as a menu tap would, waiting for game focus when the action sends input
    public void runAction(StreamAction action) {
        // Toggles run from a gesture or a bar show their new state on the bars too, just like a menu tap
        Runnable runnable = () -> {
            com.limelight.ui.SafeRun.run(game, action.id, () -> action.run(actionHost));
            game.refreshQuickActionBar();
        };
        if (action.needsGameFocus) {
            runWithGameFocus(runnable);
        } else {
            runnable.run();
        }
    }

    // Menus follow the app style (Classic or Material You), not the stream activity's own theme
    private Context themedContext() {
        return ThemeManager.dialogContext(dialogScreenContext, game.getApplicationInfo().theme);
    }

    private void showMenuDialog(String title, List<MenuEntry> entries) {
        Context themedContext = themedContext();
        AlertDialog.Builder builder = new AlertDialog.Builder(themedContext);
        builder.setTitle(title);

        // Items run by position in one RecyclerView for every style, so two items with the same label never trigger each other
        RecyclerView menuView = GameMenuRenderer.create(themedContext, GameMenuLayout.getStyle(prefs()), entry -> {
            if (currentDialog != null) {
                currentDialog.dismiss();
            }
            run(entry);
        });
        builder.setView(menuView);

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
                        GameMenuRenderer.setEntries(menuView, entries);
                        GameMenuRenderer.focusFirstItem(menuView);
                    });
                }
            });
        }
    }

    private void showSpecialKeysMenu() {
        List<MenuEntry> options = new ArrayList<>();

        // Built-in and custom keys in the user's order, each with its icon. Hidden built-in keys stay out.
        try {
            CustomKeyStore.load(game);
        } catch (CustomKeyStore.FormatException e) {
            Toast.makeText(game,getString(R.string.wrong_import_format),Toast.LENGTH_SHORT).show();
        }
        for (CustomKey key : SpecialKeys.visible(game)) {
            options.add(new MenuOption(key.name, key.icon != null ? IconCatalog.drawableFor(key.icon) : 0, false,
                    () -> game.runKey(key)));
        }

        // Recording a key opens on top of the stream, and the list reopens with the new key when it is saved
        options.add(new MenuOption(getString(R.string.game_menu_add_custom_key), R.drawable.ic_gm_add, false,
                () -> CustomKeyEditor.show(themedContext(), null, key -> {
                    try {
                        CustomKeyStore.upsert(game, key);
                    } catch (CustomKeyStore.FormatException e) {
                        Toast.makeText(game, getString(R.string.wrong_import_format), Toast.LENGTH_SHORT).show();
                    }
                    showSpecialKeysMenu();
                })));
        options.add(new MenuOption(getString(R.string.game_menu_manage_custom_keys), R.drawable.ic_gm_edit, false,
                () -> SpecialKeysManager.show(themedContext(), this::showSpecialKeysMenu)));
        options.add(cancelEntry());

        showMenuDialog(getString(R.string.game_menu_send_keys), options);
    }

    private SharedPreferences prefs() {
        return PreferenceManager.getDefaultSharedPreferences(game);
    }

    private MenuEntry entryFor(StreamAction action) {
        return new MenuEntry(getString(action.labelRes(actionHost)), action.iconRes, action.destructive,
                action.needsGameFocus, () -> action.run(actionHost));
    }

    private MenuEntry cancelEntry() {
        return new MenuEntry(getString(R.string.game_menu_cancel), R.drawable.ic_gm_cancel, false, false, null);
    }

    void openSpecialKeys() {
        hideMenu();
        showSpecialKeysMenu();
    }

    void openServerCommands() {
        ArrayList<String> serverCmds = game.getServerCmds();
        if (serverCmds.isEmpty()) {
            new AlertDialog.Builder(themedContext())
                    .setTitle(R.string.game_dialog_title_server_cmd_empty)
                    .setMessage(R.string.game_dialog_message_server_cmd_empty)
                    .show();
        } else {
            hideMenu();
            this.showServerCmd(serverCmds);
        }
    }

    // NOTE: Everything not pinned to the Quick Menu lives here, so no action can become unreachable.
    // NOTE: Session actions are left out because the Quick Menu always shows them, and "Back to Quick Menu" leads there.
    private void showAdvancedMenu(GameInputDevice device) {
        List<String> quickItems = GameMenuLayout.getQuickItems(prefs());
        List<MenuEntry> actions = new ArrayList<>();
        for (StreamAction action : GameMenuLayout.configurableActions()) {
            if (!quickItems.contains(action.id) && action.isAvailable(actionHost)) {
                actions.add(entryFor(action));
            }
        }

        List<MenuEntry> deviceOptions = new ArrayList<>();
        if (device != null) {
            deviceOptions.addAll(device.getGameMenuOptions());
        }

        List<MenuEntry> navigation = new ArrayList<>();
        navigation.add(quickBarEditorEntry());
        navigation.add(new MenuEntry(getString(R.string.game_menu_back_to_quick_menu), R.drawable.ic_gm_back, false, false,
                () -> showMenu(device)));
        navigation.add(cancelEntry());

        showMenuDialog(getString(R.string.game_menu_advanced), new MenuSections(game.getResources())
                .add(R.string.game_menu_section_actions, actions)
                .add(R.string.game_menu_section_controller, deviceOptions)
                .add(R.string.game_menu_section_menu, navigation)
                .build());
    }

    // NOTE: Opened by a long press on a quick-action bar button. Every entry works on that button's corner.
    // NOTE: A key recorded or pinned here goes straight onto this bar and into Send special keys, so it is ready in both places.
    public void showQuickBarMenu(String corner, String buttonLabel) {
        String cornerName = getString(QuickBarEditor.cornerNameFor(corner));
        List<MenuEntry> entries = new ArrayList<>();
        entries.add(new MenuEntry(getString(R.string.quick_bar_menu_add_key), R.drawable.ic_gm_add, false, false,
                () -> CustomKeyEditor.show(themedContext(), null, key -> {
                    try {
                        CustomKeyStore.upsert(game, key);
                    } catch (CustomKeyStore.FormatException e) {
                        Toast.makeText(game, getString(R.string.wrong_import_format), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    pinToBar(corner, QuickBarLayout.customKeyItem(key.id));
                })));
        entries.add(new MenuEntry(getString(R.string.quick_bar_menu_pin_key), R.drawable.ic_gm_send_keys, false, false,
                () -> showPinKeyMenu(corner)));
        entries.add(new MenuEntry(getString(R.string.quick_bar_menu_edit_bar), R.drawable.ic_gm_toggle_floating_button, false, false,
                () -> QuickBarEditor.show(themedContext(), prefs(), game::reloadQuickActionBar, corner)));
        entries.add(new MenuEntry(getString(R.string.game_menu_manage_custom_keys), R.drawable.ic_gm_edit, false, false,
                () -> SpecialKeysManager.show(themedContext(), game::reloadQuickActionBar)));
        entries.add(cancelEntry());
        showMenuDialog(game.getString(R.string.quick_bar_menu_title, buttonLabel, cornerName), entries);
    }

    // Special keys not on this bar yet, each one tap away from being pinned
    private void showPinKeyMenu(String corner) {
        List<String> items = QuickBarLayout.getItems(prefs(), corner);
        List<MenuEntry> entries = new ArrayList<>();
        for (CustomKey key : SpecialKeys.visible(game)) {
            String item = QuickBarLayout.customKeyItem(key.id);
            if (!items.contains(item)) {
                entries.add(new MenuOption(key.name, key.icon != null ? IconCatalog.drawableFor(key.icon) : 0, false,
                        () -> pinToBar(corner, item)));
            }
        }
        entries.add(cancelEntry());
        showMenuDialog(getString(R.string.quick_bar_menu_pin_key), entries);
    }

    private void pinToBar(String corner, String item) {
        List<String> items = new ArrayList<>(QuickBarLayout.getItems(prefs(), corner));
        if (!items.contains(item)) {
            items.add(item);
            QuickBarLayout.setItems(prefs(), corner, items);
        }
        game.reloadQuickActionBar();
    }

    private MenuEntry quickBarEditorEntry() {
        return new MenuEntry(getString(R.string.game_menu_customize_quick_bar), R.drawable.ic_gm_toggle_floating_button, false, false,
                () -> {
                    hideMenu();
                    QuickBarEditor.show(themedContext(), prefs(), game::reloadQuickActionBar);
                });
    }

    // The editor is an in-stream dialog, so customizing never leaves the Game activity or ends the stream
    private void showMenuEditor(GameInputDevice device) {
        hideMenu();
        GameMenuEditor.show(themedContext(), prefs(), () -> showMenu(device));
    }

    private void showServerCmd(ArrayList<String> serverCmds) {
        List<MenuEntry> options = new ArrayList<>();

        AtomicInteger index = new AtomicInteger(0);
        for (String str : serverCmds) {
            final int finalI = index.getAndIncrement();
            options.add(new MenuOption("> " + str, true, () -> game.sendExecServerCmd(finalI)));
        };

        options.add(new MenuOption(getString(R.string.game_menu_cancel), null));

        showMenuDialog(getString(R.string.game_menu_server_cmd), options);
    }

    // Quick Menu sections: the user's pinned actions, then Disconnect and Quit in red, then menu navigation
    public void showMenu(GameInputDevice device) {
        List<MenuEntry> actions = new ArrayList<>();
        for (String id : GameMenuLayout.getQuickItems(prefs())) {
            StreamAction action = StreamActionRegistry.find(id);
            if (action != null && action.isAvailable(actionHost)) {
                actions.add(entryFor(action));
            }
        }

        List<MenuEntry> session = new ArrayList<>();
        for (StreamAction action : StreamActionRegistry.sessionActions()) {
            if (action.isAvailable(actionHost)) {
                session.add(entryFor(action));
            }
        }

        List<MenuEntry> navigation = new ArrayList<>();
        navigation.add(new MenuEntry(getString(R.string.game_menu_advanced), R.drawable.ic_gm_more, false, true,
                () -> showAdvancedMenu(device)));
        navigation.add(new MenuEntry(getString(R.string.game_menu_customize), R.drawable.ic_gm_customize_menu, false, false,
                () -> showMenuEditor(device)));
        // The corner bars are edited from here too, so changing them never needs the Advanced Menu or Settings
        navigation.add(quickBarEditorEntry());
        navigation.add(cancelEntry());

        showMenuDialog(getString(R.string.quick_menu_title), new MenuSections(game.getResources())
                .add(R.string.game_menu_section_actions, actions)
                .add(R.string.game_menu_section_session, session)
                .add(R.string.game_menu_section_menu, navigation)
                .build());
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
