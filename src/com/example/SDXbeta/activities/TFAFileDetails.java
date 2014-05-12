package com.example.SDXbeta.activities;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import com.example.SDXbeta.PacketHelper;
import com.example.SDXbeta.R;
import com.example.xbee_i2r.InitializeDevice;
import com.example.xbee_i2r.TxRequest16;
import com.example.xbee_i2r.XBeeAddress16;
import com.example.xbee_i2r.XBeePacket;
import com.ftdi.j2xx.FT_Device;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;

/**
 * Author: sahil
 * Date: 12/5/14
 */
public class TFAFileDetails extends Activity {
    FT_Device ftDev;
    String fileData;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tfa_file_details);

        ftDev = InitializeDevice.getDevice();

        try {
            JSONArray files = new JSONArray(getIntent().getStringExtra("files"));
            int position = getIntent().getIntExtra("position", 0);

            JSONObject fileObject = files.getJSONObject(position);

            TextView fileName = (TextView) findViewById(R.id.textViewFilename);
            TextView fileSize = (TextView) findViewById(R.id.textViewFilesize);
            TextView fileContents = (TextView) findViewById(R.id.textViewContents);

            fileName.setText(fileObject.getString("name"));
            fileSize.setText(fileObject.getString("size") + " bytes");

            fileData = fileObject.getString("data");
            fileContents.setText(fileData);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void onSendFilesClicked(View view) {
        Toast toast = Toast.makeText(getBaseContext(), fileData, Toast.LENGTH_SHORT);
        toast.show();
        byte numPackets;

        try {
            byte[] fileBytes = fileData.getBytes(Charset.forName("UTF-8"));
            byte[] result;

            if (fileBytes.length < 94) {
                result = new byte[fileBytes.length + 2];

                numPackets = 1;
                result[0] = numPackets;
                result[1] = (byte) fileBytes.length;

                for (int i = 0; i < fileBytes.length; i++) {
                    result[i + 2] = fileBytes[i];
                }

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

            else {
                result = new byte[96];

                numPackets = (byte) Math.ceil(fileBytes.length / 94.0);
                result[0] = numPackets;
                result[1] = (byte) fileBytes.length;
                int count = 0;

                for (int i = 0; i < 94; i++) {
                    result[i + 2] = fileBytes[i];
                }

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

                while (++count < numPackets) {
                    result = new byte[fileBytes.length - 94 < 96 ? fileBytes.length - 94 : 96];
                    for (int j = 0; j < fileBytes.length - 94; j++) {
                        result[j] = fileBytes[j + 94];
                    }

                    request = new TxRequest16(destination, PacketHelper.createPayload(result));
                    packet = new XBeePacket(request.getFrameData());

                    outData = PacketHelper.createOutData(packet);
                    Thread.sleep(1000);

                    synchronized (ftDev) {
                        if (ftDev.isOpen() == false) {
                            return;
                        }

                        ftDev.write(outData, outData.length);
                    }
                }
            }
        }

        catch (Exception e) {
            e.printStackTrace();
        }
    }
}