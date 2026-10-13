package com.limelight.actions;

// NOTE: One thing a user can trigger during a stream, identified by a stable ID that menus, the bar and gestures store.
// NOTE: Availability, on/off state and the label are computed from the host each time, so a stale menu never shows a wrong label.
public final class StreamAction {
    public interface Condition {
        boolean test(StreamActionHost host);
    }

    public interface Runner {
        void run(StreamActionHost host);
    }

    private static final Condition ALWAYS = host -> true;
    private static final Condition NEVER = host -> false;

    public final String id;
    // Short name used in the editor and the bar, where the label cannot change with state
    public final int nameRes;
    public final int iconRes;
    // Ends the session, so menus keep it apart and draw it in red
    public final boolean destructive;
    // Sends input to the host, so it waits until the stream window has focus again
    public final boolean needsGameFocus;

    private final int labelRes;
    private final int activeLabelRes;
    private final Condition available;
    private final Condition active;
    private final Runner runner;

    private StreamAction(Builder builder) {
        this.id = builder.id;
        this.nameRes = builder.nameRes;
        this.iconRes = builder.iconRes;
        this.destructive = builder.destructive;
        this.needsGameFocus = builder.needsGameFocus;
        this.labelRes = builder.labelRes != 0 ? builder.labelRes : builder.nameRes;
        this.activeLabelRes = builder.activeLabelRes;
        this.available = builder.available;
        this.active = builder.active;
        this.runner = builder.runner;
    }

    public boolean isAvailable(StreamActionHost host) {
        return available.test(host);
    }

    public boolean isActive(StreamActionHost host) {
        return active.test(host);
    }

    // Toggles describe what tapping does next, for example "Mute microphone" while it is on
    public int labelRes(StreamActionHost host) {
        return activeLabelRes != 0 && isActive(host) ? activeLabelRes : labelRes;
    }

    public void run(StreamActionHost host) {
        runner.run(host);
    }

    static Builder builder(String id, int nameRes, int iconRes, Runner runner) {
        return new Builder(id, nameRes, iconRes, runner);
    }

    static final class Builder {
        private final String id;
        private final int nameRes;
        private final int iconRes;
        private final Runner runner;
        private int labelRes;
        private int activeLabelRes;
        private boolean destructive;
        private boolean needsGameFocus = true;
        private Condition available = ALWAYS;
        private Condition active = NEVER;

        private Builder(String id, int nameRes, int iconRes, Runner runner) {
            this.id = id;
            this.nameRes = nameRes;
            this.iconRes = iconRes;
            this.runner = runner;
        }

        Builder labels(int inactiveLabelRes, int activeLabelRes) {
            this.labelRes = inactiveLabelRes;
            this.activeLabelRes = activeLabelRes;
            return this;
        }

        Builder destructive() {
            this.destructive = true;
            this.needsGameFocus = false;
            return this;
        }

        Builder withoutGameFocus() {
            this.needsGameFocus = false;
            return this;
        }

        Builder availableWhen(Condition condition) {
            this.available = condition;
            return this;
        }

        Builder activeWhen(Condition condition) {
            this.active = condition;
            return this;
        }

        StreamAction build() {
            return new StreamAction(this);
        }
    }
}
