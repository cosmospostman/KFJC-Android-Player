package org.kfjc.android.player.model;

import org.json.JSONException;
import org.json.JSONObject;
import org.kfjc.android.player.Constants;

import android.util.Log;

public class TrackInfo {

    // Returned by the website when no dj signed in.
    private static final String NULL_STRING = "null";
    private static final String NULL_DJ_NAME = "George Foothill";

    private boolean couldNotFetch;
	private String artist;
	private String trackTitle;
	private String djName;

    public TrackInfo() {
        setCouldNotFetchTrue();
    }
	
	public TrackInfo(String trackApiResponse) {
		try {
			JSONObject trackJson = new JSONObject(trackApiResponse);
			this.artist = trackJson.getString("artist");
			this.trackTitle = trackJson.getString("track_title");
            String djName = trackJson.getString("air_name");
            this.djName = djName.equals(NULL_STRING) ? NULL_DJ_NAME : djName;
            this.couldNotFetch = false;
		} catch (JSONException e) {
            Log.d(Constants.LOG_TAG, e.getLocalizedMessage());
            setCouldNotFetchTrue();
		}
	}

    private void setCouldNotFetchTrue() {
        this.couldNotFetch = true;
        this.artist = "";
        this.trackTitle = "";
        this.djName = "";
    }
	
	public String getArtist() {
		return this.artist;
	}
	
	public String getTrackTitle() {
		return this.trackTitle;
	}
	
	public String getDjName() {
		return this.djName;
	}

    public boolean getCouldNotFetch() {
        return this.couldNotFetch;
    }

}
