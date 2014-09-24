package org.kfjc.android.player;

import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

public class LiveStreamService extends Service implements
		MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener,
		MediaPlayer.OnCompletionListener, MediaPlayer.OnBufferingUpdateListener {
	
	public class LiveStreamBinder extends Binder {
		LiveStreamService getService() {
			return LiveStreamService.this;
		}
	}
	
	public interface OnPlayListener {
		public void onPlay();
	};

	private static final String AAC_HI = "http://netcast6.kfjc.org:80/";
	private MediaPlayer mPlayer;
	private final IBinder liveStreamBinder = new LiveStreamBinder();
	private OnBufferingUpdateListener bufferingListener;
	private OnPlayListener onPlayListener;

	
	public void setBufferingInfoListener(OnBufferingUpdateListener listener) {
		this.bufferingListener = listener;
	}
	
	public void setOnPlayListener(OnPlayListener listener) {
		this.onPlayListener = listener;
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
		mPlayer.setOnBufferingUpdateListener(this);
		try {
			mPlayer.setDataSource(AAC_HI);
		} catch (Exception e) {
            Log.d("Error setting media player datasource", e.getLocalizedMessage());
		}
	}

	@Override
	public void onCompletion(MediaPlayer arg0) {
		// TODO Replay the stream. Should only stop explicitly.
	}

	@Override
	public boolean onError(MediaPlayer arg0, int arg1, int arg2) {
		this.mPlayer.release();
		this.mPlayer = null;
		return false;
	}

	@Override
	public void onPrepared(MediaPlayer arg0) {
		mPlayer.seekTo(0);
		mPlayer.start();
		if (onPlayListener != null) {
			onPlayListener.onPlay();
		}
	}

	@Override
	public void onBufferingUpdate(MediaPlayer mp, int percent) {
		bufferingListener.onBufferingUpdate(mp, percent);
	}
}
