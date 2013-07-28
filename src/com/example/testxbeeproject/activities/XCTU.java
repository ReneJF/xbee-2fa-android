
package com.example.testxbeeproject.activities;

import java.util.HashMap;

import org.json.JSONException;
import org.json.JSONObject;
import com.example.xbee_i2r.R;
import com.example.JSON_format.XCTUValues;
import com.ftdi.j2xx.FT_Device;
import com.google.gson.Gson;
import com.example.xbee_i2r.*;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
/** Test Activity class that can read from the XBEE's registers and write permanently to the XBEE's registers.  
 * 
 * @author Nirav Gandhi A0088471@nus.edu.sg
 *
 */
public class XCTU extends Activity {

	protected static final String TAG = "com.example.xbee_i2r_XCTUTest";
	private BroadcastReceiver receiver;
	private Gson gson;
	private EditText etMY,etDL,etDH,etID,etNT; // Text where the values are displayed. 
	private Spinner spinnerCH,spinnerBD,spinnerAP; // Spinner from where the values can be selected. 
	private ArrayAdapter<CharSequence> adapterCH,adapterBD,adapterAP; // Adapters for the spinners
	private Context context;
	private Button readButton,applyChangesButton;
	private HashMap<Integer,Integer> baudMap; // A hashMap that keys the baud numbers with the corresponding baudRates.  
	private HashMap<Integer,Integer> baudMap2;
	private SendCommands send;
	public static FT_Device ftDev = null;
	private int numFiles;
	private AlertDialog dialog;
	private SharedPreferences pref;
	private Editor editor;
	private String[] fileArray;
	private int itemClicked = -1;

	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.xctu_layout);
		context = this;
		readButton = (Button) findViewById(R.id.readButton);
		applyChangesButton = (Button) findViewById(R.id.applyButton);
		etMY = (EditText) findViewById(R.id.etMY);
		etID = (EditText) findViewById(R.id.etID);
		etDL = (EditText) findViewById(R.id.etDL);
		etDH = (EditText) findViewById(R.id.etDH);
		etNT = (EditText) findViewById(R.id.etNT);
		// All the Edit TextViews are disabled if the XBEE is not connected to the FT_Device
		etMY.setEnabled(false); 
		etID.setEnabled(false);
		etDL.setEnabled(false);
		etDH.setEnabled(false);
		etNT.setEnabled(false);
		spinnerCH = (Spinner) findViewById(R.id.spinner1);
		spinnerCH.setEnabled(false);
		spinnerBD = (Spinner) findViewById(R.id.spinner3);
		spinnerBD.setEnabled(false);
		spinnerAP = (Spinner) findViewById(R.id.spinner2);
		spinnerAP.setEnabled(false);
		applyChangesButton.setEnabled(false);
		gson = new Gson();
		baudMap = new HashMap<Integer,Integer>();
		int[] baudNumbers = new int[]{0,1,2,3,4,5,6,7};
		int[] baudRates = new int[]{1200,2400,4800,9600,19200,38400,57600,115200};
		for(int j=0;j<baudNumbers.length;j++){
			baudMap.put(baudNumbers[j], baudRates[j]);
		}
		baudMap2 = new HashMap<Integer,Integer>();
		for(int j=0;j<baudNumbers.length;j++){
			baudMap2.put(baudRates[j], baudNumbers[j]);
		}
		if(ftDev == null){
			InitializeDevice init = new InitializeDevice(context);
			ftDev = init.initialize();
			Intent intent = new Intent(context,ReadService.class);
			startService(intent);
		}
		send = new SendCommands();
		applyChangesButton.setEnabled(true);
		readButton.setOnClickListener(new View.OnClickListener() {
		// Will provide the JSONData in onReceive().
		@Override
		public void onClick(View v) {
			if(ftDev != null)
				send.readXCTUValues();
		}
		});
		
		applyChangesButton.setOnClickListener(new View.OnClickListener() {
			// Should give data to library in the form of JSON.
			// The function needs to be made robust and point out errors 
			// in case of wrong input or no input. For example. alphabets in channel number. 
			@Override
			public void onClick(View v) {
				if(InitializeDevice.isConnected()){
					JSONObject object;
					if(isValuesCorrect()){
						object = collectValues();
						Log.d(TAG,"JSON String of XCTU Values:" + object.toString());
						send.writeXCTUValues(gson.fromJson(object.toString(), XCTUValues.class));
					}
				}
			}
		});
	}

	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu_xctu,menu);
		return super.onCreateOptionsMenu(menu);
	}

	
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		Intent intent;
		switch(item.getItemId()){
		case R.id.tabSniffer:
			intent = new Intent(context,Sniffer.class);
			startActivity(intent);
			break;
		case R.id.tabXCTU:
			break;
		case R.id.tabND:
			intent = new Intent(context,NodeDiscovery.class);
			startActivity(intent);
			break;
		case R.id.settings:
			intent = new Intent(context,Settings.class);
			startActivity(intent);
			break;
		case R.id.saveValues:
			pref = getApplicationContext().getSharedPreferences("savedFiles", MODE_PRIVATE);
			editor = pref.edit();
			if(pref.getInt("numFiles", -1) == -1){
				editor.putInt("numFiles",0);
				editor.commit();
				numFiles = 0;
			}
			else{
				numFiles = pref.getInt("numFiles",-1);
			}
			AlertDialog.Builder builder = new AlertDialog.Builder(context);
			LayoutInflater inflater = this.getLayoutInflater();
			builder.setView(inflater.inflate(R.layout.save_layout, null))
					.setTitle("Save Values")
					.setPositiveButton(R.string.saveValues, new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialogInterface, int which) {
					EditText etFile = (EditText) dialog.findViewById(R.id.fileName);
					final String fileName = etFile.getText().toString();
					boolean fileExists = false;
					for(int i=1;i<=numFiles;i++){
						if(fileName.equals(pref.getString(Integer.valueOf(i).toString(), null))){
							fileExists = true;
							AlertDialog.Builder builder3 = new AlertDialog.Builder(context);
							builder3.setTitle("Overwrite File");
							builder3.setMessage("Overwrite this file");
							builder3.setNegativeButton("NO", new DialogInterface.OnClickListener() {
								
								@Override
								public void onClick(DialogInterface dialog, int which) {
									
								}
							});
							builder3.setPositiveButton("YES", new DialogInterface.OnClickListener() {
								
								@Override
								public void onClick(DialogInterface dialog, int which) {
									if(isValuesCorrect()){
										editor.putString(fileName, collectValues().toString());
										editor.commit();
									}
								}
							});
							AlertDialog dialog = builder3.create();
							dialog.show();
						}
					}
					if(!fileExists){
						numFiles++;
						editor.putInt("numFiles", numFiles);
						editor.putString(Integer.valueOf(numFiles).toString(), fileName);
						if(isValuesCorrect()){
							editor.putString(fileName, collectValues().toString());
							editor.commit();
						}
					}
				}
			})
					.setNegativeButton(R.string.cancelAlert, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					
				}
			});
			dialog = builder.create();
			dialog.show();
			break;
		case R.id.loadValues:
			pref = getApplicationContext().getSharedPreferences("savedFiles", MODE_PRIVATE);
			editor = pref.edit();
			int loadFiles;
			if(pref.getInt("numFiles", -1) == -1){
				loadFiles = 0;
			}
			else{
				loadFiles = pref.getInt("numFiles",-1);
			}
			fileArray = new String[loadFiles];
			for(int i=1;i<=loadFiles;i++){
				fileArray[i-1] = pref.getString(Integer.valueOf(i).toString(), null);
			}
			AlertDialog.Builder builder2 = new AlertDialog.Builder(context);
			builder2.setTitle("Load Values");
			
			builder2.setSingleChoiceItems(fileArray, -1, new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					itemClicked = which;
				}
			});
			builder2.setPositiveButton(R.string.loadValues, new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					String jsonString;
					if((jsonString = pref.getString(fileArray[itemClicked], null)) != null){
						Gson gson = new Gson();
						Log.d(TAG,"jsonString is" + jsonString);
						fillUI(gson.fromJson(jsonString, XCTUValues.class));
					}else{
						
					}
				}
			} );
			builder2.setNegativeButton(R.string.cancelAlert, new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					
				}
			});
			AlertDialog dialog2 = builder2.create();
			dialog2.show();
			break;
		}
		return true;
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		IntentFilter filter = new IntentFilter(ReadService.AT_RESPONSE_ACTION);
		receiver = new BroadcastReceiver(){
			@Override
			public void onReceive(Context arg1, Intent intent) {
				if(intent.getStringExtra("XCTUValues") != null){
					XCTUValues values = gson.fromJson(intent.getStringExtra("XCTUValues"), XCTUValues.class);
					fillUI(values);
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
	
	private void fillUI(XCTUValues values){
		adapterBD = ArrayAdapter.createFromResource(context,R.array.baud_rates,android.R.layout.simple_spinner_item);
		adapterBD.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinnerBD.setAdapter(adapterBD);
		spinnerBD.setEnabled(true);
		adapterAP = ArrayAdapter.createFromResource(context,R.array.APmodes,android.R.layout.simple_spinner_item);
		adapterAP.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinnerAP.setAdapter(adapterAP);
		spinnerAP.setEnabled(true);
		etMY.setText("" + values.getMyAddr());
		etMY.setEnabled(true);
		etDL.setText("" + values.getDlAddr());
		etDL.setEnabled(true);
		etDH.setText("" + values.getDhAddr());
		etDH.setEnabled(true);
		etID.setText("" + values.getPanId());
		etID.setEnabled(true);
		etNT.setText("" + values.getNd_time());
		etNT.setEnabled(true);
		spinnerBD.setSelection(adapterBD.getPosition("" + baudMap.get(values.getBaudNumber())));
		spinnerAP.setSelection(adapterAP.getPosition("" + values.getAPmode()));
		if(values.getHardwareVersion() == 0x17){
			adapterCH = ArrayAdapter.createFromResource(context,R.array.s1_channels,android.R.layout.simple_spinner_item);
		}
		else if(values.getHardwareVersion() == 0x18){
			adapterCH = ArrayAdapter.createFromResource(context,R.array.s1_pro_channels,android.R.layout.simple_spinner_dropdown_item);
		}
		adapterCH.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinnerCH.setAdapter(adapterCH);
		spinnerCH.setEnabled(true);
		spinnerCH.setSelection(adapterCH.getPosition("" + values.getChannel()));
	}
	
	private boolean isValuesCorrect(){
		
		boolean isCorrect = true;
		try{
			if( Integer.parseInt(etID.getText().toString()) > 65535){
				showDialog("Incorrect Value", "The PANID provided is invalid!");
				isCorrect = false;
			}
			if( Integer.parseInt(etMY.getText().toString()) > 65535){
				showDialog("Incorrect Value", "The MYAddress provided is invalid!");
				isCorrect = false;
			}
		
			if( Integer.parseInt(etDH.getText().toString()) > Integer.MAX_VALUE ){
				showDialog("Incorrect Value", "The DHAddress provided is invalid!");
				isCorrect = false;
			}

			if( Integer.parseInt(etDL.getText().toString()) > Integer.MAX_VALUE ){
				showDialog("Incorrect Value", "The DLAddress provided is invalid!");
				isCorrect = false;
			}
		
			if( Integer.parseInt(etNT.getText().toString()) > 255){
				showDialog("Incorrect Value", "The NT value provided is invalid!");
				isCorrect = false;
			}
		}
		catch(Exception e){
			isCorrect = false;	
		}
			return isCorrect;
	}
	
	private void showDialog(String title, String message)
    {
      AlertDialog.Builder localBuilder = new AlertDialog.Builder(this);
      localBuilder.setTitle(title);
      localBuilder.setMessage(message);
      localBuilder.setPositiveButton("OK", null);
      localBuilder.show();
    }
	
	private JSONObject collectValues(){
		JSONObject object = new JSONObject();
		
		try{
			object.put("panId", etID.getText());
			object.put("channel", adapterCH.getItem(spinnerCH.getSelectedItemPosition()));
			object.put("myAddr", etMY.getText());
			object.put("dlAddr", etDL.getText());
			object.put("dhAddr", etDH.getText());
			object.put("nd_time", etNT.getText());
			object.put("baudNumber", baudMap2.get(Integer.parseInt(adapterBD.getItem(spinnerBD.getSelectedItemPosition()).toString())));
			object.put("APmode", adapterAP.getItem(spinnerAP.getSelectedItemPosition()));
			if(adapterCH.getCount() == 12){
				object.put("hardware_version", 24);
			}
			else if(adapterCH.getCount() == 16){
					object.put("hardware_version", 23);
			}
			
		}catch(JSONException e){}
		
		return object;
	}
}
