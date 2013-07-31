package com.example.SDXbeta.activities;

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
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
/** Test Activity class that displays the remote XBEE's info such as battery percentage, serial Number etc. 
 * 
 * @author Nirav Gandhi A0088471@nus.edu.sg
 *
 */
public class RemoteXBeeInfo extends Activity{

	private BroadcastReceiver remoteInfoReceiver;
	private BroadcastReceiver batteryInfoReceiver;
	private BroadcastReceiver positiveResponseReceiver;
	private Gson gson;
	private TextView serialHigherText;
	private TextView serialLowerText;
	private TextView batteryText;
	private EditText myText;
	private TextView voltageText;
	private TextView modelText;
	private TextView niText;
	private EditText panIdText;
	private Spinner spinnerCH;
	private Button applyChangesButton;
	private SendCommands send;
	private Context context;
	private FT_Device ftDev= null;
	private XBeeAddress16 destAddr;
	private int operatingChannel;
	private ArrayAdapter<CharSequence> adapterCH;

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
		myText = (EditText) findViewById(R.id.AnswerMY);
		voltageText = (TextView) findViewById(R.id.AnswerVoltage);
		modelText = (TextView) findViewById(R.id.AnswerModel);
		niText = (TextView) findViewById(R.id.AnswerNI);
		spinnerCH = (Spinner) findViewById(R.id.SpinnerCH);
		panIdText = (EditText) findViewById(R.id.AnswerID);
		applyChangesButton = (Button) findViewById(R.id.applyChangesButton);
		
		applyChangesButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				RemoteXBEEValues values= collectValues();
				send.sendRemoteWriteQueries(values,destAddr);
			}
		});
		// Receiver that receives the RemoteAtResponses of the values that were changed and written on the remote XBee. 
		positiveResponseReceiver = new BroadcastReceiver(){

			@Override
			public void onReceive(Context context, Intent intent) {
				String command = intent.getStringExtra("command");
				switch(command.charAt(0)){
					case 'C':
						spinnerCH.setBackgroundColor(Color.GREEN);
						break;
					case 'I':
						panIdText.setBackgroundColor(Color.GREEN);
						break;
					case 'M':
						myText.setBackgroundColor(Color.GREEN);
						break;
					case 'W':
						Toast.makeText(context, "Selected values written permanently", Toast.LENGTH_SHORT).show();
						break;				
				}	
			}
		};
		// Receiver that receives remoteAtResponse containing values of registers : HV,CH,SL,SH,ID,NI	
		remoteInfoReceiver = new BroadcastReceiver(){
			@Override
			public void onReceive(Context context, Intent intent) {
				RemoteXBEEValues values = gson.fromJson(intent.getStringExtra("JSONRemoteAtResponse"), RemoteXBEEValues.class);
				if(values.getNode_identifier() !=null){
					niText.setText("" + values.getNode_identifier());
				}
				if(values.getHardware_version() != -1){
					if(values.getHardware_version() == 0x17){
						modelText.setText("Series 1");
						adapterCH = ArrayAdapter.createFromResource(context,R.array.s1_channels,android.R.layout.simple_spinner_item);
					}
					else{
						modelText.setText("Series 1 PRO");
						adapterCH = ArrayAdapter.createFromResource(context,R.array.s1_pro_channels,android.R.layout.simple_spinner_item);
					}
				}
				if(values.getSerial_higher() != null){
					serialHigherText.setText(values.getSerial_higher());
				}
				if(values.getSerial_lower() != null){
					serialLowerText.setText(values.getSerial_lower());
				}
				if(values.getChannel() != -1){
					if(adapterCH != null){
						spinnerCH.setAdapter(adapterCH);
						spinnerCH.setSelection(adapterCH.getPosition("" + values.getChannel()));
					}
				}
				if(values.getPan_id() != -1){
					panIdText.setText("" + values.getPan_id());
				}
				
				// Code snippet to get Battery Information. The results are broadcasted and received by a receiver registered to
				// BATTERY_INFO_ACTION.
				
				Intent i = new Intent(context,BatteryService.class);
				i.putExtra("destAddr", destAddr.getAddress());
				IntentFilter filter2 = new IntentFilter(ReadService.BATTERY_INFO_ACTION);
				registerReceiver(batteryInfoReceiver,filter2);
				startService(i);
			}
		};
		
		// A receiver that receives the battery voltage and battery percentage. Command is sent through the Battery service.
		// This service is started when the time for remoteAtResponse is up.
		batteryInfoReceiver = new BroadcastReceiver(){
			@Override
			public void onReceive(Context context, Intent intent) {
				batteryText.setText("" + intent.getIntExtra("battery", -1) + "%");
				voltageText.setText("" + intent.getIntExtra("voltage", -1));
			}	
		};
	}

	@Override
	protected void onStart() {
		super.onStart();
		IntentFilter filter = new IntentFilter(ReadService.REMOTE_AT_RESPONSE_ACTION);
		registerReceiver(remoteInfoReceiver,filter);
		registerReceiver(positiveResponseReceiver,new IntentFilter(ReadService.POSITIVE_REPONSE_ACTION));
		destAddr = new XBeeAddress16(getIntent().getIntArrayExtra("MYAddress"));
		operatingChannel = getIntent().getIntExtra("channel",-1);
		if(operatingChannel != -1){
			send.changeChannel(operatingChannel, true);
		}
		send.sendRemoteQueries(destAddr);
		myText.setText(destAddr.toString());
	}

	@Override
	protected void onStop() {
		super.onStop();
		try{
			unregisterReceiver(remoteInfoReceiver);
			unregisterReceiver(batteryInfoReceiver);
			unregisterReceiver(positiveResponseReceiver);
		}catch(IllegalArgumentException e){
			e.printStackTrace();
		}
	}
	
	private RemoteXBEEValues collectValues(){
		RemoteXBEEValues values = new RemoteXBEEValues();
		values.setMy_addr(Integer.parseInt(myText.getText().toString()));
		values.setPan_id(Integer.parseInt(panIdText.getText().toString()));
		values.setChannel(Integer.parseInt(adapterCH.getItem(spinnerCH.getSelectedItemPosition()).toString()));
		return values;
	}
}
