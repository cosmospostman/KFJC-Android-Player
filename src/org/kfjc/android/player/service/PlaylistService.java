package org.kfjc.android.player.service;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import org.kfjc.android.player.Constants;
import org.kfjc.android.player.model.TrackInfo;
import org.kfjc.android.player.util.HttpUtil;

/**
 * Created by mitchell on 8/1/15.
 */
public class PlaylistService extends Service {

    private static final String TAG = "kfjc.playlistservice";

    public interface PlaylistCallback {
        void onTrackInfoFetched(TrackInfo trackInfo);
    }

    public class PlaylistBinder extends Binder {
        public PlaylistService getService() {
            return PlaylistService.this;
        }
    }

    private Handler handler = new Handler();
    private TrackInfo lastFetchedTrackInfo;
    private PlaylistBinder binder = new PlaylistBinder();
    private PlaylistCallback playlistCallback;
    private boolean isStarted = false;
    private Runnable fetchRunner = new Runnable() {
        @Override public void run() {
            makeFetchTask().execute();
            handler.postDelayed(this, Constants.CURRENT_TRACK_POLL_DELAY_MS);
        }
    };

    @Nullable
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
            handler.postDelayed(fetchRunner, 0);
            isStarted = true;
        }
    }

    public void stop() {
        handler.removeCallbacks(null);
    }

    private AsyncTask<Void, Void, TrackInfo> makeFetchTask() {
        return new AsyncTask<Void, Void, TrackInfo>() {
            protected TrackInfo doInBackground(Void... unusedParams) {
                Log.i(TAG, "Fetching current track info");
                try {
                    return new TrackInfo(HttpUtil.getUrl(Constants.CURRENT_TRACK_URL));
                } catch (Exception e) {
                    Log.e(TAG, e.getMessage());
                    return new TrackInfo();
                }
            }

            protected void onPostExecute(TrackInfo nowPlaying) {
                onTrackInfoFetched(nowPlaying);
            }
        };
    }

    public void registerPlaylistCallback(PlaylistCallback callback) {
        this.playlistCallback = callback;
        if (lastFetchedTrackInfo != null) {
            callback.onTrackInfoFetched(lastFetchedTrackInfo);
        }
    }

    private void onTrackInfoFetched(TrackInfo trackInfo) {
        lastFetchedTrackInfo = trackInfo;
        if (playlistCallback != null) {
            playlistCallback.onTrackInfoFetched(trackInfo);
        }
    }
}
