package com.limelight.customkeys;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.limelight.binding.input.KeyboardTranslator;
import com.limelight.shadows.ShadowMoonBridge;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.TimeUnit;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, shadows = {ShadowMoonBridge.class})
public class FlowStepsTest {
    private static final int CTRL = 0xA2;
    private static final int SHIFT = 0xA0;
    private static final int KEY_C = 0x43;
    private static final int KEY_V = 0x56;

    // Runs each task on the calling thread, so a flow finishes before run() returns
    private static final class DirectExecutor extends AbstractExecutorService {
        private boolean shutdown;

        @Override
        public void execute(Runnable command) {
            command.run();
        }

        @Override
        public void shutdown() {
            shutdown = true;
        }

        @Override
        public List<Runnable> shutdownNow() {
            shutdown = true;
            return Collections.emptyList();
        }

        @Override
        public boolean isShutdown() {
            return shutdown;
        }

        @Override
        public boolean isTerminated() {
            return shutdown;
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) {
            return true;
        }
    }

    // Records every event in order, with the sleeps in between
    private static final class Recorder implements FlowRunner.Sink, FlowRunner.Sleeper {
        final List<String> events = new ArrayList<>();
        Runnable onSleep;

        @Override
        public void keyDown(short key, byte modifiers) {
            events.add(String.format("down %02X m%d", key, modifiers));
        }

        @Override
        public void keyUp(short key, byte modifiers) {
            events.add(String.format("up %02X m%d", key, modifiers));
        }

        @Override
        public void text(String text) {
            events.add("text " + text);
        }

        @Override
        public void action(String actionId) {
            events.add("action " + actionId);
        }

        @Override
        public void sleep(long ms) {
            events.add("sleep " + ms);
            if (onSleep != null) {
                onSleep.run();
            }
        }
    }

    private static CustomKey flow(KeyStep... steps) {
        return CustomKey.withSteps("f", "Flow", false, null, Arrays.asList(steps));
    }

    @Test
    public void stepsRoundTripThroughJson() throws Exception {
        CustomKey key = flow(KeyStep.combo(Arrays.asList(CTRL, KEY_C)), KeyStep.text("héllo 👋"), KeyStep.waitMs(250),
                KeyStep.hold(SHIFT), KeyStep.release(SHIFT), KeyStep.repeat(3), KeyStep.action("pan_zoom"));

        List<CustomKey> loaded = CustomKeyStore.parse(CustomKeyStore.toJson(Collections.singletonList(key)));

        assertEquals(1, loaded.size());
        assertEquals(key.steps, loaded.get(0).steps);
        // Older versions read keys, which is the first shortcut of the flow
        assertEquals(Arrays.asList(CTRL, KEY_C), loaded.get(0).keys);
        assertFalse(loaded.get(0).isSingleCombo());
    }

    @Test
    public void oldKeyWithoutStepsReadsAsOneShortcut() throws Exception {
        List<CustomKey> loaded = CustomKeyStore.parse("{\"data\":[{\"id\":\"k1\",\"name\":\"Copy\",\"sticky\":true,\"keys\":[\"0xA2\",\"0x43\"]}]}");

        CustomKey key = loaded.get(0);
        assertTrue(key.isSingleCombo());
        assertEquals(Collections.singletonList(KeyStep.combo(Arrays.asList(CTRL, KEY_C))), key.steps);
        assertTrue(key.sticky);
    }

    @Test
    public void singleShortcutIsSavedWithoutSteps() {
        String json = CustomKeyStore.toJson(Collections.singletonList(new CustomKey("k1", "Copy", false, null, Arrays.asList(CTRL, KEY_C))));

        assertFalse(json.contains("steps"));
    }

    @Test
    public void unknownStepTypesAreSkippedAndBrokenStepsRejected() throws Exception {
        List<CustomKey> loaded = CustomKeyStore.parse("{\"data\":[{\"id\":\"k1\",\"name\":\"F\",\"keys\":[\"0x43\"],"
                + "\"steps\":[{\"type\":\"teleport\"},{\"type\":\"text\",\"text\":\"hi\"}]}]}");
        assertEquals(Collections.singletonList(KeyStep.text("hi")), loaded.get(0).steps);

        try {
            CustomKeyStore.parse("{\"data\":[{\"id\":\"k1\",\"name\":\"F\",\"keys\":[\"0x43\"],\"steps\":[{\"type\":\"wait\"}]}]}");
            fail("A wait step without milliseconds must be rejected");
        }
        catch (CustomKeyStore.FormatException expected) {
            // expected
        }
    }

