package org.kfjc.android.player.service;

import android.app.Notification;
import android.content.Context;

import org.kfjc.android.player.intent.PlayerControl;
import org.kfjc.android.player.model.KfjcMediaSource;
import org.kfjc.android.player.util.NotificationUtil;

import java.io.File;

public abstract class AbstractPlayback {

    protected KfjcMediaSource mediaSource;
    protected int activeSourceNumber = -1;
    protected NotificationUtil notificationUtil;
    protected Context context;

    public AbstractPlayback(Context context) {
        this.context = context;
    }

    public KfjcMediaSource getSource() {
        return mediaSource;
    }

    public void play(KfjcMediaSource mediaSource) {
        this.mediaSource = mediaSource;
        stop(false);
        if (mediaSource.type == KfjcMediaSource.Type.LIVESTREAM) {
            play(mediaSource.url, true);
        } else if (mediaSource.type == KfjcMediaSource.Type.ARCHIVE) {
            activeSourceNumber = -1;
            playArchiveHour(0);
        }
    }

    /**
     * @return true if a next hour was started.
     */
    protected boolean playNextArchiveHour() {
        if (mediaSource.show.getUrls().size() - 1 > activeSourceNumber) {
            playArchiveHour(activeSourceNumber + 1);
            seek(2 * mediaSource.show.getHourPaddingTimeMillis());
            return true;
        }
        return false;
    }

    protected void playArchiveHour(int hour) {
        if (activeSourceNumber == hour) {
            return;
        }
        activeSourceNumber = hour;
        File expectedSavedHour = mediaSource.show.getSavedHourUrl(hour);
        stop(false);
        if (expectedSavedHour.exists()) {
            play(expectedSavedHour.getPath(), false);
        } else {
            play(mediaSource.show.getUrls().get(hour), false);
        }
        seek(mediaSource.show.getHourPaddingTimeMillis());
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

    public Notification getNotification() {
        if (mediaSource.type == KfjcMediaSource.Type.LIVESTREAM) {
            return notificationUtil.kfjcStreamNotification(
                    context,
                    getSource(),
                    PlayerControl.INTENT_STOP,
                    true);
        } else if (mediaSource.type == KfjcMediaSource.Type.ARCHIVE) {
            return notificationUtil.kfjcStreamNotification(
                    context.getApplicationContext(),
                    getSource(),
                    PlayerControl.INTENT_PAUSE,
                    false);
        }
        // TODO: brittle, fixme
        return null;
    }

    public abstract void play(String streamUrl, boolean isLive);
    public abstract void stop(boolean alsoReset);
    public abstract void pause();
    public abstract void unpause();
    public abstract void seek(long positionMillis);
    public abstract boolean isPlaying();
    public abstract long getPlayerPosition();
    abstract boolean isPaused();
    abstract void destroy();

}
