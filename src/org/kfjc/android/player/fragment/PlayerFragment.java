package org.kfjc.android.player.fragment;

import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.kfjc.android.player.model.MediaSource;
import org.kfjc.android.player.receiver.MediaStateReceiver;
import org.kfjc.android.player.service.StreamService;
import org.kfjc.android.player.service.StreamService.PlayerState;

public abstract class PlayerFragment extends KfjcFragment {

    public static final String TAG = PlayerFragment.class.getSimpleName();

    protected PlayerState displayState = PlayerState.STOP;
    protected PlayerState playerState;
    protected MediaSource playerSource;
    protected Handler handler = new Handler();

    @Override
    public void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mediaStateReceiver,
                new IntentFilter(StreamService.INTENT_PLAYER_STATE));
        mediaStateReceiver.onReceive(getActivity(), StreamService.getLastPlayerState());
    }

    private BroadcastReceiver mediaStateReceiver = new MediaStateReceiver() {
        @Override
        protected void onStateChange(StreamService.PlayerState state, MediaSource source) {
            playerState = state;
            playerSource = source;
            if (!PlayerFragment.this.isAdded()) {
                return;
            }
            onStateChanged(state, source);
        }

        @Override
        protected void onError(StreamService.PlayerState state, String message) {}
    };

    @Override
    public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mediaStateReceiver);
        handler.removeCallbacks(playClockUpdater);
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
