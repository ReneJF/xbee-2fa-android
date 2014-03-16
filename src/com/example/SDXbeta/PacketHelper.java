package com.example.SDXbeta;

import android.widget.Toast;

/**
 * Created by sahil on 10/3/14.
 */
public class PacketHelper {

    public static Boolean isValidPacket(byte[] byteResponseData, String xbeeNodeId, String deviceId, String nonce) {

        try {
            // Decrypt data
//                    byteResponseData = SimpleCrypto.decrypt(SimpleCrypto.toByte(authKey), byteResponseData);

            Boolean verified = true;

            // Get node id and compare
            byte[] hexNodeId = { byteResponseData[0], byteResponseData[1] };
            String nodeId = SimpleCrypto.toHex(hexNodeId);
            nodeId = nodeId.replaceAll("^0+", "");

            if (!xbeeNodeId.toUpperCase().equals(nodeId)) {
                verified = false;
            }

            // Get Device ID and compare
            byte[] hexDeviceId = new byte[8];
            for (int i = 0; i < hexDeviceId.length; i++) {
                hexDeviceId[i] = byteResponseData[i + 2];
            }

            if (!deviceId.toUpperCase().equals(SimpleCrypto.toHex(hexDeviceId))) {
                verified = false;
            }

            // Get nonce and compare
            byte[] hexNonce = { byteResponseData[12], byteResponseData[13] };

            if (!nonce.toUpperCase().equals(SimpleCrypto.toHex(hexNonce))) {
                verified = false;
            }

            return verified;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }
}
