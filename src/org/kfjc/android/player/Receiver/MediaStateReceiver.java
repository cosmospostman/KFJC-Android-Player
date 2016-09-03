package org.kfjc.android.player.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.kfjc.android.player.model.MediaSource;
import org.kfjc.android.player.service.StreamService;

public abstract class MediaStateReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        StreamService.PlayerState state = (StreamService.PlayerState)
                intent.getSerializableExtra(StreamService.INTENT_KEY_PLAYER_STATE);
        MediaSource source = intent.getParcelableExtra(StreamService.INTENT_KEY_PLAYER_SOURCE);
        String errorMessage = intent.getStringExtra(StreamService.INTENT_KEY_PLAYER_MESSAGE);

        if (StreamService.PlayerState.ERROR.equals(state)) {
            onError(state, errorMessage);
        } else {
            onStateChange(state, source);
        }
    }

    protected abstract void onStateChange(StreamService.PlayerState state, MediaSource source);
    protected abstract void onError(StreamService.PlayerState state, String message);
}
