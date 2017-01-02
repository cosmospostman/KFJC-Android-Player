package org.kfjc.android.player.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.google.android.gms.cast.framework.CastSession;

import org.kfjc.android.player.intent.PlayerControl;
import org.kfjc.android.player.model.KfjcMediaSource;
import org.kfjc.android.player.util.NotificationUtil;

public class StreamService extends Service {

    private static final String TAG = StreamService.class.getSimpleName();

    public class LiveStreamBinder extends Binder {
		public StreamService getService() {
			return StreamService.this;
		}
	}

	private final IBinder liveStreamBinder = new LiveStreamBinder();

    private AbstractPlayback playback;

    public void setLocalPlayback() {
        playback = new ExoPlayback(getApplicationContext());
    }

    public void setCastPlayback(CastSession castSession) {
        playback = new ChromecastPlayback(getApplicationContext(), castSession);
    }

    public void setCastPlayback() {
        playback = new ChromecastPlayback(getApplicationContext());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            if (PlayerControl.INTENT_STOP.equals(intent.getAction())) {
                stop(true);
            } else if (PlayerControl.INTENT_PAUSE.equals(intent.getAction())) {
                playback.pause();
            } else if (PlayerControl.INTENT_UNPAUSE.equals(intent.getAction())) {
                playback.unpause();
            } else if (PlayerControl.INTENT_PLAY.equals(intent.getAction())) {
                KfjcMediaSource source = intent.getParcelableExtra(PlayerControl.INTENT_SOURCE);
                if (playback.getSource() == null || !playback.getSource().equals(source) || !isPlaying()) {
                    stop(true);
                    playback.play(source);
                    startForeground(NotificationUtil.KFJC_NOTIFICATION_ID, playback.getNotification());
                }
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
	public IBinder onBind(Intent intent) {
        return liveStreamBinder;
	}

	@Override
	public boolean onUnbind(Intent intent) {
		return false;
	}

    @Override
    public void onDestroy() {
        super.onDestroy();
        NotificationUtil.cancelKfjcNotification();
        playback.destroy();
    }

    private void stop(boolean alsoReset) {
        playback.stop(alsoReset);
        if (alsoReset) {
            stopForeground(true);
        }
    }

    public boolean isPlaying() {
        return playback.isPlaying();
	}

    public KfjcMediaSource getSource() {
        return playback.getSource();
    }

    public void seekOverEntireShow(long seekToMillis) {
        playback.seekOverEntireShow(seekToMillis);
    }

    public long getPlayerPosition() {
        return playback.getPlayerPosition();
    }

}
