package com.example.testxbeeproject.activities;
import java.util.ArrayList;
import java.util.Arrays;
import com.example.xbee_i2r.*;
import com.ftdi.j2xx.FT_Device;
import com.google.gson.Gson;
import com.example.JSON_format.XCTUValues;
import com.example.testxbeeproject.activities.XCTUtest;
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
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
/** Test Activity class that checks for all the nearby nodes and displays them in a list. 
 * 
 * @author Nirav Gandhi A0088471@nus.edu.sg
 *
 */
public class NodeDiscoveryTest extends Activity{

	protected static final String TAG = "com.example.NodeDiscoveryTest";
	private BroadcastReceiver receiver;
	private Gson gson;
	private Button refreshButton;
	private SendCommands send;
	private SharedPreferences pref;
	private Editor editor;
	private int counter;
	private static Context context;
	private FT_Device ftDev= null;
	private CheckBox checkAllChannels;
	private XCTUValues currentValues;
	public BroadcastReceiver receiver2;
	public BroadcastReceiver batteryInfoReceiver;
	public BroadcastReceiver NDFinishReceiver;
	private ArrayList<Node> nodeList;
	private ListView nodeListView;
	private LazyAdapter adapter;
	private TextView temp;
	private static ProgressDialog progressDialog;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		context = this;
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.node_discovery_layout);
		nodeListView = (ListView) findViewById(R.id.nodeListView);
		refreshButton = (Button) findViewById(R.id.refreshButton);
		checkAllChannels = (CheckBox) findViewById(R.id.checkAll);
		temp = (TextView) findViewById(R.id.temporary_tv);
		gson = new Gson();
		nodeList = new ArrayList<Node>();
		adapter = new LazyAdapter(this,nodeList);
		nodeListView.setAdapter(adapter);
		pref = getApplicationContext().getSharedPreferences("list_preference",MODE_PRIVATE);
		editor = pref.edit();
		counter = 0;
		int tempCounter = 0;
		int int1,int2;
		while((int1 = pref.getInt(Integer.toString(tempCounter) + "0", -1)) != -1){
			int2 = pref.getInt(Integer.toString(tempCounter) + "1", -1);
			Node tempNode = new Node();
			tempNode.setAddr16(new int[]{int1,int2});
			tempCounter++;
			nodeList.add(tempNode);
			adapter.notifyDataSetChanged();
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		ftDev = InitializeDevice.getDevice();
		send = new SendCommands();
		IntentFilter filter = new IntentFilter(ReadService.ND_RESPONSE_ACTION);
		receiver = new BroadcastReceiver(){

			@Override
			public void onReceive(Context context, Intent intent) {
				if(intent.getStringExtra("JSONAtResponse") != null ){
					Node node = gson.fromJson(intent.getStringExtra("JSONAtResponse"), Node.class);			
					Log.d(TAG,"JSON ND Response" + intent.getStringExtra("JSONAtResponse"));
					nodeList.add(node);
					/*editor.putInt(Integer.toString(counter) + "0", node.getAddr16()[0]);
					editor.putInt(Integer.toString(counter) + "1", node.getAddr16()[1]);
					counter++;
					editor.commit();
					AlarmManager mgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
					Intent i = new Intent(context,DeleteSharedPreferences.class);
					PendingIntent pi = PendingIntent.getService(context, 0, i, 0);
					Calendar cal = Calendar.getInstance();
					cal.add(Calendar.MINUTE, 10);
					mgr.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi);*/
					adapter.notifyDataSetChanged();
				}
			}
		};
		registerReceiver(receiver, filter);
		receiver2 = new BroadcastReceiver(){

			@Override
			public void onReceive(Context context, Intent intent) {
				Gson gson = new Gson();
				currentValues = gson.fromJson(intent.getStringExtra("XCTUValues"), XCTUValues.class);
			}
			
		};
		registerReceiver(receiver2,new IntentFilter(ReadService.AT_RESPONSE_ACTION));
		batteryInfoReceiver = new BroadcastReceiver(){

			@Override
			public void onReceive(Context context, Intent intent) {
				Log.d(TAG,"Battery Info packet received");
				try{
					for(int i=0;i<nodeList.size();i++){
						Node node = nodeList.get(i);
						int[] addr = intent.getIntArrayExtra("MYAddress");
						if((node.getAddr16()[0]*256 + node.getAddr16()[1]) == (addr[0]*256 + addr[1])){
							Log.d(TAG,"Address in nodeList " + Arrays.toString(node.getAddr16()));
							Log.d(TAG,"Address received from intent" + Arrays.toString(addr));
							Log.d(TAG,"Match found at " + i);
							View view = nodeListView.getChildAt(i - nodeListView.getFirstVisiblePosition());
							TextView batteryText = (TextView) view.findViewById(R.id.etBattery);
							batteryText.setText("" + intent.getIntExtra("battery", -1) + "%");
							temp.setText("Pos:" + i);
							temp.append("" + nodeListView.indexOfChild(view));
							break;
						}
					}
				}catch(Exception e){
					e.printStackTrace();
					Toast.makeText(context, "Exception", Toast.LENGTH_SHORT).show();
				}
				progressDialog.dismiss();
			}
			
		};
		registerReceiver(batteryInfoReceiver, new IntentFilter(ReadService.BATTERY_INFO_ACTION));
		NDFinishReceiver = new BroadcastReceiver(){

			@Override
			public void onReceive(Context context, Intent intent) {
				setProgressBarIndeterminateVisibility(false);
			}
		};
		registerReceiver(NDFinishReceiver,new IntentFilter(NDService.ACTION));
		if(InitializeDevice.isConnected()){
			send.readXCTUValues();
		}
		refreshButton.setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View v) {
				if(InitializeDevice.isConnected()){
					if(!adapter.isEmpty()){
						/*editor.clear();
						editor.commit();*/
						nodeList.clear();
						adapter.notifyDataSetChanged();
					}
					((LazyAdapter)adapter).setIsChecked(checkAllChannels.isChecked());
					Intent intent = new Intent(context,NDService.class);
					intent.putExtra("checkboxChecked", checkAllChannels.isChecked());
					intent.putExtra("originalChannel", currentValues.getChannel());
					intent.putExtra("hardware_version", currentValues.getHardwareVersion());
					setProgressBarIndeterminateVisibility(true);
					startService(intent);
					send.sendNDcommand(checkAllChannels.isChecked(),currentValues.getChannel(),currentValues.getHardwareVersion());
				}
			}
		});	
		nodeListView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View view, int pos,
					long arg3) {
				if(InitializeDevice.isConnected()){
					Intent intent = new Intent(context,RemoteXBeeInfoTest.class);
					intent.putExtra("MYAddress", nodeList.get(pos).getAddr16());
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

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		Intent intent;
		switch(item.getItemId()){
		case R.id.tabSniffer:
			intent = new Intent(context,SnifferTest.class);
			startActivity(intent);
			break;
		case R.id.tabXCTU:
			intent = new Intent(context,XCTUtest.class);
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
	
	public static void showProgressDialog(){
		progressDialog = ProgressDialog.show(context, "In progress", "Loading", true);
	}

	@Override
	protected void onStop() {
		super.onStop();
		unregisterReceiver(receiver);
		unregisterReceiver(receiver2);
		unregisterReceiver(batteryInfoReceiver);
		unregisterReceiver(NDFinishReceiver);
	}
}
