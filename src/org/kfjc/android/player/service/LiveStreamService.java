package org.kfjc.android.player.service;

import org.kfjc.android.player.NowPlayingInfo;

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
		public void onTrackInfoFetched(NowPlayingInfo trackInfo);
	};

	private static final String AAC_HI = "http://netcast6.kfjc.org:80/";
	private MediaPlayer mPlayer;
	private MediaListener mediaListener;
	private NowPlayingFetcher nowPlayingFetcher;
	private final IBinder liveStreamBinder = new LiveStreamBinder();
	private boolean isPlaying = false;
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
		return isPlaying;
	}
	
	public void setMediaEventListener(MediaListener listener) {
		this.mediaListener = listener;
		this.nowPlayingFetcher = new NowPlayingFetcher(mediaListener);
	}
	
	public void play() {
		initPlayer();
		mPlayer.prepareAsync();
	}

	public void stop() {
		releaseAsync(mPlayer);
		isPlaying = false;
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
	
	public void stopPlaylistFetcher() {
		nowPlayingFetcher.stop();
		isFetching = false;
	}
	
	private void releaseAsync(MediaPlayer mPlayer) {
		new AsyncTask<MediaPlayer, Void, Void>() {
			@Override
			protected Void doInBackground(MediaPlayer... mediaPlayers) {
				for (int i = 0; i < mediaPlayers.length; i++ ) {
					if (mediaPlayers[i] != null) {
						mediaPlayers[i].release();
					}
				}
				return null;
			}
		}.execute(mPlayer, null, null);
	};

	private void initPlayer() {
		mPlayer = new MediaPlayer();
		mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
		mPlayer.setWakeMode(getApplicationContext(),
				PowerManager.PARTIAL_WAKE_LOCK);
		mPlayer.setOnPreparedListener(onPrepared);
		mPlayer.setOnErrorListener(onError);
		mPlayer.setOnCompletionListener(onComplete);
		try {
			mPlayer.setDataSource(AAC_HI);
		} catch (Exception e) {
            Log.d("Error setting media player datasource", e.getLocalizedMessage());
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
					isPlaying = true;
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
			isPlaying = false;
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
			isPlaying = false;
		}
	};	
}
