package org.kfjc.android.player.service;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.MediaStatus;
import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.Session;
import com.google.android.gms.cast.framework.SessionManagerListener;
import com.google.android.gms.cast.framework.media.RemoteMediaClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.images.WebImage;

import org.kfjc.android.player.intent.PlayerState;
import org.kfjc.android.player.model.KfjcMediaSource;

public class ChromecastPlayback extends AbstractPlayback {

    private static final String TAG = ChromecastPlayback.class.getSimpleName();

    public enum CastPlaybackState {
        PLAYING, PAUSED, BUFFERING, IDLE
    }

    public interface CastConnectionChangedCallback {
        void onApplicationConnected();
        void onApplicationDisconnected();
    }

    CastContext mCastContext;
    private CastSession mCastSession;
    private SessionManagerListener<CastSession> mSessionManagerListener;

    public ChromecastPlayback(Context context, CastContext castContext) {
        super(context);
        mCastContext = castContext;
        mCastSession = mCastContext.getSessionManager().getCurrentCastSession();
    }

    @Override
    public void seek(long positionMillis) {
        mCastSession.getRemoteMediaClient().seek(positionMillis);
    }

    @Override
    public void play(String streamUrl, boolean isLive) {
        if (mCastSession == null) {
            return;
        }
        RemoteMediaClient remoteMediaClient = mCastSession.getRemoteMediaClient();
        if (remoteMediaClient == null) {
            return;
        }
        Log.i("RMC", remoteMediaClient.toString());
        remoteMediaClient.load(buildMediaInfo(streamUrl, isLive), true);
        PlayerState.send(context, PlayerState.State.PLAY, mediaSource);
    }

    @Override
    public void stop(boolean alsoReset) {
        RemoteMediaClient remoteMediaClient = mCastSession.getRemoteMediaClient();
        Log.i("RMC", remoteMediaClient.toString());
        remoteMediaClient.stop();
        PlayerState.send(context, PlayerState.State.STOP, mediaSource);
    }

    @Override
    public boolean isPlaying() {
        return false;
    }

    @Override
    boolean isPaused() {
        return false;
    }

    @Override
    void destroy() {

    }

    @Override
    public long getPlayerPosition() {
        return 0;
    }

    private MediaInfo buildMediaInfo(String streamUrl, boolean isLive) {
        Log.i(TAG, "Playing stream over chromecast: " + streamUrl + " MIMETYPE: " + getSource().getMimeType());
        MediaMetadata metadata = new MediaMetadata(MediaMetadata.MEDIA_TYPE_GENERIC);
        metadata.putString(MediaMetadata.KEY_TITLE, "KFJC Live Stream #TODO");
        metadata.addImage(new WebImage(Uri.parse("http://4.bp.blogspot.com/_YsgOkAVMc0A/SobPzt5dHwI/AAAAAAAABAE/Ua5fDHXXPYg/s320/kfjc.jpg")));
        return new MediaInfo.Builder(streamUrl + (isLive ? ";" : ""))
                .setStreamType(isLive ? MediaInfo.STREAM_TYPE_LIVE : MediaInfo.STREAM_TYPE_NONE)
                .setContentType(getSource().getMimeType())
                .setMetadata(metadata)
                .build();
    }

    public boolean isConnected() {
        return mCastSession != null && mCastSession.isConnected();
    }

    @Override
    public void pause() {

    }

    @Override
    public void unpause() {

    }

    public void setupCastListener(final CastConnectionChangedCallback callback) {
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
            public void onSessionStarting(CastSession session) {
            }

            @Override
            public void onSessionEnding(CastSession session) {
            }

            @Override
            public void onSessionResuming(CastSession session, String sessionId) {}

            @Override
            public void onSessionSuspended(CastSession session, int reason) {}

            private void onApplicationConnected(CastSession castSession) {
                Log.i("kfjc-cast", "application connected");
                mCastSession = castSession;
                callback.onApplicationConnected();
            }

            private void onApplicationDisconnected() {
                callback.onApplicationDisconnected();
                PlayerState.send(context, PlayerState.State.STOP, mediaSource);
            }
        };

        CastContext mCastContext = CastContext.getSharedInstance(context);
        mCastContext.getSessionManager().addSessionManagerListener(
                mSessionManagerListener, CastSession.class);
    }
}