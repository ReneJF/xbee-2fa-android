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
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

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

//                ToggleButton toggle = (ToggleButton) findViewById(R.id.toggleButton);
//                toggle.setChecked(ledState == 0 ? false : true);

//                TextView deviceId = (TextView) findViewById(R.id.deviceIdTextView);
//                deviceId.setText(intent.getStringExtra("sourceAddress"));
//
//                TextView rssi = (TextView) findViewById(R.id.rssiTextView);
//                rssi.setText(Integer.toString(intent.getIntExtra("rssi", 0)));

//                if (intent.getIntExtra("ledState", null)Extra("XCTUValues") != null) {
//                    XCTUValues values = gson.fromJson(intent.getStringExtra("XCTUValues"), XCTUValues.class);
//                    fillUI(values);
//                } else if (intent.getStringExtra("Response Status") != null) {
//                    Toast.makeText(arg1, "Values Changed Successfully", Toast.LENGTH_SHORT).show();
//                }

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
            AuthServer authServer = new AuthServer();
            DefaultHttpClient httpClient = authServer.getNewHttpClient();

            // Set username and password for request
            CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(
                    new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT),
                    new UsernamePasswordCredentials(editTextUsername.getText().toString(), editTextPassword.getText().toString())
            );

            httpClient.setCredentialsProvider(credentialsProvider);

//            HttpPost httpPost = new HttpPost(urls[0]); // TODO change to GET
            HttpGet httpGet = new HttpGet(urls[0]);

//            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
//            nameValuePairs.add(new BasicNameValuePair("username", editTextUsername.getText().toString()));
//            nameValuePairs.add(new BasicNameValuePair("password", editTextPassword.getText().toString()));

//            try {
//                httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
//            } catch (UnsupportedEncodingException e) {
//                e.printStackTrace();
//            }

            try {
                HttpResponse response = httpClient.execute(httpGet);

                statusCode = response.getStatusLine().getStatusCode();
                // Success; username/password is correct
//                if (statusCode == 200) {
//                    Log.d("USER LOGIN", " SUCCESS");

                    // Request 2FA authentication with sensor node
                    // TODO start new activity
//                    HttpGet httpGet = new HttpGet("http://172.22.83.134:4000/key/209");
//                    response = httpClient.execute(httpGet);
//
//                    BufferedReader rd = new BufferedReader(
//                            new InputStreamReader(response.getEntity().getContent()));
//
//                    StringBuffer result = new StringBuffer();
//                    String line;
//
//                    while ((line = rd.readLine()) != null) {
//                        result.append(line);
//                    }
//
//                    Log.d("KEY RESULT: ", result.toString());
//
//                    try {
//                        XBeeAddress16 destination = new XBeeAddress16(0xFF, 0xFF);
//                        int[] payload = new int[] { 0x00, 0x01, 0x02, 0x03 };
//
//                        TxRequest16 request = new TxRequest16(destination, payload);
//                        XBeePacket packet = new XBeePacket(request.getFrameData());
//
//                        byte[] outData = new byte[packet.getIntegerArray().length];
//
//                        for(int j=0;j<packet.getIntegerArray().length;j++){
//                            outData[j] = (byte) (packet.getIntegerArray()[j] & 0xff);
//                        }
//
//                        synchronized(ftDev){
//                            if(ftDev.isOpen() == false){
//                                return 1L;
//                            }
//
//                            ftDev.write(outData,outData.length);
//                        }
//                    }
//
//                    catch (NullPointerException e) {
//                        e.printStackTrace();
//                    }

//                }

                // Unauthorized; try again
//                else if (statusCode == 401) {
//                    Log.d("USER LOGIN", " FAILURE");
//                }

//                HttpEntity entity = response.getEntity();
//
//                StringBuilder sb = new StringBuilder();
////                try {
//                BufferedReader reader = new BufferedReader(new InputStreamReader(entity.getContent()), 65728);
//                String line;
//
//                while ((line = reader.readLine()) != null) {
//                    sb.append(line);
//                }
//
//                Log.e("RESPONSE LINE: ", line);
//                }
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