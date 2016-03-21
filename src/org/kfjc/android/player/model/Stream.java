package org.kfjc.android.player.model;

public class Stream {
    public final String url;
    public final String name;
    public final String description;

    public Stream(String url, String name, String description) {
        this.url = url;
        this.name = name;
        this.description = description;
    }

    @Override
    public boolean equals(Object that) {
        if (!(that instanceof Stream)) {
            return false;
        }
        return ((Stream) that).url.equals(this.url);
    }
}
