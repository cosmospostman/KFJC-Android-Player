package org.kfjc.droid;

import java.io.IOException;

import android.app.IntentService;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.widget.Toast;

public class LiveStreamService extends IntentService {

	private static final String AAC_HI = "http://netcast6.kfjc.org:80/";

	MediaPlayer mPlayer;

	public LiveStreamService(String name) {
		super(name);
		mPlayer = new MediaPlayer();
		mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
	}
	
	public LiveStreamService() {
		this("Default lss");
	}

	@Override
	protected void onHandleIntent(Intent arg0) {

		// Set source
		try {
			mPlayer.setDataSource(AAC_HI);
		} catch (IllegalArgumentException e) {
			Toast.makeText(getApplicationContext(),
					"You might not set the URI correctly!", Toast.LENGTH_LONG)
					.show();
		} catch (SecurityException e) {
			Toast.makeText(getApplicationContext(),
					"You might not set the URI correctly!", Toast.LENGTH_LONG)
					.show();
		} catch (IllegalStateException e) {
			Toast.makeText(getApplicationContext(),
					"You might not set the URI correctly!", Toast.LENGTH_LONG)
					.show();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		mPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
			
			@Override
			public void onPrepared(MediaPlayer mp) {
				mp.start();	
			}
		});

		// Prepare for playback
		try {
			mPlayer.prepareAsync();
		} catch (IllegalStateException e) {
			Toast.makeText(getApplicationContext(),
					"You might not set the URI correctly!", Toast.LENGTH_LONG)
					.show();
		}
		

	}
}
