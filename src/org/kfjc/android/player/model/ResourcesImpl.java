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

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

public class ResourcesImpl implements Resources {

    private static final String TAG = ResourcesImpl.class.getSimpleName();
    ListeningExecutorService service = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(10));
    private Context context;
    private SettableFuture<Map<String, String>> streams;

    private static final int[] imagesOfTheHour = new int[] {
            R.drawable.b0, R.drawable.b0, R.drawable.b2,   // 0 1 2
            R.drawable.b2, R.drawable.b4, R.drawable.b4,    // 3 4 5
            R.drawable.b6, R.drawable.b6, R.drawable.b8,   // 6 7 8
            R.drawable.b8, R.drawable.b10, R.drawable.b10,    // 9 10 11
            R.drawable.b12, R.drawable.b12, R.drawable.b14,   // 12 13 14
            R.drawable.b14, R.drawable.b16, R.drawable.b16,    // 15 16 17
            R.drawable.b18, R.drawable.b18, R.drawable.b20,   // 18 19 20
            R.drawable.b20, R.drawable.b22, R.drawable.b22     // 21 22 23
    };

    public ResourcesImpl(Context context) {
        this.context = context;
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
    public ListenableFuture<Drawable> getBackgroundImage(final int hourOfDay) {
        return service.submit(new Callable<Drawable>() {
            @Override
            public Drawable call() throws Exception {
//                return HttpUtil.getDrawable("http://kfjc.org/images/home_images_rotate/black_mountain.jpg");
                return ContextCompat.getDrawable(context, imagesOfTheHour[hourOfDay]);
            }
        });

    }
}
