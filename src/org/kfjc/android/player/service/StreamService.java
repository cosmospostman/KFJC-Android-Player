package org.kfjc.android.player.service;

import android.app.Notification;
import android.app.NotificationManager;
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

import java.io.File;
import java.util.List;

public class StreamService extends Service {

//    public static final String INTENT_CONTROL = "controlIntent";
    public static final String INTENT_STOP = "action_stop";
    public static final String INTENT_PAUSE = "action_pause";
    public static final String INTENT_UNPAUSE = "action_unpause";

    private static final String TAG = StreamService.class.getSimpleName();
    private static final int BUFFER_SEGMENT_SIZE = 64 * 1024;
    private static final int BUFFER_SEGMENT_COUNT = 256;
    private static final IntentFilter becomingNoisyIntentFilter =
            new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
    private static final IntentFilter onControlIntentFilter;
    static {
        onControlIntentFilter = new IntentFilter();
        onControlIntentFilter.addAction(INTENT_STOP);
        onControlIntentFilter.addAction(INTENT_PAUSE);
        onControlIntentFilter.addAction(INTENT_UNPAUSE);

    }

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
    private boolean onControlReceiverRegistered = false;

    private int activeSourceNumber = -1;

    /**
     * The Becoming Noisy broadcast intent is sent when audio output hardware changes, perhaps
     * from headphones to internal speaker. In such cases, we stop the stream to avoid
     * embarrassment.
     */
    private BroadcastReceiver onAudioBecomingNoisyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
                switch (mediaSource.type) {
                    case ARCHIVE:
                        pause();
                        break;
                    case LIVESTREAM:
                        stop();
                        break;
                }
            }
        }
    };

    private BroadcastReceiver onControlReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (INTENT_STOP.equals(intent.getAction())) {
                stop();
            } else if (INTENT_PAUSE.equals(intent.getAction())) {
                pause();
            } else if (INTENT_UNPAUSE.equals(intent.getAction())) {
                unpause();
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
            return isPaused()
                    ? PlayerFragment.PlayerState.PAUSE
                    : PlayerFragment.PlayerState.PLAY;
        }
        return PlayerFragment.PlayerState.STOP;
    }

    public boolean isPlaying() {
        return player != null && (
               player.getPlaybackState() == ExoPlayer.STATE_READY ||
               player.getPlaybackState() == ExoPlayer.STATE_BUFFERING);
	}

    private boolean isPaused() {
        return player.getPlaybackState() == ExoPlayer.STATE_READY
                && !player.getPlayWhenReady();
    }

	public void setMediaEventListener(MediaListener listener) {
		this.mediaListener = listener;
	}

    public void play(MediaSource mediaSource) {
        this.mediaSource = mediaSource;
        stop();
        if (mediaSource.type == MediaSource.Type.LIVESTREAM) {
            Notification n = NotificationUtil.bufferingNotification(getApplicationContext());
            startForeground(NotificationUtil.KFJC_NOTIFICATION_ID, n);
            play(mediaSource.url);
        } else if (mediaSource.type == MediaSource.Type.ARCHIVE) {
            startForeground(NotificationUtil.KFJC_NOTIFICATION_ID, buildNotification(INTENT_PAUSE));
            activeSourceNumber = -1;
            playArchiveHour(0);
        }
    }

    private void play(String streamUrl) {
        Log.i(TAG, "Playing stream " + streamUrl);
        if (player == null) {
            player = ExoPlayer.Factory.newInstance(1, MIN_BUFFER_MS, MIN_REBUFFER_MS);
            player.addListener(exoPlayerListener);
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
                new DefaultUriDataSource(getApplicationContext(), Constants.USER_AGENT),
                new DefaultAllocator(BUFFER_SEGMENT_SIZE),
                BUFFER_SEGMENT_COUNT * BUFFER_SEGMENT_SIZE,
                5,
                extractor);

        MediaCodecAudioTrackRenderer audioRenderer = new MediaCodecAudioTrackRenderer(sampleSource,
                MediaCodecSelector.DEFAULT);

        player.prepare(audioRenderer);
        player.setPlayWhenReady(true);
    }

    private Notification buildNotification(String action) {
        return NotificationUtil.kfjcNotification(
                this, mediaSource.show.getAirName(), mediaSource.show.getTimestampString(), action);
    }

    public void pause() {
        player.setPlayWhenReady(false);
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Service.NOTIFICATION_SERVICE);
        notificationManager.notify(NotificationUtil.KFJC_NOTIFICATION_ID, buildNotification(INTENT_UNPAUSE));
    }

    public void unpause() {
        if (isPaused()) {
            Log.i(TAG, "Unpausing");
            player.setPlayWhenReady(true);
            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(Service.NOTIFICATION_SERVICE);
            notificationManager.notify(NotificationUtil.KFJC_NOTIFICATION_ID, buildNotification(INTENT_PAUSE));
            return;
        }
    }

    public void stop() {
        stop(true);
	}

    private void stop(boolean alsoReset) {
        if (player != null) {
            player.stop();
            Log.i(TAG, "Player stopped");
        }
        if (alsoReset) {
            reset();
        }
    }

    private void reset() {
        if (player != null) {
            player.release();
            player = null;
            mediaListener.onStateChange(PlayerFragment.PlayerState.STOP, mediaSource);
        }
        unregisterReceivers();
        becomingNoisyReceiverRegistered = false;
        onControlReceiverRegistered = false;
        stopForeground(true);
        Log.i(TAG, "Service stopped");
    }

    public void reload(MediaSource mediaSource) {
        if (player != null) {
            player.stop();
        }
        play(mediaSource);
    }

    private void unregisterReceivers() {
        try {
            if (becomingNoisyReceiverRegistered) {
                unregisterReceiver(onAudioBecomingNoisyReceiver);
            }
        } catch (IllegalArgumentException e) {
            // receiver was already unregistered.
        }
        try {
            if (onControlReceiverRegistered) {
                unregisterReceiver(onControlReceiver);
            }
        } catch (IllegalArgumentException e) {}
    }

    private ExoPlayer.Listener exoPlayerListener = new ExoPlayer.Listener() {
        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int state) {
            switch (state) {
                case ExoPlayer.STATE_READY:
                    if (playWhenReady) {
                        mediaListener.onStateChange(PlayerFragment.PlayerState.PLAY, mediaSource);
                        registerReceiver(onAudioBecomingNoisyReceiver, becomingNoisyIntentFilter);
                        registerReceiver(onControlReceiver, onControlIntentFilter);
                        becomingNoisyReceiverRegistered = true;
                        onControlReceiverRegistered = true;
                    } else {
                         mediaListener.onStateChange(PlayerFragment.PlayerState.PAUSE, mediaSource);
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
                    playNextArchiveHour();
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

    /**
     * @return true if a next hour was started.
     */
    private boolean playNextArchiveHour() {
        if (mediaSource.show.getUrls().size() > activeSourceNumber) {
            playArchiveHour(activeSourceNumber + 1);
            seek(2 * mediaSource.show.getHourPaddingTimeMillis());
            return true;
        }
        return false;
    }

    public long getPlayerPosition() {
        if (mediaSource.show == null || player == null) {
            return 0;
        }
        long segmentOffset = (activeSourceNumber == 0)
                ? 0 : mediaSource.show.getSegmentBounds()[activeSourceNumber - 1];
        long extra = (activeSourceNumber == 0)
                ? 0 : mediaSource.show.getHourPaddingTimeMillis();
        return player.getCurrentPosition() + segmentOffset - extra;
    }

    public void seek(long positionMillis) {
        player.seekTo(positionMillis);
    }

    public MediaSource getSource() {
        return mediaSource;
    }

    public void seekOverEntireShow(long seekToMillis) {
        long[] segmentBounds = mediaSource.show.getSegmentBounds();
        for (int i = 0; i < segmentBounds.length; i++) {
            if (seekToMillis < segmentBounds[i]) {
                // load segment i
                playArchiveHour(i);
                //seek to adjusted position
                long thisSegmentStart = (i == 0) ? 0 : segmentBounds[i-1];
                long extraSeek = (i == 0) ? 0 : mediaSource.show.getHourPaddingTimeMillis();
                long localSeekTo = seekToMillis - thisSegmentStart + extraSeek;
                seek(localSeekTo);
                return;
            }
        }
    }

    private void playArchiveHour(int hour) {
        if (activeSourceNumber == hour) {
            return;
        }
        activeSourceNumber = hour;
        File expectedSavedHour = mediaSource.show.getSavedHourUrl(hour);
        stop(false);
        if (expectedSavedHour.exists()) {
            play(expectedSavedHour.getPath());
        } else {
            play(mediaSource.show.getUrls().get(hour));
        }
        seek(mediaSource.show.getHourPaddingTimeMillis());
    }
}
