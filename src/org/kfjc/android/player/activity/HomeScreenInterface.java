package org.kfjc.android.player.activity;

import org.kfjc.android.player.model.ShowDetails;
import org.kfjc.android.player.model.Playlist;
import org.kfjc.android.player.model.MediaSource;

public interface HomeScreenInterface {
    void setActionbarTitle(String title);
    void playStream();
    void stopPlayer();
    void pausePlayer();
    void unpausePlayer();
    void restartStream();
    void playArchive(MediaSource source);
    boolean isStreamServicePlaying();
    String getString(int resId);
    void snack(String message, int duration);
    void snackDone();
    Playlist getLatestPlaylist();
    void setNavigationItemChecked(int navigationItemId);
    void updateBackground();
    void requestExternalWritePermission();
    void loadPodcastPlayer(ShowDetails show, boolean animate);
    void loadPodcastListFragment(boolean animate);
    void registerDownload(long downloadId, ShowDetails show);
    long getPlayerPosition();
    void seekPlayer(long positionMillis);
    void syncState();
    MediaSource getPlayerSource();
    void setSegmentBounds(long[] segmentBounds);
    long[] getSegmentBounds();
    void setTotalPlayTime(long playTime);
    long getTotalPlayTime();
    void setActionBarBackArrow(boolean isBackArrorw);
}
