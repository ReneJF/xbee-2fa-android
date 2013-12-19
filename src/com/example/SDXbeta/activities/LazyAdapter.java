package com.example.SDXbeta.activities;
/** An adapter class that inflates the res/layout/simple_list_item. Starts battery service when 
 *  list button is clicked. 
 * 
 * @author Nirav Gandhi A0088471@nus.edu.sg
 */
import java.util.ArrayList;

import com.example.SDXbeta.R;
import com.example.xbee_i2r.BatteryService;
import com.example.xbee_i2r.Node;
import com.example.xbee_i2r.SendCommands;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.TextView;


public class LazyAdapter extends BaseAdapter{
	
	protected static final String TAG = "com.example.SDXbeta.LazyAdapter";
	private Activity activity;
	private ArrayList<Node> nodeList;
	private static LayoutInflater inflater = null;
	private Messenger messenger;
	
	public LazyAdapter(Activity a,ArrayList<Node> d, Messenger messenger){
		activity = a;
		nodeList = d;
		inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		this.messenger = messenger;
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
		v = inflater.inflate(R.layout.simple_list_item, null); // each item follows the layout of res/layout/simple_list_item
		final Node node = nodeList.get(position);
		TextView address = (TextView) v.findViewById(R.id.simple_list_textView); 
		address.setTypeface(null,Typeface.BOLD);
		address.setText(node.toString());
		if(node.getBatteryPerc() != -1){ // Battery is displayed is the percentage is not default
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
				}catch(InterruptedException e){} // Buffer time given to the XBee to change the channel to the node's channel
				Intent intent = new Intent(activity,BatteryService.class);
				intent.putExtra("destAddr", node.getAddr16());
				Message msg = Message.obtain();
				try {
					messenger.send(msg);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
				activity.startService(intent); // Battery service called. The battery percentage is updated in the receiver of NodeDiscovery activity. 
			}
		});
		return v;
	}
	
	
}
