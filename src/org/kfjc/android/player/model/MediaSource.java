package org.kfjc.android.player.model;

public class MediaSource {

    public enum Format { MP3, AAC, NONE }
    public enum Type { LIVESTREAM, ARCHIVE }

    public final Type type;
    public final String url;
    public final String name;
    public final String description;
    public final Format format;

    public MediaSource(Type type, String url, Format format) {
        this.url = url;
        this.name = "";
        this.description = "";
        this.format = format;
        this.type = type;
    }

    public MediaSource(Type type, String url, String name, String description, Format format) {
        this.url = url;
        this.name = name;
        this.description = description;
        this.format = format;
        this.type = type;
    }

    @Override
    public boolean equals(Object that) {
        if (!(that instanceof MediaSource)) {
            return false;
        }
        return ((MediaSource) that).url.equals(this.url);
    }
}
