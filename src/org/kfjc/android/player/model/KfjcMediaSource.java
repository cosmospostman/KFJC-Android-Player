package org.kfjc.android.player.model;

import android.os.Parcel;
import android.os.Parcelable;

public class KfjcMediaSource implements Parcelable {

    public enum Format { MP3, AAC }
    public enum Type { LIVESTREAM, ARCHIVE }

    public final Type type;
    public final String url;
    public final String name;
    public final String description;
    public final Format format;
    public final ShowDetails show;

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
                return "audio/mpeg";
            default:
                return "";
        }
    }
}
