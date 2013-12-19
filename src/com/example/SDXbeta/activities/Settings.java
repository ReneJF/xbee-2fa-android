package com.example.SDXbeta.activities;

import java.util.HashMap;

import com.example.JSON_format.XCTUValues;
import com.example.xbee_i2r.InitializeDevice;
import com.example.xbee_i2r.R;
import com.example.xbee_i2r.ReadService;
import com.example.xbee_i2r.SendCommands;
import com.google.gson.Gson;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Checkable;
import android.widget.Spinner;

public class Settings extends Activity{

	private static final String TAG = "com.exampe.testxbeeproject.activities.Settings";
	private int baudNumber;
	private ArrayAdapter<CharSequence> adapterBD;
	private HashMap<Integer,Integer> baudMap;
	private Spinner spinner;
	private BroadcastReceiver receiver;
	private Button applySettingsButton;
	private CheckBox changeAPCheckBox;
	private XCTUValues values;
	private SendCommands send;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.settings_layout);
		spinner = (Spinner) findViewById(R.id.spinner4);
		applySettingsButton = (Button) findViewById(R.id.applySettingsButton);
		changeAPCheckBox = (CheckBox) findViewById(R.id.changeAP);
		baudMap = new HashMap<Integer,Integer>();
		int[] baudNumbers = new int[]{0,1,2,3,4,5,6,7};
		int[] baudRates = new int[]{1200,2400,4800,9600,19200,38400,57600,115200};
		for(int j=0;j<baudNumbers.length;j++){
			baudMap.put(baudNumbers[j], baudRates[j]);
		}
		send = new SendCommands();
		adapterBD = ArrayAdapter.createFromResource(this,R.array.baud_rates,android.R.layout.simple_spinner_item);
		adapterBD.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(adapterBD);
		final Gson gson = new Gson();
		receiver = new BroadcastReceiver(){

			@Override
			public void onReceive(Context context, Intent intent) {
				values = gson.fromJson(intent.getStringExtra("XCTUValues"),XCTUValues.class);
				baudNumber = values.getBaudNumber();
				spinner.setSelection(adapterBD.getPosition("" + baudMap.get(baudNumber)));
			}
		};
		applySettingsButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				int selectedBD = Integer.parseInt(adapterBD.getItem(spinner.getSelectedItemPosition()).toString());
				InitializeDevice.getDevice().setBaudRate(selectedBD);
				if(changeAPCheckBox.isChecked()){
					send.changeAPmode();
				}
			}
		});
	}

	@Override
	protected void onStart() {
		super.onStart();
		SendCommands send = new SendCommands();
		if(InitializeDevice.isConnected()){
			Log.d(TAG,"The device is connected");
			send.readXCTUValues();
		}
		registerReceiver(receiver,new IntentFilter(ReadService.AT_RESPONSE_ACTION));
	}

	@Override
	protected void onStop() {
		super.onStop();
		unregisterReceiver(receiver);
	}
}
