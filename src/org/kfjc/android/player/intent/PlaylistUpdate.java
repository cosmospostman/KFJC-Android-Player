package org.kfjc.android.player.intent;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import org.kfjc.android.player.model.Playlist;
import org.kfjc.android.player.model.PlaylistJsonImpl;

public class PlaylistUpdate {
    public static final String INTENT_PLAYLIST_UPDATE = "kfjc_intent_playlist_update";

    public static final String INTENT_KEY_PLAYLIST_JSON = "kfjc_key_playlist_json";

    private static Intent lastPlaylist;

    private static Intent getLastIntent() {
        if (lastPlaylist == null) {
            lastPlaylist = playlistUpdateIntent("");
        }
        return lastPlaylist;
    }

    public static Playlist getLastPlaylist() {
        String playlistJson = getLastIntent().getStringExtra(INTENT_KEY_PLAYLIST_JSON);
        return new PlaylistJsonImpl(playlistJson);
    }

    public static void send(Context context, String playlistJson) {
        lastPlaylist = playlistUpdateIntent(playlistJson);
        LocalBroadcastManager.getInstance(context).sendBroadcast(lastPlaylist);
    }

    private static Intent playlistUpdateIntent(String playlistJson) {
        Intent i = new Intent(INTENT_PLAYLIST_UPDATE);
        i.putExtra(INTENT_KEY_PLAYLIST_JSON, playlistJson);
        return i;
    }

}
