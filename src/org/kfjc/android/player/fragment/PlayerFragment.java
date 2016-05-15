package org.kfjc.android.player.fragment;

import org.kfjc.android.player.model.MediaSource;

public abstract class PlayerFragment extends KfjcFragment {

    protected PlayerState playerState;
    protected PlayerState displayState;
    protected MediaSource playerSource;

    public enum PlayerState {
        PLAY,
        PAUSE,
        STOP,
        BUFFER
    }

    public void setState(PlayerState state, MediaSource source) {
        playerState = state;
        playerSource = source;
        if (!this.isAdded()) {
            return;
        }
        onStateChanged(state, source);
    }

    abstract void onStateChanged(PlayerState state, MediaSource source);

}
