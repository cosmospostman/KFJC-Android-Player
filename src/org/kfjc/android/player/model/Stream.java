package org.kfjc.android.player.model;

public class Stream {

    public enum Format { MP3, AAC, NONE }

    public final String url;
    public final String name;
    public final String description;
    public final Format format;

    public Stream(String url, String name, String description, Format format) {
        this.url = url;
        this.name = name;
        this.description = description;
        this.format = format;
    }

    @Override
    public boolean equals(Object that) {
        if (!(that instanceof Stream)) {
            return false;
        }
        return ((Stream) that).url.equals(this.url);
    }
}
