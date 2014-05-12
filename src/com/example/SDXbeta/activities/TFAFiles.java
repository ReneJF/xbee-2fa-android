package com.example.SDXbeta.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import com.example.SDXbeta.R;
import com.example.xbee_i2r.InitializeDevice;
import com.ftdi.j2xx.FT_Device;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Author: sahil
 * Date: 12/5/14
 */
public class TFAFiles extends Activity {
    private FT_Device ftDev;
    String intentFiles;
    String authKey;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tfa_files);

        ftDev = InitializeDevice.getDevice();
        final ListView listView = (ListView) findViewById(R.id.listView);
        final ArrayList<String> list = new ArrayList<String>();

        // Get data from intent
        try {
            intentFiles = getIntent().getStringExtra("files");
            authKey = getIntent().getStringExtra("authKey");

            JSONArray files = new JSONArray(intentFiles);
            for (int i = 0; i < files.length(); i++) {
                JSONObject fileObject = files.getJSONObject(i);
                list.add("Filename: " + fileObject.getString("name") + " (" + fileObject.getString("size") + " bytes)");
            }

            final StableArrayAdapter adapter = new StableArrayAdapter(this, android.R.layout.simple_list_item_1, list);
            listView.setAdapter(adapter);

            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, final View view, int position, long id) {
                    Intent intent = new Intent(getBaseContext(), TFAFileDetails.class);
                    intent.putExtra("files", intentFiles);
                    intent.putExtra("position", position);
                    intent.putExtra("authKey", authKey);

                    startActivity(intent);
                }
            });

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private class StableArrayAdapter extends ArrayAdapter<String> {
        HashMap<String, Integer> mIdMap = new HashMap<String, Integer>();

        public StableArrayAdapter(Context context, int textViewResourceId, List<String> objects) {
            super(context, textViewResourceId, objects);

            for (int i = 0; i < objects.size(); i++) {
                mIdMap.put(objects.get(i), i);
            }
        }

        @Override
        public long getItemId(int position) {
            String item = getItem(position);
            return mIdMap.get(item);
        }

        public boolean hasStableIds() {
            return true;
        }
    }
}