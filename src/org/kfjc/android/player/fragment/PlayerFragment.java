package org.kfjc.android.player.fragment;

import android.app.Fragment;

import org.kfjc.android.player.activity.HomeScreenInterface;

public abstract class PlayerFragment extends Fragment {

    protected HomeScreenInterface homeScreen;
    protected PlayerState playerState = PlayerState.STOP;

    public enum PlayerState {
        PLAY,
        STOP,
        BUFFER
    }

    public void setState(PlayerState state) {
        playerState = state;
        if (!this.isAdded()) {
            return;
        }
        switch(state) {
            case STOP:
                onStateStop();
                break;
            case PLAY:
                onStatePlay();
                break;
            case BUFFER:
                onStateBuffer();
                break;
        }
    }

    abstract void onStateStop();
    abstract void onStatePlay();
    abstract void onStateBuffer();

}
