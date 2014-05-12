
package com.example.SDXbeta.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import com.example.JSON_format.XCTUValues;
import com.example.SDXbeta.R;
import com.example.xbee_i2r.InitializeDevice;
import com.example.xbee_i2r.ReadService;
import com.example.xbee_i2r.SendCommands;
import com.ftdi.j2xx.FT_Device;
import com.google.gson.Gson;

import java.util.HashMap;

/** UI class that displays the XBEE registers.   
 * 
 * @author Nirav Gandhi A0088471@nus.edu.sg
 *
 */

public class XCTU extends Activity {

	protected static final String TAG = "com.example.xbee_i2r_XCTUTest";
	private BroadcastReceiver receiver;
	private Gson gson;
	private ArrayAdapter<CharSequence> adapterCH,adapterBD; // Adapters for the spinners
	private Context context;
	private HashMap<Integer,Integer> baudMap; // A hashMap that keys the baud numbers with the corresponding baudRates.
	private HashMap<Integer,Integer> baudMap2;
	private SendCommands send;
	public static FT_Device ftDev = null;
	private int numFiles;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_xctu);
		context = this;

		gson = new Gson();
		// BaudMap is a hashMap that maps baudNumbers to baudRates
		baudMap = new HashMap<Integer,Integer>();
		int[] baudNumbers = new int[]{0,1,2,3,4,5,6,7};
		int[] baudRates = new int[]{1200,2400,4800,9600,19200,38400,57600,115200};
		for(int j=0;j<baudNumbers.length;j++){
			baudMap.put(baudNumbers[j], baudRates[j]);
		}
		// BaudMap2 is a hashMap that maps baudRates to baudNumbers
		baudMap2 = new HashMap<Integer,Integer>();
		for(int j=0;j<baudNumbers.length;j++){
			baudMap2.put(baudRates[j], baudNumbers[j]);
		}
		// Since XCTU is the launcher activity, the ftDevice is initialized by the 'InitializeDevice' class. 
		if(ftDev == null){
			InitializeDevice init = new InitializeDevice(context);
			ftDev = init.initialize();
			// As soon as the device is connected, the readService is started. 
			Intent intent = new Intent(context,ReadService.class);
			startService(intent);
		}
		
		send = new SendCommands();
	}

    public void onLogInClicked(View view) {
        Intent intent = new Intent(getBaseContext(), TFALogin.class);
        startActivity(intent);
    }

	@Override
	protected void onStart() {
		super.onStart();
		IntentFilter filter = new IntentFilter(ReadService.AT_RESPONSE_ACTION);
		/** A receiver that receives the XCTUValues from the ReadService. 
		 * 
		 */
		receiver = new BroadcastReceiver(){
			@Override
			public void onReceive(Context arg1, Intent intent) {
				if(intent.getStringExtra("XCTUValues") != null){
					XCTUValues values = gson.fromJson(intent.getStringExtra("XCTUValues"), XCTUValues.class);
//					fillUI(values);
				}
				else if(intent.getStringExtra("Response Status") !=null){
					Toast.makeText(arg1, "Values Changed Successfully", Toast.LENGTH_SHORT).show();	
				}
			}
		};
		registerReceiver(receiver,filter);
	}

	@Override
	protected void onStop(){
		super.onStop();
		unregisterReceiver(receiver);
	}
	
	/** Displays a dialog box
	 * 
	 * @param title - title of the displayed dialog box 
	 * @param message - message of the displayed dialog box 
	 */
	
	private void showDialog(String title, String message)
    {
      AlertDialog.Builder localBuilder = new AlertDialog.Builder(this);
      localBuilder.setTitle(title);
      localBuilder.setMessage(message);
      localBuilder.setPositiveButton("OK", null);
      localBuilder.show();
    }
}
