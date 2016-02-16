package org.kfjc.android.player.service;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import org.kfjc.android.player.Constants;
import org.kfjc.android.player.model.Playlist;
import org.kfjc.android.player.model.PlaylistJsonImpl;
import org.kfjc.android.player.util.HttpUtil;

public class PlaylistService extends Service {

    private static final String TAG = PlaylistService.class.getSimpleName();

    public interface PlaylistCallback {
        void onPlaylistUpdate(Playlist playlist);
    }

    public class PlaylistBinder extends Binder {
        public PlaylistService getService() {
            return PlaylistService.this;
        }
    }

    private Handler handler = new Handler();
    private Playlist lastFetchedPlaylist;
    private PlaylistBinder binder = new PlaylistBinder();
    private PlaylistCallback playlistCallback;
    private boolean isStarted = false;
    private Runnable fetchRunner = new Runnable() {
        @Override public void run() {
            makeFetchTask().execute();
            handler.postDelayed(this, Constants.CURRENT_TRACK_POLL_DELAY_MS);
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "service bound");
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        stop();
        super.onDestroy();
    }

    public void start() {
        if (!isStarted) {
            Log.i(TAG, "Service (re)started");
            handler.postDelayed(fetchRunner, 0);
            isStarted = true;
        }
    }

    public void stop() {
        Log.i(TAG, "Service stopped");
        handler.removeCallbacksAndMessages(null);
        isStarted = false;
    }

    private AsyncTask<Void, Void, Playlist> makeFetchTask() {
        return new AsyncTask<Void, Void, Playlist>() {
            protected Playlist doInBackground(Void... unusedParams) {
                Log.i(TAG, "Fetching current track info");
                try {
                    return new PlaylistJsonImpl(HttpUtil.getUrl(Constants.PLAYLIST_URL));
                } catch (Exception e) {
                    Log.e(TAG, e.getMessage());
                    return new PlaylistJsonImpl("");
                }
            }

            protected void onPostExecute(Playlist playlist) {
                onTrackInfoFetched(playlist);
            }
        };
    }

    public Playlist getPlaylist() {
        return lastFetchedPlaylist;
    }

    public void registerPlaylistCallback(PlaylistCallback callback) {
        this.playlistCallback = callback;
        if (lastFetchedPlaylist != null) {
            callback.onPlaylistUpdate(lastFetchedPlaylist);
        }
    }

    private void onTrackInfoFetched(Playlist playlist) {
        // TODO: consider doing nothing if new playlist has error
        if (playlistCallback != null) {
            lastFetchedPlaylist = playlist;
            playlistCallback.onPlaylistUpdate(playlist);
        }
    }
}
