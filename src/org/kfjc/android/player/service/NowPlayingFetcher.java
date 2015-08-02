package org.kfjc.android.player.service;

import org.kfjc.android.player.Constants;
import org.kfjc.android.player.model.TrackInfo;
import org.kfjc.android.player.service.LiveStreamService.MediaListener;
import org.kfjc.android.player.util.HttpUtil;

import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;

public class NowPlayingFetcher {

//    private MediaListener nowPlayingHandler;
//	private Handler handler;
//
//	public NowPlayingFetcher() {
//        this.handler = new Handler();
//	}
//
//	public void setNowPlayingHandler(final MediaListener nowPlayingHandler) {
//		this.nowPlayingHandler = nowPlayingHandler;
//		if (lastFetchedTrackInfo != null) {
//			nowPlayingHandler.onTrackInfoFetched(lastFetchedTrackInfo);
//		}
//	}
//
//	Runnable fetchRunner = new Runnable() {
//		@Override public void run() {
//			makeFetchTask().execute();
//			handler.postDelayed(this, Constants.CURRENT_TRACK_POLL_DELAY_MS);
//		}
//	};
//
//    public void run() {
//    	stop();
//    	runOnce();
//    	handler.postDelayed(fetchRunner, Constants.CURRENT_TRACK_POLL_DELAY_MS);
//    }
//
//    public void runOnce() {
//    	makeFetchTask().execute();
//    }
//
//    public void stop() {
//    	handler.removeCallbacks(null);
//    }
}
