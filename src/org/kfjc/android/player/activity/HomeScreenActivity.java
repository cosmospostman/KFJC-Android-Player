package org.kfjc.android.player.activity;

import org.kfjc.android.player.NowPlayingInfo;
import org.kfjc.android.player.SettingsDialog;
import org.kfjc.android.player.control.HomeScreenControl;
import org.kfjc.android.player.util.GraphicsUtil;
import org.kfjc.android.player.util.UiUtil;
import org.kfjc.droid.R;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class HomeScreenActivity extends Activity {
	
	enum PlayStopButtonState {
		PLAY, // When showing the play icon
		STOP  // When showing the stop icon
	}

	private ImageView playStopButton;
	private ImageView fullscreenButton;
	private ImageView settingsButton;
	private TextView currentDjTextView;
	private TextView currentTrackTextView;
	private TextView currentArtistTextView;
	private ImageView radioDevil;
	private PlayStopButtonState playStopButtonState = PlayStopButtonState.PLAY;
	private GraphicsUtil graphics;
	private HomeScreenControl control;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_home_screen);
		graphics = new GraphicsUtil(getResources());
		playStopButton = (ImageView) findViewById(R.id.playstopbutton);
		fullscreenButton = (ImageView) findViewById(R.id.fullscreenbutton);
		settingsButton = (ImageView) findViewById(R.id.settingsbutton);
		radioDevil = (ImageView) findViewById(R.id.logo);
		radioDevil.setImageResource(graphics.radioDevilOff());
		currentDjTextView = (TextView) findViewById(R.id.currentDJ);
		currentTrackTextView = (TextView) findViewById(R.id.currentTrack);
		currentArtistTextView = (TextView) findViewById(R.id.currentArtist);
		
		addButtonListeners();
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		control = new HomeScreenControl(this);
	}

	@Override
	public void onDestroy() {
		control.destroy();
		super.onDestroy();
	}

	private void addButtonListeners() {
		playStopButton.setOnTouchListener(UiUtil.buttonTouchListener);
		playStopButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if (playStopButtonState == PlayStopButtonState.STOP) {
					control.stopStream();
				} else {
					control.playStream();
				}
			}
		});
		fullscreenButton.setOnTouchListener(UiUtil.buttonTouchListener);
		fullscreenButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent fullscreenIntent = new Intent(
						HomeScreenActivity.this, FullScreenActivity.class);
				fullscreenIntent.setAction("org.kfjc.android.player.FULLSCREEN");
				startActivity(fullscreenIntent);
			}
		});
		settingsButton.setOnTouchListener(UiUtil.buttonTouchListener);
		settingsButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				DialogFragment settingsFragment = new SettingsDialog();
				settingsFragment.show(getFragmentManager(), "settings");
			}
		});
	}
	
	public void updateTrackInfo(NowPlayingInfo nowPlaying) {
		currentDjTextView.setText(UiUtil.getAppTitle(getApplicationContext(), nowPlaying));
		currentTrackTextView.setText(nowPlaying.getTrackTitle());
		currentArtistTextView.setText(nowPlaying.getArtist());
	}
	
	public void onPlayerBuffer() {
		graphics.bufferDevil(radioDevil, true);
		playStopButton.setImageResource(R.drawable.ic_stop);
		playStopButtonState = PlayStopButtonState.STOP;	
	};
	
	public void onPlayerBufferComplete() {
		graphics.bufferDevil(radioDevil, false);
		radioDevil.setImageResource(graphics.radioDevilOn());
		playStopButton.setImageResource(R.drawable.ic_stop);
		playStopButtonState = PlayStopButtonState.STOP;
	}
	
	public void onPlayerStop() {
		graphics.bufferDevil(radioDevil, false);
		radioDevil.setImageResource(graphics.radioDevilOff());
		playStopButton.setImageResource(R.drawable.ic_play);
		playStopButtonState = PlayStopButtonState.PLAY;		
	}
}
