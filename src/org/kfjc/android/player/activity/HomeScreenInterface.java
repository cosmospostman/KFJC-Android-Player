package org.kfjc.android.player.activity;

import org.kfjc.android.player.model.Playlist;
import org.kfjc.android.player.model.Resources;

public interface HomeScreenInterface {
    void setActionbarTitle(String title);
    void playStream();
    void stopStream();
    void restartStream();
    boolean isStreamServicePlaying();
    String getString(int resId);
    void snack(String message, int duration);
    void snackDone();
    Playlist getLatestPlaylist();
    void setNavigationItemChecked(int navigationItemId);
    void updateBackground();
}
