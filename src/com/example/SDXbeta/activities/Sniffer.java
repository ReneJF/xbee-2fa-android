package com.example.SDXbeta.activities;

import java.util.ArrayList;

import org.json.JSONObject;

import com.example.xbee_i2r.*;
import com.example.JSON_format.RxResponseJSON;
import com.google.gson.Gson;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Adapter;
import android.widget.ImageButton;
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
	private SharedPreferences pref;
	private Editor editor;
	private int counter;
	private ImageButton discardButton;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.sniffer_layout);
		discardButton = (ImageButton) findViewById(R.id.discardButton);
		context = this;
		pref = getApplicationContext().getSharedPreferences("stored_response", MODE_PRIVATE);
		editor = pref.edit();
		responseList = new ArrayList<RxResponseJSON>();
		adapter = new SnifferAdapter(this,responseList);
		responseListView = (ListView) findViewById(R.id.responseListView);
		responseListView.setAdapter(adapter);
		gson = new Gson();
		counter = 0;
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
		int tempCounter = -1;
		if((tempCounter = pref.getInt("counter", -1)) != -1){
			counter = tempCounter;
		}
		for(int i=0;i<counter;i++){
			RxResponseJSON response = gson.fromJson(pref.getString(Integer.valueOf(i).toString(), null),RxResponseJSON.class);
			Log.d(TAG,"JSON String of Response added" + pref.getString(Integer.valueOf(i).toString(),null));
			responseList.add(response);
		}
		adapter.notifyDataSetChanged();
		discardButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				editor.clear();
				editor.putInt("counter", 0);
				counter = 0;
				editor.commit();
				responseList.clear();
				adapter.notifyDataSetChanged();
			}
		});
		adapter.notifyDataSetChanged();
		IntentFilter filter = new IntentFilter(ReadService.RX_RESPONSE_16_ACTION);
		receiver = new BroadcastReceiver(){
			@Override
			public void onReceive(Context context, Intent intent){ 
				String[] JSONResponses = intent.getStringArrayExtra("JSONRxResponse");
				for(int i=0;i<JSONResponses.length;i++){
					responseList.add(gson.fromJson(JSONResponses[i], RxResponseJSON.class));
					editor.putString(Integer.valueOf(counter).toString(), JSONResponses[i]);
					adapter.notifyDataSetChanged();
					counter++;
				}
				editor.putInt("counter",counter);
				editor.commit();
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
