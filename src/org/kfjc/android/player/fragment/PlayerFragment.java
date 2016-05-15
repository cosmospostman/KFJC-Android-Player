package org.kfjc.android.player.fragment;

import android.os.Handler;

import org.kfjc.android.player.model.MediaSource;

public abstract class PlayerFragment extends KfjcFragment {

    protected PlayerState playerState;
    protected PlayerState displayState;
    protected MediaSource playerSource;
    protected Handler handler = new Handler();

    public enum PlayerState {
        PLAY,
        PAUSE,
        STOP,
        BUFFER
    }

    @Override
    public void onResume() {
        super.onResume();
        if (playerState != null && playerSource != null) {
            onStateChanged(playerState, playerSource);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        handler.removeCallbacks(playClockUpdater);
    }


    public void setState(PlayerState state, MediaSource source) {
        playerState = state;
        playerSource = source;
        if (!this.isAdded()) {
            return;
        }
        onStateChanged(state, source);
    }

    protected Runnable playClockUpdater = new Runnable() {
        @Override public void run() {
            updateClock();
            handler.postDelayed(this, 1000);
        }
    };

    protected void startPlayClockUpdater() {
        handler.removeCallbacks(playClockUpdater);
        handler.postDelayed(playClockUpdater, 0);
    }

    abstract void updateClock();

    abstract void onStateChanged(PlayerState state, MediaSource source);

}
