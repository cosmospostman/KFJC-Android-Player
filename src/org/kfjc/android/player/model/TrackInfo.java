package org.kfjc.android.player.model;

import org.json.JSONException;
import org.json.JSONObject;
import org.kfjc.android.player.Constants;
import org.kfjc.android.player.R;

import android.content.Context;
import android.text.Html;
import android.util.Log;

public class TrackInfo {

    // Returned by the website when no dj signed in.
    private static final String NULL_STRING = "null";
    private static final String NULL_DJ_NAME = "George Foothill";

    private boolean couldNotFetch;
	private String artist;
	private String track;
	private String djName;

    public TrackInfo() {
        setCouldNotFetchTrue();
    }
	
	public TrackInfo(String trackApiResponse) {
		try {
			JSONObject trackJson = new JSONObject(trackApiResponse);
            String djName = trackJson.getString("air_name");

            this.artist = emptyStringIfNull(trackJson.getString("artist"));
			this.track = emptyStringIfNull(trackJson.getString("track_title"));
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
        this.track = "";
        this.djName = "";
    }

	public String getDjName() {
		return this.djName;
	}

    public boolean getCouldNotFetch() {
        return this.couldNotFetch;
    }

    /**
     * Artist and track information displayed in the notification.
     */
    public String artistTrackStringNotification(Context context) {
        String artistTrackString;

        if (!artist.isEmpty() && !track.isEmpty()) {    // Both artist and track supplied
            artistTrackString = context.getString(R.string.artist_track_format, artist, track);
        } else if (artist.isEmpty()) {                  // Only track title
            artistTrackString = track;
        } else if (track.isEmpty()){                    // Only artist
            artistTrackString = artist;
        } else {                                        // Neither artist nor track
            artistTrackString = context.getString(R.string.empty_string);
        }

        return artistTrackString;
    }

    /**
     * Formatted string displayed in the main activity text view.
     */
    public android.text.Spanned artistTrackHtml() {
        String spacer = artist.isEmpty() ? "" : "&nbsp&nbsp&nbsp";
        return Html.fromHtml(artist + spacer + "<i>" + track + "</i>");
    }

    private static String emptyStringIfNull(String s) {
        return s == null ? "" : s;
    }

}
