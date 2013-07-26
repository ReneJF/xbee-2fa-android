package com.example.testxbeeproject.activities;

import com.example.JSON_format.RemoteXBEEValues;
import com.example.xbee_i2r.InitializeDevice;
import com.example.xbee_i2r.R;
import com.example.xbee_i2r.XBeeAddress16;
import com.ftdi.j2xx.FT_Device;
import com.google.gson.Gson;
import com.example.xbee_i2r.*;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.widget.TextView;
/** Test Activity class that displays the remote XBEE's info such as battery percentage, serial Number etc. 
 * 
 * @author Nirav Gandhi A0088471@nus.edu.sg
 *
 */
public class RemoteXBeeInfoTest extends Activity{

	private BroadcastReceiver remoteInfoReceiver;
	private BroadcastReceiver batteryInfoReceiver;
	private Gson gson;
	private TextView serialHigherText;
	private TextView serialLowerText;
	private TextView batteryText;
	private TextView myText;
	private TextView voltageText;
	private TextView modelText;
	private TextView niText;
	private SendCommands send;
	private Context context;
	private FT_Device ftDev= null;
	private XBeeAddress16 destAddr;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.xbee_info_layout);
		gson = new Gson();
		context = this;
		ftDev = InitializeDevice.getDevice();
		send = new SendCommands();
		
		//Edittable TextViews that displays the results. 
		serialHigherText = (TextView) findViewById(R.id.AnswerSerialHigher);
		serialLowerText = (TextView) findViewById(R.id.AnswerSerialLower);
		batteryText = (TextView) findViewById(R.id.AnswerBattery);
		myText = (TextView) findViewById(R.id.AnswerMY);
		voltageText = (TextView) findViewById(R.id.AnswerVoltage);
		modelText = (TextView) findViewById(R.id.AnswerModel);
		niText = (TextView) findViewById(R.id.AnswerNI);
		
		remoteInfoReceiver = new BroadcastReceiver(){
			@Override
			public void onReceive(Context context, Intent intent) {
				RemoteXBEEValues values = gson.fromJson(intent.getStringExtra("JSONRemoteAtResponse"), RemoteXBEEValues.class);
				if(values.getNode_identifier() !=null){
					niText.setText("" + values.getNode_identifier());
				}
				if(values.getHardware_version() != null){
					modelText.setText(values.getHardware_version());
				}
				if(values.getSerial_higher() != -1){
					serialHigherText.setText("" + values.getSerial_higher());
				}
				if(values.getSerial_lower() != -1){
					serialLowerText.setText("" + values.getSerial_lower());
				}
				Intent i = new Intent(context,BatteryService.class);
				i.putExtra("destAddr", destAddr.getAddress());
				IntentFilter filter2 = new IntentFilter(ReadService.BATTERY_INFO_ACTION);
				registerReceiver(batteryInfoReceiver,filter2);
				startService(i);
			}
		};
		batteryInfoReceiver = new BroadcastReceiver(){

			@Override
			public void onReceive(Context context, Intent intent) {
				batteryText.setText("" + intent.getIntExtra("battery", -1));
				voltageText.setText("" + intent.getIntExtra("voltage", -1));
			}
			
		};
	}

	@Override
	protected void onStart() {
		super.onStart();
		IntentFilter filter = new IntentFilter(ReadService.REMOTE_AT_RESPONSE_ACTION);
		registerReceiver(remoteInfoReceiver,filter);
		destAddr = new XBeeAddress16(getIntent().getIntArrayExtra("MYAddress"));
		send.sendRemoteQueries(destAddr);
		myText.setText(destAddr.toString());
		
		// Code snippet to get Battery Information. The results are broadcasted and received by a receiver registered to
		// BATTERY_INFO_ACTION.
		
		Intent intent = new Intent(context,BatteryService.class);
		intent.putExtra("destAddr", destAddr.getAddress());
		IntentFilter filter2 = new IntentFilter(ReadService.BATTERY_INFO_ACTION);
		registerReceiver(batteryInfoReceiver,filter2);
		startService(intent);
	}

	@Override
	protected void onStop() {
		super.onStop();
		unregisterReceiver(remoteInfoReceiver);
		unregisterReceiver(batteryInfoReceiver);
	}
}
