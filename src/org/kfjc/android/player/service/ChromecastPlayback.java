package org.kfjc.android.player.service;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.media.RemoteMediaClient;
import com.google.android.gms.common.images.WebImage;

import org.kfjc.android.player.intent.PlayerState;

public class ChromecastPlayback extends AbstractPlayback {

    private static final String TAG = ChromecastPlayback.class.getSimpleName();

    public enum CastPlaybackState {
        PLAYING, PAUSED, BUFFERING, IDLE
    }

    private CastPlaybackState castPlaybackState;
    private CastSession mCastSession;

    public ChromecastPlayback(Context context, CastSession castSession) {
        super(context);
        mCastSession = castSession;
    }

    public ChromecastPlayback(Context context) {
        super(context);
        CastContext mCastContext = CastContext.getSharedInstance(context);
        mCastSession =  mCastContext.getSessionManager().getCurrentCastSession();
    }

    @Override
    public void seek(long positionMillis) {}

    @Override
    public void play(String streamUrl) {}

    @Override
    public void stop(boolean alsoReset) {
        mCastSession.getRemoteMediaClient().stop();
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

    private MediaInfo buildMediaInfo(String streamUrl) {
        MediaMetadata metadata = new MediaMetadata(MediaMetadata.MEDIA_TYPE_GENERIC);
        metadata.putString(MediaMetadata.KEY_TITLE, "KFJC Live Stream #TODO");
        metadata.addImage(new WebImage(Uri.parse("http://4.bp.blogspot.com/_YsgOkAVMc0A/SobPzt5dHwI/AAAAAAAABAE/Ua5fDHXXPYg/s320/kfjc.jpg")));
        return new MediaInfo.Builder(streamUrl + ";")
                .setStreamType(MediaInfo.STREAM_TYPE_LIVE)
                .setContentType(getSource().getMimeType())
                .setMetadata(metadata)
                .build();
    }


    public void setCastSession(CastSession session) {
        mCastSession = session;
    }

    public void setCastPlaybackState(CastPlaybackState state) {
        castPlaybackState = state;
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
        PlayerState.send(context, PlayerState.State.PLAY, mediaSource);
    }

    @Override
    public void pause() {

    }

    @Override
    public void unpause() {

    }
}
