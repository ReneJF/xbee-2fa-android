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
import com.example.SDXbeta.SimpleCrypto;
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
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by sahil on 3/3/14.
 */
public class TFARequest extends Activity {
    EditText editTextNodeId;
    String username;
    String password;
    Toast toast;
    private FT_Device ftDev;
    private BroadcastReceiver receiver;
    private String authKey;
    private String xbeeNodeId;
    private String deviceId;
    private String nonceNode;
    private String nonce;
    private DefaultHttpClient httpClient;
    int[] responseData;

    protected void onStart() {
        super.onStart();

        IntentFilter filter = new IntentFilter(ReadService.TFA_REQUEST_ACTION);

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                responseData = intent.getIntArrayExtra("responseData");

                // Convert integers to bytes
                byte[] byteResponseData = new byte[responseData.length];
                for (int i = 0; i < byteResponseData.length; i++) {
                    byteResponseData[i] = (byte) responseData[i];
                }

                // Get Fio nonce
                byte[] hexFioNonce = { byteResponseData[10], byteResponseData[11] };
                nonceNode = SimpleCrypto.toHex(hexFioNonce);

                // Get timestamp
                byte[] hexTimestamp = { byteResponseData[14], byteResponseData[15], byteResponseData[16], byteResponseData[17] };

                // If verified is still true, go ahead
                if (PacketHelper.isValidPacket(byteResponseData, xbeeNodeId, deviceId, nonce)) {
                    toast = Toast.makeText(getBaseContext(), "Verified by node, requesting server for 2FA key", Toast.LENGTH_LONG);

                    // Request for 2FA token
                    new RequestTokenTask().execute(AuthServer.SERVER_URL + "token-requests");
                }

                else {
                    toast = Toast.makeText(getBaseContext(), "Node verification failed", Toast.LENGTH_SHORT);
                }

                toast.show();
            }
        };

        registerReceiver(receiver,filter);
    }

    @Override
    protected void onStop(){
        super.onStop();
        unregisterReceiver(receiver);
    }


    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tfa_request);

        // Get username/password from intent (sent by Login activity)
        username = getIntent().getStringExtra("username");
        password = getIntent().getStringExtra("password");

        // Create an HttpClient
        httpClient = new AuthServer().getNewHttpClient(username, password);

        ftDev = InitializeDevice.getDevice();
    }

    public void onRequestKeyClicked(View view) {
        editTextNodeId = (EditText) findViewById(R.id.editTextNodeId);
        xbeeNodeId = editTextNodeId.getText().toString();
        new RequestKeyTask().execute(AuthServer.SERVER_URL + "keys/" + xbeeNodeId);
    }

    public void onRequest2FAClicked(View view) {
        // Request format (encrypted)
        // Nonce
        // IMEI/device ID
        // NodeId
        // timestamp

        try {
            nonce = Short.toString((short) Math.floor(Math.random() * Short.MAX_VALUE));
            deviceId = android.provider.Settings.Secure.getString(getContentResolver(), android.provider.Settings.Secure.ANDROID_ID); // Returns hex string of device identifier
            Short nodeId = Short.parseShort(xbeeNodeId, 16);
            String timestamp = Long.toHexString(System.currentTimeMillis() / 1000);

            byte[] hexNonce = SimpleCrypto.toByte(nonce); // 2 bytes
            byte[] hexDeviceId = SimpleCrypto.toByte(deviceId); // 8 bytes
            byte[] hexNodeId = { (byte) ((nodeId >> 8) & 0xFF), (byte) (nodeId & 0xFF) }; // 2 bytes
            byte[] hexTimestamp = SimpleCrypto.toByte(timestamp); // 4 bytes

            byte[] result = new byte[hexNonce.length + hexDeviceId.length + hexNodeId.length + hexTimestamp.length];

            nonce = SimpleCrypto.toHex(hexNonce);

            System.arraycopy(hexNonce, 0, result, 0, hexNonce.length);
            System.arraycopy(hexDeviceId, 0, result, hexNonce.length, hexDeviceId.length);
            System.arraycopy(hexNodeId, 0, result, hexNonce.length + hexDeviceId.length, hexNodeId.length);
            System.arraycopy(hexTimestamp, 0, result, hexNonce.length + hexDeviceId.length + hexNodeId.length, hexTimestamp.length);

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

    private class RequestKeyTask extends AsyncTask<String, Void, String> {

        protected String doInBackground(String... urls) {
            HttpGet httpGet = new HttpGet(urls[0]);

            try {
                HttpResponse response = httpClient.execute(httpGet);
                Integer statusCode = response.getStatusLine().getStatusCode();

                if (statusCode == 200) {
                    // Get response content
                    String line;
                    StringBuilder stringBuilder = new StringBuilder();
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));

                    while ((line = bufferedReader.readLine()) != null) {
                        stringBuilder.append(line);
                    }

                    JSONObject nodeObject = new JSONObject(stringBuilder.toString());

                    authKey = nodeObject.getString("authKey");
                    return authKey;
                }
            }
            catch (ClientProtocolException e) {
                e.printStackTrace();
            }
            catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return null;
        }

        protected void onPostExecute(String authKey) {
            if (authKey == null) {
                toast = Toast.makeText(getBaseContext(), "An error occurred", Toast.LENGTH_SHORT);
            }

            else {
                toast = Toast.makeText(getBaseContext(), "Key: " + authKey, Toast.LENGTH_SHORT);

                // Enable the request 2FA button
                findViewById(R.id.requestNode2FA).setEnabled(true);
            }

            toast.show();
        }
    }

    // Request server for 2FA token
    private class RequestTokenTask extends AsyncTask<String, Void, Boolean> {

        protected Boolean doInBackground(String... urls) {
            // Make post request
            HttpPost httpPost = new HttpPost(urls[0]);

            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
            nameValuePairs.add(new BasicNameValuePair("nodeId", xbeeNodeId));
            nameValuePairs.add(new BasicNameValuePair("deviceId", deviceId));

            try {
                httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            try {
                HttpResponse response = httpClient.execute(httpPost);
                Integer statusCode = response.getStatusLine().getStatusCode();

                if (statusCode == 200) {
                    // Get response content
                    String line;
                    StringBuilder stringBuilder = new StringBuilder();
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));

                    while ((line = bufferedReader.readLine()) != null) {
                        stringBuilder.append(line);
                    }

//                    JSONObject nodeObject = new JSONObject(stringBuilder.toString());

//                    authKey = nodeObject.getString("nodeId");
                    return true;
                }
            }
            catch (ClientProtocolException e) {
                e.printStackTrace();
            }
            catch (IOException e) {
                e.printStackTrace();
            } /*catch (JSONException e) {
                e.printStackTrace();
            }*/

            return false;
        }

        protected void onPostExecute(Boolean success) {
            if (success) {
                toast = Toast.makeText(getBaseContext(), "2FA token created and sent", Toast.LENGTH_SHORT);
                toast.show();

                // Start new activity, passing it the details
                Intent intent = new Intent(getBaseContext(), TFAToken.class);
                intent.putExtra("authKey", authKey);
                intent.putExtra("deviceId", deviceId);
                intent.putExtra("nodeId", xbeeNodeId);
                intent.putExtra("nonceSelf", nonce);
                intent.putExtra("nonceNode", nonceNode);

                startActivity(intent);
            }

            else {
                toast = Toast.makeText(getBaseContext(), "An error occurred", Toast.LENGTH_SHORT);
                toast.show();
            }
        }
    }
}