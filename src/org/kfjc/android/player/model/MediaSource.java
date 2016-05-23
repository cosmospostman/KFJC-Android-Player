package org.kfjc.android.player.model;

public class MediaSource {

    public enum Format { MP3, AAC, NONE }
    public enum Type { LIVESTREAM, ARCHIVE }

    public final Type type;
    public final String url;
    public final String name;
    public final String description;
    public final Format format;
    public final ShowDetails show;

    public MediaSource(String url, Format format, String name, String description) {
        this.url = url;
        this.name = name;
        this.description = description;
        this.format = format;
        this.type = Type.LIVESTREAM;
        this.show = null;
    }

    public MediaSource(ShowDetails show) {
        this.url = null;
        this.format = Format.MP3;
        this.type = Type.ARCHIVE;
        this.name = show.getAirName();
        this.description = show.getTimestampString();
        this.show = show;
    }

    @Override
    public boolean equals(Object that) {
        if (that == this) {
            return true;
        }
        if (!(that instanceof MediaSource)) {
            return false;
        }
        MediaSource thatSource = (MediaSource) that;
        if (thatSource.url != null) {
            return thatSource.url.equals(this.url);
        }
        if (thatSource.show != null && this.show != null) {
            return thatSource.show.getTimestamp() == this.show.getTimestamp();
        }
        return false;
    }
}
