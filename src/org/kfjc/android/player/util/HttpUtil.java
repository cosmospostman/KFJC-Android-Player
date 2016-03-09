package org.kfjc.android.player.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class HttpUtil {
	
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
        try {
            InputStream in = new BufferedInputStream(urlConnection.getInputStream());
            return BitmapDrawable.createFromStream(in, "");
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
