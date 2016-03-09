package org.kfjc.android.player.control;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.text.TextUtils;

import org.kfjc.android.player.Constants;
import org.kfjc.android.player.activity.HomeScreenInterface;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class PreferenceControl {

	private static final String TAG = PreferenceControl.class.getSimpleName();
	private static final String PREFERENCE_KEY = "kfjc.preferences";
	private static final String STREAM_PREFERENCE_KEY = "kfjc.preferences.streamname";
    private static final int PREFERENCE_MODE = Context.MODE_PRIVATE;
	
	private static SharedPreferences preferences;
	private static Map<String, String> streamMap = new LinkedHashMap<>();

	private HomeScreenInterface activity;

	public PreferenceControl(Context context, final HomeScreenInterface activity) {
		this.activity = activity;

		preferences = context.getSharedPreferences(PREFERENCE_KEY, PREFERENCE_MODE);
		new AsyncTask<Void, Void, Void>() {
			@Override protected Void doInBackground(Void... unsedParams) {
				try {
					streamMap = activity.getKfjcResources().getStreams().get();
				} catch (InterruptedException | ExecutionException e) {
					// TODO
				}
				return null;
			}
		}.execute();
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
