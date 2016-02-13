package org.kfjc.android.player.model;

import java.util.List;

public interface Playlist {
    String getDjName();
    String getTime();
    List<PlaylistEntry> getTrackEntries();

    interface PlaylistEntry {
        String getTime();
        String getTrack();
        String getArtist();
        String getAlbum();
    }
}
