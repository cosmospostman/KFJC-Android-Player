package org.kfjc.android.player.util;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.http.HttpResponseCache;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class HttpUtil {

    private static final String TAG = HttpUtil.class.getSimpleName();

    public static void installCache(Context context) {
        try {
            File httpCacheDir = new File(context.getCacheDir(), "http");
            long httpCacheSize = 20 * 1024 * 1024; // 20 MiB
            HttpResponseCache.install(httpCacheDir, httpCacheSize);
        } catch (IOException e) {
            Log.i(TAG, "HTTP response cache installation failed:" + e);
        }
    }

    public static String getUrl(String urlString) throws IOException {
        return getUrl(urlString, false);
    }
	
	/**
	 * Fetches a URL over HTTP and returns content as a String.
	 */
	public static String getUrl(String urlString, boolean useCache) throws IOException {
        URL url = new URL(urlString);
        String result = "";
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.setUseCaches(useCache);
        try {
            InputStream in = new BufferedInputStream(urlConnection.getInputStream());
            result = convertInputStreamToString(in);
        } finally {
            urlConnection.disconnect();
        }
        return result;
    }

    /**
     * Fetches a URL over HTTP and returns content as a String.
     */
    public static Drawable getDrawable(String urlString) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.setUseCaches(true);
        int responseCode = urlConnection.getResponseCode();
        if (responseCode != 200) {
            throw new IOException(urlString + " got response code " + responseCode);
        }
        try {
            return BitmapDrawable.createFromStream(urlConnection.getInputStream(), "");
        } finally {
            urlConnection.disconnect();
        }
    }
 
	/**
	 * Reads an InputStream until it's dry and returns streamed contents as a String.
	 */
    private static String convertInputStreamToString(InputStream inputStream) throws IOException{
        BufferedReader bufferedReader = new BufferedReader( new InputStreamReader(inputStream));
        StringBuilder result = new StringBuilder();
        String line = "";
        while ((line = bufferedReader.readLine()) != null) {
            result.append(line);
        }
        inputStream.close();
        return result.toString();
    }
}
