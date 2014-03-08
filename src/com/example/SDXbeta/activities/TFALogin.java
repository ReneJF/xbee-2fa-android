package com.example.SDXbeta.activities;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import com.example.SDXbeta.AuthServer;
import com.example.SDXbeta.R;
import com.example.SDXbeta.SimpleCrypto;
import com.example.xbee_i2r.*;
import com.ftdi.j2xx.FT_Device;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by sahil on 18/2/14.
 */
public class TFALogin extends Activity {
    EditText editTextUsername;
    EditText editTextPassword;
    private FT_Device ftDev;
    private BroadcastReceiver receiver;
    int[] revData;

    protected void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter(ReadService.TWO_FA__ACTION);

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                revData = intent.getIntArrayExtra("reverseData");

                Log.d("REVERSED DATA: ", revData.toString());
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
        setContentView(R.layout.activity_tfa_login);

        ftDev = InitializeDevice.getDevice();
    }

    public void onSubmitClicked(View view) {
        editTextUsername = (EditText)findViewById(R.id.editTextUsername);
        editTextPassword = (EditText)findViewById(R.id.editTextPassword);
        new LoginUserTask().execute(AuthServer.SERVER_URL + "login");
    }

    public void onSendDataClicked(View view) {
        byte[] data = { 0x0, 0x1, 0x2, 0x3, 0x4, 0x0, 0x1, 0x2, 0x3, 0x4, 0x0, 0x1, 0x2, 0x3, 0x4, 0x0, 0x1, 0x2, 0x3, 0x4 };
        byte[] key = { 0x0, 0x1, 0x2, 0x3, 0x4, 0x5, 0x6, 0x7, 0x0, 0x1, 0x2, 0x3, 0x4, 0x5, 0x6, 0x7 };

        try {
            String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
            byte[] result = SimpleCrypto.toByte(deviceId);
//            byte[] result = SimpleCrypto.encrypt(key, data);

            ByteBuffer byteBuffer = ByteBuffer.wrap(result);

            int[] payload = new int[result.length];
            for (int i = 0; i < result.length; i++) {
                payload[i] = (int) result[i];
            }


            XBeeAddress16 destination = new XBeeAddress16(0xFF, 0xFF);
//            int[] payload = new int[] { isOn ? 0xFF : 0x0 };

            TxRequest16 request = new TxRequest16(destination, payload);
            XBeePacket packet = new XBeePacket(request.getFrameData());

            byte[] outData = new byte[packet.getIntegerArray().length];

            for(int j=0;j<packet.getIntegerArray().length;j++){
                outData[j] = (byte) (packet.getIntegerArray()[j] & 0xff);
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

    private class LoginUserTask extends AsyncTask<String, Void, Integer> {
        Integer statusCode;
        Toast toast;

        protected Integer doInBackground(String... urls) {
            DefaultHttpClient httpClient = new AuthServer().getNewHttpClient(editTextUsername.getText().toString(), editTextPassword.getText().toString());

            HttpGet httpGet = new HttpGet(urls[0]);

            try {
                HttpResponse response = httpClient.execute(httpGet);
                statusCode = response.getStatusLine().getStatusCode();
            } catch (ClientProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return statusCode;
        }

        protected void onPostExecute(Integer statusCode) {
            // User exists; login successful
            if (statusCode == 200) {
                toast = Toast.makeText(getBaseContext(), "Login successful!", Toast.LENGTH_SHORT);

                // Start new activity, passing it the credentials
                Intent intent = new Intent(getBaseContext(), TFARequest.class);
                intent.putExtra("username", editTextUsername.getText().toString());
                intent.putExtra("password", editTextPassword.getText().toString());

                startActivity(intent);
            }

            // Username/password don't match
            else if (statusCode == 401) {
                toast = Toast.makeText(getBaseContext(), "Username/password don't match. Please try again.", Toast.LENGTH_SHORT);
            }

            // some other error occurred
            else {
                toast = Toast.makeText(getBaseContext(), "An error occurred. Please try again.", Toast.LENGTH_SHORT);
            }

            toast.show();
        }
    }
}