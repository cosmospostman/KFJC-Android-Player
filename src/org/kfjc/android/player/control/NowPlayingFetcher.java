package org.kfjc.android.player.control;

import java.util.Timer;
import java.util.TimerTask;

import org.kfjc.android.player.NowPlayingInfo;
import org.kfjc.android.player.util.HttpUtil;

import android.os.AsyncTask;

public class NowPlayingFetcher {

	private static final String METADATA = "http://kfjc.org/api/playlists/current.php";
	private static final int POLL_DELAY_MS = 30000;
	private Timer timer = new Timer("NowPlaying timer", true);
	private TimerTask timerTask;
	private NowPlayingHandler handler;
	private boolean isRunning = false;
	
	public interface NowPlayingHandler {
		public void onTrackInfoFetched(NowPlayingInfo trackInfo);
	}
    
	public NowPlayingFetcher(final NowPlayingHandler handler) {
		this.handler = handler;
		this.timerTask = new TimerTask() {
    		@Override 
    		public void run() {
    			makeFetchTask().execute();	
    		}
    	};
	}
	
	private AsyncTask<Void, Void, NowPlayingInfo> makeFetchTask() {
		return new AsyncTask<Void, Void, NowPlayingInfo>() {
		    protected NowPlayingInfo doInBackground(Void... unusedParams) {
		    	return new NowPlayingInfo(HttpUtil.getUrl(METADATA));
		    }

		    protected void onPostExecute(NowPlayingInfo nowPlaying) {
		        handler.onTrackInfoFetched(nowPlaying);
		    }
		};
	}
	
    public void run() {
    	if (!this.isRunning) {
    		timer.scheduleAtFixedRate(this.timerTask, 0, POLL_DELAY_MS);
    	}
    	this.isRunning = true;
    }
    
    public void runOnce() {
	    this.timerTask.run();
    }
}
