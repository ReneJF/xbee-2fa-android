package com.example.SDXbeta.activities;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import com.example.SDXbeta.PacketHelper;
import com.example.SDXbeta.R;
import com.example.xbee_i2r.InitializeDevice;
import com.example.xbee_i2r.TxRequest16;
import com.example.xbee_i2r.XBeeAddress16;
import com.example.xbee_i2r.XBeePacket;
import com.ftdi.j2xx.FT_Device;

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

    // Send an encrypted packet to the node with the token
    // The node will compare the token with the one it receives from the server
    public void onSubmitTokenClicked(View view) {
        editTextToken = (EditText) findViewById(R.id.editTextToken);

        try {
            byte[] result = PacketHelper.create2FATokenPacket(editTextToken.getText().toString(), deviceId, nodeId, nonceSelf, nonceNode, authKey);

            XBeeAddress16 destination = new XBeeAddress16(0xFF, 0xFF);

            TxRequest16 request = new TxRequest16(destination, PacketHelper.createPayload(result));
            XBeePacket packet = new XBeePacket(request.getFrameData());

            byte[] outData = PacketHelper.createOutData(packet);

            synchronized (ftDev) {
                if (ftDev.isOpen() == false) {
                    return;
                }

                ftDev.write(outData, outData.length);
            }
        }

        catch (Exception e) {
            e.printStackTrace();
        }
    }
}