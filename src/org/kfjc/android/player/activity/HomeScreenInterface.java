package org.kfjc.android.player.activity;

import org.kfjc.android.player.model.BroadcastShow;
import org.kfjc.android.player.model.Playlist;
import org.kfjc.android.player.model.Resources;
import org.kfjc.android.player.model.Stream;

public interface HomeScreenInterface {
    void setActionbarTitle(String title);
    void playStream();
    void stopStream();
    void restartStream();
    void playArchive(Stream source);
    boolean isStreamServicePlaying();
    String getString(int resId);
    void snack(String message, int duration);
    void snackDone();
    Playlist getLatestPlaylist();
    void setNavigationItemChecked(int navigationItemId);
    void updateBackground();
    void requestExternalWritePermission();
    void loadPodcastPlayer(BroadcastShow show, boolean animate);
    void registerDownload(long downloadId, BroadcastShow show);
    long getPlayerPosition();
    long getPlayerDuration();
}
