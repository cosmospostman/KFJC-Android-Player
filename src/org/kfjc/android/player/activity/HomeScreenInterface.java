package org.kfjc.android.player.activity;

import android.view.View;

public interface HomeScreenInterface {
    void setActionbarTitle(String title);
    void playStream();
    void stopStream();
    boolean isStreamServicePlaying();
    String getString(int resId);
    void snack(String message, int duration);
    void snackDone();
}
