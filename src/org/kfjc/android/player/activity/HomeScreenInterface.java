package org.kfjc.android.player.activity;

import org.kfjc.android.player.model.ShowDetails;
import org.kfjc.android.player.model.Playlist;
import org.kfjc.android.player.model.MediaSource;

public interface HomeScreenInterface {
    void setActionbarTitle(String title);
    boolean isStreamServicePlaying();
    String getString(int resId);
    void snack(String message, int duration);
    void snackDone();
    Playlist getLatestPlaylist();
    void setNavigationItemChecked(int navigationItemId);
    void updateBackground();
    void startDownload(ShowDetails showDetails);
    void loadPodcastPlayer(ShowDetails show, boolean animate);
    void loadPodcastListFragment(boolean animate);
    long getPlayerPosition();
    void seekPlayer(long positionMillis);
    void syncState();
    MediaSource getPlayerSource();
    void setActionBarBackArrow(boolean isBackArrorw);
    void requestAndroidWritePermissions();
}
