package org.kfjc.android.player.model;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.kfjc.android.player.util.DateUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ShowDetails implements Parcelable {

    private static final String KEY_PLAYLIST_ID = "playlistId";
    private static final String KEY_AIRNAME = "airName";
    private static final String KEY_STARTTIME = "startTime";
    private static final String KEY_URLS = "urls";
    private static final String KEY_PLAYTIME = "playtime";
    private static final String KEY_PADDING = "padding";

    private String playlistId;
    private String airName;
    private long timestamp;
    private List<String> urls;
    private List<File> files;
    private boolean hasError;

    // These are assumed to be the same for all hours.
    private long hourPlayTimeMillis;
    private long hourPaddingTimeMillis;

    ShowDetails(Collection<BroadcastHour> hours) {
        urls = new ArrayList<>();
        for (BroadcastHour hour : hours) {
            this.playlistId = hour.getPlaylistId();
            this.airName = hour.getAirName();
            // Use earliest timestamp as start of show.
            timestamp = (timestamp == 0)
                ? hour.getTimestamp()
                : Math.min(timestamp, hour.getTimestamp());
            this.hourPaddingTimeMillis = hour.getPaddingTimeMillis();
            this.hourPlayTimeMillis = hour.getPlayTimeMillis();
            urls.add(hour.getUrl());
        }
        Collections.sort(urls);
    }

    public ShowDetails(String jsonString) {
        // TODO: ripe for a unit test!
        urls = new ArrayList<>();
        String playlistId = "";
        String airName = "";
        long hourPlayTime = 0L;
        long hourPaddingTime = 0L;
        long timestamp = 0L;
        try {
            JSONObject in = new JSONObject(jsonString);
            playlistId = in.getString(KEY_PLAYLIST_ID);
            airName = in.getString(KEY_AIRNAME);
            timestamp = in.getLong(KEY_STARTTIME);
            hourPaddingTime = in.getLong(KEY_PADDING);
            hourPlayTime = in.getLong(KEY_PLAYTIME);
            JSONArray inUrls = in.getJSONArray(KEY_URLS);
            for (int i = 0; i < inUrls.length(); i++) {
                urls.add(inUrls.getString(i));
            }
            hasError = false;
        } catch (JSONException e) {
            hasError = true;
        }
        Collections.sort(urls);
        this.playlistId = playlistId;
        this.airName = airName;
        this.timestamp = timestamp;
        this.hourPlayTimeMillis = hourPlayTime;
        this.hourPaddingTimeMillis = hourPaddingTime;
    }

    public ShowDetails(Parcel in) {
        urls = new ArrayList<>();
        playlistId = in.readString();
        airName = in.readString();
        timestamp = in.readLong();
        hourPaddingTimeMillis = in.readLong();
        hourPlayTimeMillis = in.readLong();
        in.readStringList(urls);
        Collections.sort(urls);
    }

    public String getPlaylistId() {
        return playlistId;
    }

    public String getAirName() {
        return airName;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public long getHourPlayTimeMillis() {
        return hourPlayTimeMillis;
    }

    public long getHourPaddingTimeMillis() {
        return hourPaddingTimeMillis;
    }

    public boolean hasError() {
        return hasError;
    }

    public String getTimestampString() {
        return DateUtil.roundUpHourFormat(timestamp, DateUtil.FORMAT_DELUXE_DATE);
    }

    public List<String> getUrls() {
        return urls;
    }

    public String toJsonString() {
        JSONObject out = new JSONObject();
        try {
            out.put(KEY_PLAYLIST_ID, playlistId);
            out.put(KEY_AIRNAME, airName);
            out.put(KEY_STARTTIME, timestamp);
            out.put(KEY_PADDING, hourPaddingTimeMillis);
            out.put(KEY_PLAYTIME, hourPlayTimeMillis);
            JSONArray urls = new JSONArray(this.urls);
            out.put(KEY_URLS, urls);

        } catch (JSONException e) {}
        return out.toString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(playlistId);
        dest.writeString(airName);
        dest.writeLong(timestamp);
        dest.writeLong(hourPaddingTimeMillis);
        dest.writeLong(hourPlayTimeMillis);
        dest.writeStringList(urls);
    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public ShowDetails createFromParcel(Parcel in) {
            return new ShowDetails(in);
        }

        public ShowDetails[] newArray(int size) {
            return new ShowDetails[size];
        }
    };

    @Override
    public boolean equals(Object that) {
        if (that == this) {
            return true;
        }
        if (!(that instanceof ShowDetails)) {
            return false;
        }
        ShowDetails thatShow = (ShowDetails) that;

        return thatShow.playlistId.equals(this.playlistId)
                && thatShow.airName.equals(this.airName)
                && thatShow.urls.equals(this.urls)
                && thatShow.timestamp == this.timestamp
                && thatShow.hasError == this.hasError;
    }

    public void setFiles(List<File> files) {
        this.files = files;
    }

    public List<File> getFiles() {
        return this.files;
    }
}
