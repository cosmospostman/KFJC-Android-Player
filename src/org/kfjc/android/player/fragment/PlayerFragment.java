package org.kfjc.android.player.fragment;

import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.os.Handler;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.util.Log;

import org.kfjc.android.player.intent.PlayerState;
import org.kfjc.android.player.model.KfjcMediaSource;
import org.kfjc.android.player.receiver.MediaStateReceiver;
import org.kfjc.android.player.intent.PlayerState.State;

public abstract class PlayerFragment extends KfjcFragment {

    public static final String TAG = PlayerFragment.class.getSimpleName();

    protected State displayState;
    protected State playerState;
    protected KfjcMediaSource playerSource;
    protected Handler handler = new Handler();

    @Override
    public void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mediaStateReceiver,
                new IntentFilter(PlayerState.INTENT_PLAYER_STATE));
        mediaStateReceiver.onReceive(getActivity(), PlayerState.getLastPlayerState());
    }

    private BroadcastReceiver mediaStateReceiver = new MediaStateReceiver() {
        @Override
        protected void onStateChange(State state, KfjcMediaSource source) {
            playerState = state;
            playerSource = source;
            if (!PlayerFragment.this.isAdded()) {
                return;
            }
            onStateChanged(state, source);
        }

        @Override
        protected void onError(State state, String message) {}
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

    abstract void onStateChanged(State state, KfjcMediaSource source);

}
