package org.kfjc.android.player.model;

import org.json.JSONException;
import org.json.JSONObject;
import org.kfjc.android.player.Constants;

import android.util.Log;

public class TrackInfo {
	
	private String artist;
	private String trackTitle;
	private String djAirName;
	
	public TrackInfo(String trackApiResponse) {
		try {
			JSONObject trackJson = new JSONObject(trackApiResponse);
			this.artist = trackJson.getString("artist");
			this.trackTitle = trackJson.getString("track_title");
			this.djAirName = trackJson.getString("air_name");
		} catch (JSONException e) {
            Log.d(Constants.LOG_TAG, e.getLocalizedMessage());
			this.artist = "";
			this.trackTitle = "";
			this.djAirName = "";
		}
	}
	
	public String getArtist() {
		return this.artist;
	}
	
	public String getTrackTitle() {
		return this.trackTitle;
	}
	
	public String getDjAirName() {
		return this.djAirName;
	}

}
