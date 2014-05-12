package com.example.SDXbeta.activities;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import com.example.SDXbeta.AuthServer;
import com.example.SDXbeta.PacketHelper;
import com.example.SDXbeta.R;
import com.example.xbee_i2r.*;
import com.ftdi.j2xx.FT_Device;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

public class TFAToken extends Activity {
    EditText editTextToken;
    Toast toast;
    private String authKey;
    private String deviceId;
    private String nodeId;
    private String nonceSelf;
    private String nonceNode;
    private FT_Device ftDev;
    private BroadcastReceiver receiver;
    int[] responseData;

    protected void onStart() {
        super.onStart();

        IntentFilter filter = new IntentFilter(ReadService.TFA_AUTH_CLEARED);

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                responseData = intent.getIntArrayExtra("responseData");

//                // Convert integers to bytes
//                byte[] byteResponseData = new byte[responseData.length];
//                for (int i = 0; i < byteResponseData.length; i++) {
//                    byteResponseData[i] = (byte) responseData[i];
//                }

                if (responseData[2] == 0xFF && responseData[3] == 0xFF) {
                    toast = Toast.makeText(getBaseContext(), "2FA Authentication cleared!", Toast.LENGTH_LONG);

                    // Request the server for the list of files and their data
                    new RequestFilesTask().execute();
                }

                else {
                    toast = Toast.makeText(getBaseContext(), "An error occurred during 2FA authentication", Toast.LENGTH_SHORT);
                }

                toast.show();
            }
        };

        registerReceiver(receiver,filter);
    }

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

    private class RequestFilesTask extends AsyncTask<String, Void, String> {
        int statusCode;

        protected String doInBackground(String... params) {
            DefaultHttpClient httpClient = new AuthServer().getNewHttpClient();
            HttpGet httpGet = new HttpGet(AuthServer.SERVER_URL + "files");

            try {
                HttpResponse response = httpClient.execute(httpGet);
                statusCode = response.getStatusLine().getStatusCode();

                if (statusCode == 200) {
                    // Get response content
                    String line;
                    StringBuilder stringBuilder = new StringBuilder();
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));

                    while ((line = bufferedReader.readLine()) != null) {
                        stringBuilder.append(line);
                    }

                    return stringBuilder.toString();
                }
            } catch (ClientProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }

        protected void onPostExecute(String result) {
            if (result != null) {
                toast = Toast.makeText(getBaseContext(), "File data received", Toast.LENGTH_SHORT);
                toast.show();
//
                // Start new activity, passing it the details
                Intent intent = new Intent(getBaseContext(), TFAFiles.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                intent.putExtra("files", result);
//                intent.putExtra("deviceId", deviceId);
//                intent.putExtra("nodeId", xbeeNodeId);
//                intent.putExtra("nonceSelf", nonce);
//                intent.putExtra("nonceNode", nonceNode);
//
                startActivity(intent);
            }

            else {
                toast = Toast.makeText(getBaseContext(), "An error occurred", Toast.LENGTH_SHORT);
                toast.show();
            }
        }
    }
}