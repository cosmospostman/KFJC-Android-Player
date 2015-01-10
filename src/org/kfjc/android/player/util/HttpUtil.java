package org.kfjc.android.player.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

public class HttpUtil {
	
	/**
	 * Fetches a URL over HTTP and returns content as a String.
	 */
	public static String getUrl(String url) throws IOException {
        InputStream inputStream;
        String result = "";
        HttpClient httpclient = new DefaultHttpClient();
        HttpResponse httpResponse = httpclient.execute(new HttpGet(url));
        inputStream = httpResponse.getEntity().getContent();
        if (inputStream != null) {
            result = convertInputStreamToString(inputStream);
        }
        return result;
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
