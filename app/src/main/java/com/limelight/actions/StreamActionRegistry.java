package com.limelight.actions;

import com.limelight.R;
import com.limelight.binding.input.KeyboardTranslator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

// NOTE: The single catalog of stream actions. IDs are persisted in menu layouts, bar items and gesture bindings, so never rename one.
// NOTE: Catalog order is the order the Advanced Menu and the editors list actions in.
public final class StreamActionRegistry {
    public static final String ID_DISCONNECT = "disconnect";
    public static final String ID_QUIT = "quit";
    public static final String ID_UPLOAD_CLIPBOARD = "upload_clipboard";
    public static final String ID_FETCH_CLIPBOARD = "fetch_clipboard";
    public static final String ID_SERVER_CMD = "server_cmd";
    public static final String ID_TOGGLE_KEYBOARD = "toggle_keyboard";
    public static final String ID_ZOOM = "zoom";
    public static final String ID_ROTATE_SCREEN = "rotate_screen";
    public static final String ID_MICROPHONE = "microphone";
    public static final String ID_MOUSE_MODE = "mouse_mode";
    public static final String ID_TOGGLE_HUD = "toggle_hud";
    public static final String ID_TOGGLE_FLOATING_BUTTON = "toggle_floating_button";
    public static final String ID_TOGGLE_KEYBOARD_MODEL = "toggle_keyboard_model";
    public static final String ID_TOGGLE_VIRTUAL_CONTROLLER = "toggle_virtual_controller";
    public static final String ID_TOGGLE_FULL_KEYBOARD = "toggle_full_keyboard";
    public static final String ID_TASK_MANAGER = "task_manager";
    public static final String ID_SEND_KEYS = "send_keys";
    public static final String ID_TOUCH_SENSITIVITY = "touch_sensitivity";

    public static final List<StreamAction> ALL = Collections.unmodifiableList(Arrays.asList(
            StreamAction.builder(ID_DISCONNECT, R.string.game_menu_disconnect, R.drawable.ic_gm_disconnect,
                    StreamActionHost::disconnect).destructive().build(),
            StreamAction.builder(ID_QUIT, R.string.game_menu_quit_session, R.drawable.ic_gm_quit,
                    StreamActionHost::quit).destructive().build(),
            StreamAction.builder(ID_UPLOAD_CLIPBOARD, R.string.game_menu_upload_clipboard, R.drawable.ic_gm_upload_clipboard,
                    StreamActionHost::uploadClipboard).build(),
            StreamAction.builder(ID_FETCH_CLIPBOARD, R.string.game_menu_fetch_clipboard, R.drawable.ic_gm_fetch_clipboard,
                    StreamActionHost::fetchClipboard).build(),
            StreamAction.builder(ID_SERVER_CMD, R.string.game_menu_server_cmd, R.drawable.ic_gm_server_cmd,
                    StreamActionHost::openServerCommands).build(),
            StreamAction.builder(ID_TOGGLE_KEYBOARD, R.string.game_menu_toggle_keyboard, R.drawable.ic_gm_toggle_keyboard,
                    StreamActionHost::toggleKeyboard).build(),
            StreamAction.builder(ID_ZOOM, R.string.game_menu_item_zoom, R.drawable.ic_gm_zoom, StreamActionHost::toggleZoom)
                    .labels(R.string.game_menu_enable_zoom_mode, R.string.game_menu_disable_zoom_mode)
                    .activeWhen(StreamActionHost::isZoomEnabled).build(),
            StreamAction.builder(ID_ROTATE_SCREEN, R.string.game_menu_rotate_screen, R.drawable.ic_gm_rotate_screen,
                    StreamActionHost::rotateScreen).availableWhen(StreamActionHost::canRotateScreen).build(),
            // Based on the in-stream mic toggle idea by @ShadowLemoon (qiin2333/moonlight-vplus#314)
            // Only offered when the host negotiated the mic stream, so toggling just pauses or resumes local capture
            StreamAction.builder(ID_MICROPHONE, R.string.game_menu_item_microphone, R.drawable.ic_gm_microphone,
                    StreamActionHost::toggleMicrophone)
                    .labels(R.string.game_menu_unmute_microphone, R.string.game_menu_mute_microphone)
                    .availableWhen(StreamActionHost::isMicrophoneAvailable)
                    .activeWhen(StreamActionHost::isMicrophoneStreaming).build(),
            StreamAction.builder(ID_MOUSE_MODE, R.string.game_menu_select_mouse_mode, R.drawable.ic_gm_mouse_mode,
                    StreamActionHost::selectMouseMode).availableWhen(StreamActionHost::canChangeMouseMode).build(),
            StreamAction.builder(ID_TOGGLE_HUD, R.string.game_menu_toggle_hud, R.drawable.ic_gm_toggle_hud,
                    StreamActionHost::toggleHud).build(),
            StreamAction.builder(ID_TOGGLE_FLOATING_BUTTON, R.string.game_menu_toggle_floating_button, R.drawable.ic_gm_toggle_floating_button,
                    StreamActionHost::toggleFloatingButton).build(),
            StreamAction.builder(ID_TOGGLE_KEYBOARD_MODEL, R.string.game_menu_toggle_keyboard_model, R.drawable.ic_gm_toggle_keyboard_model,
                    StreamActionHost::toggleKeyboardModel).build(),
            StreamAction.builder(ID_TOGGLE_VIRTUAL_CONTROLLER, R.string.game_menu_toggle_virtual_model, R.drawable.ic_gm_toggle_virtual_controller,
                    StreamActionHost::toggleVirtualController).availableWhen(StreamActionHost::canShowVirtualController).build(),
            StreamAction.builder(ID_TOGGLE_FULL_KEYBOARD, R.string.game_menu_toggle_virtual_keyboard_model, R.drawable.ic_gm_toggle_full_keyboard,
                    StreamActionHost::toggleFullKeyboard).build(),
            StreamAction.builder(ID_TASK_MANAGER, R.string.game_menu_task_manager, R.drawable.ic_gm_task_manager,
                    host -> host.sendKeys(new short[]{KeyboardTranslator.VK_LCONTROL, KeyboardTranslator.VK_LSHIFT, KeyboardTranslator.VK_ESCAPE})).build(),
            // Opens another menu, so it does not wait for game focus
            StreamAction.builder(ID_SEND_KEYS, R.string.game_menu_send_keys, R.drawable.ic_gm_send_keys,
                    StreamActionHost::openSpecialKeys).withoutGameFocus().build(),
            StreamAction.builder(ID_TOUCH_SENSITIVITY, R.string.game_menu_switch_touch_sensitivity_model, R.drawable.ic_gm_touch_sensitivity,
                    StreamActionHost::switchTouchSensitivity).build()
    ));

    private StreamActionRegistry() {
    }

    public static StreamAction find(String id) {
        for (StreamAction action : ALL) {
            if (action.id.equals(id)) {
                return action;
            }
        }
        return null;
    }

    // Actions that end the session, which the Quick Menu always shows in its own section
    public static List<StreamAction> sessionActions() {
        List<StreamAction> actions = new ArrayList<>();
        for (StreamAction action : ALL) {
            if (action.destructive) {
                actions.add(action);
            }
        }
        return actions;
    }
}
