package org.kfjc.android.player.service;

import android.app.Notification;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaCodec;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.google.android.exoplayer.ExoPlaybackException;
import com.google.android.exoplayer.ExoPlayer;
import com.google.android.exoplayer.MediaCodecAudioTrackRenderer;
import com.google.android.exoplayer.MediaCodecTrackRenderer;
import com.google.android.exoplayer.audio.AudioTrack;
import com.google.android.exoplayer.extractor.Extractor;
import com.google.android.exoplayer.extractor.ExtractorSampleSource;
import com.google.android.exoplayer.extractor.mp3.Mp3Extractor;
import com.google.android.exoplayer.extractor.ts.AdtsExtractor;
import com.google.android.exoplayer.upstream.Allocator;
import com.google.android.exoplayer.upstream.DataSource;
import com.google.android.exoplayer.upstream.DefaultAllocator;
import com.google.android.exoplayer.upstream.DefaultUriDataSource;

import org.kfjc.android.player.activity.HomeScreenActivity;
import org.kfjc.android.player.util.NotificationUtil;

// TODO: Stop playlist fetcher when not playing and in background.
public class StreamService extends Service {

    private static final String TAG = StreamService.class.getSimpleName();
    private static final int BUFFER_SEGMENT_SIZE = 64 * 1024;
    private static final int BUFFER_SEGMENT_COUNT = 160;
    private static final IntentFilter becomingNoisyIntentFilter =
            new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);

    private static final int MIN_BUFFER_MS = 5000;
    private static final int MIN_REBUFFER_MS = 2000;

    public interface MediaListener {
        void onBuffer();
        void onPlay();
        void onError(String message);
        void onEnd();
    }

	public class LiveStreamBinder extends Binder {
		public StreamService getService() {
			return StreamService.this;
		}
	}

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

    public HomeScreenActivity.PlayerState getPlayerState() {
        if (player == null) {
            return HomeScreenActivity.PlayerState.STOP;
        }
        if (player.getPlaybackState() == ExoPlayer.STATE_BUFFERING) {
            return HomeScreenActivity.PlayerState.BUFFER;
        }
        if (player.getPlaybackState() == ExoPlayer.STATE_READY) {
            return HomeScreenActivity.PlayerState.PLAY;
        }
        return HomeScreenActivity.PlayerState.STOP;
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
        player = ExoPlayer.Factory.newInstance(1, MIN_BUFFER_MS, MIN_REBUFFER_MS);

        Notification n = NotificationUtil.bufferingNotification(context);
        startForeground(NotificationUtil.KFJC_NOTIFICATION_ID, n);

        Uri streamUri = Uri.parse(streamUrl);
        Allocator allocator = new DefaultAllocator(BUFFER_SEGMENT_SIZE * 128);
        DataSource dataSource = new DefaultUriDataSource(context, "kfjc4droid");
        Extractor extractor = new Mp3Extractor();
        if (streamUrl.toLowerCase().contains("aac")) {
            extractor = new AdtsExtractor();
        }
        ExtractorSampleSource sampleSource = new ExtractorSampleSource(
                streamUri,
                dataSource,
                extractor,
                allocator,
                BUFFER_SEGMENT_COUNT * BUFFER_SEGMENT_SIZE * 128);

        MediaCodecAudioTrackRenderer audioRenderer = new MediaCodecAudioTrackRenderer(sampleSource,
                null, true, new Handler(), eventListener);

        player.prepare(audioRenderer);
        player.setPlayWhenReady(true);
        player.addListener(exoPlayerListener);

    }

	public void stop() {
        if (player != null) {
            player.stop();
        }
        unregisterRecievers();
        becomingNoisyReceiverRegistered = false;
        stopForeground(true);
        Log.i(TAG, "Service stopped");
	}

    private void unregisterRecievers() {
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
                        mediaListener.onPlay();
                        registerReceiver(onAudioBecomingNoisyReceiver, becomingNoisyIntentFilter);
                        becomingNoisyReceiverRegistered = true;
                    }
                    break;
                case ExoPlayer.STATE_PREPARING:
                    mediaListener.onBuffer();
                    break;
                case ExoPlayer.STATE_ENDED:
                case ExoPlayer.STATE_IDLE:
                    mediaListener.onEnd();
                    unregisterRecievers();
                    break;
            }
        }

        @Override
        public void onPlayWhenReadyCommitted() {}

        @Override
        public void onPlayerError(ExoPlaybackException e) {
           mediaListener.onError(e.getMessage());
        }
    };

    MediaCodecAudioTrackRenderer.EventListener eventListener =
            new MediaCodecAudioTrackRenderer.EventListener() {
        @Override
        public void onDecoderInitializationError(
                MediaCodecTrackRenderer.DecoderInitializationException e) {}

        @Override
        public void onCryptoError(MediaCodec.CryptoException e) {}

        @Override
        public void onDecoderInitialized(
                String decoderName, long elapsedRealtimeMs, long initializationDurationMs) {}

        @Override
        public void onAudioTrackInitializationError(AudioTrack.InitializationException e) {}

        @Override
        public void onAudioTrackWriteError(AudioTrack.WriteException e) {}
    };


}
