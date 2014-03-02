package com.example.SDXbeta.activities;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Credentials;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.*;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ToggleButton;
import com.example.SDXbeta.MyHttpClient;
import com.example.SDXbeta.R;
import com.example.SDXbeta.SimpleCrypto;
import com.example.xbee_i2r.*;
import com.ftdi.j2xx.FT_Device;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;

import javax.net.ssl.*;
import javax.net.ssl.TrustManagerFactory;
import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by sahil on 18/2/14.
 */
public class TwoFactorAuthenticationLogin extends Activity {
    EditText editTextUsername;
    EditText editTextPassword;
    private FT_Device ftDev;
    private BroadcastReceiver receiver;
    int[] revData;
    private static final String SERVER_URL = "https://10.0.1.2:8000/";

    protected void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter(ReadService.TWO_FA__ACTION);

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                revData = intent.getIntArrayExtra("reverseData");

//                ToggleButton toggle = (ToggleButton) findViewById(R.id.toggleButton);
//                toggle.setChecked(ledState == 0 ? false : true);

//                TextView deviceId = (TextView) findViewById(R.id.deviceIdTextView);
//                deviceId.setText(intent.getStringExtra("sourceAddress"));
//
//                TextView rssi = (TextView) findViewById(R.id.rssiTextView);
//                rssi.setText(Integer.toString(intent.getIntExtra("rssi", 0)));

//                if (intent.getIntExtra("ledState", null)Extra("XCTUValues") != null) {
//                    XCTUValues values = gson.fromJson(intent.getStringExtra("XCTUValues"), XCTUValues.class);
//                    fillUI(values);
//                } else if (intent.getStringExtra("Response Status") != null) {
//                    Toast.makeText(arg1, "Values Changed Successfully", Toast.LENGTH_SHORT).show();
//                }

