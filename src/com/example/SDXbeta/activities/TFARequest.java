package com.example.SDXbeta.activities;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import com.example.SDXbeta.AuthServer;
import com.example.SDXbeta.R;
import com.example.SDXbeta.SimpleCrypto;
import com.example.xbee_i2r.InitializeDevice;
import com.example.xbee_i2r.TxRequest16;
import com.example.xbee_i2r.XBeeAddress16;
import com.example.xbee_i2r.XBeePacket;
import com.ftdi.j2xx.FT_Device;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;

/**
 * Created by sahil on 3/3/14.
 */
public class TFARequest extends Activity {
    EditText editTextNodeId;
    String username;
    String password;
    private FT_Device ftDev;
    private String authKey;
    private String nodeXbee;


    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tfa_request);

        // Get username/password from intent (sent by Login activity)
        username = getIntent().getStringExtra("username");
        password = getIntent().getStringExtra("password");

        ftDev = InitializeDevice.getDevice();
    }

    public void onRequestKeyClicked(View view) {
        editTextNodeId = (EditText) findViewById(R.id.editTextNodeId);
        new RequestKeyTask().execute(AuthServer.SERVER_URL + "keys/" + editTextNodeId.getText().toString());
    }

    public void onRequest2FAClicked(View view) {
        // Request format (encrypted)
        // Nonce
        // IMEI/device ID
        // NodeId
        // timestamp

        try {
            String nonce = Short.toString((short) Math.floor(Math.random() * Short.MAX_VALUE));
            String deviceId = android.provider.Settings.Secure.getString(getContentResolver(), android.provider.Settings.Secure.ANDROID_ID); // Returns hex string of device identifier
            Short nodeId = Short.parseShort(editTextNodeId.getText().toString(), 16);
            String timestamp = Long.toHexString(System.currentTimeMillis() / 1000);

            byte[] hexNonce = SimpleCrypto.toByte(nonce); // 2 bytes
            byte[] hexDeviceId = SimpleCrypto.toByte(deviceId); // 8 bytes
            byte[] hexNodeId = { (byte) ((nodeId >> 8) & 0xFF), (byte) (nodeId & 0xFF) }; // 2 bytes
            byte[] hexTimestamp = SimpleCrypto.toByte(timestamp); // 4 bytes

            byte[] result = new byte[hexNonce.length + hexDeviceId.length + hexNodeId.length + hexTimestamp.length];

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
        Toast toast;

        protected String doInBackground(String... urls) {
            AuthServer authServer = new AuthServer();
            DefaultHttpClient httpClient = authServer.getNewHttpClient();
            HttpGet httpGet = new HttpGet(urls[0]);

            // Set username and password for request
            CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(
                    new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT),
                    new UsernamePasswordCredentials(username, password)
            );

            httpClient.setCredentialsProvider(credentialsProvider);

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
}