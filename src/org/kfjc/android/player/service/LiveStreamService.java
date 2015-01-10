package org.kfjc.android.player.service;

import org.kfjc.android.player.Constants;
import org.kfjc.android.player.model.TrackInfo;

import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

// TODO: Stop playlist fetcher when not playing and in background.
public class LiveStreamService extends Service {
	
	public class LiveStreamBinder extends Binder {
		public LiveStreamService getService() {
			return LiveStreamService.this;
		}
	}
	
	public interface MediaListener {
		public void onPlay();
		public void onError();
		public void onTrackInfoFetched(TrackInfo trackInfo);
	}

	private MediaPlayer mPlayer;
	private MediaListener mediaListener;
	private NowPlayingFetcher nowPlayingFetcher;
	private final IBinder liveStreamBinder = new LiveStreamBinder();
	private boolean isFetching = false;
	
	@Override
	public IBinder onBind(Intent intent) {
		return liveStreamBinder;
	}

	@Override
	public boolean onUnbind(Intent intent) {
		nowPlayingFetcher.stop();
		if (this.mPlayer == null) {
			return false;
		}
		try {
			mPlayer.stop();
			mPlayer.release();
		} catch (IllegalStateException e) {
			// Thrown if player has already been released.
		}
		return false;
	}
	
	public boolean isPlaying() {
        try {
            return mPlayer != null && mPlayer.isPlaying();
        } catch (IllegalStateException e) {
            // Fall through and return false
        }
        return false;
	}
	
	public void setMediaEventListener(MediaListener listener) {
		this.mediaListener = listener;
		this.nowPlayingFetcher = new NowPlayingFetcher(mediaListener);
	}
	
	public void play(String streamUrl) {
		initPlayer(streamUrl);
		mPlayer.prepareAsync();
	}

	public void stop() {
		releaseAsync(mPlayer);
	}
	
	public void runPlaylistFetcherOnce() {
		nowPlayingFetcher.runOnce();
	}
	
	public void runPlaylistFetcher() {
		if (!isFetching) {
			nowPlayingFetcher.runOnce();
			nowPlayingFetcher.run();
		}
		isFetching = true;
	}
	
	private void releaseAsync(MediaPlayer mPlayer) {
		new AsyncTask<MediaPlayer, Void, Void>() {
			@Override
			protected Void doInBackground(MediaPlayer... mediaPlayers) {
				for (MediaPlayer mp : mediaPlayers) {
					if (mp != null) {
						mp.release();
					}
				}
				return null;
			}
		}.execute(mPlayer, null, null);
	}

	private void initPlayer(String streamUrl) {
		mPlayer = new MediaPlayer();
		mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
		mPlayer.setWakeMode(getApplicationContext(),
				PowerManager.PARTIAL_WAKE_LOCK);
		mPlayer.setOnPreparedListener(onPrepared);
		mPlayer.setOnErrorListener(onError);
		mPlayer.setOnCompletionListener(onComplete);
		try {
			mPlayer.setDataSource(streamUrl);
			Log.i(Constants.LOG_TAG, "Set stream source to " + streamUrl);
		} catch (Exception e) {
            Log.d(Constants.LOG_TAG,
                    "Error setting media player datasource:" + e.getLocalizedMessage());
		}
	}
	
	private	MediaPlayer.OnPreparedListener onPrepared = new MediaPlayer.OnPreparedListener() {
		@Override
		public void onPrepared(MediaPlayer mp) {
			if (mp == mPlayer) {
				mPlayer.seekTo(0);
				mPlayer.start();
				if (mediaListener != null) {
					mediaListener.onPlay();
				}
			}
		}
	};
	
	private MediaPlayer.OnErrorListener onError = new MediaPlayer.OnErrorListener() {
		@Override
		public boolean onError(MediaPlayer arg0, int arg1, int arg2) {
			mediaListener.onError();
			mPlayer.release();
			mPlayer = null;
			return true;
		}
	};
	
	private MediaPlayer.OnCompletionListener onComplete = new MediaPlayer.OnCompletionListener() {
		@Override
		public void onCompletion(MediaPlayer arg0) {
			// Reach this stage if, for example, network connection lost.
			mediaListener.onError();
			mPlayer.release();
			mPlayer = null;
		}
	};	
}
