package org.kfjc.android.player.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
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
    private ImageView settingsButton;
    private ImageView backgroundImageView;
    private FloatingActionButton playStopButton;

    private LinearLayout nowPlayingContainer;
	private TextView currentDjTextView;
	private TextView currentTrackTextView;

    private LinearLayout statusContainer;
    private TextView statusMessageTextView;

    private StatusState connectionStatusState = StatusState.CONNECTION_ERROR;
	private PlayStopButtonState playStopButtonState = PlayStopButtonState.PLAY;
	private GraphicsUtil graphics;
	private static HomeScreenControl control;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

		setContentView(R.layout.activity_home_screen);
		graphics = new GraphicsUtil(getResources());
		playStopButton = (FloatingActionButton) findViewById(R.id.playstopbutton);
        backgroundImageView = (ImageView) findViewById(R.id.backgroundImageView);
		settingsButton = (ImageView) findViewById(R.id.settingsButton);
		radioDevil = (ImageView) findViewById(R.id.logo);
		radioDevil.setImageResource(graphics.radioDevilOff());

        nowPlayingContainer = (LinearLayout) findViewById(R.id.nowPlayingContainer);
		currentDjTextView = (TextView) findViewById(R.id.currentDJ);
		currentTrackTextView = (TextView) findViewById(R.id.currentTrack);

        statusContainer = (LinearLayout) findViewById(R.id.statusContainer);
        statusMessageTextView = (TextView) findViewById(R.id.statusMessage);

		addButtonListeners();
        setStatusState(StatusState.CONNECTING);
	}
	
	@Override
	protected void onStart() {
		super.onStart();
        if (control == null) {
            control = new HomeScreenControl(this);
        }
        control.onStart();
	}

    @Override
    protected void onStop() {
        super.onStop();
        control.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        int hourOfDay = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        backgroundImageView.setImageResource(GraphicsUtil.imagesOfTheHour[hourOfDay]);
    }

    @Override
	public void onDestroy() {
        if (isFinishing()) {
            control.destroy();
        }
		super.onDestroy();
	}

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
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
		radioDevil.setOnTouchListener(UiUtil.buttonTouchListener);
		radioDevil.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent fullscreenIntent = new Intent(
                        HomeScreenActivity.this, LavaLampActivity.class);
                fullscreenIntent.setAction("org.kfjc.android.player.FULLSCREEN");
                startActivity(fullscreenIntent);
            }
        });
        radioDevil.setEnabled(false);
		settingsButton.setOnTouchListener(UiUtil.buttonTouchListener);
		settingsButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				control.showSettings();
			}
		});
	}

	public void updateTrackInfo(TrackInfo nowPlaying) {
        setStatusState(StatusState.HIDDEN);
        if (nowPlaying.getCouldNotFetch()) {
            setStatusState(StatusState.CONNECTION_ERROR);
        } else {
            currentDjTextView.setText(nowPlaying.getDjName());
            currentTrackTextView.setText(Html.fromHtml(nowPlaying.getArtist()
                    + " <i>" + nowPlaying.getTrackTitle() + "</i>"));
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
        radioDevil.setEnabled(true);
	}
	
	public void onPlayerStop() {
		graphics.bufferDevil(radioDevil, false);
        radioDevil.setEnabled(false);
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

    public void showDebugAlert(final String message) {
        new AlertDialog.Builder(this)
                .setTitle("Error details")
                .setMessage(message)
                .setPositiveButton("Copy", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface arg0, int arg1) {
                        ClipboardManager clipboard = (ClipboardManager)
                                getSystemService(Context.CLIPBOARD_SERVICE);
                        ClipData clip = ClipData.newPlainText("KFJC error", message);
                        clipboard.setPrimaryClip(clip);
                    }
                })
                .show();
    }
}
