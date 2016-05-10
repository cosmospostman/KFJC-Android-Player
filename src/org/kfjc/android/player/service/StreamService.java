package org.kfjc.android.player.service;

import android.app.Notification;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.google.android.exoplayer.ExoPlaybackException;
import com.google.android.exoplayer.ExoPlayer;
import com.google.android.exoplayer.MediaCodecAudioTrackRenderer;
import com.google.android.exoplayer.MediaCodecSelector;
import com.google.android.exoplayer.extractor.Extractor;
import com.google.android.exoplayer.extractor.ExtractorSampleSource;
import com.google.android.exoplayer.extractor.mp3.Mp3Extractor;
import com.google.android.exoplayer.extractor.ts.AdtsExtractor;
import com.google.android.exoplayer.upstream.DefaultAllocator;
import com.google.android.exoplayer.upstream.DefaultUriDataSource;

import org.kfjc.android.player.Constants;
import org.kfjc.android.player.fragment.PlayerFragment;
import org.kfjc.android.player.model.MediaSource;
import org.kfjc.android.player.util.NotificationUtil;

public class StreamService extends Service {

    private static final String TAG = StreamService.class.getSimpleName();
    private static final int BUFFER_SEGMENT_SIZE = 64 * 1024;
    private static final int BUFFER_SEGMENT_COUNT = 256;
    private static final IntentFilter becomingNoisyIntentFilter =
            new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);

    private static final int MIN_BUFFER_MS = 5000;
    private static final int MIN_REBUFFER_MS = 5000;

    public interface MediaListener {
        void onStateChange(PlayerFragment.PlayerState state, MediaSource source);
        void onError(String message);
    }

	public class LiveStreamBinder extends Binder {
		public StreamService getService() {
			return StreamService.this;
		}
	}

    private MediaSource mediaSource;
	private MediaListener mediaListener;
	private final IBinder liveStreamBinder = new LiveStreamBinder();
    private ExoPlayer player;
    private boolean becomingNoisyReceiverRegistered = false;

    /**
     * The Becoming Noisy broadcast intent is sent when audio output hardware changes, perhaps
     * from headphones to internal speaker. In such cases, we stop the stream to avoid
     * embarrassment.
     */
    private BroadcastReceiver onAudioBecomingNoisyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
                stop();
            }
        }
    };
	
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

    public PlayerFragment.PlayerState getPlayerState() {
        if (player == null) {
            return PlayerFragment.PlayerState.STOP;
        }
        if (player.getPlaybackState() == ExoPlayer.STATE_BUFFERING) {
            return PlayerFragment.PlayerState.BUFFER;
        }
        if (player.getPlaybackState() == ExoPlayer.STATE_READY) {
            return PlayerFragment.PlayerState.PLAY;
        }
        return PlayerFragment.PlayerState.STOP;
    }

    public boolean isPlaying() {
        return player != null && (
               player.getPlaybackState() == ExoPlayer.STATE_READY ||
               player.getPlaybackState() == ExoPlayer.STATE_BUFFERING);
	}

	public void setMediaEventListener(MediaListener listener) {
		this.mediaListener = listener;
	}

    public void play(Context context, MediaSource mediaSource) {
        this.mediaSource = mediaSource;
        String streamUrl = mediaSource.url;
        Log.i(TAG, "Playing stream " + streamUrl);
        player = ExoPlayer.Factory.newInstance(1, MIN_BUFFER_MS, MIN_REBUFFER_MS);
        player.addListener(exoPlayerListener);

        if (mediaSource.type == MediaSource.Type.LIVESTREAM) {
            Notification n = NotificationUtil.bufferingNotification(context);
            startForeground(NotificationUtil.KFJC_NOTIFICATION_ID, n);
        } else if (mediaSource.type == MediaSource.Type.ARCHIVE) {
            Notification n = NotificationUtil.kfjcNotification(
                    context, mediaSource.name, mediaSource.description);
            startForeground(NotificationUtil.KFJC_NOTIFICATION_ID, n);
        }

        Extractor extractor = null;
        switch (mediaSource.format) {
            case AAC:
                extractor = new AdtsExtractor();
                break;
            case MP3:
                extractor = new Mp3Extractor();
                break;
        }
        ExtractorSampleSource sampleSource = new ExtractorSampleSource(
                Uri.parse(streamUrl),
                new DefaultUriDataSource(context, Constants.USER_AGENT),
                new DefaultAllocator(BUFFER_SEGMENT_SIZE),
                BUFFER_SEGMENT_COUNT * BUFFER_SEGMENT_SIZE,
                5,
                extractor);

        MediaCodecAudioTrackRenderer audioRenderer = new MediaCodecAudioTrackRenderer(sampleSource,
                MediaCodecSelector.DEFAULT);

        player.prepare(audioRenderer);
        player.setPlayWhenReady(true);
    }

	public void stop() {
        if (player != null) {
            player.stop();
            mediaListener.onStateChange(PlayerFragment.PlayerState.STOP, mediaSource);
        }
        unregisterReceivers();
        becomingNoisyReceiverRegistered = false;
        stopForeground(true);
        Log.i(TAG, "Service stopped");
	}

    public void reload(Context context, MediaSource mediaSource) {
        if (player != null) {
            player.stop();
        }
        play(context, mediaSource);
    }

    private void unregisterReceivers() {
        try {
            if (becomingNoisyReceiverRegistered) {
                unregisterReceiver(onAudioBecomingNoisyReceiver);
            }
        } catch (IllegalArgumentException e) {
            // receiver was already unregistered.
        }
    }

    private ExoPlayer.Listener exoPlayerListener = new ExoPlayer.Listener() {
        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int state) {
            switch (state) {
                case ExoPlayer.STATE_READY:
                    if (playWhenReady) {
                        mediaListener.onStateChange(PlayerFragment.PlayerState.PLAY, mediaSource);
                        registerReceiver(onAudioBecomingNoisyReceiver, becomingNoisyIntentFilter);
                        becomingNoisyReceiverRegistered = true;
                    }
                    break;
                case ExoPlayer.STATE_PREPARING:
                    mediaListener.onStateChange(PlayerFragment.PlayerState.BUFFER, mediaSource);
                    break;
                case ExoPlayer.STATE_BUFFERING:
                    if (!isPlaying()) {
                        mediaListener.onStateChange(PlayerFragment.PlayerState.BUFFER, mediaSource);
                    }
                    break;
                case ExoPlayer.STATE_ENDED:
                    mediaListener.onStateChange(PlayerFragment.PlayerState.STOP, mediaSource);
                    unregisterReceivers();
                    break;
                case ExoPlayer.STATE_IDLE:
                    break;
            }
        }

        @Override
        public void onPlayWhenReadyCommitted() {}

        @Override
        public void onPlayerError(ExoPlaybackException e) {
            Log.e(TAG, "ExoPlaybackException: " + e.getMessage());
            mediaListener.onError(e.getMessage());
        }
    };

    public long getPlayerPosition() {
        return player.getCurrentPosition();
    }

    public long getPlayerDuration() {
        return player.getDuration();
    }

    public void seekPlayer(long positionMillis) {
        player.seekTo(positionMillis);
    }

    public MediaSource getSource() {
        return mediaSource;
    }

}
