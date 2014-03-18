package com.example.SDXbeta;

import com.example.xbee_i2r.XBeePacket;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class PacketHelper {

    public static int[] createPayload(byte[] packet) {
        int[] payload = new int[packet.length];

        for (int i = 0; i < packet.length; i++) {
            payload[i] = packet[i];
        }

        return payload;
    }

    public static byte[] createOutData(XBeePacket packet) {
        byte[] outData = new byte[packet.getIntegerArray().length];
        int[] packetIntArray = packet.getIntegerArray();

        for(int k = 0; k < packetIntArray.length; k++){
            outData[k] = (byte) (packetIntArray[k] & 0xff);
        }

        return outData;
    }

    // Create a packet for requesting a node for 2FA
    // Request format:
    // * [2 bytes] Nonce
    // * [8 bytes] IMEI/device ID
    // * [2 bytes] NodeId
    // * [4 bytes] timestamp
    // Total: 16 bytes
    public static byte[] create2FARequestPacket(String nonce, String deviceId, String xbeeNodeId, String authKey) {

        // Convert the node Id to four characters (zero-pad if necessary) so that it can be easily converted to 2 bytes
        xbeeNodeId = String.format("%04d", Integer.parseInt(xbeeNodeId, 16));

        // Get the current time
        String timestamp = Long.toHexString(System.currentTimeMillis() / 1000);;

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        try {
            byteArrayOutputStream.write(SimpleCrypto.toByte(nonce)); // 2 bytes
            byteArrayOutputStream.write(SimpleCrypto.toByte(deviceId)); // 8 bytes
            byteArrayOutputStream.write(SimpleCrypto.toByte(xbeeNodeId)); // 2 bytes
            byteArrayOutputStream.write(SimpleCrypto.toByte(timestamp)); // 4 bytes

            byte[] result = byteArrayOutputStream.toByteArray();

            result = SimpleCrypto.encrypt(SimpleCrypto.toByte(authKey), result);

            return result;

        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    // Create a packet with the token received from the server
    // Format:
    // * [2 bytes] Token
    // * [8 bytes] Device ID
    // * [2 bytes] NodeId
    // * [2 bytes] Nonce(android) XOR Nonce(node)
    // * [4 bytes] Timestamp
    // Total: 18 bytes
    public static byte[] create2FATokenPacket(String token, String deviceId, String xbeeNodeId, String nonceSelf, String nonceNode, String authKey) {

        // Convert the node Id to four characters (zero-pad if necessary) so that it can be easily converted to 2 bytes
        xbeeNodeId = String.format("%04d", Integer.parseInt(xbeeNodeId, 16));

        // Get the current time
        String timestamp = Long.toHexString(System.currentTimeMillis() / 1000);;

        byte[] hexNonceSelf = SimpleCrypto.toByte(nonceSelf);
        byte[] hexNonceNode = SimpleCrypto.toByte(nonceNode);

        byte[] hexNonceXOR = { (byte) (hexNonceSelf[0] ^ hexNonceNode[0]), (byte) (hexNonceSelf[1] ^ hexNonceNode[1]) };

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(32);

        try {
            byteArrayOutputStream.write(SimpleCrypto.toByte(token)); // 2 bytes
            byteArrayOutputStream.write(SimpleCrypto.toByte(deviceId)); // 8 bytes
            byteArrayOutputStream.write(SimpleCrypto.toByte(xbeeNodeId)); // 2 bytes
            byteArrayOutputStream.write(hexNonceXOR); // 2 bytes
            byteArrayOutputStream.write(SimpleCrypto.toByte(timestamp)); // 4 bytes

            byteArrayOutputStream.write(new byte[32 - byteArrayOutputStream.size()]); // for padding

            byte[] result = byteArrayOutputStream.toByteArray();

            result = SimpleCrypto.encrypt(authKey, result);

            return result;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public static Boolean isValidPacket(byte[] byteResponseData, String xbeeNodeId, String deviceId, String nonce) {

        try {
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
