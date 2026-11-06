package com.limelight.session;

import static org.junit.Assert.assertEquals;

import com.limelight.nvstream.http.ComputerDetails;

import org.junit.Test;

import java.util.List;

// NOTE: The wait for the PC tries the address of the stream first, then every other saved address once, so a switch from Wi-Fi to mobile data can still find the PC by its VPN or remote address.
public class ResumeAddressesTest {
    @Test
    public void theStreamAddressComesFirstAndEachOtherAddressIsListedOnce() {
        ComputerDetails details = new ComputerDetails();
        details.localAddress = new ComputerDetails.AddressTuple("192.168.0.207", 47989);
        details.remoteAddress = new ComputerDetails.AddressTuple("100.91.122.7", 47989);
        details.manualAddress = new ComputerDetails.AddressTuple("192.168.0.207", 47989);
        details.ipv6Address = new ComputerDetails.AddressTuple("[2401:4900::1]", 47989);

        List<ComputerDetails.AddressTuple> found = ResumeAddresses.candidates("192.168.0.207", 47989, details);

        assertEquals(3, found.size());
        assertEquals("192.168.0.207", found.get(0).address);
        assertEquals("100.91.122.7", found.get(1).address);
        assertEquals("2401:4900::1", found.get(2).address);
    }

    @Test
    public void aMissingPcLeavesOnlyTheStreamAddress() {
        assertEquals(1, ResumeAddresses.candidates("10.0.0.5", 47989, null).size());
        assertEquals(0, ResumeAddresses.candidates(null, 47989, null).size());
    }
}
