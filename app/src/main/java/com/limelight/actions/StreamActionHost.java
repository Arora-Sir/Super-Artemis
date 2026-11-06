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

    // Hides the quick bars until the same action shows them again, touch does not bring them back
    void toggleQuickBars();

    void toggleKeyboardModel();

    boolean canShowVirtualController();

    void toggleVirtualController();

    void toggleFullKeyboard();

    void sendKeys(short[] keys);

    void openSpecialKeys();

    void switchTouchSensitivity();

    // A picker for the Quick Menu style (list, grid or compact), applied the next time the menu opens
    void chooseMenuStyle();

    // A picker for the look of the quick bars (dock rail, round buttons or tap to expand), applied at once
    void chooseQuickBarStyle();

    // The drag list that orders the sections of the Quick Menu
    void openMenuSectionOrder();

    // Opens Settings and reconnects to the same app when Settings closes
    void openSettings();
}
