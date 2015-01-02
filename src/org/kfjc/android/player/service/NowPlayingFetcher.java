package org.kfjc.android.player.service;

import org.kfjc.android.player.NowPlayingInfo;
import org.kfjc.android.player.service.LiveStreamService.MediaListener;
import org.kfjc.android.player.util.HttpUtil;


import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;

public class NowPlayingFetcher {

	private static final String METADATA = "http://kfjc.org/api/playlists/current.php";
	private static final int POLL_DELAY_MS = 10000;
	private MediaListener nowPlayingHandler;
	private Handler handler = new Handler();
    
	public NowPlayingFetcher(final MediaListener nowPlayingHandler) {
		this.nowPlayingHandler = nowPlayingHandler;
	}
	
	Runnable fetchRunner = new Runnable() {
		@Override 
		public void run() {
			makeFetchTask().execute();
			handler.postDelayed(this, POLL_DELAY_MS);
		}
	};
	
	private AsyncTask<Void, Void, NowPlayingInfo> makeFetchTask() {
		return new AsyncTask<Void, Void, NowPlayingInfo>() {
		    protected NowPlayingInfo doInBackground(Void... unusedParams) {
		    	Log.i("kfjc", "Fetching track info");
		    	return new NowPlayingInfo(HttpUtil.getUrl(METADATA));
		    }

		    protected void onPostExecute(NowPlayingInfo nowPlaying) {
				nowPlayingHandler.onTrackInfoFetched(nowPlaying);
		    }
		};
	}
	
    public void run() {
    	stop();
    	runOnce();
    	handler.postDelayed(fetchRunner, POLL_DELAY_MS);
    }
    
    public void runOnce() {
    	makeFetchTask().execute();
    }
    
    public void stop() {
    	handler.removeCallbacks(null);
    }
}
