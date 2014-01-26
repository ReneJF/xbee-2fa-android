package com.example.SDXbeta.activities;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import com.example.JSON_format.XCTUValues;
import com.example.SDXbeta.R;
import com.example.xbee_i2r.*;
import com.ftdi.j2xx.FT_Device;

/**
 * Created by sahil on 27/1/14.
 */
public class LedOnOff extends Activity {

    private FT_Device ftDev;
    private Context context;
    private BroadcastReceiver receiver;
    private int ledState;

    @Override
    protected void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter(ReadService.LED_ON_OFF_ACTION);

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                ledState = intent.getIntExtra("ledState", 0);

                ToggleButton toggle = (ToggleButton) findViewById(R.id.toggleButton);
                toggle.setChecked(ledState == 0 ? false : true);

                TextView deviceId = (TextView) findViewById(R.id.deviceIdTextView);
                deviceId.setText(intent.getStringExtra("sourceAddress"));
//
                TextView rssi = (TextView) findViewById(R.id.rssiTextView);
                rssi.setText(Integer.toString(intent.getIntExtra("rssi", 0)));

//                if (intent.getIntExtra("ledState", null)Extra("XCTUValues") != null) {
//                    XCTUValues values = gson.fromJson(intent.getStringExtra("XCTUValues"), XCTUValues.class);
//                    fillUI(values);
//                } else if (intent.getStringExtra("Response Status") != null) {
//                    Toast.makeText(arg1, "Values Changed Successfully", Toast.LENGTH_SHORT).show();
//                }
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
        setContentView(R.layout.activity_ledonoff);

        context = this;

//        ToggleButton toggle = (ToggleButton) findViewById(R.id.toggleButton);
//        toggle.setChecked(ledState == 0 ? false : true);


        // This should be performed by the launcher activity, whichever that may be
        // Since XCTU is the launcher activity, the ftDevice is initialized by the 'InitializeDevice' class.
//        if(ftDev == null){
//            InitializeDevice init = new InitializeDevice(context);
//            ftDev = init.initialize();
//
//            // As soon as the device is connected, the readService is started.
//            Intent intent = new Intent(context,ReadService.class);
//            startService(intent);
//        }

        ftDev = InitializeDevice.getDevice();
    }

    public void onToggleLedClicked(View view) {

        // Is the toggle on?
        boolean isOn = ((ToggleButton) view).isChecked();

        try {
            XBeeAddress16 destination = new XBeeAddress16(0xFF, 0xFF);
            int[] payload = new int[] { isOn ? 0xFF : 0x0 };

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

        catch (NullPointerException e) {
            e.printStackTrace();
        }
    }
}