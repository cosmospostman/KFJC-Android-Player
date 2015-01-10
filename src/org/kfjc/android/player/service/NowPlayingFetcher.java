package org.kfjc.android.player.service;

import org.kfjc.android.player.Constants;
import org.kfjc.android.player.model.TrackInfo;
import org.kfjc.android.player.service.LiveStreamService.MediaListener;
import org.kfjc.android.player.util.HttpUtil;

import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;

public class NowPlayingFetcher {

    private MediaListener nowPlayingHandler;
	private Handler handler;
    
	public NowPlayingFetcher(final MediaListener nowPlayingHandler) {
		this.nowPlayingHandler = nowPlayingHandler;
        this.handler = new Handler();
	}
	
	Runnable fetchRunner = new Runnable() {
		@Override public void run() {
			makeFetchTask().execute();
			handler.postDelayed(this, Constants.CURRENT_TRACK_POLL_DELAY_MS);
		}
	};
	
	private AsyncTask<Void, Void, TrackInfo> makeFetchTask() {
		return new AsyncTask<Void, Void, TrackInfo>() {
		    protected TrackInfo doInBackground(Void... unusedParams) {
		    	Log.i(Constants.LOG_TAG, "Fetching current track info");
		    	return new TrackInfo(HttpUtil.getUrl(Constants.CURRENT_TRACK_URL));
		    }

		    protected void onPostExecute(TrackInfo nowPlaying) {
				nowPlayingHandler.onTrackInfoFetched(nowPlaying);
		    }
		};
	}
	
    public void run() {
    	stop();
    	runOnce();
    	handler.postDelayed(fetchRunner, Constants.CURRENT_TRACK_POLL_DELAY_MS);
    }
    
    public void runOnce() {
    	makeFetchTask().execute();
    }
    
    public void stop() {
    	handler.removeCallbacks(null);
    }
}
