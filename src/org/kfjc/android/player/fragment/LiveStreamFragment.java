package org.kfjc.android.player.fragment;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.kfjc.android.player.R;
import org.kfjc.android.player.activity.HomeScreenInterface;
import org.kfjc.android.player.activity.LavaLampActivity;
import org.kfjc.android.player.dialog.SettingsDialog;
import org.kfjc.android.player.model.Playlist;
import org.kfjc.android.player.util.GraphicsUtil;

public class LiveStreamFragment extends Fragment {

    public enum PlayerState {
        PLAY,
        STOP,
        BUFFER
    }

    private HomeScreenInterface homeScreen;
    private GraphicsUtil graphics;

    private TextView currentTrackTextView;
    private FloatingActionButton playStopButton;
    private FloatingActionButton settingsButton;
    private ImageView radioDevil;

    private PlayerState playerState = PlayerState.STOP;

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
        updatePlaylist(homeScreen.getLatestPlaylist());
        setState(playerState);

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
        radioDevil.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
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

    public void setState(PlayerState state) {
        playerState = state;
        if (!this.isAdded()) {
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
                break;
            case BUFFER:
                graphics.bufferDevil(radioDevil, true);
                playStopButton.setImageResource(R.drawable.ic_stop_white_48dp);
                radioDevil.setEnabled(false);
                break;
        }
    }

    public void updatePlaylist(Playlist playlist) {
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
        String spacer = TextUtils.isEmpty(e.getArtist()) ? "" : "&nbsp&nbsp&nbsp";
        return Html.fromHtml(e.getArtist() + spacer + "<i>" + e.getTrack() + "</i>");
    }

    public void showSettings() {
        SettingsDialog settingsFragment = new SettingsDialog();
        settingsFragment.setUrlPreferenceChangeHandler(
                new SettingsDialog.StreamUrlPreferenceChangeHandler() {
            @Override public void onStreamUrlPreferenceChange() {
                if (homeScreen.isStreamServicePlaying()) {
                    homeScreen.restartStream();
                }
            }
        });
        settingsFragment.show(getActivity().getFragmentManager(), "settings");
    }

}
