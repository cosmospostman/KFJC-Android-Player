package org.kfjc.android.player.fragment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
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
import org.kfjc.android.player.control.PreferenceControl;
import org.kfjc.android.player.dialog.SettingsDialog;
import org.kfjc.android.player.model.Playlist;
import org.kfjc.android.player.util.GraphicsUtil;

public class LiveStreamFragment extends PlayerFragment {

    private GraphicsUtil graphics;

    private TextView currentTrackTextView;
    private FloatingActionButton playStopButton;
    private FloatingActionButton settingsButton;
    private ImageView radioDevil;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        graphics = new GraphicsUtil();
        homeScreen.setActionbarTitle(getString(R.string.fragment_title_stream));
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
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            homeScreen = (HomeScreenInterface) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.getClass().getSimpleName() + " must implement "
                + HomeScreenInterface.class.getSimpleName());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        homeScreen.setNavigationItemChecked(R.id.nav_livestream);
    }

    @Override
    public void onPause() {
        super.onPause();
        graphics.bufferDevil(radioDevil, false);
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
                        homeScreen.stopPlayer();
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
        settingsFragment.show(getFragmentManager(), "settings");
    }

    @Override
    void onStatePlay() {
        graphics.bufferDevil(radioDevil, false);
        playStopButton.setImageResource(R.drawable.ic_stop_white_48dp);
        graphics.radioDevilOn(radioDevil);
        radioDevil.setEnabled(true);
    }

    @Override
    void onStateStop() {
        graphics.bufferDevil(radioDevil, false);
        playStopButton.setImageResource(R.drawable.ic_play_arrow_white_48dp);
        graphics.radioDevilOff(radioDevil);
        radioDevil.setEnabled(false);
    }

    @Override
    void onStateBuffer() {
        graphics.bufferDevil(radioDevil, true);
        playStopButton.setImageResource(R.drawable.ic_stop_white_48dp);
        radioDevil.setEnabled(false);
    }
}
