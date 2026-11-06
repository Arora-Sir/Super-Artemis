package com.limelight.session;

import android.content.Context;

import com.limelight.LimeLog;
import com.limelight.computers.ComputerDatabaseManager;
import com.limelight.nvstream.http.ComputerDetails;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;

// NOTE: One TCP check of the PC on its HTTP port over every address it is known by. It blocks, so it runs on a background thread.
// NOTE: The saved PC is read again on each call, because its addresses can change while a stream is down or on another network.
public final class HostProbe {
    private static final String TAG = "HostProbe";

    private HostProbe() {
    }

    public static boolean reachable(Context context, String host, int port, String pcUuid, int singleTimeoutMs, int perAddressTimeoutMs) {
        ComputerDetails details = null;
        if (pcUuid != null) {
            ComputerDatabaseManager db = new ComputerDatabaseManager(context.getApplicationContext());
            try {
                details = db.getComputerByUUID(pcUuid);
            }
            catch (RuntimeException e) {
                LimeLog.warning(TAG + ": no saved PC for " + pcUuid + ": " + e.getMessage());
            }
            finally {
                db.close();
            }
        }
        List<ComputerDetails.AddressTuple> addresses = ResumeAddresses.candidates(host, port, details);
        for (ComputerDetails.AddressTuple address : addresses) {
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(address.address, address.port), addresses.size() > 1 ? perAddressTimeoutMs : singleTimeoutMs);
                return true;
            }
            catch (IOException e) {
                // Try the next address
            }
        }
        return false;
    }
}
