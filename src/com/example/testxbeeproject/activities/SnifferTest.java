package com.example.testxbeeproject.activities;

import com.example.xbee_i2r.*;
import com.example.JSON_format.RxResponseJSON;
import com.google.gson.Gson;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
/** Test Activity class that prints out the parsed data into a textView
 * 
 * @author Nirav Gandhi A0088471@nus.edu.sg
 *
 */
public class SnifferTest extends Activity{

	protected static final String TAG = "com.example.test.SnifferTest";
	private BroadcastReceiver receiver;
	private TextView responseText;
	private Gson gson;
	private Context context;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.sniffer_layout);
		context = this;
		responseText = (TextView) findViewById(R.id.responseTextView);
		gson = new Gson();
	}

	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main,menu);
		return super.onCreateOptionsMenu(menu);
	}
	

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		Intent intent;
		switch(item.getItemId()){
		case R.id.tabSniffer:
			break;
		case R.id.tabXCTU:
			intent = new Intent(context,XCTUtest.class);
			startActivity(intent);
			break;
		case R.id.tabND:
			intent = new Intent(context,NodeDiscoveryTest.class);
			startActivity(intent);
			break;
		case R.id.settings:
			intent = new Intent(context,Settings.class);
			startActivity(intent);
			break;
		}
		return true;
	}


	@Override
	protected void onStart() {
		super.onStart();
		IntentFilter filter = new IntentFilter(ReadService.RX_RESPONSE_16_ACTION);
		receiver = new BroadcastReceiver(){
			@Override
			public void onReceive(Context context, Intent intent){ 
				Log.d(TAG,"In Nirav onReceive()");
				String[] JSONResponses = intent.getStringArrayExtra("JSONRxResponse");
				for(int i=0;i<JSONResponses.length;i++){
					Log.d(TAG,"Extra string is" + JSONResponses[i]);
				}
				for(int i=0;i<JSONResponses.length;i++){
					String printString = new String();
					RxResponseJSON responseJSON = gson.fromJson(JSONResponses[i], RxResponseJSON.class);
					String dl_frame_header = "DL FRAME HEADER" + "\n" +
							"DL Version: " + responseJSON.getDl_version()
							+ " Frame Type: " + responseJSON.getDl_frame_type()
							+ " Source: " + responseJSON.getDl_source()
							+ " Destination: " + responseJSON.getDl_dest()
							+ " Sequence Number: " + responseJSON.getDl_seqNo();
					printString = printString.concat(dl_frame_header);
					String nwHdr ="\n" + "NW_HDR " + "\n" 
							+ " Source:" + responseJSON.getNw_source()
							+ " Destination:" + responseJSON.getNw_dest()
							+ " Version:" +	responseJSON.getNw_version()
									+ " Prototype:" + responseJSON.getNw_proto()
											+ " Packet Type:" + responseJSON.getNw_pkt_type();
					printString = printString.concat(nwHdr);
					String helloSeqNum = "\n" + "HELLO SEQ NUM: " + responseJSON.getHello_seq_num();
					String unixTime = "\n" + "UNIX TIME: " + responseJSON.getUnix_time();
					printString = printString.concat(helloSeqNum + unixTime);
					responseText.append(printString + "RSSI : " +  responseJSON.getRSSI());
				}
			}
			
		};
		registerReceiver(receiver,filter);
	}

	@Override
	protected void onStop() {
		super.onStop();
		unregisterReceiver(receiver);
	}
	
	
	
}
