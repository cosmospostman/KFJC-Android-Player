package org.kfjc.android.player.activity;

import org.kfjc.android.player.model.TrackInfo;

public interface HomeScreenInterface {
    void setActionbarTitle(String title);
    void playStream();
    void stopStream();
    void restartStream();
    boolean isStreamServicePlaying();
    String getString(int resId);
    void snack(String message, int duration);
    void snackDone();
    TrackInfo getLatestTrackInfo();
}
