package org.kfjc.android.player.model;

public class MediaSource {

    public enum Format { MP3, AAC, NONE }
    public enum Type { LIVESTREAM, ARCHIVE }

    public final Type type;
    public final String url;
    public final String name;
    public final String description;
    public final Format format;
    public final int sequenceNumber;
    public final ShowDetails show;

    public MediaSource(Type type, String url, Format format, int sequenceNumber, String name, String description) {
        this.url = url;
        this.name = name;
        this.description = description;
        this.format = format;
        this.type = type;
        this.sequenceNumber = sequenceNumber;
        this.show = null;
    }

    public MediaSource(Type type, String url, Format format, int sequenceNumber, ShowDetails show) {
        this.url = url;
        this.format = format;
        this.type = type;
        this.sequenceNumber = sequenceNumber;
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
        return ((MediaSource) that).url.equals(this.url);
    }
}
