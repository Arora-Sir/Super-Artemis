package com.limelight;

import android.content.Context;

import com.limelight.actions.StreamActionHost;

// Adapts the running Game activity to StreamActionHost, so menus, the quick-action bar and gestures share one way to run actions
final class GameActionHost implements StreamActionHost {
    private final Game game;
    private final GameMenu menu;
    // The screen menus are shown on, which is an external display controller when streaming to another display
    private final Context screenContext;

    GameActionHost(Game game, GameMenu menu, Context screenContext) {
        this.game = game;
        this.menu = menu;
        this.screenContext = screenContext;
    }

    @Override
    public void disconnect() {
        game.disconnect();
    }

    @Override
    public void quit() {
        game.quit();
    }

    @Override
    public void uploadClipboard() {
        game.sendClipboard(true);
    }

    @Override
    public void fetchClipboard() {
        game.getClipboard(0);
    }

    @Override
    public void openServerCommands() {
        menu.openServerCommands();
    }

    @Override
    public void toggleKeyboard() {
        game.toggleKeyboard();
    }

    @Override
    public boolean isZoomEnabled() {
        return game.isZoomModeEnabled();
    }

    @Override
    public void toggleZoom() {
        game.toggleZoomMode();
    }

    // Rotating only makes sense when the stream is on this device's own screen
    @Override
    public boolean canRotateScreen() {
        return screenContext == game;
    }

    @Override
    public void rotateScreen() {
        game.rotateScreen();
    }

    @Override
    public boolean isMicrophoneAvailable() {
        return game.isMicrophoneAvailable();
    }

    @Override
    public boolean isMicrophoneStreaming() {
        return game.isMicrophoneStreaming();
    }

    @Override
    public void toggleMicrophone() {
        game.toggleMicrophoneCapture();
    }

    @Override
    public boolean canChangeMouseMode() {
        return game.allowChangeMouseMode;
    }

    @Override
    public void selectMouseMode() {
        game.selectMouseMode(screenContext);
    }

    @Override
    public void toggleHud() {
        game.toggleHUD();
    }

    @Override
    public void toggleFloatingButton() {
        game.toggleFloatingButtonVisibility();
    }

    @Override
    public void toggleKeyboardModel() {
        game.toggleKeyboardController();
    }

    @Override
    public boolean canShowVirtualController() {
        return !game.isOnExternalDisplay();
    }

    @Override
    public void toggleVirtualController() {
        game.toggleVirtualController();
    }

    @Override
    public void toggleFullKeyboard() {
        game.toggleFullKeyboard();
    }

    @Override
    public void sendKeys(short[] keys) {
        game.sendKeys(keys);
    }

    @Override
    public void openSpecialKeys() {
        menu.openSpecialKeys();
    }

    @Override
    public void switchTouchSensitivity() {
        game.switchTouchSensitivity();
    }
}
