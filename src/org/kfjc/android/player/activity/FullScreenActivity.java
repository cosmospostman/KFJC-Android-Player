package org.kfjc.android.player.activity;

import android.app.Activity;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.VideoView;

import org.kfjc.droid.R;

public class FullScreenActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		super.onCreate(savedInstanceState);
		overridePendingTransition(R.anim.fadein, R.anim.fadeout);
		setContentView(R.layout.activity_lavaplayer);
		getWindow().setFlags(
				WindowManager.LayoutParams.FLAG_FULLSCREEN, 
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		final VideoView videoView = (VideoView) findViewById(R.id.lavaView);
		videoView.setLayoutParams(new FrameLayout.LayoutParams(1440, 2160));
		videoView.setOnPreparedListener (new OnPreparedListener() {                    
		    @Override
		    public void onPrepared(MediaPlayer mp) {
		        mp.setLooping(true);
		    }
		});
		Uri video = Uri.parse(
				"android.resource://" + getPackageName() + "/" + R.raw.lavalava);
		videoView.setVideoURI(video);
       	videoView.setMediaController(null);
		videoView.start();
	}
	
}
