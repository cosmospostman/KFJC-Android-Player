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
import android.util.Log;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import org.kfjc.android.player.Constants;
import org.kfjc.android.player.intent.PlayerControl;
import org.kfjc.android.player.intent.PlayerState;
import org.kfjc.android.player.model.KfjcMediaSource;
import org.kfjc.android.player.util.NotificationUtil;

public class ExoPlayback extends AbstractPlayback {

    private static final String TAG = ExoPlayback.class.getSimpleName();
    private ExoPlayer player;
    private boolean becomingNoisyReceiverRegistered = false;
    private static final IntentFilter becomingNoisyIntentFilter =
            new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);

    public ExoPlayback(Context context) {
        super(context);
    }

    public void play(String streamUrl, boolean isLive) {
        Log.i(TAG, "Playing stream locally: " + streamUrl);

        if (player == null) {
            // 1. Create a default TrackSelector
            TrackSelector trackSelector =
                    new DefaultTrackSelector();

            // 2. Create a default LoadControl
            LoadControl loadControl = new DefaultLoadControl();

            // 3. Create the player
            player = ExoPlayerFactory.newSimpleInstance(context, trackSelector, loadControl);

            player.addListener(exoEventListener);
        }
        requestAudioFocus();

        // Produces DataSource instances through which media data is loaded.
        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(context,
                Util.getUserAgent(context, Constants.USER_AGENT), null);
        // Produces Extractor instances for parsing the media data.
        ExtractorsFactory extractorsFactory = new DefaultExtractorsFactory();
        // This is the MediaSource representing the media to be played.
        MediaSource audioSource = new ExtractorMediaSource(Uri.parse(streamUrl),
                dataSourceFactory, extractorsFactory, null, null);

        player.prepare(audioSource);
        player.setPlayWhenReady(true);
    }

    private ExoPlayer.EventListener exoEventListener = new ExoPlayer.EventListener() {
        @Override
        public void onLoadingChanged(boolean isLoading) {}

        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int state) {
            switch (state) {
                case ExoPlayer.STATE_READY:
                    if (playWhenReady) {
                        PlayerState.send(context, PlayerState.State.PLAY, mediaSource);
                        context.registerReceiver(onAudioBecomingNoisyReceiver, becomingNoisyIntentFilter);
                    } else {
                        PlayerState.send(context, PlayerState.State.PAUSE, mediaSource);
                    }
                    break;
                case ExoPlayer.STATE_BUFFERING:
                    PlayerState.send(context, PlayerState.State.BUFFER, mediaSource);
                    break;
                case ExoPlayer.STATE_ENDED:
                    PlayerState.send(context, PlayerState.State.STOP, mediaSource);
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
        public void onTimelineChanged(Timeline timeline, Object manifest) {}

        @Override
        public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
            Log.i("KFJC StreamService", "exoplayer onTracksChanged");
        }

        @Override
        public void onPlayerError(ExoPlaybackException error) {
            Log.e(TAG, "ExoPlaybackException: " + error.getMessage());
            PlayerState.send(context, PlayerState.State.ERROR, error.getMessage());
        }

        @Override
        public void onPositionDiscontinuity() {}
    };

    public void seek(long positionMillis) {
        player.seekTo(positionMillis);
    }

    @Override
    public void stop(boolean alsoReset) {
        abandonAudioFocus();
        if (player != null) {
            player.stop();
            Log.i(TAG, "Player stopped");
        }
        if (alsoReset) {
            reset();
        }
    }

    @Override
    public void pause() {
        player.setPlayWhenReady(false);
        abandonAudioFocus();
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Service.NOTIFICATION_SERVICE);
        Notification n = NotificationUtil.kfjcStreamNotification(
                context,
                getSource(),
                PlayerControl.INTENT_UNPAUSE,
                false);
        notificationManager.notify(NotificationUtil.KFJC_NOTIFICATION_ID, n);
    }

    @Override
    public void unpause() {
        if (mediaSource.type == KfjcMediaSource.Type.LIVESTREAM) {
            play(mediaSource);
        }
        else if (isPaused()) {
            Log.i(TAG, "Unpausing");
            requestAudioFocus();
            player.setPlayWhenReady(true);
            NotificationManager notificationManager =
                    (NotificationManager) context.getSystemService(Service.NOTIFICATION_SERVICE);
            Notification n = NotificationUtil.kfjcStreamNotification(
                    context,
                    getSource(),
                    PlayerControl.INTENT_PAUSE,
                    false);
            notificationManager.notify(NotificationUtil.KFJC_NOTIFICATION_ID, n);
            return;
        }
    }

    @Override
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

    private void reset() {
        if (player != null) {
            player.release();
            player = null;
            PlayerState.send(context, PlayerState.State.STOP, mediaSource);
        }
        unregisterReceivers();
        becomingNoisyReceiverRegistered = false;
    }

    @Override
    public boolean isPlaying() {
        return player != null && (
                player.getPlayWhenReady() &&
                        player.getPlaybackState() == ExoPlayer.STATE_READY ||
                        player.getPlaybackState() == ExoPlayer.STATE_BUFFERING);
    }

    @Override
    boolean isPaused() {
        return player.getPlaybackState() == ExoPlayer.STATE_READY
                && !player.getPlayWhenReady();
    }

    @Override
    void destroy() {
        if (player != null) {
            player.stop();
            player.release();
        }
    }

    private void requestAudioFocus() {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        audioManager.requestAudioFocus(
                audioFocusListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN);
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
            AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            switch (focusChange) {
                case AudioManager.AUDIOFOCUS_LOSS:
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                    Log.i(TAG, "Lost audio focus");
                    volumeBeforeLoss = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                    if (mediaSource != null && mediaSource.type == KfjcMediaSource.Type.ARCHIVE) {
                        pause();
                    } else {
                        stop(false);
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
                        stop(false);
                        break;
                }
            }
        }
    };

    private void unregisterReceivers() {
        try {
            if (becomingNoisyReceiverRegistered) {
                context.unregisterReceiver(onAudioBecomingNoisyReceiver);
            }
        } catch (IllegalArgumentException e) {
            // receiver was already unregistered.
        }
    }

    private void abandonAudioFocus() {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        audioManager.abandonAudioFocus(audioFocusListener);
    }

}
