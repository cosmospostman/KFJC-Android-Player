package org.kfjc.android.player.control;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.design.widget.Snackbar;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.kfjc.android.player.Constants;
import org.kfjc.android.player.R;
import org.kfjc.android.player.activity.HomeScreenInterface;
import org.kfjc.android.player.util.HttpUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PreferenceControl {

	private static final String TAG = PreferenceControl.class.getSimpleName();
	private static final String PREFERENCE_KEY = "kfjc.preferences";
	private static final String STREAM_PREFERENCE_KEY = "kfjc.preferences.streamname";
    private static final int PREFERENCE_MODE = Context.MODE_PRIVATE;
	
	private static SharedPreferences preferences;
	private static Map<String, String> streamMap = new LinkedHashMap<>();

	private HomeScreenInterface activity;

	public PreferenceControl(Context context, HomeScreenInterface activity) {
		this.activity = activity;
		preferences = context.getSharedPreferences(PREFERENCE_KEY, PREFERENCE_MODE);
		new AsyncTask<Void, Void, Void>() {
			@Override protected Void doInBackground(Void... unsedParams) {
				loadStreams();
				return null;
			}
		}.execute();
	}
	
	private void loadStreams() {
		try {
			activity.snack(
					activity.getString(R.string.status_connecting), Snackbar.LENGTH_INDEFINITE);
			JSONArray streams = new JSONArray(getAvailableStreams());
			for (int i = 0; i < streams.length(); i++) {
				JSONObject stream = streams.getJSONObject(i);
				String name = stream.getString("name");
				String url = stream.getString("url");
				streamMap.put(name, url);
			}
			activity.snackDone();
		} catch (JSONException e) {
			Log.e(TAG, "Caught exception parsing streams: " + e.getMessage());
			streamMap = new LinkedHashMap<>();
		}
	}
	
	private String getAvailableStreams() {
        try {
            return HttpUtil.getUrl(Constants.AVAILABLE_STREAMS_URL);
        } catch (IOException e) {
			activity.snack(
					activity.getString(R.string.status_default_stream), Snackbar.LENGTH_LONG);
			Log.e(TAG, "Using fallback stream");
			return Constants.FALLBACK_STREAM_JSON;
        }
	}

	private static Map.Entry<String, String> getFirstStream() {
		Iterator<Map.Entry<String, String>> it = streamMap.entrySet().iterator();
		if (it.hasNext()) {
			return it.next();
		}
		return null;
	}

	public static String getStreamNamePreference() {
		String pref = preferences.getString(STREAM_PREFERENCE_KEY, "");
		// Stored preference still available
		if (!TextUtils.isEmpty(pref)) {
			if (streamMap.keySet().contains(pref)) {
				return pref;
			}
		}
		// Try first loaded stream, otherwise fallback.
		Map.Entry<String, String> firstStream = getFirstStream();
		return (firstStream == null) ? Constants.FALLBACK_STREAM_NAME : firstStream.getKey();
	}
	
	public static String getUrlPreference() {
		String pref = streamMap.get(getStreamNamePreference());
		// Stored preference still available
		if (!TextUtils.isEmpty(pref)) {
			if (streamMap.values().contains(pref)) {
				return pref;
			}
		}
		// Try first loaded stream, otherwise fallback.
		Map.Entry<String, String> firstStream = getFirstStream();
		return (firstStream == null) ? Constants.FALLBACK_STREAM_NAME : firstStream.getValue();
    }
	
	public List<String> getStreamNames() {
		return new ArrayList<>(streamMap.keySet());
	}
	
	public void setStreamNamePreference(String streamName) {
		SharedPreferences.Editor editor = preferences.edit();
		editor.putString(STREAM_PREFERENCE_KEY, streamName);
		editor.commit();
	}
}
