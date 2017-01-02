package org.kfjc.android.player.fragment;

import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Html;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.kfjc.android.player.R;
import org.kfjc.android.player.activity.LavaLampActivity;
import org.kfjc.android.player.control.PreferenceControl;
import org.kfjc.android.player.dialog.PlaylistDialog;
import org.kfjc.android.player.dialog.SettingsDialog;
import org.kfjc.android.player.intent.PlayerControl;
import org.kfjc.android.player.intent.PlayerState.State;
import org.kfjc.android.player.intent.PlaylistUpdate;
import org.kfjc.android.player.model.KfjcMediaSource;
import org.kfjc.android.player.model.Playlist;
import org.kfjc.android.player.receiver.PlaylistUpdateReceiver;
import org.kfjc.android.player.util.GraphicsUtil;
import org.kfjc.android.player.util.NotificationUtil;

public class LiveStreamFragment extends PlayerFragment {

    public static final String TAG = LiveStreamFragment.class.getSimpleName();

    private GraphicsUtil graphics;

    private TextView currentTrackTextView;
    private FloatingActionButton playStopButton;
    private View settingsButton;
    private ImageView radioDevil;
    private NotificationUtil notificationUtil;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        graphics = new GraphicsUtil();
        homeScreen.setActionbarTitle(getString(R.string.fragment_title_stream));
        View view = inflater.inflate(R.layout.fragment_livestream, container, false);
        currentTrackTextView = (TextView) view.findViewById(R.id.currentTrack);
        settingsButton = view.findViewById(R.id.settingsButton);
        playStopButton = (FloatingActionButton) view.findViewById(R.id.playstopbutton);
        View playlistButton = view.findViewById(R.id.playlist);
        playlistButton.setOnClickListener(showPlaylist);
        radioDevil = (ImageView) view.findViewById(R.id.logo);
        addButtonListeners();
        notificationUtil = new NotificationUtil(getActivity());

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(playlistUpdateReceiver,
                new IntentFilter(PlaylistUpdate.INTENT_PLAYLIST_UPDATE));
        playlistUpdateReceiver.onPlaylistUpdate(PlaylistUpdate.getLastPlaylist());
        homeScreen.setNavigationItemChecked(R.id.nav_livestream);
    }

    @Override
    public void onPause() {
        super.onPause();
        graphics.bufferDevil(radioDevil, false);
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(playlistUpdateReceiver);
    }

    @Override
    void updateClock() {}

    private PlaylistUpdateReceiver playlistUpdateReceiver = new PlaylistUpdateReceiver() {
        @Override
        public void onPlaylistUpdate(Playlist playlist) {
            updatePlaylist(playlist);
        }
    };

    private void addButtonListeners() {
        playStopButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                switch (displayState) {
                    case STOP:
                        PlayerControl.sendAction(getActivity(), PlayerControl.INTENT_PLAY, PreferenceControl.getStreamPreference());
                        break;
                    case BUFFER:
                    case PLAY:
                        PlayerControl.sendAction(getActivity(), PlayerControl.INTENT_STOP);
                        break;
                }
            }
        });
        radioDevil.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (!PreferenceControl.areBackgroundsEnabled()) {
                    return;
                }
                Intent fullscreenIntent = new Intent(getActivity(), LavaLampActivity.class);
                fullscreenIntent.setAction("org.kfjc.android.player.FULLSCREEN");
                startActivity(fullscreenIntent);
            }
        });
        radioDevil.setEnabled(false);
        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                showSettings();
            }
        });
    }

    public void updatePlaylist(Playlist playlist) {
        // TODO: move this notification logic elsewhere?
        if (homeScreen.isStreamServicePlaying()
                && playerSource.type == KfjcMediaSource.Type.LIVESTREAM) {
            notificationUtil.updateNowPlayNotification(playlist, playerSource);
        }
        if (!isAdded()) {
            return;
        }
        if (playlist == null || playlist.hasError()) {
            currentTrackTextView.setText(R.string.status_playlist_unavailable);
        } else {
            homeScreen.setActionbarTitle(playlist.getDjName());
            currentTrackTextView.setText(artistTrackHtml(playlist.getLastTrackEntry()));
        }
    }

    private android.text.Spanned artistTrackHtml(Playlist.PlaylistEntry e) {
        String spacer = TextUtils.isEmpty(e.getArtist()) ? "" : "<br>";
        return Html.fromHtml(e.getArtist() + spacer + "<i>" + e.getTrack() + "</i>");
    }

    private void showSettings() {
        SettingsDialog settingsDialog = SettingsDialog.newInstance(false);
        settingsDialog.setUrlPreferenceChangeHandler(
                new SettingsDialog.StreamUrlPreferenceChangeHandler() {
            @Override public void onStreamUrlPreferenceChange() {
                if (playerSource != null
                        && KfjcMediaSource.Type.LIVESTREAM == playerSource.type
                        && homeScreen.isStreamServicePlaying()) {
                    PlayerControl.sendAction(getActivity(), PlayerControl.INTENT_STOP);
                    PlayerControl.sendAction(getActivity(), PlayerControl.INTENT_PLAY, PreferenceControl.getStreamPreference());
                }
            }
        });
        settingsDialog.show(getFragmentManager(), "settings");
    }

    @Override
    void onStateChanged(State state, KfjcMediaSource source) {
        if (source != null && source.type == KfjcMediaSource.Type.LIVESTREAM) {
            updatePlaylist(PlaylistUpdate.getLastPlaylist());
            switch(state) {
                case PLAY:
                    setPlayState();
                    return;
                case BUFFER:
                    setBufferState();
                    return;
            }
        }
        setStopState();
    }

    private void setPlayState() {
        graphics.bufferDevil(radioDevil, false);
        playStopButton.setImageResource(R.drawable.ic_stop_white_48dp);
        graphics.radioDevilOn(radioDevil);
        radioDevil.setEnabled(true);
        displayState = State.PLAY;
    }

    private void setStopState() {
        graphics.bufferDevil(radioDevil, false);
        playStopButton.setImageResource(R.drawable.ic_play_arrow_white_48dp);
        graphics.radioDevilOff(radioDevil);
        radioDevil.setEnabled(false);
        displayState = State.STOP;
    }

    private void setBufferState() {
        graphics.bufferDevil(radioDevil, true);
        playStopButton.setImageResource(R.drawable.ic_stop_white_48dp);
        radioDevil.setEnabled(false);
        displayState = State.BUFFER;
    }

    private View.OnClickListener showPlaylist = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Playlist playlist = PlaylistUpdate.getLastPlaylist();
            PlaylistDialog d = PlaylistDialog.newInstance(
                    playlist == null ? "" : playlist.toJsonString());
            d.show(getFragmentManager(), "playlist");
        }
    };

    @Override
    public boolean setActionBarBackArrow() {
        return false;
    }
}
