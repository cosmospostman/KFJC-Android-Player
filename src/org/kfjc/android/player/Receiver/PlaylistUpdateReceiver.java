package org.kfjc.android.player.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.kfjc.android.player.intent.PlaylistUpdate;
import org.kfjc.android.player.model.Playlist;
import org.kfjc.android.player.model.PlaylistJsonImpl;

public abstract class PlaylistUpdateReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String playlistJson = intent.getStringExtra(PlaylistUpdate.INTENT_KEY_PLAYLIST_JSON);
        onPlaylistUpdate(new PlaylistJsonImpl(playlistJson));
    }

    public abstract void onPlaylistUpdate(Playlist playlist);
}
