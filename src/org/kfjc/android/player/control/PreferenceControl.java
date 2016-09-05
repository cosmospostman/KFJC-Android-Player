package org.kfjc.android.player.control;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;

import com.google.common.collect.ImmutableList;

import org.kfjc.android.player.Constants;
import org.kfjc.android.player.KfjcApplication;
import org.kfjc.android.player.model.MediaSource;

import java.util.List;
import java.util.concurrent.ExecutionException;

public class PreferenceControl {

	private static final String PREFERENCE_KEY = "kfjc.preferences";
	private static final String STREAM_URL_PREFERENCE_KEY = "kfjc.preferences.streamUrl";
	private static final String ENABLE_BACKGROUNDS_KEY = "kfjc.preferences.enableBackgrounds";
	private static final String CACHED_RESOURCES_KEY = "kfjc.preferences.cachedResources";
	private static final String LAVA_URL_KEY = "kfjc.preferences.lavaUrl";
    private static final int PREFERENCE_MODE = Context.MODE_PRIVATE;
	
	private static SharedPreferences preferences;
	private static List<MediaSource> mediaSources;
	private static KfjcApplication app;

	public static void init(final KfjcApplication kfjcApp) {
		app = kfjcApp;
		preferences = app.getSharedPreferences(PREFERENCE_KEY, PREFERENCE_MODE);
	}

	public static void updateStreams() {
		new AsyncTask<Void, Void, Void>() {
			@Override protected Void doInBackground(Void... unsedParams) {
				try {
					mediaSources = app.getKfjcResources().getStreamsList().get();
				} catch (InterruptedException | ExecutionException e) {
					// TODO
				}
				return null;
			}
		}.execute();
	}

	public static List<MediaSource> getMediaSources() {
		return mediaSources == null ? ImmutableList.of(Constants.FALLBACK_MEDIA_SOURCE) : mediaSources;
	}

	public static boolean areBackgroundsEnabled() {
		return preferences.getBoolean(ENABLE_BACKGROUNDS_KEY, true);
	}

	public static void setEnableBackgrounds(boolean enabled) {
		SharedPreferences.Editor editor = preferences.edit();
		editor.putBoolean(ENABLE_BACKGROUNDS_KEY, enabled);
		editor.commit();
	}

	public static String getLavaUrl() {
		return preferences.getString(LAVA_URL_KEY, "");
	}

	public static void setLavaUrl(String resourceString) {
		SharedPreferences.Editor editor = preferences.edit();
		editor.putString(LAVA_URL_KEY, resourceString);
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
	
	public static MediaSource getStreamPreference() {
		String urlPref = preferences.getString(STREAM_URL_PREFERENCE_KEY, "");
		if (mediaSources == null) {
			return Constants.FALLBACK_MEDIA_SOURCE;
		}
		// Stored URL preference matches a loaded stream
		for (MediaSource s : mediaSources) {
			if (s.url.equals(urlPref)) {
				return s;
			}
		}
		// Try first loaded stream
		if (mediaSources.size() > 0) {
			return mediaSources.get(0);
		}
		// Otherwise fallback.
		return Constants.FALLBACK_MEDIA_SOURCE;
    }
	
	public static void setStreamPreference(MediaSource mediaSource) {
		SharedPreferences.Editor editor = preferences.edit();
		editor.putString(STREAM_URL_PREFERENCE_KEY, mediaSource.url);
		editor.commit();
	}
}
