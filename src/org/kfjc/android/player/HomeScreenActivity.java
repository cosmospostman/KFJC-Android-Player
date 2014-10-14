package org.kfjc.android.player;

import java.util.Timer;
import java.util.TimerTask;

import org.kfjc.android.player.LiveStreamService.LiveStreamBinder;
import org.kfjc.android.player.LiveStreamService.OnPlayListener;
import org.kfjc.android.player.NowPlayingFetcher.NowPlayingHandler;
import org.kfjc.droid.R;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class HomeScreenActivity extends Activity {
	
	enum PlayStopButtonState {
		PLAY, // When showing the play icon
		STOP  // When showing the stop icon
	}

	private LiveStreamService streamService;
	private Intent playIntent;
	private ImageView playStopButton;
	private ImageView fullscreenButton;
	private ImageView settingsButton;
	private TextView currentDjTextView;
	private TextView currentTrackTextView;
	private TextView currentArtistTextView;
	private ImageView radioDevil;
	private ServiceConnection musicConnection;
	private boolean isPlaying = false;
	private PlayStopButtonState playStopButtonState = PlayStopButtonState.PLAY;
	private GraphicsUtil graphics;
	private NotificationCompat.Builder nowPlayingNotification =
			new NotificationCompat.Builder(this);
	private static final int NOWPLAYING_NOTIFICATION_ID = 1;
	private NotificationManager notificationManager;
	private Timer timer = new Timer("NowPlaying timer", true);

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_home_screen);
		graphics = new GraphicsUtil();
		playStopButton = (ImageView) findViewById(R.id.playstopbutton);
		fullscreenButton = (ImageView) findViewById(R.id.fullscreenbutton);
		settingsButton = (ImageView) findViewById(R.id.settingsbutton);
		radioDevil = (ImageView) findViewById(R.id.logo);
		radioDevil.setImageResource(graphics.radioDevilOff());
		currentDjTextView = (TextView) findViewById(R.id.currentDJ);
		currentTrackTextView = (TextView) findViewById(R.id.currentTrack);
		currentArtistTextView = (TextView) findViewById(R.id.currentArtist);
		notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		if (musicConnection == null) {
			musicConnection = new ServiceConnection() {
				@Override
				public void onServiceConnected(ComponentName name, IBinder service) {
					LiveStreamBinder binder = (LiveStreamBinder) service;
					streamService = binder.getService();
					addButtonListeners();
					timer.scheduleAtFixedRate(trackInfoUpdater, 0, 30000);
				}

				@Override
				public void onServiceDisconnected(ComponentName arg0) {}
			};
		}
		if (playIntent == null) {
			playIntent = new Intent(this, LiveStreamService.class);
			bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE);
			startService(playIntent);
		}
	}

	@Override
	public void onDestroy() {
		stopService(playIntent);
		streamService = null;
		//TODO: stop buffering timeouts, 
		super.onDestroy();
	}
		
	private OnBufferingUpdateListener onBufferingUpdate = new OnBufferingUpdateListener() {
		@Override
		public void onBufferingUpdate(MediaPlayer mp, int percent) {}
	};

	private OnPlayListener onPlay = new OnPlayListener() {
		@Override
		public void onPlay() {
			graphics.bufferDevil(radioDevil, false);
			radioDevil.setImageResource(graphics.radioDevilOn());
			isPlaying = true;
			trackInfoUpdater.run();
		}	
	};

	private NowPlayingHandler onTrackInfoChange = new NowPlayingHandler() {
		@Override
		public void onTrackInfoFetched(NowPlayingInfo nowPlaying) {
			currentDjTextView.setText(UiUtil.getAppTitle(getApplicationContext(), nowPlaying));
			currentTrackTextView.setText(nowPlaying.getTrackTitle());
			currentArtistTextView.setText(nowPlaying.getArtist());
			if (HomeScreenActivity.this.isPlaying) {
				updateNowPlayNotification(nowPlaying);
			}
		}
	};

	private void addButtonListeners() {
		playStopButton.setOnTouchListener(
				UiUtil.makeButtonTouchListener(R.drawable.ic_play, R.drawable.ic_play_blur));
		playStopButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if (playStopButtonState == PlayStopButtonState.STOP) { // Then stop
					streamService.stop();
					isPlaying = false;
					cancelNowPlayNotification();
					graphics.bufferDevil(radioDevil, false);
					radioDevil.setImageResource(graphics.radioDevilOff());
					playStopButton.setImageResource(R.drawable.ic_play);
					playStopButtonState = PlayStopButtonState.PLAY;
				} else { // Then play
					streamService.setOnPlayListener(onPlay);
					streamService.setBufferingInfoListener(onBufferingUpdate);
					streamService.play();
					trackInfoUpdater.run();
					playStopButton.setImageResource(R.drawable.ic_stop);
					graphics.bufferDevil(radioDevil, true);
					playStopButtonState = PlayStopButtonState.STOP;
				}
			}
		});
		fullscreenButton.setOnTouchListener(
				UiUtil.makeButtonTouchListener(R.drawable.ic_fullscreen, R.drawable.ic_fullscreen_blur));
		fullscreenButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent fullscreenIntent = new Intent(
						HomeScreenActivity.this, FullScreenActivity.class);
				fullscreenIntent.setAction("org.kfjc.android.player.FULLSCREEN");
				startActivity(fullscreenIntent);
			}
		});
		settingsButton.setOnTouchListener(
				UiUtil.makeButtonTouchListener(R.drawable.ic_fullscreen, R.drawable.ic_fullscreen_blur));
		settingsButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				DialogFragment settingsFragment = new SettingsDialog();
				settingsFragment.show(getFragmentManager(), "settings");
			}
		});
	}
	
	private TimerTask trackInfoUpdater = new TimerTask() {
		@Override 
		public void run() {
			new NowPlayingFetcher(onTrackInfoChange).execute();	
		}
	};

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.live_stream, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private void updateNowPlayNotification(NowPlayingInfo nowPlaying) {
		String artistTrackString = nowPlaying.getArtist() +
				" - " + nowPlaying.getTrackTitle();
		PendingIntent contentIntent = PendingIntent.getActivity(
				this, 0, new Intent(this, HomeScreenActivity.class),
				Notification.FLAG_ONGOING_EVENT);    
		nowPlayingNotification = new NotificationCompat.Builder(this)
		    .setSmallIcon(R.drawable.ic_kfjc_notification)
		    .setContentTitle(UiUtil.getAppTitle(getApplicationContext(), nowPlaying))
		    .setContentText(artistTrackString)
			.setOngoing(true)
			.setWhen(0)
			.setContentIntent(contentIntent);
		notificationManager.notify(
				NOWPLAYING_NOTIFICATION_ID, nowPlayingNotification.build());
	}
	
	private void cancelNowPlayNotification() {
		notificationManager.cancel(NOWPLAYING_NOTIFICATION_ID);
	}
}
