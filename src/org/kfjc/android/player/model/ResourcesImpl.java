package org.kfjc.android.player.model;

import android.content.Context;
import android.graphics.drawable.Drawable;
import androidx.core.content.ContextCompat;
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
import org.kfjc.android.player.control.PreferenceControl;
import org.kfjc.android.player.util.HttpUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

public class ResourcesImpl implements Resources {

    private static final String TAG = ResourcesImpl.class.getSimpleName();
    ListeningExecutorService service = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(10));
    private Context context;
    private SettableFuture<List<KfjcMediaSource>> streamsList;
    private List<String> backgroundsUrls;
    private SettableFuture<String> lavaUrl;

    public ResourcesImpl(Context context) {
        this.context = context;
        streamsList = SettableFuture.create();
        lavaUrl = SettableFuture.create();
    }

    @Override
    public void loadResources() {
        try {
            String resourcesString = HttpUtil.getUrl(Constants.RESOURCES_URL);
            PreferenceControl.setCachedResources(resourcesString);
            parseResourceString(resourcesString);
        } catch (IOException e) {
            parseResourceString(PreferenceControl.getCachedResources());
        }
    }

    private void parseResourceString(String resources) {
        try {
            JSONObject jResources = new JSONObject(resources);

            // Streams
            JSONArray jStreams = jResources.getJSONArray("streams");
            Map<String, String> streamMap = new HashMap<>();
            List<KfjcMediaSource> kfjcMediaSourceList = new ArrayList<>();
            for (int i = 0; i < jStreams.length(); i++) {
                JSONObject stream = jStreams.getJSONObject(i);
                String url = stream.getString("url");
                String name = stream.getString("name");
                String description = stream.getString("desc");
                String formatString = stream.getString("format");
                KfjcMediaSource.Format format = KfjcMediaSource.Format.NONE;
                if (formatString.toLowerCase().equals("aac")) {
                    format = KfjcMediaSource.Format.AAC;
                } else if (formatString.toLowerCase().equals("mp3")) {
                    format = KfjcMediaSource.Format.MP3;
                }
                kfjcMediaSourceList.add(new KfjcMediaSource(url, format, name, description));
                streamMap.put(name, url);
            }
            streamsList.set(kfjcMediaSourceList);

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
            lavaUrl.set(jDrawables.getString("lava"));
        } catch (JSONException e) {
            Log.e(TAG, "Caught exception parsing streams: " + e.getMessage());
            streamsList.set(Arrays.asList(Constants.FALLBACK_MEDIA_SOURCE));
        }
    }

    @Override
    public SettableFuture<List<KfjcMediaSource>> getStreamsList() {
        return streamsList;
    }

    @Override
    public ListenableFuture<Drawable> getBackgroundImage(final int hourOfDay) {
        return service.submit(new Callable<Drawable>() {
            @Override
            public Drawable call() throws Exception {
                try {
                    Drawable newBackgroundImage = HttpUtil.getDrawable(backgroundsUrls.get(hourOfDay));
                    if (newBackgroundImage != null) {
                        return newBackgroundImage;
                    }
                } catch (IOException e) {}
                return ContextCompat.getDrawable(context, R.drawable.bg_default);
            }
        });
    }

    @Override
    public SettableFuture<String> getLavaUrl() {
        return lavaUrl;
    }
}
