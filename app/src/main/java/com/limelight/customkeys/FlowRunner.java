package com.limelight.customkeys;

import com.limelight.binding.input.KeyboardTranslator;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

// NOTE: Runs the steps of a special key one after another on one background thread, so a shortcut, typed text and waits never overlap.
// NOTE: One key runs at a time. Every key a step holds down is released when the key ends, when it is cancelled, or when the stream stops.
// NOTE: The Sink is the stream connection, so the runner can be tested without one.
public final class FlowRunner {
    public interface Sink {
        void keyDown(short key, byte modifiers);

        void keyUp(short key, byte modifiers);

        void text(String text);

        // Runs a stream action on the main thread
        void action(String actionId);
    }

    public interface Sleeper {
        void sleep(long ms) throws InterruptedException;
    }

    // Same hold time as a shortcut from the menu, so a shortcut step behaves exactly like one
    static final long COMBO_HOLD_MS = 25;
    // Breathing room between steps, so apps on the PC see each step as its own input
    static final long STEP_GAP_MS = 60;
    // Text goes out in chunks every 15 ms, so typing waits about that long per chunk before the next step
    static final long TEXT_MS_PER_CHUNK = 15;
    static final int TEXT_CHUNK_BYTES = 512;

    private final Sink sink;
    private final Sleeper sleeper;
    private final ExecutorService executor;
    private final AtomicBoolean running = new AtomicBoolean();
    private final Deque<Short> held = new ArrayDeque<>();
    private volatile boolean cancelled;

    public FlowRunner(Sink sink) {
        this(sink, Thread::sleep, Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "SpecialKeyFlow");
            thread.setDaemon(true);
            return thread;
        }));
    }

    FlowRunner(Sink sink, Sleeper sleeper, ExecutorService executor) {
        this.sink = sink;
        this.sleeper = sleeper;
        this.executor = executor;
    }

    // Starts the key and returns true, or returns false when another key is still running
    public boolean run(CustomKey key) {
        if (!running.compareAndSet(false, true)) {
            return false;
        }
        cancelled = false;
        executor.execute(() -> {
            try {
                runSteps(key.steps);
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            finally {
                releaseHeld();
                running.set(false);
            }
        });
        return true;
    }

    public boolean isRunning() {
        return running.get();
    }

    // Stops after the current step and lets go of every held key, for example when the stream ends
    public void cancel() {
        cancelled = true;
    }

    public void shutdown() {
        cancel();
        executor.shutdown();
    }

    void runSteps(List<KeyStep> steps) throws InterruptedException {
        KeyStep previous = null;
        for (KeyStep step : steps) {
            if (cancelled) {
                return;
            }
            if (step.type == KeyStep.Type.REPEAT) {
                if (previous != null) {
                    for (int i = 0; i < step.amount && !cancelled; i++) {
                        runOne(previous);
                        sleeper.sleep(STEP_GAP_MS);
                    }
                }
                continue;
            }
            runOne(step);
            previous = step;
            sleeper.sleep(STEP_GAP_MS);
        }
    }

    private void runOne(KeyStep step) throws InterruptedException {
        switch (step.type) {
            case COMBO: {
                byte modifiers = heldModifiers();
                short[] codes = new short[step.keys.size()];
                for (int i = 0; i < codes.length; i++) {
                    codes[i] = (short) (int) step.keys.get(i);
                    sink.keyDown(codes[i], modifiers);
                    modifiers |= KeyboardTranslator.getModifier(codes[i]);
                }
                sleeper.sleep(COMBO_HOLD_MS);
                for (int i = codes.length - 1; i >= 0; i--) {
                    modifiers &= (byte) ~KeyboardTranslator.getModifier(codes[i]);
                    sink.keyUp(codes[i], modifiers);
                }
                break;
            }
            case TEXT: {
                sink.text(step.text);
                int bytes = step.text.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
                sleeper.sleep(((bytes / TEXT_CHUNK_BYTES) + 1) * TEXT_MS_PER_CHUNK);
                break;
            }
            case WAIT:
                sleeper.sleep(step.amount);
                break;
            case HOLD: {
                short key = (short) (int) step.keys.get(0);
                if (!held.contains(key)) {
                    sink.keyDown(key, heldModifiers());
                    held.push(key);
                }
                break;
            }
            case RELEASE: {
                short key = (short) (int) step.keys.get(0);
                if (held.remove(key)) {
                    sink.keyUp(key, heldModifiers());
                }
                break;
            }
            case ACTION:
                sink.action(step.text);
                break;
            default:
                break;
        }
    }

    private byte heldModifiers() {
        byte modifiers = 0;
        for (short key : held) {
            modifiers |= KeyboardTranslator.getModifier(key);
        }
        return modifiers;
    }

    // Most recent first, the same order a person lets go of keys
    private void releaseHeld() {
        while (!held.isEmpty()) {
            short key = held.pop();
            sink.keyUp(key, heldModifiers());
        }
    }
}
