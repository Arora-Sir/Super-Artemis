package com.limelight.backup;

import android.content.Context;

import com.limelight.computers.ComputerDatabaseManager;
import com.limelight.nvstream.http.ComputerDetails;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.Set;

// Converts the paired PC rows of computers4.db to and from JSON
final class HostSnapshot {
    private HostSnapshot() {
    }

    static JSONArray capture(Context context) throws JSONException, IOException {
        JSONArray json = new JSONArray();
        ComputerDatabaseManager db = new ComputerDatabaseManager(context);
        try {
            for (ComputerDetails details : db.getAllComputers()) {
                json.put(encode(details));
            }
        }
        finally {
            db.close();
        }
        return json;
    }

    static void validate(JSONArray hostsJson) throws JSONException, InvalidBackupException {
        for (int i = 0; i < hostsJson.length(); i++) {
            decode(hostsJson.getJSONObject(i));
        }
    }

    static Set<String> localUuids(Context context) {
        Set<String> uuids = new HashSet<>();
        ComputerDatabaseManager db = new ComputerDatabaseManager(context);
        try {
            for (ComputerDetails details : db.getAllComputers()) {
                uuids.add(details.uuid);
            }
        }
        finally {
            db.close();
        }
        return uuids;
    }

    static JSONObject encode(ComputerDetails details) throws JSONException, IOException {
        JSONObject json = new JSONObject();
        json.put("uuid", details.uuid);
        json.put("name", details.name);

        JSONObject addresses = new JSONObject();
        putTuple(addresses, "local", details.localAddress);
        putTuple(addresses, "remote", details.remoteAddress);
        putTuple(addresses, "manual", details.manualAddress);
        putTuple(addresses, "ipv6", details.ipv6Address);
        json.put("addresses", addresses);

        if (details.macAddress != null) {
            json.put("macAddress", details.macAddress);
        }
        if (details.serverCert != null) {
            try {
                json.put("serverCert", BackupIo.encodeBase64(details.serverCert.getEncoded()));
            }
            catch (CertificateEncodingException e) {
                throw new IOException("Unable to encode server certificate for " + details.name);
            }
        }
        return json;
    }

    static ComputerDetails decode(JSONObject json) throws JSONException, InvalidBackupException {
        ComputerDetails details = new ComputerDetails();
        details.uuid = json.getString("uuid");
        details.name = json.getString("name");
        if (details.uuid.isEmpty()) {
            throw new InvalidBackupException("Host without UUID");
        }

        JSONObject addresses = json.getJSONObject("addresses");
        details.localAddress = ComputerDatabaseManager.tupleFromJson(addresses, "local");
        details.remoteAddress = ComputerDatabaseManager.tupleFromJson(addresses, "remote");
        details.manualAddress = ComputerDatabaseManager.tupleFromJson(addresses, "manual");
        details.ipv6Address = ComputerDatabaseManager.tupleFromJson(addresses, "ipv6");
        details.macAddress = json.has("macAddress") ? json.getString("macAddress") : null;

        if (json.has("serverCert")) {
            details.serverCert = decodeCertificate(BackupIo.decodeBase64(json.getString("serverCert")));
        }
        return details;
    }

    static X509Certificate decodeCertificate(byte[] data) throws InvalidBackupException {
        try {
            return (X509Certificate) CertificateFactory.getInstance("X.509")
                    .generateCertificate(new ByteArrayInputStream(data));
        }
        catch (CertificateException | ClassCastException e) {
            throw new InvalidBackupException("Invalid certificate");
        }
    }

    private static void putTuple(JSONObject addresses, String name, ComputerDetails.AddressTuple tuple) throws JSONException {
        JSONObject json = ComputerDatabaseManager.tupleToJson(tuple);
        if (json != null) {
            addresses.put(name, json);
        }
    }
}
