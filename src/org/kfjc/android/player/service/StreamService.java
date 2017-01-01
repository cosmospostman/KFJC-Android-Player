package org.kfjc.android.player.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
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
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.SessionManagerListener;
import com.google.android.gms.cast.framework.media.RemoteMediaClient;

import org.kfjc.android.player.Constants;
import org.kfjc.android.player.intent.PlayerControl;
import org.kfjc.android.player.intent.PlayerState;
import org.kfjc.android.player.model.KfjcMediaSource;
import org.kfjc.android.player.util.NotificationUtil;

import java.io.File;

public class StreamService extends Service {

    private static final String TAG = StreamService.class.getSimpleName();
    private static final IntentFilter becomingNoisyIntentFilter =
            new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);

    public enum PlaybackLocation {
        LOCAL,
        REMOTE
    }

    public class LiveStreamBinder extends Binder {
		public StreamService getService() {
			return StreamService.this;
		}
	}

    private KfjcMediaSource mediaSource;
    private PlayerState playerState = new PlayerState();
	private final IBinder liveStreamBinder = new LiveStreamBinder();
    private ExoPlayer player;
    private boolean becomingNoisyReceiverRegistered = false;
    private NotificationUtil notificationUtil;
    private int activeSourceNumber = -1;

    private CastContext mCastContext;
    private CastSession mCastSession;
    private SessionManagerListener<CastSession> mSessionManagerListener;

    private PlaybackLocation playbackLocation;

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
    public void onCreate() {
        Log.i("StreamService", "onCreate");
        super.onCreate();
        setupCastListener();
        mCastContext = CastContext.getSharedInstance(this);
        mCastSession = mCastContext.getSessionManager().getCurrentCastSession();
        mCastContext.getSessionManager().addSessionManagerListener(
                mSessionManagerListener, CastSession.class);
        if (mCastSession != null && mCastSession.isConnected()) {
            updatePlaybackLocation(PlaybackLocation.REMOTE);
        } else {
            updatePlaybackLocation(PlaybackLocation.LOCAL);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        notificationUtil = new NotificationUtil(this);

        if (intent != null) {
            if (PlayerControl.INTENT_STOP.equals(intent.getAction())) {
                stop();
            } else if (PlayerControl.INTENT_PAUSE.equals(intent.getAction())) {
                pause();
            } else if (PlayerControl.INTENT_UNPAUSE.equals(intent.getAction())) {
                unpause();
            } else if (PlayerControl.INTENT_PLAY.equals(intent.getAction())) {
                KfjcMediaSource source = intent.getParcelableExtra(PlayerControl.INTENT_SOURCE);
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
        //TODO: update for cast
        return player != null && (
               player.getPlayWhenReady() &&
               player.getPlaybackState() == ExoPlayer.STATE_READY ||
               player.getPlaybackState() == ExoPlayer.STATE_BUFFERING);
	}

    private boolean isPaused() {
        return player.getPlaybackState() == ExoPlayer.STATE_READY
                && !player.getPlayWhenReady();
    }

    private void play(KfjcMediaSource mediaSource) {
        this.mediaSource = mediaSource;
        stop();
        if (mediaSource.type == KfjcMediaSource.Type.LIVESTREAM) {
            Notification n = notificationUtil.kfjcStreamNotification(
                    getApplicationContext(),
                    getSource(),
                    PlayerControl.INTENT_STOP,
                    true);
            startForeground(NotificationUtil.KFJC_NOTIFICATION_ID, n);
            play(mediaSource.url);
        } else if (mediaSource.type == KfjcMediaSource.Type.ARCHIVE) {
            Notification n = notificationUtil.kfjcStreamNotification(
                    getApplicationContext(),
                    getSource(),
                    PlayerControl.INTENT_PAUSE,
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
        switch (playbackLocation) {
            case LOCAL:
                playLocal(streamUrl);
                break;
            case REMOTE:
                loadRemoteMedia(streamUrl, true);
        }
    }

    private void playLocal(String streamUrl) {
        Log.i(TAG, "Playing stream locally: " + streamUrl);
        if (player == null) {
            // 1. Create a default TrackSelector
            TrackSelector trackSelector =
                    new DefaultTrackSelector();

            // 2. Create a default LoadControl
            LoadControl loadControl = new DefaultLoadControl();

            // 3. Create the player
            player = ExoPlayerFactory.newSimpleInstance(
                    getApplicationContext(), trackSelector, loadControl);

            player.addListener(exoEventListener);
        }
        requestAudioFocus();

        // Produces DataSource instances through which media data is loaded.
        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(this,
                Util.getUserAgent(this, Constants.USER_AGENT), null);
        // Produces Extractor instances for parsing the media data.
        ExtractorsFactory extractorsFactory = new DefaultExtractorsFactory();
        // This is the MediaSource representing the media to be played.
        MediaSource audioSource = new ExtractorMediaSource(Uri.parse(streamUrl),
                dataSourceFactory, extractorsFactory, null, null);

        player.prepare(audioSource);
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
                PlayerControl.INTENT_UNPAUSE,
                false);
        notificationManager.notify(NotificationUtil.KFJC_NOTIFICATION_ID, n);
    }

    private void unpause() {
        if (mediaSource.type == KfjcMediaSource.Type.LIVESTREAM) {
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
                    PlayerControl.INTENT_PAUSE,
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
            playerState.send(getApplicationContext(), PlayerState.State.STOP, mediaSource);
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

    private ExoPlayer.EventListener exoEventListener = new ExoPlayer.EventListener() {
        @Override
        public void onLoadingChanged(boolean isLoading) {}

        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int state) {
            switch (state) {
                case ExoPlayer.STATE_READY:
                    if (playWhenReady) {
                        playerState.send(getApplicationContext(), PlayerState.State.PLAY, mediaSource);
                        registerReceiver(onAudioBecomingNoisyReceiver, becomingNoisyIntentFilter);
                    } else {
                        playerState.send(getApplicationContext(), PlayerState.State.PAUSE, mediaSource);
                    }
                    break;
                case ExoPlayer.STATE_BUFFERING:
                    playerState.send(getApplicationContext(), PlayerState.State.BUFFER, mediaSource);
                    break;
                case ExoPlayer.STATE_ENDED:
                    playerState.send(getApplicationContext(), PlayerState.State.STOP, mediaSource);
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
            playerState.send(getApplicationContext(), PlayerState.State.ERROR, error.getMessage());
        }

        @Override
        public void onPositionDiscontinuity() {}
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

    public KfjcMediaSource getSource() {
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
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                    Log.i(TAG, "Lost audio focus");
                    volumeBeforeLoss = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                    if (mediaSource != null && mediaSource.type == KfjcMediaSource.Type.ARCHIVE) {
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

    public void updatePlaybackLocation(PlaybackLocation location) {
        playbackLocation = location;
        switch (location) {
            case LOCAL:
                if (isPlaying()) {
                    // Stop chromecast staff
                    // setCoverArtStatus(null);
                    // startControllersTimer();
                } else {
                    // Start chromecast stuff
                    // stopControllersTimer();
                    // setCoverArtStatus(mSelectedMedia.getImage(0));
                }
                break;
            case REMOTE:
                // Stop local stream and start chromecast stuff
                // stopControllersTimer();
                // setCoverArtStatus(mSelectedMedia.getImage(0));
                // updateControllersVisibility(false);
                break;
        }
    }

    private void loadRemoteMedia(String streamUrl, boolean autoPlay) {
        Log.i(TAG, "Playing stream over chromecast: " + streamUrl);
        if (mCastSession == null) {
            return;
        }
        RemoteMediaClient remoteMediaClient = mCastSession.getRemoteMediaClient();
        if (remoteMediaClient == null) {
            return;
        }
        remoteMediaClient.load(buildMediaInfo(streamUrl), autoPlay);
    }

    private MediaInfo buildMediaInfo(String streamUrl) {
//        MediaMetadata movieMetadata = new MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE);
//
//        movieMetadata.putString(MediaMetadata.KEY_SUBTITLE, mSelectedMedia.getStudio());
//        movieMetadata.putString(MediaMetadata.KEY_TITLE, mSelectedMedia.getTitle());
//        movieMetadata.addImage(new WebImage(Uri.parse(mSelectedMedia.getImage(0))));
//        movieMetadata.addImage(new WebImage(Uri.parse(mSelectedMedia.getImage(1))));
        return new MediaInfo.Builder(streamUrl + ";")
                .setStreamType(MediaInfo.STREAM_TYPE_LIVE)
                .setContentType(getSource().getMimeType())
//                .setMetadata(movieMetadata)
                .setStreamDuration(10 * 1000)
                .build();
    }


    private void setupCastListener() {
        mSessionManagerListener = new SessionManagerListener<CastSession>() {

            @Override
            public void onSessionEnded(CastSession session, int error) {
                onApplicationDisconnected();
            }

            @Override
            public void onSessionResumed(CastSession session, boolean wasSuspended) {
                onApplicationConnected(session);
            }

            @Override
            public void onSessionResumeFailed(CastSession session, int error) {
                onApplicationDisconnected();
            }

            @Override
            public void onSessionStarted(CastSession session, String sessionId) {
                onApplicationConnected(session);
            }

            @Override
            public void onSessionStartFailed(CastSession session, int error) {
                onApplicationDisconnected();
            }

            @Override
            public void onSessionStarting(CastSession session) {}

            @Override
            public void onSessionEnding(CastSession session) {}

            @Override
            public void onSessionResuming(CastSession session, String sessionId) {}

            @Override
            public void onSessionSuspended(CastSession session, int reason) {}

            private void onApplicationConnected(CastSession castSession) {
                Log.i("kfjc-cast", "application connected");
                mCastSession = castSession;
                if (isPlaying()) {
                    // TODO
                    // mVideoView.pause();
                    // loadRemoteMedia(mSeekbar.getProgress(), true);
                    return;
                } else {
                    updatePlaybackLocation(PlaybackLocation.REMOTE);
                }
            }

            private void onApplicationDisconnected() {
                updatePlaybackLocation(PlaybackLocation.LOCAL);
            }
        };
    }
}
