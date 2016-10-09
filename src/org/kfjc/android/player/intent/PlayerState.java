package org.kfjc.android.player.intent;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import org.kfjc.android.player.model.KfjcMediaSource;

public class PlayerState {

    public enum State {
        PLAY,
        PAUSE,
        STOP,
        BUFFER,
        ERROR
    }

    public static final String INTENT_PLAYER_STATE = "kfjc_intent_player_state";

    public static final String INTENT_KEY_PLAYER_MESSAGE = "kfjc_key_player_message";
    public static final String INTENT_KEY_PLAYER_SOURCE = "kfjc_key_player_source";
    public static final String INTENT_KEY_PLAYER_STATE = "kfjc_key_player_state";

    private static Intent lastPlayerState;

    public static Intent getLastPlayerState() {
        if (lastPlayerState == null) {
            lastPlayerState = new Intent(PlayerState.INTENT_PLAYER_STATE);
            lastPlayerState.putExtra(PlayerState.INTENT_KEY_PLAYER_STATE, PlayerState.State.STOP);
            lastPlayerState.putExtra(PlayerState.INTENT_KEY_PLAYER_SOURCE, new KfjcMediaSource());
        }
        return lastPlayerState;
    }

    public void send(Context context, State state, KfjcMediaSource source) {
        sendStateIntent(context, state, source , null);
    }

    public void send(Context context, State state, String message) {
        sendStateIntent(context, state, null, message);
    }

    private void sendStateIntent(Context context, State state, KfjcMediaSource source, String message) {
        Intent intent = new Intent(INTENT_PLAYER_STATE);
        intent.putExtra(INTENT_KEY_PLAYER_STATE, state);
        if (source != null) {
            intent.putExtra(INTENT_KEY_PLAYER_SOURCE, source);
        }
        if (message != null) {
            intent.putExtra(INTENT_KEY_PLAYER_MESSAGE, message);
        }
        lastPlayerState = intent;
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }
}
