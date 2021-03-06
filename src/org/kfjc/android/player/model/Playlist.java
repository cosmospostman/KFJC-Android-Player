package org.kfjc.android.player.model;

import java.util.List;

public interface Playlist {
    boolean hasError();
    String getDjName();
    long getTimestampMillis();
    PlaylistEntry getLastTrackEntry();
    List<PlaylistEntry> getTrackEntries();
    String toJsonString();

    interface PlaylistEntry {
        boolean isEmpty();
        String getTime();
        String getTrack();
        String getArtist();
        String getAlbum();
        String getLabel();
    }
}
