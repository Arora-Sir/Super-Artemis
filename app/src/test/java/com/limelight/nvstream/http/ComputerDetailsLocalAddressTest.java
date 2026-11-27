package com.limelight.nvstream.http;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ComputerDetailsLocalAddressTest {
    private static ComputerDetails withLocal(String address) {
        ComputerDetails details = new ComputerDetails();
        if (address != null) {
            details.localAddress = new ComputerDetails.AddressTuple(address, 47989);
        }
        return details;
    }

    private static String localAfterUpdate(String saved, String reported) {
        ComputerDetails details = withLocal(saved);
        details.update(withLocal(reported));
        return details.localAddress.address;
    }

    @Test
    public void aTailscaleAnswerKeepsTheSavedHomeAddress() {
        assertEquals("192.168.50.10", localAfterUpdate("192.168.50.10", "100.100.20.30"));
        assertEquals("192.168.50.10", localAfterUpdate("192.168.50.10", "100.64.0.1"));
        assertEquals("192.168.50.10", localAfterUpdate("192.168.50.10", "100.127.255.254"));
        assertEquals("192.168.50.10", localAfterUpdate("192.168.50.10", "fd7a:115c:a1e0::1"));
    }

    @Test
    public void aTailscaleAnswerIsStoredWhenNothingBetterIsSaved() {
        assertEquals("100.100.20.30", localAfterUpdate(null, "100.100.20.30"));
        assertEquals("100.100.20.31", localAfterUpdate("100.100.20.30", "100.100.20.31"));
    }

    @Test
    public void aHomeAnswerReplacesTheSavedAddress() {
        assertEquals("192.168.60.11", localAfterUpdate("192.168.50.10", "192.168.60.11"));
        assertEquals("192.168.50.10", localAfterUpdate("100.100.20.30", "192.168.50.10"));
    }

    @Test
    public void addressesOutsideTheTailscaleRangeAreNotHeldBack() {
        assertEquals("100.63.0.1", localAfterUpdate("192.168.50.10", "100.63.0.1"));
        assertEquals("100.128.0.1", localAfterUpdate("192.168.50.10", "100.128.0.1"));
        assertEquals("100.example.lan", localAfterUpdate("192.168.50.10", "100.example.lan"));
    }
}
