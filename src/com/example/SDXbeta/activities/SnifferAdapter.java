package com.example.SDXbeta.activities;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import com.example.JSON_format.BeaconPkt;
import com.example.JSON_format.RxResponseJSON;
import com.example.xbee_i2r.R;

import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class SnifferAdapter extends BaseAdapter{

	protected static final String TAG = "com.example.testxbeeproject.SnifferAdapter";
	private ArrayList<RxResponseJSON> responseList;
	private static LayoutInflater inflater = null;
	
	public SnifferAdapter(Activity activity,ArrayList<RxResponseJSON> list){
		inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		responseList = list;
	}
	@Override
	public int getCount() {
		return responseList.size();
	}

	@Override
	public Object getItem(int position) {
		return responseList.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if(convertView == null){
			convertView = inflater.inflate(R.layout.response_packet_layout, null);
		}
		TextView rssi = (TextView) convertView.findViewById(R.id.rssi);
		TextView sourceAddress = (TextView) convertView.findViewById(R.id.sourceAddress);
		TextView parsedData = (TextView) convertView.findViewById(R.id.data);
		TextView time = (TextView) convertView.findViewById(R.id.time);
		
		RxResponseJSON response = responseList.get(position);
		rssi.setText("RSSI:" + response.getRSSI());
		sourceAddress.setText("MY Address:" + response.getSourceAddress() + " ");
		sourceAddress.setTypeface(null,Typeface.BOLD);
		String dl_frame_header = "DL FRAME HEADER" + "\n" +
				"DL Version: " + response.getDl_version()
				+ " Frame Type: " + response.getDl_frame_type()
				+ " Source: " + response.getDl_source()
				+ " Destination: " + response.getDl_dest()
				+ " Sequence Number: " + response.getDl_seqNo();
		String nwHdr ="\n" + "NW_HDR " + "\n" 
				+ " Source:" + response.getNw_source()
				+ " Destination:" + response.getNw_dest()
				+ " Version:" +	response.getNw_version()
				+ " Prototype:" + response.getNw_proto()
				+ " Packet Type:" + response.getNw_pkt_type();
		String helloSeqNum = "\n" + "HELLO SEQ NUM: " + response.getHello_seq_num();
		String unixTime = "\n" + "UNIX TIME: " + response.getUnix_time();
		String beaconString = "\n" + "BEACON PACKETS";
		for(int i=0;i<response.getBeacon_pkts().length;i++){
			BeaconPkt pkt = response.getBeacon_pkts()[i];
			beaconString = beaconString.concat("\n" + "Gw address:" + pkt.getGwAddr() + 
					" Sequence No. :" + pkt.getSeqNum() + 
					" Metric : " + pkt.getMetric());
		}
		parsedData.setText(dl_frame_header + nwHdr + helloSeqNum + unixTime + beaconString);
		Date date = new Date(response.getReceivedTime());
		DateFormat formatter = new SimpleDateFormat("HH:mm:ss");
		String receivedTime= formatter.format(date);
		time.setText("Received at : " + receivedTime + " ");
		return convertView;
	}
}
