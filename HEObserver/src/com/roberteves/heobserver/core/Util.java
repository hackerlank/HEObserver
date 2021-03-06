package com.roberteves.heobserver.core;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.roberteves.heobserver.BuildConfig;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.zip.GZIPInputStream;

public class Util {
    public static final int timeout = 5000;

    public static Boolean isNetworkAvailable(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (activeNetwork != null && activeNetwork.isConnectedOrConnecting()) {
            LogMessage("Network", "Connected to " + activeNetwork.getTypeName() + ": " + activeNetwork.getExtraInfo() + " " + activeNetwork.getSubtypeName());
            return true;
        } else {
            LogMessage("Network", "Not Connected");
            return false;
        }
    }

    public static Boolean isInternetAvailable() {
        try {
            URL url = new URL("http://www.hertsandessexobserver.co.uk/");
            HttpURLConnection urlc = (HttpURLConnection) url.openConnection();
            urlc.setRequestProperty("User-Agent", "test");
            urlc.setRequestProperty("Connection", "close");

            urlc.setConnectTimeout(timeout); //timeout if cant connect within 1s
            urlc.setReadTimeout(timeout); //timeout if cant read within 1s

            urlc.connect();
            if (urlc.getResponseCode() == 200) {
                LogMessage("Internet", "Connected");
                return true;
            } else {
                LogMessage("Internet", "Not Connected (" + urlc.getResponseCode() + ")");
                return false;
            }
        } catch (Exception e) {
            LogException("Internet", "Check Internet Access Error", e);
            return false;
        }
    }

    public static String getWebSource(String Url) throws IOException {
        HttpClient httpclient = new DefaultHttpClient(); // Create HTTP Client

        HttpConnectionParams.setConnectionTimeout(httpclient.getParams(), timeout); //Connection Timeout
        HttpConnectionParams.setSoTimeout(httpclient.getParams(), timeout); //Socket Timeout

        HttpGet httpget = new HttpGet(Url); // Set the action you want to do
        HttpResponse response = httpclient.execute(httpget); // Execute it
        HttpEntity entity = response.getEntity();

        InputStream is = entity.getContent();
        Header contentEncoding = response.getFirstHeader("Content-Encoding");

        BufferedReader reader;
        if ((contentEncoding != null) && contentEncoding.getValue().equalsIgnoreCase("gzip")) {
            InputStream gzipIs = new GZIPInputStream(is);
            reader = new BufferedReader(new InputStreamReader(gzipIs), 8);
        } else {
            reader = new BufferedReader(new InputStreamReader(is), 8);
        }

        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) // Read line by line
            sb.append(line).append("\n");

        String resString = sb.toString(); // Result is here

        is.close(); // Close the stream

        return resString;
    }

    public static void LogException(String action, String data, Exception e) {
        if (BuildConfig.DEBUG) {
            //only log in logcat if in debug
            Log.w("Fabric", "Caught Exception: Action: " + action + "; Data: " + data + "; Exception: " + e.toString());
        }

        if(e instanceof SocketTimeoutException || e instanceof UnknownHostException || e.getMessage().contains("junk after document element")){
            LogMessage("Exception",e.toString());
        }
        else {
            Crashlytics.setString("action", action);
            Crashlytics.setString("data", data);
            Crashlytics.logException(e);
        }
    }

    public static void LogMessage(String tag, String message) {
        if (BuildConfig.DEBUG) {
            Log.i(tag, message); //only log in logcat if in debug
        }

        Crashlytics.log("[" + tag + "] " + message);
    }

    public static void DisplayToast(final Activity activity, final String message) {
        new Thread() {
            public void run() {
                activity.runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }.start();
    }
}