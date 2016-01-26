package org.kfjc.android.player.fragment;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.kfjc.android.player.R;
import org.kfjc.android.player.activity.EventHandlerFactory;
import org.kfjc.android.player.activity.HomeScreenInterface;
import org.kfjc.android.player.activity.LavaLampActivity;
import org.kfjc.android.player.dialog.SettingsDialog;
import org.kfjc.android.player.model.TrackInfo;
import org.kfjc.android.player.service.PlaylistService;
import org.kfjc.android.player.service.StreamService;
import org.kfjc.android.player.util.GraphicsUtil;
import org.kfjc.android.player.util.NotificationUtil;
import org.kfjc.android.player.util.UiUtil;

public class LiveStreamFragment extends Fragment {

    public enum State {
        LOADING_STREAMS,
        CONNECTED,
    }

    public enum PlayerState {
        PLAY,
        STOP,
        BUFFER
    }

    private HomeScreenInterface homeScreen;
    private PlaylistService playlistService;
    private Intent playlistServiceIntent;
    private StreamService streamService;
    private GraphicsUtil graphics;

    private TextView currentTrackTextView;
    private FloatingActionButton playStopButton;
    private FloatingActionButton settingsButton;
    private ImageView radioDevil;

    private ServiceConnection playlistServiceConnection;
    private PlayerState playerState = PlayerState.STOP;
    private NotificationUtil notificationUtil;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (playlistServiceConnection == null) {
            playlistServiceConnection = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    PlaylistService.PlaylistBinder binder = (PlaylistService.PlaylistBinder) service;
                    playlistService = binder.getService();
                    playlistService.start();
                    playlistService.registerPlaylistCallback(new PlaylistService.PlaylistCallback() {
                        @Override
                        public void onTrackInfoFetched(TrackInfo trackInfo) {
                            updateTrackInfo(trackInfo);
                            if (streamService != null && streamService.isPlaying()) {
                                notificationUtil.updateNowPlayNotification(trackInfo);
                            }
                        }
                    });
                }

                @Override
                public void onServiceDisconnected(ComponentName name) {}
            };
        }

        playlistServiceIntent = new Intent(getActivity(), PlaylistService.class);
        getActivity().startService(playlistServiceIntent);
        this.notificationUtil = new NotificationUtil(getActivity());
    }

    @Override
    public void onStart() {
        super.onStart();
        getActivity().bindService(playlistServiceIntent, playlistServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (playlistService != null) {
            playlistService.start();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        getActivity().unbindService(playlistServiceConnection);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getActivity().stopService(playlistServiceIntent);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        homeScreen.setActionbarTitle(getString(R.string.fragment_title_stream));
        graphics = new GraphicsUtil(getResources());
        View view = inflater.inflate(R.layout.fragment_livestream, container, false);
        currentTrackTextView = (TextView) view.findViewById(R.id.currentTrack);
        settingsButton = (FloatingActionButton) view.findViewById(R.id.settingsButton);
        playStopButton = (FloatingActionButton) view.findViewById(R.id.playstopbutton);
        radioDevil = (ImageView) view.findViewById(R.id.logo);
        addButtonListeners();
        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            homeScreen = (HomeScreenInterface) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.getClass().getSimpleName() + " must implement "
                + HomeScreenInterface.class.getSimpleName());
        }
    }

    public void setState(State state) {
        if (!isAdded()) {
            return;
        }
        switch (state) {
            case LOADING_STREAMS:
                break;
            case CONNECTED:
                playStopButton.setEnabled(true);
                settingsButton.setEnabled(true);
                break;
        }
    }

    private void addButtonListeners() {
        playStopButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                switch (playerState) {
                    case STOP:
                        homeScreen.playStream();
                        break;
                    case BUFFER:
                    case PLAY:
                        homeScreen.stopStream();
                        break;
                }
            }
        });
        playStopButton.setEnabled(false);
        radioDevil.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent fullscreenIntent = new Intent(getActivity(), LavaLampActivity.class);
                fullscreenIntent.setAction("org.kfjc.android.player.FULLSCREEN");
                startActivity(fullscreenIntent);
            }
        });
        radioDevil.setEnabled(false);
        settingsButton.setEnabled(false);
        settingsButton.setOnTouchListener(UiUtil.buttonTouchListener);
        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                showSettings();
            }
        });
    }

    public void setState(PlayerState state) {
        //TODO: save and restore state on pause/resume?
        playerState = state;
        if (!this.isResumed()) {
            return;
        }
        switch(state) {
            case STOP:
                graphics.bufferDevil(radioDevil, false);
                playStopButton.setImageResource(R.drawable.ic_play_arrow_white_48dp);
                radioDevil.setImageResource(graphics.radioDevilOff());
                radioDevil.setEnabled(false);
                break;
            case PLAY:
                graphics.bufferDevil(radioDevil, false);
                playStopButton.setImageResource(R.drawable.ic_stop_white_48dp);
                radioDevil.setImageResource(graphics.radioDevilOn());
                radioDevil.setEnabled(true);
                notificationUtil.updateNowPlayNotification(playlistService.getLastFetchedTrackInfo());
                break;
            case BUFFER:
                graphics.bufferDevil(radioDevil, true);
                playStopButton.setImageResource(R.drawable.ic_stop_white_48dp);
                radioDevil.setEnabled(false);
                break;
        }
    }

    public void updateTrackInfo(TrackInfo nowPlaying) {
        if (nowPlaying.getCouldNotFetch()) {
            currentTrackTextView.setText(R.string.status_playlist_unavailable);
        } else {
            homeScreen.setActionbarTitle(nowPlaying.getDjName());
            currentTrackTextView.setText(nowPlaying.artistTrackHtml());
        }
    }

    public void stopPlaylistService() {
        playlistService.stop();
    }

    public void showSettings() {
        SettingsDialog settingsFragment = new SettingsDialog();
        settingsFragment.setUrlPreferenceChangeHandler(
                EventHandlerFactory.onUrlPreferenceChange(homeScreen));
        settingsFragment.show(getActivity().getFragmentManager(), "settings");
    }

}
