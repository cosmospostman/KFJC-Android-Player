package org.kfjc.android.player.activity;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.kfjc.android.player.model.TrackInfo;
import org.kfjc.android.player.control.HomeScreenControl;
import org.kfjc.android.player.util.GraphicsUtil;
import org.kfjc.android.player.util.UiUtil;
import org.kfjc.droid.R;

public class HomeScreenActivity extends Activity {
	
	enum PlayStopButtonState {
		PLAY, // When showing the play icon
		STOP  // When showing the stop icon
	}

	private ImageView playStopButton;
	private ImageView fullscreenButton;
	private LinearLayout settingsButton;
	private TextView currentDjTextView;
	private TextView currentTrackTextView;
	private TextView currentArtistTextView;
	private TextView streamNicknameTextView;
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
		settingsButton = (LinearLayout) findViewById(R.id.settingsButton);
		radioDevil = (ImageView) findViewById(R.id.logo);
		radioDevil.setImageResource(graphics.radioDevilOff());
		currentDjTextView = (TextView) findViewById(R.id.currentDJ);
		currentTrackTextView = (TextView) findViewById(R.id.currentTrack);
		currentArtistTextView = (TextView) findViewById(R.id.currentArtist);
		streamNicknameTextView = (TextView) findViewById(R.id.streamQuality);

		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB){
			setSettingsButtonAlpha();
		}
		addButtonListeners();
	}
	
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void setSettingsButtonAlpha() {
		settingsButton.setAlpha(0.4f);
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
				control.showSettings();
			}
		});
	}
	
	public void updateStreamNickname(String streamName) {
		streamNicknameTextView.setText(streamName);
	}
		
	public void updateTrackInfo(TrackInfo nowPlaying) {
		currentDjTextView.setText(UiUtil.getAppTitle(getApplicationContext(), nowPlaying));
		currentTrackTextView.setText(nowPlaying.getTrackTitle());
		currentArtistTextView.setText(nowPlaying.getArtist());
	}
	
	public void onPlayerBuffer() {
		graphics.bufferDevil(radioDevil, true);
		playStopButton.setImageResource(R.drawable.ic_stop);
		playStopButtonState = PlayStopButtonState.STOP;	
	}
	
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
