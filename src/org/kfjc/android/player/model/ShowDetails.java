package org.kfjc.android.player.model;

import android.content.Context;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.DynamicConcatenatingMediaSource;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.kfjc.android.player.Constants;
import org.kfjc.android.player.util.DateUtil;
import org.kfjc.android.player.util.ExternalStorageUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ShowDetails implements Parcelable {

    private static final String KEY_PLAYLIST_ID = "playlist_num";
    private static final String KEY_AIRNAME = "air_name";
    private static final String KEY_STARTTIME = "start_time";
    private static final String KEY_URLS = "urls";
    private static final String KEY_FILE_SIZE = "file_size";

    private String playlistId;
    private String airName;
    private long timestamp;
    private List<String> urls;
    private boolean hasError;

    private long totalFileSizeBytes;

    public ShowDetails(String jsonString) {
        // TODO: ripe for a unit test!
        urls = new ArrayList<>();
        String playlistId = "";
        String airName = "";
        long timestamp = 0L;
        try {
            JSONObject in = new JSONObject(jsonString);
            playlistId = in.getString(KEY_PLAYLIST_ID);
            airName = in.getString(KEY_AIRNAME);
            timestamp = in.getLong(KEY_STARTTIME);
            JSONArray inUrls = in.getJSONArray(KEY_URLS);
            for (int i = 0; i < inUrls.length(); i++) {
                urls.add(inUrls.getString(i));
            }
            totalFileSizeBytes = in.getLong(KEY_FILE_SIZE);
            hasError = false;
        } catch (JSONException e) {
            hasError = true;
        }
        Collections.sort(urls);
        this.playlistId = playlistId;
        this.airName = airName;
        this.timestamp = timestamp;
    }

    public ShowDetails(Parcel in) {
        urls = new ArrayList<>();
        playlistId = in.readString();
        airName = in.readString();
        timestamp = in.readLong();
        in.readStringList(urls);
        totalFileSizeBytes = in.readLong();
        Collections.sort(urls);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(playlistId);
        dest.writeString(airName);
        dest.writeLong(timestamp);
        dest.writeStringList(urls);
        dest.writeLong(totalFileSizeBytes);
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

    public long getTotalFileSizeBytes() {
        return totalFileSizeBytes;
    }

    public boolean hasError() {
        return hasError;
    }

    public String getTimestampString() {
        return DateUtil.format(timestamp, DateUtil.FORMAT_DELUXE_DATE);
    }

    public String toJsonString() {
        JSONObject out = new JSONObject();
        try {
            out.put(KEY_PLAYLIST_ID, playlistId);
            out.put(KEY_AIRNAME, airName);
            out.put(KEY_STARTTIME, timestamp);
            JSONArray urls = new JSONArray(this.urls);
            out.put(KEY_URLS, urls);
            out.put(KEY_FILE_SIZE, totalFileSizeBytes);
        } catch (JSONException e) {}
        return out.toString();
    }

    @Override
    public int describeContents() {
        return 0;
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

    public List<String> getUrls() {
        return urls;
    }

    public File getSavedHourUrl(String url) {
        return ExternalStorageUtil.getSavedArchive(playlistId, url);
    }

    public long getTotalShowTimeMillis() {
        return urls.size() * Constants.SHOW_SEGMENT_LENGTH;
    }
}
