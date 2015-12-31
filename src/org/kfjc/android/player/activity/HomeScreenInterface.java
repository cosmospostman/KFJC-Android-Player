package org.kfjc.android.player.activity;

public interface HomeScreenInterface {
    void setActionbarTitle(String title);
    void playStream();
    void stopStream();
    boolean isStreamServicePlaying();

}
