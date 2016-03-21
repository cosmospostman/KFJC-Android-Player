package org.kfjc.android.player.model;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.kfjc.android.player.Constants;
import org.kfjc.android.player.R;
import org.kfjc.android.player.util.HttpUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

public class ResourcesImpl implements Resources {

    private static final String TAG = ResourcesImpl.class.getSimpleName();
    ListeningExecutorService service = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(10));
    private Context context;
    private SettableFuture<Map<String, String>> streams;
    private List<String> backgroundsUrls;
    private String lavaUrl;

    public ResourcesImpl(Context context) {
        this.context = context;
        streams = SettableFuture.create();

        new AsyncTask<Void, Void, Void>() {
            @Override protected Void doInBackground(Void... unsedParams) {
                loadResources();
                return null;
            }
        }.execute();
    }

    private void loadResources() {
        try {
            String resourcesString = HttpUtil.getUrl(Constants.RESOURCES_URL);
            JSONObject jResources = new JSONObject(resourcesString);

            // Streams
            JSONArray jStreams = jResources.getJSONArray("streams");
            Map<String, String> streamMap = new HashMap<>();
            for (int i = 0; i < jStreams.length(); i++) {
                JSONObject stream = jStreams.getJSONObject(i);
                String name = stream.getString("name");
                String url = stream.getString("url");
                streamMap.put(name, url);
            }
            streams.set(streamMap);

            // Backgrounds
            JSONObject jDrawables = jResources.getJSONObject("drawables");
            JSONArray jBackgrounds = jDrawables.getJSONArray("backgrounds");
            backgroundsUrls = new ArrayList<>();
            for (int i = 0; i < jBackgrounds.length(); i++) {
                if (!jBackgrounds.isNull(i)) {
                    backgroundsUrls.add(jBackgrounds.getString(i));
                }
            }

            // Lava
            lavaUrl = jDrawables.getString("lava");

        } catch (JSONException | IOException e) {
            Log.e(TAG, "Caught exception parsing streams: " + e.getMessage());
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
    public ListenableFuture<Drawable> getBackgroundImage(final int hourOfDay) {
        return service.submit(new Callable<Drawable>() {
            @Override
            public Drawable call() throws Exception {
                Drawable backgroundImage = ContextCompat.getDrawable(context, R.drawable.default_background);
                try {
                    Drawable newBackgroundImage = HttpUtil.getDrawable(backgroundsUrls.get(hourOfDay));
                    if (newBackgroundImage != null) {
                        backgroundImage = newBackgroundImage;
                    }
                } catch (IOException e) {}
                return backgroundImage;
            }
        });
    }
}