    @Test
    public void stickyOnlyStaysForASingleShortcut() {
        assertFalse(CustomKey.withSteps("f", "F", true, null, Arrays.asList(KeyStep.combo(Collections.singletonList(KEY_C)),
                KeyStep.text("x"))).sticky);
        assertTrue(CustomKey.withSteps("f", "F", true, null, Collections.singletonList(KeyStep.combo(Collections.singletonList(KEY_C)))).sticky);
    }

    @Test
    public void valuesAreClamped() {
        assertEquals(KeyStep.WAIT_MIN_MS, KeyStep.waitMs(0).amount);
        assertEquals(KeyStep.WAIT_MAX_MS, KeyStep.waitMs(999_999).amount);
        assertEquals(KeyStep.REPEAT_MAX, KeyStep.repeat(500).amount);
        assertEquals(1, KeyStep.repeat(0).amount);
        assertEquals(KeyStep.TEXT_MAX, KeyStep.text(new String(new char[2000]).replace('\0', 'a')).text.length());
    }

    @Test
    public void runnerRunsStepsInOrderWithModifiersAndGaps() {
        Recorder recorder = new Recorder();
        FlowRunner runner = new FlowRunner(recorder, recorder, new DirectExecutor());
        byte ctrl = KeyboardTranslator.getModifier((short) CTRL);

        assertTrue(runner.run(flow(KeyStep.combo(Arrays.asList(CTRL, KEY_V)), KeyStep.text("hi"), KeyStep.waitMs(300),
                KeyStep.action("mic"))));

        assertEquals(Arrays.asList(
                "down A2 m0", String.format("down 56 m%d", ctrl), "sleep " + FlowRunner.COMBO_HOLD_MS,
                String.format("up 56 m%d", ctrl), "up A2 m0", "sleep " + FlowRunner.STEP_GAP_MS,
                "text hi", "sleep " + FlowRunner.TEXT_MS_PER_CHUNK, "sleep " + FlowRunner.STEP_GAP_MS,
                "sleep 300", "sleep " + FlowRunner.STEP_GAP_MS,
                "action mic", "sleep " + FlowRunner.STEP_GAP_MS), recorder.events);
        assertFalse(runner.isRunning());
    }

    @Test
    public void repeatRunsThePreviousStepAgain() {
        Recorder recorder = new Recorder();
        FlowRunner runner = new FlowRunner(recorder, recorder, new DirectExecutor());

        runner.run(flow(KeyStep.text("a"), KeyStep.repeat(2)));

        assertEquals(3, Collections.frequency(recorder.events, "text a"));
    }

    @Test
    public void heldKeysAddTheirModifierAndAreReleasedAtTheEnd() {
        Recorder recorder = new Recorder();
        FlowRunner runner = new FlowRunner(recorder, recorder, new DirectExecutor());
        byte shift = KeyboardTranslator.getModifier((short) SHIFT);

        runner.run(flow(KeyStep.hold(SHIFT), KeyStep.combo(Collections.singletonList(KEY_C))));

        assertEquals("down A0 m0", recorder.events.get(0));
        assertTrue(recorder.events.contains(String.format("down 43 m%d", shift)));
        // The flow never released Shift, so the runner lets go of it last
        assertEquals("up A0 m0", recorder.events.get(recorder.events.size() - 1));
    }

    @Test
    public void cancelStopsAfterTheCurrentStepAndReleasesHeldKeys() {
        Recorder recorder = new Recorder();
        FlowRunner runner = new FlowRunner(recorder, recorder, new DirectExecutor());
        recorder.onSleep = runner::cancel;

        runner.run(flow(KeyStep.hold(SHIFT), KeyStep.text("never typed")));

        assertFalse(recorder.events.contains("text never typed"));
        assertEquals("up A0 m0", recorder.events.get(recorder.events.size() - 1));
    }

    @Test
    public void releaseWithoutHoldSendsNothing() {
        Recorder recorder = new Recorder();
        FlowRunner runner = new FlowRunner(recorder, recorder, new DirectExecutor());

        runner.run(flow(KeyStep.release(SHIFT)));

        assertNull(recorder.events.stream().filter(e -> e.startsWith("up")).findFirst().orElse(null));
    }
}
