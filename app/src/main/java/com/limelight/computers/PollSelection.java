package com.limelight.computers;

// NOTE: A PC is polled on every saved address at once, and the answer of the highest-precedence address wins (local, manual, remote, IPv6).
// NOTE: Waiting for a higher address for its whole connect timeout made a dead local address, left over from another network, hold up a working manual or Tailscale address for 3 seconds, or 5 when the PC was online before (Tab log 2026-11-21, emulator run 2026-11-22).
// NOTE: A lower address that has answered now wins once the higher ones have had a short grace period to answer too, so the nearby LAN address is still preferred whenever it is quick.
final class PollSelection {
    // NOTE: A live LAN address answers within about 100 ms. Anything slower than this loses to a lower address that has already answered.
    static final long GRACE_MS = 600L;

    static final int KEEP_WAITING = -1;
    static final int NONE_ANSWERED = -2;

    private PollSelection() {
    }

    // Index of the address to use, in precedence order, or one of the two constants above. answeredAtMs[i] is only read when answered[i] is true.
    static int choose(boolean[] complete, boolean[] answered, long[] answeredAtMs, long nowMs) {
        int firstPending = -1;
        for (int i = 0; i < complete.length; i++) {
            if (!complete[i]) {
                if (firstPending < 0) {
                    firstPending = i;
                }
            } else if (answered[i] && firstPending < 0) {
                // Every higher address has already finished without an answer
                return i;
            }
        }
        if (firstPending < 0) {
            return NONE_ANSWERED;
        }
        // Higher addresses are still pending. A lower one that answered long enough ago wins over them.
        for (int i = firstPending + 1; i < complete.length; i++) {
            if (complete[i] && answered[i] && nowMs - answeredAtMs[i] >= GRACE_MS) {
                return i;
            }
        }
        return KEEP_WAITING;
    }
}
