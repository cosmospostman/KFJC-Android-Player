package org.kfjc.android.player.control;

import org.kfjc.android.player.NowPlayingInfo;
import org.kfjc.android.player.activity.HomeScreenActivity;
import org.kfjc.android.player.control.LiveStreamService.LiveStreamBinder;
import org.kfjc.android.player.control.LiveStreamService.MediaListener;
import org.kfjc.android.player.control.NowPlayingFetcher.NowPlayingHandler;
import org.kfjc.android.player.util.UiUtil;
import org.kfjc.droid.R;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

public class HomeScreenControl {
	
	private Intent playIntent;
	private LiveStreamService streamService;
	private ServiceConnection musicConnection;
	private HomeScreenActivity activity;
	private NowPlayingFetcher nowPlayingFetcher;
	private NotificationManager notificationManager;
	private boolean isPlaying = false;
	private NotificationCompat.Builder nowPlayingNotification;
	private static final int NOWPLAYING_NOTIFICATION_ID = 1;

	
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
				}

				@Override
				public void onServiceDisconnected(ComponentName arg0) {}
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
			if (isPlaying) {
				updateNowPlayNotification(trackInfo);
			}
		}
	};
	
	private MediaListener mediaEventHandler = new MediaListener() {
		@Override
		public void onPlay() {
			isPlaying = true;
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
		activity.onPlayerBuffer();
	}
	
	public void stopStream() {
		streamService.stop();
		cancelNowPlayNotification();
		isPlaying = false;
		activity.onPlayerStop();
	}
	
	public void destroy() {
		activity.stopService(playIntent);
		streamService = null;
		//TODO: stop buffering timeouts, 
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

}
