package com.example.SDXbeta.activities;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import com.example.SDXbeta.R;
import com.example.SDXbeta.SimpleCrypto;
import com.example.xbee_i2r.InitializeDevice;
import com.example.xbee_i2r.TxRequest16;
import com.example.xbee_i2r.XBeeAddress16;
import com.example.xbee_i2r.XBeePacket;
import com.ftdi.j2xx.FT_Device;

/**
 * Created by sahil on 3/3/14.
 */
public class TFAToken extends Activity {
    EditText editTextToken;
    private String authKey;
    private String deviceId;
    private String nodeId;
    private String nonceSelf;
    private String nonceNode;
    private FT_Device ftDev;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tfa_token);

        // Get data from intent
        authKey = getIntent().getStringExtra("authKey");
        deviceId = getIntent().getStringExtra("deviceId");
        nodeId = getIntent().getStringExtra("nodeId");
        nonceSelf = getIntent().getStringExtra("nonceSelf");
        nonceNode = getIntent().getStringExtra("nonceNode");

        ftDev = InitializeDevice.getDevice();
    }

    public void onSubmitTokenClicked(View view) {
        editTextToken = (EditText) findViewById(R.id.editTextToken);

        // Send request to XBee with token
        // Token (2)
        // Device ID (8)
        // NodeId (2)
        // Nonce(android) (2)
        // Nonce(node) (2)
        // Timestamp (4)
        try {
            Short xNodeId = Short.parseShort(nodeId, 16);
            String timestamp = Long.toHexString(System.currentTimeMillis() / 1000);
            byte[] hexNonceSelf = SimpleCrypto.toByte(nonceSelf);
            byte[] hexNonceNode = SimpleCrypto.toByte(nonceNode);

            byte[] hexToken = SimpleCrypto.toByte(editTextToken.getText().toString()); // 2 bytes
            byte[] hexDeviceId = SimpleCrypto.toByte(deviceId); // 8 bytes
            byte[] hexNodeId = { (byte) ((xNodeId >> 8) & 0xFF), (byte) (xNodeId & 0xFF) }; // 2 bytes
            byte[] hexNonceXOR = { (byte) (hexNonceSelf[0] ^ hexNonceNode[0]), (byte) (hexNonceSelf[1] ^ hexNonceNode[1]) }; // 2 bytes
            byte[] hexTimestamp = SimpleCrypto.toByte(timestamp); // 4 bytes

            byte[] result = new byte[hexToken.length + hexDeviceId.length + hexNodeId.length + hexNonceXOR.length + hexTimestamp.length];

            System.arraycopy(hexToken, 0, result, 0, hexToken.length);
            System.arraycopy(hexDeviceId, 0, result, hexToken.length, hexDeviceId.length);
            System.arraycopy(hexNodeId, 0, result, hexToken.length + hexDeviceId.length, hexNodeId.length);
            System.arraycopy(hexNonceXOR, 0, result, hexToken.length + hexDeviceId.length + hexNodeId.length, hexNonceXOR.length);
            System.arraycopy(hexTimestamp, 0, result, hexToken.length + hexDeviceId.length + hexNodeId.length + hexNonceXOR.length, hexTimestamp.length);

            result = SimpleCrypto.encrypt(SimpleCrypto.toByte(authKey), result);

            int[] payload = new int[result.length];

            for (int i = 0; i < result.length; i++) {
                payload[i] = result[i];
            }

            XBeeAddress16 destination = new XBeeAddress16(0xFF, 0xFF);

            TxRequest16 request = new TxRequest16(destination, payload);
            XBeePacket packet = new XBeePacket(request.getFrameData());

            byte[] outData = new byte[packet.getIntegerArray().length];

            for(int k = 0; k < packet.getIntegerArray().length; k++){
                outData[k] = (byte) (packet.getIntegerArray()[k] & 0xff);
            }

            synchronized(ftDev){
                if(ftDev.isOpen() == false){
                    return;
                }

                ftDev.write(outData,outData.length);
            }
        }

        catch (Exception e) {
            e.printStackTrace();
        }
    }
}