package org.kfjc.android.player.model;

import android.content.Context;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.MediaQueueItem;
import com.google.android.gms.common.images.WebImage;
import com.google.common.collect.Lists;

import org.json.JSONException;
import org.json.JSONObject;
import org.kfjc.android.player.R;

import java.util.List;

public class KfjcMediaSource implements Parcelable {

    public enum Format {
        MP3, AAC;
        private static Format[] allValues = values();
        public static Format fromOrdinal(int n) {return allValues[n];}
    }
    public enum Type {
        LIVESTREAM, ARCHIVE;
        private static Type[] allValues = values();
        public static Type fromOrdinal(int n) {return allValues[n];}
    }

    private static final String KEY_TYPE = "mediasource_type";
    private static final String KEY_URL = "mediasource_url";
    private static final String KEY_NAME = "mediasource_name";
    private static final String KEY_DESC = "mediasource_desc";
    private static final String KEY_FORMAT = "mediasource_format";
    private static final String KEY_SHOW = "mediasource_show";

    public final Type type;
    // TODO: use show.url instead
    public final String url;
    public final String name;
    public final String description;
    public final Format format;
    public final ShowDetails show;

    public JSONObject toJSONObject() {
        JSONObject out = new JSONObject();
        try {
            // TODO: don't use ordinal.
            out.put(KEY_TYPE, this.type.ordinal());
            out.put(KEY_URL, this.url);
            out.put(KEY_NAME, this.name);
            out.put(KEY_DESC, this.description);
            out.put(KEY_FORMAT, this.format.ordinal());
            out.put(KEY_SHOW, this.show.toJSONObject());
        } catch (JSONException e) {}
        return out;
    }

    public static KfjcMediaSource fromJSON(JSONObject in) {
        try {
            Type type = Type.fromOrdinal(in.getInt(KEY_TYPE));
            String name = in.getString(KEY_NAME);
            String description = in.getString(KEY_DESC);
            Format format = Format.fromOrdinal(in.getInt(KEY_FORMAT));
            ShowDetails show = new ShowDetails(in.getString(KEY_SHOW));
            switch (type) {
                case LIVESTREAM:
                    String url = in.getString(KEY_URL);
                    return new KfjcMediaSource(url, format, name, description);
                case ARCHIVE:
                    return new KfjcMediaSource(show);
            }

        } catch (NullPointerException | JSONException e) { }
        return null;
    }

    public KfjcMediaSource() {
        this.type = null;
        this.format = null;
        this.name = "";
        this.url = "";
        this.description = "";
        this.show = null;
    }

    public KfjcMediaSource(String url, Format format, String name, String description) {
        this.url = url;
        this.name = name;
        this.description = description;
        this.format = format;
        this.type = Type.LIVESTREAM;
        this.show = null;
    }

    public KfjcMediaSource(ShowDetails show) {
        this.url = null;
        this.format = Format.MP3;
        this.type = Type.ARCHIVE;
        this.name = show.getAirName();
        this.description = show.getTimestampString();
        this.show = show;
    }

    public KfjcMediaSource(Parcel in) {
        this.type = Type.values()[in.readInt()];
        this.format = Format.values()[in.readInt()];
        this.name = in.readString();
        this.description = in.readString();
        this.url = in.readString();
        this.show = in.readParcelable(ShowDetails.class.getClassLoader());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public KfjcMediaSource createFromParcel(Parcel in) {
            return new KfjcMediaSource(in);
        }

        public KfjcMediaSource[] newArray(int size) {
            return new KfjcMediaSource[size];
        }
    };

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(type.ordinal());
        out.writeInt(format.ordinal());
        out.writeString(name);
        out.writeString(description);
        out.writeString(url);
        if (show != null) {
            out.writeParcelable(show, show.describeContents());
        }
    }

    @Override
    public boolean equals(Object that) {
        if (that == this) {
            return true;
        }
        if (!(that instanceof KfjcMediaSource)) {
            return false;
        }
        KfjcMediaSource thatSource = (KfjcMediaSource) that;
        if (thatSource.url != null) {
            return thatSource.url.equals(this.url);
        }
        if (thatSource.show != null && this.show != null) {
            return thatSource.show.getTimestamp() == this.show.getTimestamp();
        }
        return false;
    }

    public String getMimeType() {
        switch (format) {
            case AAC:
                return "audio/mp4; codecs=\"mp4a.40.5\"";
            case MP3:
                return "audio/mp3";
            default:
                return "";
        }
    }
    
    public MediaQueueItem[] asQueue(Context context) {
        List<MediaQueueItem> queue = Lists.newArrayList();
        switch (this.type) {
            case LIVESTREAM:
                queue.add(buildLivestreamMediaInfo(context));
                break;
            case ARCHIVE:
                int hour = 1;
                for (String url : show.getUrls()) {
                    queue.add(buildArchiveMediaInfo(context, url, hour++, show.getUrls().size()));
                }
        }
        return queue.toArray(new MediaQueueItem[queue.size()]);
    }

    private MediaQueueItem buildArchiveMediaInfo(Context context, String url, int hourNumber, int totalHours) {
        MediaMetadata metadata = new MediaMetadata(MediaMetadata.MEDIA_TYPE_GENERIC);
        metadata.addImage(new WebImage(Uri.parse("https://dl.dropboxusercontent.com/u/7449543/chromecast-dev/kfjc-cover.jpg")));
        metadata.putString(MediaMetadata.KEY_TITLE,
                context.getString(R.string.format_archive_hour, this.name, hourNumber, totalHours));
        metadata.putInt(MediaMetadata.KEY_TRACK_NUMBER, hourNumber);
        metadata.putString(MediaMetadata.KEY_SUBTITLE, this.description);
        MediaInfo info = new MediaInfo.Builder(url)
                .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                .setContentType(getMimeType())
                .setCustomData(this.toJSONObject())
                .setMetadata(metadata)
                .build();
        return new MediaQueueItem.Builder(info)
                .setStartTime(10 * 60) // start 10 mins in
//                .setCustomData(this.toJSONObject())
                .setAutoplay(true)
                .build();
    }

    private MediaQueueItem buildLivestreamMediaInfo(Context context) {
        MediaMetadata metadata = new MediaMetadata(MediaMetadata.MEDIA_TYPE_GENERIC);
        metadata.addImage(new WebImage(Uri.parse("https://dl.dropboxusercontent.com/u/7449543/chromecast-dev/kfjc-cover.jpg")));
        metadata.putString(MediaMetadata.KEY_TITLE, context.getString(R.string.fragment_title_stream));
        metadata.putString(MediaMetadata.KEY_SUBTITLE, this.description);
        MediaInfo info = new MediaInfo.Builder(this.url + ";")
                .setStreamType(MediaInfo.STREAM_TYPE_LIVE)
                .setContentType(getMimeType())
                .setMetadata(metadata)
                .build();
        return new MediaQueueItem.Builder(info).build();
    }
}
