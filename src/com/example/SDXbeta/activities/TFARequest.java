package com.example.SDXbeta.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import com.example.SDXbeta.AuthServer;
import com.example.SDXbeta.R;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Created by sahil on 3/3/14.
 */
public class TFARequest extends Activity {
    EditText editTextNodeId;
    String username;
    String password;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tfa_request);

        // Get username/password from intent (sent by Login activity)
        username = getIntent().getStringExtra("username");
        password = getIntent().getStringExtra("password");
    }

    public void onRequestKeyClicked(View view) {
        editTextNodeId = (EditText) findViewById(R.id.editTextNodeId);
        new RequestKeyTask().execute(AuthServer.SERVER_URL + "keys/" + editTextNodeId.getText().toString());
    }

    public void onRequest2FAClicked(View view) {
        // TODO request node for 2FA
        // Request format (encrypted)
        // Nonce
        // IMEI/device ID
        // NodeId
        // timestamp
    }

    private class RequestKeyTask extends AsyncTask<String, Void, String> {
        String authKey;
        Toast toast;

        protected String doInBackground(String... urls) {
            AuthServer authServer = new AuthServer();
            DefaultHttpClient httpClient = authServer.getNewHttpClient();
            HttpGet httpGet = new HttpGet(urls[0]);

            // Set username and password for request
            CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(
                    new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT),
                    new UsernamePasswordCredentials(username, password)
            );

            httpClient.setCredentialsProvider(credentialsProvider);

            try {
                HttpResponse response = httpClient.execute(httpGet);
                Integer statusCode = response.getStatusLine().getStatusCode();

                if (statusCode == 200) {
                    // Get response content
                    String line;
                    StringBuilder stringBuilder = new StringBuilder();
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));

                    while ((line = bufferedReader.readLine()) != null) {
                        stringBuilder.append(line);
                    }

                    JSONObject nodeObject = new JSONObject(stringBuilder.toString());
                    return nodeObject.getString("authKey");
                }
            }
            catch (ClientProtocolException e) {
                e.printStackTrace();
            }
            catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return authKey;
        }

        protected void onPostExecute(String authKey) {
            if (authKey == null) {
                toast = Toast.makeText(getBaseContext(), "An error occurred", Toast.LENGTH_SHORT);
            }

            else {
                toast = Toast.makeText(getBaseContext(), "Key: " + authKey, Toast.LENGTH_SHORT);

                // Enable the request 2FA button
                findViewById(R.id.requestNode2FA).setEnabled(true);
            }

            toast.show();
        }
    }
}