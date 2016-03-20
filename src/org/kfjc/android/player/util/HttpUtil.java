package org.kfjc.android.player.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.http.HttpResponseCache;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
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
	
	/**
	 * Fetches a URL over HTTP and returns content as a String.
	 */
	public static String getUrl(String urlString) throws IOException {
        URL url = new URL(urlString);
        String result = "";
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
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
     * Fetches a URL over HTTP and returns an InputStream to the content.
     */
    public static void loadToFile(String urlString, File lavaFile) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        FileOutputStream fileOutput = new FileOutputStream(lavaFile);
        int responseCode = urlConnection.getResponseCode();
        if (responseCode != 200) {
            throw new IOException(urlString + " got response code " + responseCode);
        }
        try {
            BufferedInputStream bis = new BufferedInputStream(urlConnection.getInputStream());
            byte[] buffer = new byte[1024];
            int bufferLength = 0;
            while ((bufferLength = bis.read(buffer)) > 0 ) {
                fileOutput.write(buffer, 0, bufferLength);
            }
        } finally {
            fileOutput.close();
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
