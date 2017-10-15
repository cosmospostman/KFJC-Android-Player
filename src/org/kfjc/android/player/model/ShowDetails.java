package org.kfjc.android.player.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.common.primitives.Longs;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.kfjc.android.player.util.DateUtil;
import org.kfjc.android.player.util.ExternalStorageUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
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
    private static final String KEY_TOTAL_TIME = "totalTimeMillis";
    private static final String KEY_SEGMENT_BOUNDS = "segmentBounds";
    private static final String KEY_FILE_SIZE = "fileSize";

    private String playlistId;
    private String airName;
    private long timestamp;
    private List<String> urls;
    private boolean hasError;

    private long totalShowTimeMillis;
    private long[] segmentBounds;
    private long totalFileSizeBytes;

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
            totalFileSizeBytes += hour.getFileSize();
        }
        countTotalShowTime();
        Collections.sort(urls);
    }

    private void countTotalShowTime() {
        long[] bounds = new long[urls.size()];
        long totalTime = 0;
        for (int i = 0; i < urls.size(); i++) {
            long showTime = hourPlayTimeMillis;
            // Total
            totalTime += showTime - 2 * hourPaddingTimeMillis;
            // Bound
            bounds[i] = totalTime + hourPaddingTimeMillis;
            if (i == urls.size() - 1) {
                bounds[i] += hourPaddingTimeMillis;
            }
        }
        totalTime += 2 * hourPaddingTimeMillis;
        this.totalShowTimeMillis = totalTime;
        this.segmentBounds = bounds;
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
            JSONArray inSegmentBounds = in.getJSONArray(KEY_SEGMENT_BOUNDS);
            List<Long> segmentBounds = new ArrayList<>();
            for (int i = 0; i < inSegmentBounds.length(); i++) {
                segmentBounds.add(inSegmentBounds.getLong(i));
            }
            this.segmentBounds = Longs.toArray(segmentBounds);
            totalShowTimeMillis = in.getLong(KEY_TOTAL_TIME);
            totalFileSizeBytes = in.getLong(KEY_FILE_SIZE);
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
        segmentBounds = in.createLongArray();
        totalShowTimeMillis = in.readLong();
        totalFileSizeBytes = in.readLong();
        Collections.sort(urls);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(playlistId);
        dest.writeString(airName);
        dest.writeLong(timestamp);
        dest.writeLong(hourPaddingTimeMillis);
        dest.writeLong(hourPlayTimeMillis);
        dest.writeStringList(urls);
        dest.writeLongArray(segmentBounds);
        dest.writeLong(totalShowTimeMillis);
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

    public long getHourPaddingTimeMillis() {
        return hourPaddingTimeMillis;
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
            out.put(KEY_PADDING, hourPaddingTimeMillis);
            out.put(KEY_PLAYTIME, hourPlayTimeMillis);
            JSONArray urls = new JSONArray(this.urls);
            out.put(KEY_URLS, urls);
            JSONArray segmentBounds = new JSONArray(Longs.asList(this.segmentBounds));
            out.put(KEY_SEGMENT_BOUNDS, segmentBounds);
            out.put(KEY_TOTAL_TIME, totalShowTimeMillis);
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

    public long getTotalShowTimeMillis() {
        return totalShowTimeMillis;
    }

    public long[] getSegmentBounds() {
        return segmentBounds;
    }

    public File getSavedHourUrl(int hour) {
        return ExternalStorageUtil.getSavedArchive(playlistId, urls.get(hour));
    }
}
