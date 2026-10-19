package com.limelight.actions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.limelight.R;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StreamActionRegistryTest {
    @Test
    public void idsAreUniqueAndEveryActionHasNameAndIcon() {
        Set<String> ids = new HashSet<>();
        for (StreamAction action : StreamActionRegistry.ALL) {
            assertTrue("duplicate id " + action.id, ids.add(action.id));
            assertNotEquals(action.id, 0, action.nameRes);
            assertNotEquals(action.id, 0, action.iconRes);
        }
    }

    @Test
    public void sessionActionsAreDisconnectThenQuit() {
        List<String> ids = new ArrayList<>();
        for (StreamAction action : StreamActionRegistry.sessionActions()) {
            assertTrue(action.destructive);
            assertFalse("session actions must not wait for game focus", action.needsGameFocus);
            ids.add(action.id);
        }
        assertEquals(Arrays.asList(StreamActionRegistry.ID_DISCONNECT, StreamActionRegistry.ID_QUIT), ids);
    }

    @Test
    public void findReturnsNullForUnknownIds() {
        assertNotNull(StreamActionRegistry.find(StreamActionRegistry.ID_ZOOM));
        assertNull(StreamActionRegistry.find("removed_action"));
    }

    @Test
    public void toggleLabelsFollowTheHostState() {
        FakeHost host = new FakeHost();
        StreamAction mic = StreamActionRegistry.find(StreamActionRegistry.ID_MICROPHONE);
        StreamAction zoom = StreamActionRegistry.find(StreamActionRegistry.ID_ZOOM);

        assertEquals(R.string.game_menu_unmute_microphone, mic.labelRes(host));
        assertEquals(R.string.game_menu_enable_zoom_mode, zoom.labelRes(host));
        host.micStreaming = true;
        host.zoom = true;
        assertEquals(R.string.game_menu_mute_microphone, mic.labelRes(host));
        assertEquals(R.string.game_menu_disable_zoom_mode, zoom.labelRes(host));
        assertTrue(mic.isActive(host));
    }

    @Test
    public void availabilityFollowsTheHost() {
        FakeHost host = new FakeHost();
        assertFalse(StreamActionRegistry.find(StreamActionRegistry.ID_MICROPHONE).isAvailable(host));
        assertFalse(StreamActionRegistry.find(StreamActionRegistry.ID_ROTATE_SCREEN).isAvailable(host));
        host.micAvailable = true;
        host.canRotate = true;
        assertTrue(StreamActionRegistry.find(StreamActionRegistry.ID_MICROPHONE).isAvailable(host));
        assertTrue(StreamActionRegistry.find(StreamActionRegistry.ID_ROTATE_SCREEN).isAvailable(host));
        assertTrue(StreamActionRegistry.find(StreamActionRegistry.ID_DISCONNECT).isAvailable(host));
    }

    @Test
    public void runningAnActionCallsTheHost() {
        FakeHost host = new FakeHost();
        StreamActionRegistry.find(StreamActionRegistry.ID_DISCONNECT).run(host);
        StreamActionRegistry.find(StreamActionRegistry.ID_TASK_MANAGER).run(host);
        assertEquals(Arrays.asList("disconnect", "keys:3"), host.calls);
    }

    @Test
    public void settingsRunsThroughTheHostAndIsNotASessionEnder() {
        FakeHost host = new FakeHost();
        StreamAction settings = StreamActionRegistry.find(StreamActionRegistry.ID_SETTINGS);
        settings.run(host);
        assertEquals(java.util.Collections.singletonList("settings"), host.calls);
        org.junit.Assert.assertFalse(settings.destructive);
    }

    private static final class FakeHost implements StreamActionHost {
        final List<String> calls = new ArrayList<>();
        boolean zoom;
        boolean canRotate;
        boolean micAvailable;
        boolean micStreaming;

        @Override public void disconnect() { calls.add("disconnect"); }
        @Override public void quit() { calls.add("quit"); }
        @Override public void uploadClipboard() { calls.add("upload"); }
        @Override public void fetchClipboard() { calls.add("fetch"); }
        @Override public void openServerCommands() { calls.add("servercmd"); }
        @Override public void toggleKeyboard() { calls.add("keyboard"); }
        @Override public boolean isZoomEnabled() { return zoom; }
        @Override public void toggleZoom() { calls.add("zoom"); }
        @Override public boolean canRotateScreen() { return canRotate; }
        @Override public void rotateScreen() { calls.add("rotate"); }
        @Override public boolean isMicrophoneAvailable() { return micAvailable; }
        @Override public boolean isMicrophoneStreaming() { return micStreaming; }
        @Override public void toggleMicrophone() { calls.add("mic"); }
        @Override public boolean canChangeMouseMode() { return true; }
        @Override public void selectMouseMode() { calls.add("mouse"); }
        @Override public void toggleHud() { calls.add("hud"); }
        @Override public void toggleFloatingButton() { calls.add("floating"); }
        @Override public void toggleKeyboardModel() { calls.add("keyboardmodel"); }
        @Override public boolean canShowVirtualController() { return true; }
        @Override public void toggleVirtualController() { calls.add("controller"); }
        @Override public void toggleFullKeyboard() { calls.add("fullkeyboard"); }
        @Override public void sendKeys(short[] keys) { calls.add("keys:" + keys.length); }
        @Override public void openSpecialKeys() { calls.add("specialkeys"); }
        @Override public void switchTouchSensitivity() { calls.add("sensitivity"); }
        @Override public void openSettings() { calls.add("settings"); }
    }
}
