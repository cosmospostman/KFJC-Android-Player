package org.kfjc.android.player.model;

import java.util.List;

public interface Playlist {
    boolean hasError();
    String getDjName();
    String getTime();
    PlaylistEntry getLastTrackEntry();
    List<PlaylistEntry> getTrackEntries();

    interface PlaylistEntry {
        String getTime();
        String getTrack();
        String getArtist();
        String getAlbum();
        String getLabel();
    }
}
