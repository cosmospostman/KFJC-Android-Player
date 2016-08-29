package org.kfjc.android.player.fragment;

import android.os.Handler;
import android.util.Log;

import org.kfjc.android.player.model.MediaSource;

public abstract class PlayerFragment extends KfjcFragment {

    public static final String TAG = PlayerFragment.class.getSimpleName();

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
            if (isAdded()) {
                updateClock();
                handler.postDelayed(this, 1000);
            } else {
                Log.i(getTag(), "Not updating clock (not added)");
            }
        }
    };

    protected void startPlayClockUpdater() {
        handler.removeCallbacks(playClockUpdater);
        handler.postDelayed(playClockUpdater, 0);
    }

    abstract void updateClock();

    abstract void onStateChanged(PlayerState state, MediaSource source);

}
