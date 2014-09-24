package org.kfjc.android.player;

import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

public class NowPlayingInfo {
	
	private String artist;
	private String trackTitle;
	private String djAirName;
	
	public NowPlayingInfo(String nowPlayingApiResponse) {
		try {
			JSONObject nowPlayingJson = new JSONObject(nowPlayingApiResponse);
			this.artist = nowPlayingJson.getString("artist");
			this.trackTitle = nowPlayingJson.getString("track_title");
			this.djAirName = nowPlayingJson.getString("air_name");
		} catch (JSONException e) {
            Log.d("JSONException", e.getLocalizedMessage());
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
