package org.kfjc.android.player.model;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.kfjc.android.player.Constants;
import org.kfjc.android.player.util.HttpUtil;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class ResourcesImpl implements Resources {

    private static final String TAG = ResourcesImpl.class.getSimpleName();
    SettableFuture<Map<String, String>> streams;

    public ResourcesImpl() {
        streams = SettableFuture.create();

        new AsyncTask<Void, Void, Void>() {
            @Override protected Void doInBackground(Void... unsedParams) {
                loadStreams();
                return null;
            }
        }.execute();
    }

    private void loadStreams() {
        try {
            String availableStreams = HttpUtil.getUrl(Constants.AVAILABLE_STREAMS_URL);
            JSONArray jsonStreams = new JSONArray(availableStreams);
            Map<String, String> streamMap = new HashMap<>();
            for (int i = 0; i < jsonStreams.length(); i++) {
                JSONObject stream = jsonStreams.getJSONObject(i);
                String name = stream.getString("name");
                String url = stream.getString("url");
                streamMap.put(name, url);
            }
            streams.set(streamMap);
        } catch (JSONException e) {
            Log.e(TAG, "Caught exception parsing streams: " + e.getMessage());
            streams.set(defaultStream());
        } catch (IOException e) {
            Log.e(TAG, "Caught exception getting streams: " + e.getMessage());
            streams.set(defaultStream());
        }
    };

    private static Map<String, String> defaultStream() {
        Map<String, String> streamMap = new LinkedHashMap<>();
        streamMap.put(Constants.FALLBACK_STREAM_NAME, Constants.FALLBACK_STREAM_URL);
        return streamMap;
    }

    @Override
    public ListenableFuture<Map<String, String>> getStreams() {
        return streams;
    }

    @Override
    public ListenableFuture<Bitmap> getBackgroundImage(int hourOfDay) {
        return null;
    }
}
