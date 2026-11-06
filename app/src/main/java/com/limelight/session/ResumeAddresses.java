package com.limelight.session;

import com.limelight.nvstream.http.ComputerDetails;

import java.util.ArrayList;
import java.util.List;

// NOTE: Every address the PC is known by, with the one the stream used first.
// NOTE: After a switch from Wi-Fi to mobile data the local address cannot be reached any more, but the remote or VPN address of the same PC can, so the wait for the PC tries all of them.
public final class ResumeAddresses {
    private ResumeAddresses() {
    }

    public static List<ComputerDetails.AddressTuple> candidates(String host, int port, ComputerDetails details) {
        List<ComputerDetails.AddressTuple> out = new ArrayList<>();
        if (host != null && !host.isEmpty() && port > 0) {
            out.add(new ComputerDetails.AddressTuple(host, port));
        }
        if (details != null) {
            addIfNew(out, details.localAddress);
            addIfNew(out, details.manualAddress);
            addIfNew(out, details.remoteAddress);
            addIfNew(out, details.ipv6Address);
        }
        return out;
    }

    private static void addIfNew(List<ComputerDetails.AddressTuple> out, ComputerDetails.AddressTuple tuple) {
        if (tuple != null && !out.contains(tuple)) {
            out.add(tuple);
        }
    }
}
