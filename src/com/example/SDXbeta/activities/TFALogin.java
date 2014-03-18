package com.example.SDXbeta.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import com.example.SDXbeta.AuthServer;
import com.example.SDXbeta.R;
import com.example.xbee_i2r.InitializeDevice;
import com.ftdi.j2xx.FT_Device;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.IOException;

public class TFALogin extends Activity {
    private FT_Device ftDev;
    String username;
    String password;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tfa_login);

        ftDev = InitializeDevice.getDevice();
    }

    public void onSubmitClicked(View view) {
        username = ((EditText) findViewById(R.id.editTextUsername)).getText().toString();
        password = ((EditText) findViewById(R.id.editTextPassword)).getText().toString();

        new LoginUserTask().execute();
    }

    private class LoginUserTask extends AsyncTask<Void, Void, Integer> {
        Integer statusCode;
        Toast toast;

        protected Integer doInBackground(Void... params) {
            DefaultHttpClient httpClient = new AuthServer().getNewHttpClient(username, password);
            HttpGet httpGet = new HttpGet(AuthServer.SERVER_URL + "login");

            try {
                HttpResponse response = httpClient.execute(httpGet);
                statusCode = response.getStatusLine().getStatusCode();
            } catch (ClientProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return statusCode;
        }

        protected void onPostExecute(Integer statusCode) {
            // User exists; login successful
            if (statusCode == 200) {
                toast = Toast.makeText(getBaseContext(), "Login successful!", Toast.LENGTH_SHORT);

                // Start new activity, passing it the credentials
                Intent intent = new Intent(getBaseContext(), TFARequest.class);
                intent.putExtra("username", username);
                intent.putExtra("password", password);

                startActivity(intent);
            }

            // Username/password don't match
            else if (statusCode == 401) {
                toast = Toast.makeText(getBaseContext(), "Username/password don't match. Please try again.", Toast.LENGTH_SHORT);
            }

            // some other error occurred
            else {
                toast = Toast.makeText(getBaseContext(), "An error occurred. Please try again.", Toast.LENGTH_SHORT);
            }

            toast.show();
        }
    }
}