package com.roberteves.heobserver.core;

import android.os.StrictMode;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;

public class Util {
    public static void setupThreadPolicy() {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                .permitNetwork().build();
        StrictMode.setThreadPolicy(policy);
    }

    public static String getWebSource(String Url) throws IOException {
        HttpClient httpclient = new DefaultHttpClient(); // Create HTTP Client
        HttpGet httpget = new HttpGet(Url); // Set the action you want to do
        HttpResponse response = httpclient.execute(httpget); // Executeit
        HttpEntity entity = response.getEntity();

        InputStream is = response.getEntity().getContent();
        Header contentEncoding = response.getFirstHeader("Content-Encoding");

        BufferedReader reader;
        if ((contentEncoding != null) && contentEncoding.getValue().equalsIgnoreCase("gzip")) {
            InputStream gzipIs = new GZIPInputStream(is);
            reader = new BufferedReader(new InputStreamReader(gzipIs), 8);
        } else {
            reader = new BufferedReader(new InputStreamReader(is), 8);
        }

        StringBuilder sb = new StringBuilder();
        String line = null;
        while ((line = reader.readLine()) != null) // Read line by line
            sb.append(line + "\n");

        String resString = sb.toString(); // Result is here

        is.close(); // Close the stream

        return resString;
    }
}