                Log.d("REVERSED DATA: ", revData.toString());
            }
        };
        registerReceiver(receiver,filter);
    }

    @Override
    protected void onStop(){
        super.onStop();
        unregisterReceiver(receiver);
    }


    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tfa_login);

        ftDev = InitializeDevice.getDevice();
    }

    public void onSubmitClicked(View view) {
        editTextUsername = (EditText)findViewById(R.id.editTextUsername);
        editTextPassword = (EditText)findViewById(R.id.editTextPassword);
        new LoginUserTask().execute(SERVER_URL + "api");
    }

    public void onSendDataClicked(View view) {
        byte[] data = { 0x0, 0x1, 0x2, 0x3, 0x4, 0x0, 0x1, 0x2, 0x3, 0x4, 0x0, 0x1, 0x2, 0x3, 0x4, 0x0, 0x1, 0x2, 0x3, 0x4 };
        byte[] key = { 0x0, 0x1, 0x2, 0x3, 0x4, 0x5, 0x6, 0x7, 0x0, 0x1, 0x2, 0x3, 0x4, 0x5, 0x6, 0x7 };

        try {
            String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
            byte[] result = SimpleCrypto.toByte(deviceId);
//            byte[] result = SimpleCrypto.encrypt(key, data);

            ByteBuffer byteBuffer = ByteBuffer.wrap(result);

            int[] payload = new int[result.length];
            for (int i = 0; i < result.length; i++) {
                payload[i] = (int) result[i];
            }


            XBeeAddress16 destination = new XBeeAddress16(0xFF, 0xFF);
//            int[] payload = new int[] { isOn ? 0xFF : 0x0 };

            TxRequest16 request = new TxRequest16(destination, payload);
            XBeePacket packet = new XBeePacket(request.getFrameData());

            byte[] outData = new byte[packet.getIntegerArray().length];

            for(int j=0;j<packet.getIntegerArray().length;j++){
                outData[j] = (byte) (packet.getIntegerArray()[j] & 0xff);
            }

            synchronized(ftDev){
                if(ftDev.isOpen() == false){
                    return;
                }

                ftDev.write(outData,outData.length);
            }
        }

        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class LoginUserTask extends AsyncTask<String, Void, Long> {
        protected Long doInBackground(String... urls) {
           /* try {
                CertificateFactory cf = CertificateFactory.getInstance("X.509");
//                InputStream caInput = new BufferedInputStream(new FileInputStream("load-der.crt"));
                Certificate ca;


//                ca = cf.generateCertificate(caInput);
//                Log.d("CERTIFICATE! ", "ca=" + ((X509Certificate) ca).getSubjectDN());

                // Create KeyStore containing trusted CAs
                String keyStoreType = KeyStore.getDefaultType();
//                KeyStore keyStore = KeyStore.getInstance(keyStoreType);
                KeyStore keyStore = KeyStore.getInstance("BKS");

                InputStream in2 = getBaseContext().getResources().openRawResource(R.raw.mystore);
                keyStore.load(in2, "123456".toCharArray());

//                keyStore.load(null, null);
//                keyStore.setCertificateEntry("ca", ca);

                // Create a TrustManager that trusts the CAs in our KeyStore
                String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
//                TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
                TrustManagerFactory tmf = TrustManagerFactory.getInstance("X509");
                tmf.init(keyStore);

                // Create an SSLContext that uses our TrustManager
                SSLContext context = SSLContext.getInstance("TLS");
                context.init(null, tmf.getTrustManagers(), null);

                // Tell the URLConnection to use a SocketFactory from our SSLContext
                URL url = new URL(urls[0]);
                HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
                urlConnection.setSSLSocketFactory(context.getSocketFactory());

                InputStream in = urlConnection.getInputStream();
                Log.d("INPUT STREAM", in.toString());
            } catch (Exception e) {
                e.printStackTrace();
            }*/

            ////////////////////////////////////////////////////////////
//            HttpClient httpClient = new MyHttpClient(getBaseContext());
            HttpClient httpClient = getNewHttpClient();
            HttpPost httpPost = new HttpPost(urls[0]);

            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
            nameValuePairs.add(new BasicNameValuePair("username", editTextUsername.getText().toString()));
            nameValuePairs.add(new BasicNameValuePair("password", editTextPassword.getText().toString()));

            try {
                httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            try {
                HttpResponse response = httpClient.execute(httpPost);
                Log.d("HTTP RESPONSE: ", response.toString());

                Integer statusCode = response.getStatusLine().getStatusCode();

                if (statusCode == 200) {
                    Log.d("USER LOGIN", " SUCCESS");

                    // Request 2FA authentication with sensor node
                    HttpGet httpGet = new HttpGet("http://172.22.83.134:4000/key/209");
                    response = httpClient.execute(httpGet);

                    BufferedReader rd = new BufferedReader(
                            new InputStreamReader(response.getEntity().getContent()));

                    StringBuffer result = new StringBuffer();
                    String line;

                    while ((line = rd.readLine()) != null) {
                        result.append(line);
                    }

                    Log.d("KEY RESULT: ", result.toString());

                    try {
                        XBeeAddress16 destination = new XBeeAddress16(0xFF, 0xFF);
                        int[] payload = new int[] { 0x00, 0x01, 0x02, 0x03 };

                        TxRequest16 request = new TxRequest16(destination, payload);
                        XBeePacket packet = new XBeePacket(request.getFrameData());

                        byte[] outData = new byte[packet.getIntegerArray().length];

                        for(int j=0;j<packet.getIntegerArray().length;j++){
                            outData[j] = (byte) (packet.getIntegerArray()[j] & 0xff);
                        }

                        synchronized(ftDev){
                            if(ftDev.isOpen() == false){
                                return 1L;
                            }

                            ftDev.write(outData,outData.length);
                        }
                    }

                    catch (NullPointerException e) {
                        e.printStackTrace();
                    }

                } else if (statusCode == 401) {
                    Log.d("USER LOGIN", " FAILURE");
                }

//                HttpEntity entity = response.getEntity();
//
//                StringBuilder sb = new StringBuilder();
////                try {
//                BufferedReader reader = new BufferedReader(new InputStreamReader(entity.getContent()), 65728);
//                String line;
//
//                while ((line = reader.readLine()) != null) {
//                    sb.append(line);
//                }
//
//                Log.e("RESPONSE LINE: ", line);
//                }
            } catch (ClientProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return 0L;
        }

        protected void onPostExecute(Long result) {

        }

        public HttpClient getNewHttpClient() {
            try {
                KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
                trustStore.load(null, null);

                SSLSocketFactory sf = new MySSLSocketFactory(trustStore);
                sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

                HttpParams params = new BasicHttpParams();
                HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
                HttpProtocolParams.setContentCharset(params, HTTP.UTF_8);

                SchemeRegistry registry = new SchemeRegistry();
                registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
                registry.register(new Scheme("https", sf, 443));

                ClientConnectionManager ccm = new ThreadSafeClientConnManager(params, registry);

                CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                credentialsProvider.setCredentials(new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT), new UsernamePasswordCredentials("admin", "admin"));

                DefaultHttpClient httpClient = new DefaultHttpClient(ccm, params);
                httpClient.setCredentialsProvider(credentialsProvider);

                return httpClient;
            } catch (Exception e) {
                return new DefaultHttpClient();
            }
        }
    }

    private class MySSLSocketFactory extends SSLSocketFactory {
        SSLContext sslContext = SSLContext.getInstance("TLS");

        public MySSLSocketFactory(KeyStore truststore) throws NoSuchAlgorithmException, KeyManagementException, KeyStoreException, UnrecoverableKeyException {
            super(truststore);

            TrustManager tm = new X509TrustManager() {
                public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                }

                public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                }

                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
            };

            sslContext.init(null, new TrustManager[] { tm }, null);
        }

        @Override
        public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException, UnknownHostException {
            return sslContext.getSocketFactory().createSocket(socket, host, port, autoClose);
        }

        @Override
        public Socket createSocket() throws IOException {
            return sslContext.getSocketFactory().createSocket();
        }
    }
}