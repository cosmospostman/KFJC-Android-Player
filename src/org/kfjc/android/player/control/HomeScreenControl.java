package org.kfjc.android.player.control;

import org.kfjc.android.player.NowPlayingInfo;
import org.kfjc.android.player.activity.HomeScreenActivity;
import org.kfjc.android.player.util.UiUtil;
import org.kfjc.droid.R;

import service.LiveStreamService;
import service.LiveStreamService.LiveStreamBinder;
import service.LiveStreamService.MediaListener;
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
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

public class HomeScreenControl {
	
	private Intent playIntent;
	private LiveStreamService streamService;
	private ServiceConnection musicConnection;
	private HomeScreenActivity activity;
	private NotificationManager notificationManager;
	private NotificationCompat.Builder nowPlayingNotification;
	private static final int NOWPLAYING_NOTIFICATION_ID = 1;
	private IntentFilter intentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
	AudioManager audioManager;
	private int musicStreamVolumeBeforeDuck;
	public boolean isPlaying = false;
	
	public HomeScreenControl(HomeScreenActivity activity) {
		this.activity = activity;
		this.notificationManager =
				(NotificationManager) activity.getSystemService(Context.NOTIFICATION_SERVICE);
		this.audioManager = (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);
		this.nowPlayingNotification =  new NotificationCompat.Builder(activity);

		if (musicConnection == null) {
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
		
		if (playIntent == null) {
			playIntent = new Intent(activity, LiveStreamService.class);
			activity.bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE);
			activity.startService(playIntent);
		}
	};
	
	private MediaListener mediaEventHandler = new MediaListener() {
		@Override
		public void onPlay() {
			streamService.runPlaylistFetcherOnce();
			activity.onPlayerBufferComplete();
		}
		
		@Override
		public void onError() {
			stopStream();
		}
		
		@Override
		public void onTrackInfoFetched(NowPlayingInfo trackInfo) {
			activity.updateTrackInfo(trackInfo);
			if (streamService.isPlaying()) {
				updateNowPlayNotification(trackInfo);
			}
		}
	};

	public void playStream() {
		audioManager.requestAudioFocus(afChangeListener,
				AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
		streamService.play();
		activity.registerReceiver(onAudioBecomingNoisy, intentFilter);
		activity.onPlayerBuffer();
		isPlaying = true;
	}
	
	public void stopStream() {
		activity.onPlayerStop();
		streamService.stop();
		audioManager.abandonAudioFocus(afChangeListener);
	    unregisterBecomingNoisyReceiver();
		cancelNowPlayNotification();
		isPlaying = false;
	}
	
	public void destroy() {
		stopStream();
		activity.stopService(playIntent);
		activity.unbindService(musicConnection);
		streamService = null;
		notificationManager.cancel(NOWPLAYING_NOTIFICATION_ID);
	}
	
	private void updateNowPlayNotification(NowPlayingInfo nowPlaying) {
		String artistTrackString = nowPlaying.getArtist() +
				" - " + nowPlaying.getTrackTitle();
		PendingIntent contentIntent = PendingIntent.getActivity(
				activity, 0, new Intent(activity, HomeScreenActivity.class),
				Notification.FLAG_ONGOING_EVENT);    
		nowPlayingNotification = new NotificationCompat.Builder(activity)
		    .setSmallIcon(R.drawable.ic_kfjc_notification)
		    .setContentTitle(UiUtil.getAppTitle(activity.getApplicationContext(), nowPlaying))
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
	
	private void unregisterBecomingNoisyReceiver() {
		try {
			activity.unregisterReceiver(onAudioBecomingNoisy);
		} catch (IllegalArgumentException e) {
			// receiver was already unregistered.
		}
	}
	
	private BroadcastReceiver onAudioBecomingNoisy = new BroadcastReceiver() {
	    @Override
	    public void onReceive(Context context, Intent intent) {
	        if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
	            stopStream();
	        }
	    }
	};
	
	private OnAudioFocusChangeListener afChangeListener = new OnAudioFocusChangeListener() {
	    public void onAudioFocusChange(int focusChange) {
	    	switch (focusChange) {
		    	case AudioManager.AUDIOFOCUS_LOSS:
		    		stopStream();
		            audioManager.abandonAudioFocus(afChangeListener);
		            break;
	    		case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
	    			musicStreamVolumeBeforeDuck = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
	    			audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, musicStreamVolumeBeforeDuck/2, 0);
	    			break;
	    		case AudioManager.AUDIOFOCUS_GAIN:
	    			audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, musicStreamVolumeBeforeDuck, 0);
	    			break;
	    	}
	    }
	};
}
