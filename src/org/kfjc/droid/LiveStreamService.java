package org.kfjc.droid;

import java.io.IOException;

import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;

public class LiveStreamService extends Service implements
		MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener,
		MediaPlayer.OnCompletionListener {

	private static final String AAC_HI = "http://netcast6.kfjc.org:80/";
	private MediaPlayer mPlayer;
	private final IBinder liveStreamBinder = new LiveStreamBinder();

	@Override
	public void onCreate() {
		super.onCreate();
		mPlayer = new MediaPlayer();
		initPlayer();
	}

	private void initPlayer() {
		// TODO: Add permissions to manifest.
		mPlayer.setWakeMode(getApplicationContext(),
				PowerManager.PARTIAL_WAKE_LOCK);
		mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
		mPlayer.setOnPreparedListener(this);
		try {
			mPlayer.setDataSource(AAC_HI);
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void onCompletion(MediaPlayer arg0) {
		// TODO Auto-generated method stub
	}

	@Override
	public boolean onError(MediaPlayer arg0, int arg1, int arg2) {
		// TODO Auto-generated method stub
		return false;
	}

	public void play() {
		mPlayer.prepareAsync();
	}
	
	public void stop() {
		mPlayer.stop();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return liveStreamBinder;
	}

	@Override
	public boolean onUnbind(Intent intent) {
		mPlayer.stop();
		mPlayer.release();
		return false;
	}

	public class LiveStreamBinder extends Binder {
		LiveStreamService getService() {
			return LiveStreamService.this;
		}
	}

	@Override
	public void onPrepared(MediaPlayer arg0) {
		mPlayer.seekTo(0);
		mPlayer.start();
	}
}
