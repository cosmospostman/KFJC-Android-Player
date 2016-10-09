package org.kfjc.android.player.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.kfjc.android.player.intent.PlayerState;
import org.kfjc.android.player.model.KfjcMediaSource;

public abstract class MediaStateReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        PlayerState.State state = (PlayerState.State)
                intent.getSerializableExtra(PlayerState.INTENT_KEY_PLAYER_STATE);
        KfjcMediaSource source = intent.getParcelableExtra(PlayerState.INTENT_KEY_PLAYER_SOURCE);
        String errorMessage = intent.getStringExtra(PlayerState.INTENT_KEY_PLAYER_MESSAGE);

        if (PlayerState.State.ERROR.equals(state)) {
            onError(state, errorMessage);
        } else {
            onStateChange(state, source);
        }
    }

    protected abstract void onStateChange(PlayerState.State state, KfjcMediaSource source);
    protected abstract void onError(PlayerState.State state, String message);
}
