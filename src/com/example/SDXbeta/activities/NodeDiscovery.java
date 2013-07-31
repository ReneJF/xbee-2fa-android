package com.example.SDXbeta.activities;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import com.example.xbee_i2r.*;
import com.google.gson.Gson;
import com.example.JSON_format.XCTUValues;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
/** UI class that checks for all the nearby nodes and displays them in a list. 
 * 
 * @author Nirav Gandhi A0088471@nus.edu.sg
 *
 */
public class NodeDiscovery extends Activity{

	protected static final String TAG = "com.example.NodeDiscoveryTest";
	private BroadcastReceiver NDResponseReceiver;
	private BroadcastReceiver xctuReceiver;
	private BroadcastReceiver batteryInfoReceiver;
	private BroadcastReceiver txResponseReceiver;
	private Gson gson;
	private ImageButton refreshButton;
	private SendCommands send; // an object of the class that sends commands to the XBee
	private SharedPreferences pref;
	private Editor editor; // editor used to edit the shared preferences
	private int counter; 
	private static Context context; // Activity's context
	private CheckBox checkAllChannels; // Checkbox for user to select whether all channels need to be checked.
	private XCTUValues currentValues; // to store the XBEE register values when the activity was created. 
	private ArrayList<Node> nodeList; // An ArrayList that stores all the nodes discovered. These nodes are stored as 'Node'.
	private ListView nodeListView; 
	private LazyAdapter adapter;
	private TextView lastRefreshed;
	private static ProgressDialog progressDialog;
	private static Object object;
	private Handler handler;
	private boolean txReceived;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		context = this;
		setContentView(R.layout.node_discovery_layout);
		nodeListView = (ListView) findViewById(R.id.nodeListView);
		refreshButton = (ImageButton) findViewById(R.id.refreshButton);
		checkAllChannels = (CheckBox) findViewById(R.id.checkAll);
		lastRefreshed = (TextView) findViewById(R.id.refresh_tv);
		gson = new Gson();
		nodeList = new ArrayList<Node>();
		adapter = new LazyAdapter(this,nodeList);
		nodeListView.setAdapter(adapter);
		pref = getApplicationContext().getSharedPreferences("list_preference",MODE_PRIVATE);
		editor = pref.edit();
		counter = 0;
		int tempCounter = 0;
		String dateString = null;
		int int1,int2,int3;
		if((dateString = pref.getString("Date", null)) != null ){
			lastRefreshed.setText("  Last refreshed : " + "\n" + dateString); // Displays the date and time of the last 'Node Discovery' performed. 
		}
		
		// The following while loop extracts previously saved nodes and displays them as a list.
		while((int1 = pref.getInt(Integer.toString(tempCounter) + "0", -1)) != -1){
			int2 = pref.getInt(Integer.toString(tempCounter) + "1", -1);
			int3 = pref.getInt(Integer.toString(tempCounter)+"2", -1);
			Node tempNode = new Node();
			tempNode.setAddr16(new int[]{int1,int2});
			tempNode.setChannel(int3);
			int3 = pref.getInt(Integer.toString(tempCounter)+"3", -1);
			tempNode.setRssi(int3);
			tempCounter++;
			tempNode.setBatteryPerc(-1);
			nodeList.add(tempNode);
			adapter.notifyDataSetChanged();
		}
		//Handler that dismisses the progress Dialog box on receiving a message. 
		handler = new Handler(){
			public void handleMessage(Message msg){
				progressDialog.dismiss();
			}
		};
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		send = new SendCommands();
		IntentFilter filter = new IntentFilter(ReadService.ND_RESPONSE_ACTION);
		
		/** This receiver converts the intent string using the GSON - library into an object of 'Node'. This object is then added to the list.    
		 * 
		 */
		
		NDResponseReceiver = new BroadcastReceiver(){

			@Override
			public void onReceive(Context context, Intent intent) {
				if(intent.getStringExtra("JSONAtResponse") != null ){
					Node node = gson.fromJson(intent.getStringExtra("JSONAtResponse"), Node.class);			
					Log.d(TAG,"JSON ND Response" + intent.getStringExtra("JSONAtResponse"));
					nodeList.add(node);
					editor.putInt(Integer.toString(counter) + "0", node.getAddr16()[0]);
					editor.putInt(Integer.toString(counter) + "1", node.getAddr16()[1]);
					editor.putInt(Integer.toString(counter) + "2", node.getChannel());
					editor.putInt(Integer.toString(counter) + "3", node.getRssi());
					counter++;
					editor.commit();
					adapter.notifyDataSetChanged();
				}
			}
		};
		registerReceiver(NDResponseReceiver, filter);
		
		/**
		 * 
		 */
		
		txResponseReceiver = new BroadcastReceiver(){

			@Override
			public void onReceive(Context arg0, Intent intent) {
				Toast.makeText(context, "TX Request received", Toast.LENGTH_SHORT).show();
				txReceived = true;
			}
			
		};
		registerReceiver(txResponseReceiver,new IntentFilter(ReadService.TX_RECIEVED_ACTION));
		
		/** receives the current XCTUValues and puts them into currentValues.
		 * 
		 */
		
