package org.kfjc.android.player.control;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.os.AsyncTask;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

import org.kfjc.android.player.model.TrackInfo;
import org.kfjc.android.player.dialog.SettingsDialog;
import org.kfjc.android.player.dialog.SettingsDialog.StreamUrlPreferenceChangeHandler;
import org.kfjc.android.player.activity.HomeScreenActivity;
import org.kfjc.android.player.activity.HomeScreenActivity.StatusState;
import org.kfjc.android.player.service.LiveStreamService;
import org.kfjc.android.player.service.LiveStreamService.LiveStreamBinder;
import org.kfjc.android.player.service.LiveStreamService.MediaListener;
import org.kfjc.droid.R;

public class HomeScreenControl {
	
	public static PreferenceControl preferenceControl;
	private static final int NOWPLAYING_NOTIFICATION_ID = 1;
    private static final IntentFilter becomingNoisyIntentFilter =
			new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
		
	private Intent playIntent;
	private LiveStreamService streamService;
	private ServiceConnection musicConnection;
	private HomeScreenActivity activity;
	private NotificationManager notificationManager;
	private PendingIntent kfjcPlayerIntent;
	private AudioManager audioManager;
    private TelephonyManager telephonyManager;

    private OnAudioFocusChangeListener audioFocusListener;
	private BroadcastReceiver audioBecomingNoisyReceiver;
	private StreamUrlPreferenceChangeHandler streamUrlPreferenceChangeListener;
    private PhoneStateListener phoneStateListener;
	
	public HomeScreenControl(HomeScreenActivity activity) {
        loadAvailableStreams(activity);

        this.activity = activity;
		this.notificationManager =
				(NotificationManager) activity.getSystemService(Context.NOTIFICATION_SERVICE);
		this.audioManager =
                (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);
        this.telephonyManager =
                (TelephonyManager) activity.getSystemService(Context.TELEPHONY_SERVICE);
        this.audioFocusListener = EventHandlerFactory.onAudioFocusChange(this, audioManager);
		this.audioBecomingNoisyReceiver = EventHandlerFactory.onAudioBecomingNoisy(this);
        this.phoneStateListener = EventHandlerFactory.onPhoneStateChange(this);
		this.streamUrlPreferenceChangeListener =
				EventHandlerFactory.onUrlPreferenceChange(this, activity);

        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);

        kfjcPlayerIntent = PendingIntent.getActivity(
				activity, 0,
				new Intent(activity, HomeScreenActivity.class),
				Notification.FLAG_ONGOING_EVENT); 

		if (musicConnection == null) {
			createMusicConnection();
		}
		
		if (playIntent == null) {
			bindMusicConnection();
		}
	}

    private void loadAvailableStreams(final HomeScreenActivity activity) {
        new AsyncTask<Void, Void, Void>() {
            @Override protected Void doInBackground(Void... unsedParams) {
                HomeScreenControl.preferenceControl =
                        new PreferenceControl(activity.getApplicationContext());
                return null;
            }

            @Override protected void onPostExecute(Void aVoid) {
                onStreamUrlsLoaded();
            }
        }.execute();
    }

    public void onStreamUrlsLoaded() {
        activity.updateStreamNickname(PreferenceControl.getStreamNamePreference());
        activity.enablePlayStopButton();
    }
	
	private void createMusicConnection() {
		musicConnection = new ServiceConnection() {
			@Override
			public void onServiceConnected(ComponentName name, IBinder service) {
				LiveStreamBinder binder = (LiveStreamBinder) service;
				streamService = binder.getService();
				streamService.setMediaEventListener(mediaEventHandler);
				streamService.runPlaylistFetcher();
			}

			@Override
			public void onServiceDisconnected(ComponentName arg0) {
			}
		};
	}
	
	private void bindMusicConnection() {
		playIntent = new Intent(activity, LiveStreamService.class);
		activity.bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE);
		activity.startService(playIntent);
	}
	
	private MediaListener mediaEventHandler = new MediaListener() {
		@Override
		public void onPlay() {
			streamService.runPlaylistFetcherOnce();
			activity.onPlayerBufferComplete();
		}
		
		@Override
		public void onError() {
			stopStream();
            activity.setStatusState(HomeScreenActivity.StatusState.CONNECTION_ERROR);
        }
		
		@Override
		public void onTrackInfoFetched(TrackInfo trackInfo) {
			activity.updateTrackInfo(trackInfo);
			if (streamService.isPlaying()) {
				updateNowPlayNotification(trackInfo);
			}
		}
	};

	public void playStream() {
		audioManager.requestAudioFocus(
				audioFocusListener,
				AudioManager.STREAM_MUSIC,
				AudioManager.AUDIOFOCUS_GAIN);
		streamService.play(PreferenceControl.getUrlPreference());
		activity.registerReceiver(audioBecomingNoisyReceiver, becomingNoisyIntentFilter);
        postBufferNotification();
		activity.onPlayerBuffer();
        activity.setStatusState(StatusState.CONNECTING);
	}
	
	public void stopStream() {
		activity.onPlayerStop();
		streamService.stop();
		audioManager.abandonAudioFocus(audioFocusListener);
        EventHandlerFactory.unregisterReceiver(activity, audioBecomingNoisyReceiver);
		cancelNowPlayNotification();
	}
	
	public void destroy() {
		stopStream();
		activity.stopService(playIntent);
		activity.unbindService(musicConnection);
		streamService = null;
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
        notificationManager.cancel(NOWPLAYING_NOTIFICATION_ID);
	}
	
	public void showSettings() {
		SettingsDialog settingsFragment = new SettingsDialog();
		settingsFragment.setUrlPreferenceChangeHandler(streamUrlPreferenceChangeListener);
		settingsFragment.show(activity.getFragmentManager(), "settings");
	}

    private void postBufferNotification() {
        postNotification(
                "KFJC",
                "Buffering stream: " + PreferenceControl.getStreamNamePreference());
    }
	
	private void updateNowPlayNotification(TrackInfo nowPlaying) {
        String artistTrackString = nowPlaying.getArtist() +
				" - " + nowPlaying.getTrackTitle();
        postNotification(nowPlaying.getDjName(), artistTrackString);
	}

    private void postNotification(String title, String text) {
        Notification notification = new NotificationCompat.Builder(activity)
                .setSmallIcon(R.drawable.ic_kfjc_notification)
                .setContentTitle(title)
                .setContentText(text)
                .setOngoing(true)
                .setWhen(0)
                .setContentIntent(kfjcPlayerIntent)
                .build();
        notificationManager.notify(NOWPLAYING_NOTIFICATION_ID, notification);
    }
	
	private void cancelNowPlayNotification() {
		notificationManager.cancel(NOWPLAYING_NOTIFICATION_ID);
	}
	
	public boolean isStreamServicePlaying() {
		return streamService.isPlaying();
	}

}
