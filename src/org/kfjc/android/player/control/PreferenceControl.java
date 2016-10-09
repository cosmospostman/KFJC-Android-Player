package org.kfjc.android.player.control;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;

import com.google.common.collect.ImmutableList;

import org.kfjc.android.player.Constants;
import org.kfjc.android.player.KfjcApplication;
import org.kfjc.android.player.model.KfjcMediaSource;

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
	private static List<KfjcMediaSource> kfjcMediaSources;
	private static KfjcApplication app;

	public static void init(final KfjcApplication kfjcApp) {
		app = kfjcApp;
		preferences = app.getSharedPreferences(PREFERENCE_KEY, PREFERENCE_MODE);
	}

	public static void updateStreams() {
		new AsyncTask<Void, Void, Void>() {
			@Override protected Void doInBackground(Void... unsedParams) {
				try {
					kfjcMediaSources = app.getKfjcResources().getStreamsList().get();
				} catch (InterruptedException | ExecutionException e) {
					// TODO
				}
				return null;
			}
		}.execute();
	}

	public static List<KfjcMediaSource> getKfjcMediaSources() {
		return kfjcMediaSources == null ? ImmutableList.of(Constants.FALLBACK_MEDIA_SOURCE) : kfjcMediaSources;
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
	
	public static KfjcMediaSource getStreamPreference() {
		String urlPref = preferences.getString(STREAM_URL_PREFERENCE_KEY, "");
		if (kfjcMediaSources == null) {
			return Constants.FALLBACK_MEDIA_SOURCE;
		}
		// Stored URL preference matches a loaded stream
		for (KfjcMediaSource s : kfjcMediaSources) {
			if (s.url.equals(urlPref)) {
				return s;
			}
		}
		// Try first loaded stream
		if (kfjcMediaSources.size() > 0) {
			return kfjcMediaSources.get(0);
		}
		// Otherwise fallback.
		return Constants.FALLBACK_MEDIA_SOURCE;
    }
	
	public static void setStreamPreference(KfjcMediaSource kfjcMediaSource) {
		SharedPreferences.Editor editor = preferences.edit();
		editor.putString(STREAM_URL_PREFERENCE_KEY, kfjcMediaSource.url);
		editor.commit();
	}
}
