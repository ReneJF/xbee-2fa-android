package com.example.testxbeeproject.activities;

import java.util.ArrayList;

import com.example.testxbeeproject.R;
import com.example.xbee_i2r.BatteryService;
import com.example.xbee_i2r.Node;
import com.example.xbee_i2r.SendCommands;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

public class LazyAdapter extends BaseAdapter{
	
	protected static final String TAG = "com.example.testxbeeproject.LazyAdapter";
	private Activity activity;
	private ArrayList<Node> nodeList;
	private static LayoutInflater inflater = null;
	private boolean isChecked;
	
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
		v = inflater.inflate(R.layout.simple_list_item, null);
		final Node node = nodeList.get(position);
		TextView address = (TextView) v.findViewById(R.id.simple_list_textView);
		address.setTypeface(null,Typeface.BOLD);
		address.setText(node.toString());
		if(node.getBatteryPerc() != -1){
			TextView batteryText = (TextView) v.findViewById(R.id.etBattery);
			batteryText.setText("" + node.getBatteryPerc() + "%");
		}
		TextView channelText = (TextView) v.findViewById(R.id.etChannel);
		channelText.setText("" + node.getChannel());
		TextView RSSIText = (TextView) v.findViewById(R.id.etRSSI);
		RSSIText.setText("" + node.getRssi());
		final ImageButton batteryButton = (ImageButton) v.findViewById(R.id.batteryButton);
		batteryButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				SendCommands send = new SendCommands();
				send.changeChannel(node.getChannel(),true);
				try{
					Thread.sleep(25);
				}catch(InterruptedException e){}
				Intent intent = new Intent(activity,BatteryService.class);
				intent.putExtra("destAddr", node.getAddr16());
				final Object object = new Object();
				NodeDiscovery.showProgressDialog(object);
				Thread dismissDialog = new Thread(){
					public void run(){
						try {
							synchronized(object){
								object.wait(10000);
							}
						} catch (InterruptedException e1) {
							e1.printStackTrace();
						}
						NodeDiscovery.dismissProgressDialog();
					}
				};
				dismissDialog.start();
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
