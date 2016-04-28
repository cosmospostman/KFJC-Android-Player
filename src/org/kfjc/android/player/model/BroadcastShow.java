package org.kfjc.android.player.model;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class BroadcastShow implements Parcelable {

    private static final String KEY_PLAYLIST_ID = "playlistId";
    private static final String KEY_AIRNAME = "airName";
    private static final String KEY_STARTTIME = "startTime";
    private static final String KEY_URLS = "urls";

    private final String playlistId;
    private final String airName;
    private final String startDateTime;
    private final List<String> urls;

    BroadcastShow(BroadcastHour hour) {
        urls = new ArrayList<>();
        this.playlistId = hour.getPlaylistId();
        this.airName = hour.getAirName();
        this.startDateTime = "6am Thursday April 24th 2016";
        urls.add(hour.getUrl());
    }

    public BroadcastShow(String jsonString) {
        // TODO: ripe for a unit test!
        urls = new ArrayList<>();
        String playlistId = "";
        String airName = "";
        String startDateTime = "";
        try {
            JSONObject in = new JSONObject(jsonString);
            playlistId = in.getString(KEY_PLAYLIST_ID);
            airName = in.getString(KEY_AIRNAME);
            startDateTime = in.getString(KEY_STARTTIME);
            JSONArray inUrls = in.getJSONArray(KEY_URLS);
            for (int i = 0; i < inUrls.length(); i++) {
                urls.add(inUrls.getString(i));
            }

        } catch (JSONException e) {}
        this.playlistId = playlistId;
        this.airName = airName;
        this.startDateTime = startDateTime;
    }

    public BroadcastShow(Parcel in) {
        urls = new ArrayList<>();
        playlistId = in.readString();
        airName = in.readString();
        startDateTime = in.readString();
        in.readStringList(urls);
    }

    void addHour(BroadcastHour hour) {
        if (!hour.getPlaylistId().equals(playlistId)) {
            return;
        }
        urls.add(hour.getUrl());
    }

    public String getPlaylistId() {
        return playlistId;
    }

    public String getAirName() {
        return airName;
    }

    public String getStartDateTime() {
        return startDateTime;
    }

    public List<String> getUrls() {
        return urls;
    }

    public String toJsonString() {
        JSONObject out = new JSONObject();
        try {
            out.put(KEY_PLAYLIST_ID, playlistId);
            out.put(KEY_AIRNAME, airName);
            out.put(KEY_STARTTIME, startDateTime);
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
        dest.writeString(startDateTime);
        dest.writeStringList(urls);
    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public BroadcastShow createFromParcel(Parcel in) {
            return new BroadcastShow(in);
        }

        public BroadcastShow[] newArray(int size) {
            return new BroadcastShow[size];
        }
    };
}