		xctuReceiver = new BroadcastReceiver(){

			@Override
			public void onReceive(Context context, Intent intent) {
				Gson gson = new Gson();
				currentValues = gson.fromJson(intent.getStringExtra("XCTUValues"), XCTUValues.class);
			}
			
		};
		registerReceiver(xctuReceiver,new IntentFilter(ReadService.AT_RESPONSE_ACTION));
		
		/** receives the battery data and displays the percentage in the required field. 
		 * 
		 */
		
		batteryInfoReceiver = new BroadcastReceiver(){

			@Override
			public void onReceive(Context arg0, Intent intent) {
				Log.d(TAG,"Battery Info packet received");
				try{
					for(int i=0;i<nodeList.size();i++){
						Node node = nodeList.get(i);
						int[] addr = intent.getIntArrayExtra("MYAddress");
						if((node.getAddr16()[0]*256 + node.getAddr16()[1]) == (addr[0]*256 + addr[1])){
							View view = nodeListView.getChildAt(i - nodeListView.getFirstVisiblePosition());
							TextView batteryText = (TextView) view.findViewById(R.id.etBattery);
							batteryText.setText("" + intent.getIntExtra("battery", -1) + "%");
							node.setBatteryPerc(intent.getIntExtra("battery", -1));
							break;
						}
					}
				}catch(Exception e){
					e.printStackTrace();
					Toast.makeText(context, "Exception", Toast.LENGTH_SHORT).show();
				}
				dismissProgressDialog();
			}
			
		};
		registerReceiver(batteryInfoReceiver, new IntentFilter(ReadService.BATTERY_INFO_ACTION));
		
		// loading the currentValues in onStart()
		if(InitializeDevice.isConnected()){
			send.readXCTUValues();
		}
		refreshButton.setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View v) {
				if(InitializeDevice.isConnected()){
					SimpleDateFormat dateTime = new SimpleDateFormat("dd/MM HH:mm:ss");
					String dateString = dateTime.format(new Date());
					lastRefreshed.setText("  Last refreshed : " + "\n" + dateString); // changes the time the node list was refreshsed to current time. 
					counter = 0;
					editor.clear(); // The shared preferences are cleared up. 
					editor.commit();
					editor.putString("Date",dateString); 
					editor.commit();
					if(!adapter.isEmpty()){
						nodeList.clear();
						adapter.notifyDataSetChanged();
					}
					
					Messenger messenger = new Messenger(handler); // Used for communication between the service and the NodeDiscovery activity. 
					// Progress dialog is dismissed when a message is sent through the messenger
					Intent intent = new Intent(context,NDService.class);
					intent.putExtra("checkboxChecked", checkAllChannels.isChecked());// To inform the service whether all the channels need to be checked. 
					intent.putExtra("originalChannel", currentValues.getChannel());// The service reverts back to the original channel once ND is over
					intent.putExtra("hardware_version", currentValues.getHardwareVersion());// No. of channels checked depends on the hardware version
					intent.putExtra("discoveryTime", currentValues.getNd_time()); // Current discoveryTime
					intent.putExtra("MESSENGER", messenger);
					progressDialog = ProgressDialog.show(context, "Node Discovery", "Discovering..");
					startService(intent);
				}
			}
		});	
		/**
		 * Opens up the remoteXBeeInfo activity on itemClick
		 */
		nodeListView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View view, int pos,
					long arg3) {
				if(InitializeDevice.isConnected()){
					Intent intent = new Intent(context,RemoteXBeeInfo.class);
					intent.putExtra("MYAddress", nodeList.get(pos).getAddr16());
					intent.putExtra("channel",nodeList.get(pos).getChannel());
					startActivity(intent);
				}
			}
		});
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main,menu);
		return super.onCreateOptionsMenu(menu);
	}
	
	/** Selection of activity on Menu-item press.  
	 * 
	 */
	
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		Intent intent;
		switch(item.getItemId()){
		case R.id.tabSniffer:
			intent = new Intent(context,Sniffer.class);
			startActivity(intent);
			break;
		case R.id.tabXCTU:
			intent = new Intent(context,XCTU.class);
			startActivity(intent);
			break;
		case R.id.tabND:
			break;
		case R.id.settings:
			intent = new Intent(context,Settings.class);
			startActivity(intent);
			break;
		}
		return super.onMenuItemSelected(featureId, item);
	}
	
	/** This function shows the progress dialog box.
	 *  
	 * @param o to notify the waiting thread that battery data has been received.  
	 */

	public static void showProgressDialog(Object o){
		progressDialog = ProgressDialog.show(context, "In progress", "Retrieving", true);
		object = o;
	}
	
	/** The function is called when no battery data is received, but progress dialog 
	 * needs to be removed from screen after time-out. 
	 * 
	 */
	
	public static void dismissProgressDialog(){
		if(progressDialog !=null){
			progressDialog.dismiss();
			synchronized(object){
				object.notify();
			}
		}
	}

	/** All registers to be unregistered. 
	 * 
	 */
	@Override
	protected void onStop() {
		super.onStop();
		unregisterReceiver(NDResponseReceiver);
		unregisterReceiver(xctuReceiver);
		unregisterReceiver(batteryInfoReceiver);
		unregisterReceiver(txResponseReceiver);
		if(progressDialog!=null){
			progressDialog.dismiss();
		}
	}
}
