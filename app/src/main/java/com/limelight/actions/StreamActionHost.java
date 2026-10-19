package com.limelight.actions;

// NOTE: Everything a stream action can ask of the running stream, so actions never depend on the Game activity directly.
// NOTE: The in-stream menu, the quick-action bar and gestures all run actions through one implementation of this interface.
public interface StreamActionHost {
    void disconnect();

    void quit();

    void uploadClipboard();

    void fetchClipboard();

    void openServerCommands();

    void toggleKeyboard();

    boolean isZoomEnabled();

    void toggleZoom();

    boolean canRotateScreen();

    void rotateScreen();

    boolean isMicrophoneAvailable();

    boolean isMicrophoneStreaming();

    void toggleMicrophone();

    boolean canChangeMouseMode();

    void selectMouseMode();

    void toggleHud();

    void toggleFloatingButton();

    void toggleKeyboardModel();

    boolean canShowVirtualController();

    void toggleVirtualController();

    void toggleFullKeyboard();

    void sendKeys(short[] keys);

    void openSpecialKeys();

    void switchTouchSensitivity();

    // Opens Settings and reconnects to the same app when Settings closes
    void openSettings();
}
