package com.example.SDXbeta.activities;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ToggleButton;
import com.example.SDXbeta.R;
import com.example.xbee_i2r.*;
import com.ftdi.j2xx.FT_Device;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by sahil on 18/2/14.
 */
public class TwoFactorAuthenticationLogin extends Activity {
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
        new LoginUserTask().execute("http://localhost:4000/login");
    }

    private class LoginUserTask extends AsyncTask<String, Void, Long> {
        protected Long doInBackground(String... urls) {
            HttpClient httpClient = new DefaultHttpClient();
            HttpPost httpPost = new HttpPost("http://172.22.83.134:4000/login");

            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
            nameValuePairs.add(new BasicNameValuePair("username", editTextUsername.getText().toString()));
            nameValuePairs.add(new BasicNameValuePair("password", editTextPassword.getText().toString()));

            try {
                httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            try {
                HttpResponse response = httpClient.execute(httpPost);
                Log.d("HTTP RESPONSE: ", response.toString());

                Integer statusCode = response.getStatusLine().getStatusCode();

                if (statusCode == 200) {
                    Log.d("USER LOGIN", " SUCCESS");

                    // Request 2FA authentication with sensor node
                    HttpGet httpGet = new HttpGet("http://172.22.83.134:4000/key/209");
                    response = httpClient.execute(httpGet);

                    BufferedReader rd = new BufferedReader(
                            new InputStreamReader(response.getEntity().getContent()));

                    StringBuffer result = new StringBuffer();
                    String line;

                    while ((line = rd.readLine()) != null) {
                        result.append(line);
                    }

                    Log.d("KEY RESULT: ", result.toString());

                    try {
                        XBeeAddress16 destination = new XBeeAddress16(0xFF, 0xFF);
                        int[] payload = new int[] { 0x00, 0x01, 0x02, 0x03 };

                        TxRequest16 request = new TxRequest16(destination, payload);
                        XBeePacket packet = new XBeePacket(request.getFrameData());

                        byte[] outData = new byte[packet.getIntegerArray().length];

                        for(int j=0;j<packet.getIntegerArray().length;j++){
                            outData[j] = (byte) (packet.getIntegerArray()[j] & 0xff);
                        }

                        synchronized(ftDev){
                            if(ftDev.isOpen() == false){
                                return 1L;
                            }

                            ftDev.write(outData,outData.length);
                        }
                    }

                    catch (NullPointerException e) {
                        e.printStackTrace();
                    }

                } else if (statusCode == 401) {
                    Log.d("USER LOGIN", " FAILURE");
                }

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

            return 0L;
        }

        protected void onPostExecute(Long result) {

        }
    }
}