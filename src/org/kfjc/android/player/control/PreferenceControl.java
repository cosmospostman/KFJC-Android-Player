package org.kfjc.android.player.control;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;

import org.kfjc.android.player.Constants;
import org.kfjc.android.player.activity.HomeScreenInterface;
import org.kfjc.android.player.model.Stream;

import java.util.List;
import java.util.concurrent.ExecutionException;

public class PreferenceControl {

	private static final String TAG = PreferenceControl.class.getSimpleName();
	private static final String PREFERENCE_KEY = "kfjc.preferences";
	private static final String STREAM_PREFERENCE_KEY = "kfjc.preferences.streamname";
	private static final String STREAM_URL_PREFERENCE_KEY = "kfjc.preferences.streamUrl";
	private static final String ENABLE_BACKGROUNDS_KEY = "kfjc.preferences.enableBackgrounds";
	private static final String CACHED_RESOURCES_KEY = "kfjc.preferences.cachedResources";
    private static final int PREFERENCE_MODE = Context.MODE_PRIVATE;
	
	private static SharedPreferences preferences;
	private static List<Stream> streams;
	private HomeScreenInterface activity;

	public PreferenceControl(Context context, final HomeScreenInterface activity) {
		this.activity = activity;
		preferences = context.getSharedPreferences(PREFERENCE_KEY, PREFERENCE_MODE);
	}

	public void updateStreams() {
		new AsyncTask<Void, Void, Void>() {
			@Override protected Void doInBackground(Void... unsedParams) {
				try {
					streams = activity.getKfjcResources().getStreamsList().get();
				} catch (InterruptedException | ExecutionException e) {
					// TODO
				}
				return null;
			}
		}.execute();
	}

	public static List<Stream> getStreams() {
		return streams;
	}

	public static boolean areBackgroundsEnabled() {
		return preferences.getBoolean(ENABLE_BACKGROUNDS_KEY, true);
	}

	public static void setEnableBackgrounds(boolean enabled) {
		SharedPreferences.Editor editor = preferences.edit();
		editor.putBoolean(ENABLE_BACKGROUNDS_KEY, enabled);
		editor.commit();
	}

	public static String getCachedResources() {
		return preferences.getString(CACHED_RESOURCES_KEY, "");
	}

	public static void setCachedResources(String resourceString) {
		SharedPreferences.Editor editor = preferences.edit();
		editor.putString(CACHED_RESOURCES_KEY, resourceString);
		editor.commit();
	}
	
	public static Stream getStreamPreference() {
		String urlPref = preferences.getString(STREAM_URL_PREFERENCE_KEY, "");
		// TODO: block on streams being non-null or maybe set streams to fallback initailly?
		if (streams == null) {
			return Constants.FALLBACK_STREAM;
		}
		// Stored URL preference matches a loaded stream
		for (Stream s : streams) {
			if (s.url.equals(urlPref)) {
				return s;
			}
		}
		// Try first loaded stream
		if (streams.size() > 0) {
			return streams.get(0);
		}
		// Otherwise fallback.
		return Constants.FALLBACK_STREAM;
    }
	
	public void setStreamPreference(Stream stream) {
		SharedPreferences.Editor editor = preferences.edit();
		editor.putString(STREAM_URL_PREFERENCE_KEY, stream.url);
		editor.commit();
	}
}
