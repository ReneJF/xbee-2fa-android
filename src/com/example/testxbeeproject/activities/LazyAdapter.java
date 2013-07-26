package com.example.testxbeeproject.activities;

import java.util.ArrayList;

import com.example.testxbeeproject.R;
import com.example.xbee_i2r.BatteryService;
import com.example.xbee_i2r.ByteUtils;
import com.example.xbee_i2r.Node;
import com.example.xbee_i2r.SendCommands;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

public class LazyAdapter extends BaseAdapter{
	
	protected static final String TAG = "com.example.testxbeeproject.LazyAdapter";
	private Activity activity;
	private ArrayList<Node> nodeList;
	private static LayoutInflater inflater = null;
	private boolean isChecked;
	private ProgressDialog progressDialog;
	
	public LazyAdapter(Activity a,ArrayList<Node> d){
		activity = a;
		nodeList = d;
		inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}
	@Override
	public int getCount() {
		return nodeList.size();
	}

	@Override
	public Object getItem(int position ) {
		return position;
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		View v= convertView;

		if(v == null){
			v = inflater.inflate(R.layout.simple_list_item, null);
		}
		
		final Node node = nodeList.get(position);
		TextView address = (TextView) v.findViewById(R.id.simple_list_textView);
		address.setText(node.toString());
		
		Button batteryButton = (Button) v.findViewById(R.id.batteryButton);
		batteryButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				SendCommands send = new SendCommands();
				send.changeChannel(node.getChannel());
				try{Thread.sleep(25);
				}catch(InterruptedException e){}
				Intent intent = new Intent(activity,BatteryService.class);
				intent.putExtra("destAddr", node.getAddr16());
				Log.d(TAG,"Node button clicked " + ByteUtils.toBase16(node.getAddr16()));
				NodeDiscoveryTest.showProgressDialog();
				activity.startService(intent);
			}
		});
		if(isChecked){
		}
		return v;
	}
	
	public void setIsChecked(boolean isChecked){
		this.isChecked = isChecked;
	}
}
