package org.kfjc.android.player.control;

import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

public class LiveStreamService extends Service implements
		MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener,
		MediaPlayer.OnCompletionListener {
	
	public class LiveStreamBinder extends Binder {
		public LiveStreamService getService() {
			return LiveStreamService.this;
		}
	}
	
	public interface MediaListener {
		public void onPlay();
		public void onError();
	};

	private static final String AAC_HI = "http://netcast6.kfjc.org:80/";
	private MediaPlayer mPlayer;
	private final IBinder liveStreamBinder = new LiveStreamBinder();
	private MediaListener mediaListener;
	
	public void setOnPlayListener(MediaListener listener) {
		this.mediaListener = listener;
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return liveStreamBinder;
	}

	@Override
	public boolean onUnbind(Intent intent) {
		if (this.mPlayer != null) {
			if (mPlayer.isPlaying()) {
				mPlayer.stop();
			}
			mPlayer.release();
		}
		return false;
	}
	
	public void play() {
		if (mPlayer != null && mPlayer.isPlaying()) {
			return;
		}
		initPlayer();
		mPlayer.prepareAsync();
	}

	public void stop() {
		if (mPlayer != null) {
			mPlayer.release();
			mPlayer = null;
		}
	}

	private void initPlayer() {
		mPlayer = new MediaPlayer();
		// TODO: Add permissions to manifest.
		mPlayer.setWakeMode(getApplicationContext(),
				PowerManager.PARTIAL_WAKE_LOCK);
		mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
		mPlayer.setOnPreparedListener(this);
		mPlayer.setOnErrorListener(this);
		mPlayer.setOnCompletionListener(this);
		try {
			mPlayer.setDataSource(AAC_HI);
		} catch (Exception e) {
            Log.d("Error setting media player datasource", e.getLocalizedMessage());
		}
	}

	@Override
	public void onCompletion(MediaPlayer arg0) {
		// Reach this stage if, for example, network connection lost.
		mediaListener.onError();
		this.mPlayer.release();
		this.mPlayer = null;
	}

	@Override
	public boolean onError(MediaPlayer arg0, int arg1, int arg2) {
		mediaListener.onError();
		this.mPlayer.release();
		this.mPlayer = null;
		return true;
	}

	@Override
	public void onPrepared(MediaPlayer arg0) {
		mPlayer.seekTo(0);
		mPlayer.start();
		if (mediaListener != null) {
			mediaListener.onPlay();
		}
	}
}
