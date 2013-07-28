package com.example.testxbeeproject.activities;

import java.util.ArrayList;

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
import android.widget.ListView;
/** Test Activity class that prints out the parsed data into a textView
 * 
 * @author Nirav Gandhi A0088471@nus.edu.sg
 *
 */
public class Sniffer extends Activity{

	protected static final String TAG = "com.example.test.Sniffer";
	private BroadcastReceiver receiver;
	private Gson gson;
	private Context context;
	private ArrayList<RxResponseJSON> responseList;
	private SnifferAdapter adapter;
	private ListView responseListView;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.sniffer_layout);
		context = this;
		responseList = new ArrayList<RxResponseJSON>();
		adapter = new SnifferAdapter(this,responseList);
		responseListView = (ListView) findViewById(R.id.responseListView);
		responseListView.setAdapter(adapter);
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
			intent = new Intent(context,XCTU.class);
			startActivity(intent);
			break;
		case R.id.tabND:
			intent = new Intent(context,NodeDiscovery.class);
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
					responseList.add(gson.fromJson(JSONResponses[i], RxResponseJSON.class));
					adapter.notifyDataSetChanged();
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
