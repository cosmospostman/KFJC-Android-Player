package org.kfjc.android.player.service;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.exoplayer.ExoPlaybackException;
import com.google.android.exoplayer.ExoPlayer;
import com.google.android.exoplayer.MediaCodecAudioTrackRenderer;
import com.google.android.exoplayer.source.DefaultSampleSource;
import com.google.android.exoplayer.source.FrameworkSampleExtractor;

import org.kfjc.android.player.model.TrackInfo;
import org.kfjc.android.player.util.NotificationUtil;
import org.kfjc.droid.R;

// TODO: Stop playlist fetcher when not playing and in background.
public class LiveStreamService extends Service {

    private static final String TAG = LiveStreamService.class.getSimpleName();

    public interface MediaListener {
        void onBuffer();
        void onPlay();
        void onError(String message);
        void onEnd();
    }

	public class LiveStreamBinder extends Binder {
		public LiveStreamService getService() {
			return LiveStreamService.this;
		}
	}

	private MediaListener mediaListener;
	private final IBinder liveStreamBinder = new LiveStreamBinder();
    private ExoPlayer player;
	
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
        if (player != null) {
            player.stop();
            player.release();
        }
    }

    public boolean isPlaying() {
        return player != null && (
               player.getPlaybackState() == ExoPlayer.STATE_READY ||
               player.getPlaybackState() == ExoPlayer.STATE_BUFFERING);
	}
	
	public void setMediaEventListener(MediaListener listener) {
		this.mediaListener = listener;
	}
	
	public void play(Context context, String streamUrl) {
        initPlayer();

        Notification n = NotificationUtil.kfjcNotification(getApplicationContext(), "KFJC", "StreamService");
        startForeground(NotificationUtil.KFJC_NOTIFICATION_ID, n);

        Uri streamUri = Uri.parse(streamUrl);
        FrameworkSampleExtractor sampleExtractor = new FrameworkSampleExtractor(context, streamUri, null);
        DefaultSampleSource sampleSource = new DefaultSampleSource(sampleExtractor, 1);
        MediaCodecAudioTrackRenderer audioRenderer = new MediaCodecAudioTrackRenderer(
                sampleSource, null, true);
        player.prepare(audioRenderer);
        player.setPlayWhenReady(true);
        player.addListener(makeExoplayerListener());

    }

	public void stop() {
        if (player != null) {
            player.stop();
        }
        stopForeground(true);
        Log.i(TAG, "Service stopped");
	}

    private void initPlayer() {
        player = ExoPlayer.Factory.newInstance(1);
    }

    private ExoPlayer.Listener makeExoplayerListener() {
        return new ExoPlayer.Listener() {
            @Override
            public void onPlayerStateChanged(boolean playWhenReady, int state) {
                switch (state) {
                    case ExoPlayer.STATE_READY:
                        if (playWhenReady) {
                            mediaListener.onPlay();
                        }
                        break;
                    case ExoPlayer.STATE_PREPARING:
                        mediaListener.onBuffer();
                        break;
                    case ExoPlayer.STATE_ENDED:
                    case ExoPlayer.STATE_IDLE:
                        mediaListener.onEnd();
                }

            }

            @Override
            public void onPlayWhenReadyCommitted() {}

            @Override
            public void onPlayerError(ExoPlaybackException e) {
               mediaListener.onError(e.getMessage());
            }
        };
    }

}
