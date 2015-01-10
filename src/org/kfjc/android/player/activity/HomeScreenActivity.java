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

import java.util.Calendar;

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

    public enum StatusState {
        HIDDEN,                 // Shows current track instead
        CONNECTING,
        CONNECTION_ERROR
    }

    private ImageView radioDevil;
    private LinearLayout settingsButton;
    private ImageView backgroundImageView;
    private TextView streamNicknameTextView;
    private ImageView fullscreenButton;
    private ImageView playStopButton;

    private LinearLayout nowPlayingContainer;
	private TextView currentDjTextView;
	private TextView currentTrackTextView;
	private TextView currentArtistTextView;

    private LinearLayout statusContainer;
    private TextView statusMessageTextView;

    private StatusState connectionStatusState = StatusState.CONNECTION_ERROR;
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
        backgroundImageView = (ImageView) findViewById(R.id.backgroundImageView);
		settingsButton = (LinearLayout) findViewById(R.id.settingsButton);
		radioDevil = (ImageView) findViewById(R.id.logo);
		radioDevil.setImageResource(graphics.radioDevilOff());

        nowPlayingContainer = (LinearLayout) findViewById(R.id.nowPlayingContainer);
		currentDjTextView = (TextView) findViewById(R.id.currentDJ);
		currentTrackTextView = (TextView) findViewById(R.id.currentTrack);
		currentArtistTextView = (TextView) findViewById(R.id.currentArtist);
		streamNicknameTextView = (TextView) findViewById(R.id.streamQuality);

        statusContainer = (LinearLayout) findViewById(R.id.statusContainer);
        statusMessageTextView = (TextView) findViewById(R.id.statusMessage);

		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB){
			setSettingsButtonAlpha();
		}
		addButtonListeners();

        setStatusState(StatusState.CONNECTING);
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
    protected void onResume() {
        super.onResume();
        int hourOfDay = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        backgroundImageView.setImageResource(GraphicsUtil.imagesOfTheHour[hourOfDay]);
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
        playStopButton.setEnabled(false);
		fullscreenButton.setOnTouchListener(UiUtil.buttonTouchListener);
		fullscreenButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent fullscreenIntent = new Intent(
						HomeScreenActivity.this, LavaLampActivity.class);
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
        setStatusState(StatusState.HIDDEN);
        if (nowPlaying.getCouldNotFetch()) {
            setStatusState(StatusState.CONNECTION_ERROR);
        } else {
            currentDjTextView.setText(nowPlaying.getDjName());
            currentTrackTextView.setText(nowPlaying.getTrackTitle());
            currentArtistTextView.setText(nowPlaying.getArtist());
        }
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

    public void enablePlayStopButton() {
        this.playStopButton.setEnabled(true);
    }

    public void setStatusState(StatusState state) {
        nowPlayingContainer.setVisibility(View.GONE);
        statusContainer.setVisibility(View.GONE);
        switch(state) {
            case HIDDEN:
                nowPlayingContainer.setVisibility(View.VISIBLE);
                break;
            case CONNECTING:
                if (this.connectionStatusState == StatusState.CONNECTION_ERROR) {
                    statusContainer.setVisibility(View.VISIBLE);
                    statusMessageTextView.setText(R.string.status_connecting);
                } else {
                    setStatusState(StatusState.HIDDEN);
                }
                break;
            case CONNECTION_ERROR:
                statusContainer.setVisibility(View.VISIBLE);
                statusMessageTextView.setText(R.string.status_not_connected);
                break;
        }
        this.connectionStatusState = state;
    }
}
