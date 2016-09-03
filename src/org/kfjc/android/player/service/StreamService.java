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
import org.kfjc.android.player.intent.PlayerState;
import org.kfjc.android.player.model.MediaSource;
import org.kfjc.android.player.intent.PlayerControl;
import org.kfjc.android.player.util.NotificationUtil;

import java.io.File;

public class StreamService extends Service {

    private static final String TAG = StreamService.class.getSimpleName();
    private static final int BUFFER_SEGMENT_SIZE = 64 * 1024;
    private static final int BUFFER_SEGMENT_COUNT = 256;
    private static final IntentFilter becomingNoisyIntentFilter =
            new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);

    private static final int MIN_BUFFER_MS = 5000;
    private static final int MIN_REBUFFER_MS = 5000;

	public class LiveStreamBinder extends Binder {
		public StreamService getService() {
			return StreamService.this;
		}
	}

    private MediaSource mediaSource;
    private PlayerState playerState = new PlayerState();
	private final IBinder liveStreamBinder = new LiveStreamBinder();
    private ExoPlayer player;
    private boolean becomingNoisyReceiverRegistered = false;
    private NotificationUtil notificationUtil;
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

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        notificationUtil = new NotificationUtil(this);

        if (intent != null) {
                stop();
                pause();
                unpause();
                if (getSource() == null || !getSource().equals(source) || !isPlaying()) {
                    stop();
                    play(source);
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
        notificationUtil.cancelKfjcNotification();
        if (player != null) {
            player.stop();
            player.release();
        }
    }

    public boolean isPlaying() {
        return player != null && (
               player.getPlayWhenReady() &&
               player.getPlaybackState() == ExoPlayer.STATE_READY ||
               player.getPlaybackState() == ExoPlayer.STATE_BUFFERING);
	}

    private boolean isPaused() {
        return player.getPlaybackState() == ExoPlayer.STATE_READY
                && !player.getPlayWhenReady();
    }

    private void play(MediaSource mediaSource) {
        this.mediaSource = mediaSource;
        stop();
        if (mediaSource.type == MediaSource.Type.LIVESTREAM) {
            Notification n = notificationUtil.kfjcStreamNotification(
                    getApplicationContext(),
                    getSource(),
                    true);
            startForeground(NotificationUtil.KFJC_NOTIFICATION_ID, n);
            play(mediaSource.url);
        } else if (mediaSource.type == MediaSource.Type.ARCHIVE) {
            Notification n = notificationUtil.kfjcStreamNotification(
                    getApplicationContext(),
                    getSource(),
                    false);
            startForeground(NotificationUtil.KFJC_NOTIFICATION_ID, n);
            activeSourceNumber = -1;
            playArchiveHour(0);
        }
    }

    private void requestAudioFocus() {
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioManager.requestAudioFocus(
                audioFocusListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN);
    }

    private void abandonAudioFocus() {
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioManager.abandonAudioFocus(audioFocusListener);
    }

    private void play(String streamUrl) {
        Log.i(TAG, "Playing stream " + streamUrl);
        if (player == null) {
            player = ExoPlayer.Factory.newInstance(1, MIN_BUFFER_MS, MIN_REBUFFER_MS);
            player.addListener(exoPlayerListener);
        }
        requestAudioFocus();

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

    private void pause() {
        player.setPlayWhenReady(false);
        abandonAudioFocus();
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Service.NOTIFICATION_SERVICE);
        Notification n = NotificationUtil.kfjcStreamNotification(
                getApplicationContext(),
                getSource(),
                false);
        notificationManager.notify(NotificationUtil.KFJC_NOTIFICATION_ID, n);
    }

    private void unpause() {
        if (mediaSource.type == MediaSource.Type.LIVESTREAM) {
            play(mediaSource);
        }
        else if (isPaused()) {
            Log.i(TAG, "Unpausing");
            requestAudioFocus();
            player.setPlayWhenReady(true);
            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(Service.NOTIFICATION_SERVICE);
            Notification n = NotificationUtil.kfjcStreamNotification(
                    getApplicationContext(),
                    getSource(),
                    false);
            notificationManager.notify(NotificationUtil.KFJC_NOTIFICATION_ID, n);
            return;
        }
    }

    private void stop() {
        stop(true);
	}

    private void stop(boolean alsoReset) {
        abandonAudioFocus();
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
        }
        unregisterReceivers();
        becomingNoisyReceiverRegistered = false;
        stopForeground(true);
        Log.i(TAG, "Service stopped");
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
                        registerReceiver(onAudioBecomingNoisyReceiver, becomingNoisyIntentFilter);
                    } else {
                    }
                    break;
                case ExoPlayer.STATE_PREPARING:
                    becomingNoisyReceiverRegistered = true;
                    break;
                case ExoPlayer.STATE_BUFFERING:
                    if (!isPlaying()) {
                    }
                    break;
                case ExoPlayer.STATE_ENDED:
                    unregisterReceivers();
                    if (!playNextArchiveHour()) {
                        stop(true);
                    }
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
        }
    };

    /**
     * @return true if a next hour was started.
     */
    private boolean playNextArchiveHour() {
        if (mediaSource.show.getUrls().size() - 1 > activeSourceNumber) {
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
            if (seekToMillis <= segmentBounds[i]) {
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

    /**
     * Audio Focus changes when another app requests access to the audio output stream. It could be
     * total access (eg. another music player) or transient (eg. spoken directions from maps). In
     * the latter case, we dip the volume for the other app and raise it again when the other app is
     * done.
     */
    private AudioManager.OnAudioFocusChangeListener audioFocusListener = new AudioManager.OnAudioFocusChangeListener() {

        private int volumeBeforeLoss;
        private static final String AUDIOFOCUS_KEY =
                "org.kfjc.android.player.control_AUDIO_FOCUS_CHANGE_LISTENER";

        @Override public void onAudioFocusChange(int focusChange) {
            AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            switch (focusChange) {
                case AudioManager.AUDIOFOCUS_LOSS:
                    Log.i(TAG, "Lost audio focus");
                    volumeBeforeLoss = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                    if (mediaSource != null && mediaSource.type == MediaSource.Type.ARCHIVE) {
                        pause();
                    } else {
                        stop();
                    }
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    Log.i(TAG, "Ducking audio focus");
                    volumeBeforeLoss = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volumeBeforeLoss / 2, 0);
                    break;
                case AudioManager.AUDIOFOCUS_GAIN:
                    Log.i(TAG, "Gained audio focus");
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volumeBeforeLoss, 0);
                    break;
            }
        }

        // Objects are recreated after activity resume. Audio focus is requested and released
        // by reference to an OnAudioFocusChangeListener, and subsequently its toString()
        // method. By returning a constant string, we can consistently refer to the audio
        // focus we requested before the app was paused and resumed.
        @Override public String toString() {
            return AUDIOFOCUS_KEY;
        }
    };

}
