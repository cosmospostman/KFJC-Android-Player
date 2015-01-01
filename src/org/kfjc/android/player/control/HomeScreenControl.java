package org.kfjc.android.player.control;

import org.kfjc.android.player.NowPlayingInfo;
import org.kfjc.android.player.activity.HomeScreenActivity;
import org.kfjc.android.player.control.NowPlayingFetcher.NowPlayingHandler;
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
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

public class HomeScreenControl {
	
	private Intent playIntent;
	private LiveStreamService streamService;
	private ServiceConnection musicConnection;
	private HomeScreenActivity activity;
	private NowPlayingFetcher nowPlayingFetcher;
	private NotificationManager notificationManager;
	private NotificationCompat.Builder nowPlayingNotification;
	private static final int NOWPLAYING_NOTIFICATION_ID = 1;
	private IntentFilter intentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
	private boolean isMusicConnectionBound = false;
	
	public HomeScreenControl(HomeScreenActivity activity) {
		this.activity = activity;
		this.nowPlayingFetcher = new NowPlayingFetcher(nph);
		this.notificationManager =
				(NotificationManager) activity.getSystemService(Context.NOTIFICATION_SERVICE);
		this.nowPlayingNotification =  new NotificationCompat.Builder(activity);

		if (musicConnection == null) {
			musicConnection = new ServiceConnection() {
				@Override
				public void onServiceConnected(ComponentName name, IBinder service) {
					LiveStreamBinder binder = (LiveStreamBinder) service;
					streamService = binder.getService();
					streamService.setOnPlayListener(mediaEventHandler);
					nowPlayingFetcher.run();
					isMusicConnectionBound = true;
				}

				@Override
				public void onServiceDisconnected(ComponentName arg0) {
					isMusicConnectionBound = false;
				}
			};
		}
		if (playIntent == null) {
			playIntent = new Intent(activity, LiveStreamService.class);
			activity.bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE);
			activity.startService(playIntent);
		}		
	};
	
	NowPlayingHandler nph = new NowPlayingHandler() {
		@Override
		public void onTrackInfoFetched(NowPlayingInfo trackInfo) {
			activity.updateTrackInfo(trackInfo);
			updateNowPlayNotification(trackInfo);
		}
	};
	
	private MediaListener mediaEventHandler = new MediaListener() {
		@Override
		public void onPlay() {
			nowPlayingFetcher.runOnce();
			activity.onPlayerBufferComplete();
		}
		
		@Override
		public void onError() {
			stopStream();
		}
	};

	public void playStream() {
		streamService.play();
		activity.registerReceiver(onAudioBecomingNoisy, intentFilter);
		activity.onPlayerBuffer();
	}
	
	public void stopStream() {
		activity.onPlayerStop();
		streamService.stop();
		nowPlayingFetcher.stop();
	    unregisterBecomingNoisyReceiver();
		cancelNowPlayNotification();
	}
	
	public void destroy() {
		stopStream();
		activity.stopService(playIntent);
		if (isMusicConnectionBound) {
			activity.unbindService(musicConnection);
		}
		streamService = null;
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
}
