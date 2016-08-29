package org.kfjc.android.player.fragment;

import android.Manifest;
import android.app.DownloadManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import org.kfjc.android.player.Constants;
import org.kfjc.android.player.R;
import org.kfjc.android.player.dialog.OfflineDialog;
import org.kfjc.android.player.dialog.PlaylistDialog;
import org.kfjc.android.player.dialog.SettingsDialog;
import org.kfjc.android.player.model.MediaSource;
import org.kfjc.android.player.model.Playlist;
import org.kfjc.android.player.model.PlaylistJsonImpl;
import org.kfjc.android.player.model.ShowDetails;
import org.kfjc.android.player.util.DateUtil;
import org.kfjc.android.player.util.DownloadUtil;
import org.kfjc.android.player.util.ExternalStorageUtil;
import org.kfjc.android.player.util.HttpUtil;
import org.kfjc.android.player.util.Intents;

import java.io.File;
import java.io.IOException;

public class PodcastPlayerFragment extends PlayerFragment {

    public static final String BROADCAST_SHOW_KEY = "broadcastShowKey";

    private ShowDetails show;

    private View playlistButton;
    private TextView dateTime;
    private SeekBar playtimeSeekBar;
    private FloatingActionButton fab;
    private ImageButton downloadButton;
    private View settingsButton;
    private TextView podcastDetails;
    private LinearLayout bottomControls;
    private ProgressBar loadingProgress;

    private View.OnClickListener fabClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            onPlayStopButtonClick();
        }
    };

    private View.OnClickListener settingsButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            SettingsDialog settingsFragment = SettingsDialog.newInstance(true);
            settingsFragment.show(getFragmentManager(), "settings");        }
    };

    private View.OnClickListener showPlaylist = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            PlaylistDialog.newInstance(show.getAirName(), show.getTimestamp(), show.getPlaylistId())
                    .show(getFragmentManager(), "playlist");
        }
    };

    private boolean hasWritePermission() {
        return ContextCompat.checkSelfPermission(
                getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    private View.OnClickListener downloadClicklistener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!hasWritePermission()) {
                homeScreen.requestAndroidWritePermissions();
            } else {
                showOfflineDialog();
            }
        }
    };

    private void showOfflineDialog() {
        OfflineDialog offlineDialog = OfflineDialog.newInstance(show);
        offlineDialog.setOnDismissListener(new OfflineDialog.OnDismissListener() {
            @Override
            public void onDismiss() {
                updateDownloadState();
            }
        });
        offlineDialog.show(getFragmentManager(), "offline");
    }

    SeekBar.OnSeekBarChangeListener onSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
        boolean isTrackingTouch = false;
        @Override
        public void onStopTrackingTouch(SeekBar arg0) {
            isTrackingTouch = false;
        }

        @Override
        public void onStartTrackingTouch(SeekBar arg0) {
            isTrackingTouch = true;
        }

        @Override
        public void onProgressChanged(SeekBar arg0, int progress, boolean arg2) {
            long seekToMillis = (long) (progress) * 100;
            if (isTrackingTouch && displayState != PlayerState.STOP) {
                handler.removeCallbacks(playClockUpdater);
                updateClockHelper(seekToMillis);
                homeScreen.seekPlayer(seekToMillis);
            }
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        homeScreen.setActionbarTitle(getString(R.string.fragment_title_podcast));
        View view = inflater.inflate(R.layout.fragment_podcastplayer, container, false);

        playlistButton = view.findViewById(R.id.playlist);
        dateTime = (TextView) view.findViewById(R.id.podcastDateTime);
        fab = (FloatingActionButton) view.findViewById(R.id.fab);
        settingsButton = view.findViewById(R.id.settingsButton);
        playtimeSeekBar = (SeekBar) view.findViewById(R.id.playtimeSeekBar);
        podcastDetails = (TextView) view.findViewById(R.id.playtimeDisplay);
        bottomControls = (LinearLayout) view.findViewById(R.id.bottomControls);
        loadingProgress = (ProgressBar) view.findViewById(R.id.loadingProgress);

        downloadButton = (ImageButton) view.findViewById(R.id.downloadButton);
        downloadButton.setOnClickListener(downloadClicklistener);

        playlistButton.setOnClickListener(showPlaylist);
        fab.setOnClickListener(fabClickListener);
        settingsButton.setOnClickListener(settingsButtonClickListener);
        playtimeSeekBar.setOnSeekBarChangeListener(onSeekBarChangeListener);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        Bundle bundle = getArguments();

        // Don't do null check. Prefer to crash if show is null.
        this.show = bundle.getParcelable(BROADCAST_SHOW_KEY);
        homeScreen.setActionbarTitle(show.getAirName());
        dateTime.setText(show.getTimestampString());

        updateDownloadState();
        fab.setImageResource(R.drawable.ic_play_arrow_white_48dp);
        homeScreen.syncState();
        bottomControls.setVisibility(View.VISIBLE);
        loadingProgress.setVisibility(View.INVISIBLE);

        homeScreen.setActionBarBackArrow(true);
        homeScreen.syncState();
    }

    private void updateDownloadState() {
        int iconResource = ExternalStorageUtil.hasAllContent(show)
                ? R.drawable.ic_offline_pin_white_48dp
                : R.drawable.ic_file_download_white_48dp;
        downloadButton.setImageResource(iconResource);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        Bundle bundle = new Bundle();
        bundle.putParcelable(PodcastPlayerFragment.BROADCAST_SHOW_KEY, show);
        outState.putAll(bundle);
    }

    private void onPlayStopButtonClick() {
        if (displayState == null) {
            displayState = PlayerState.STOP;
        }
        switch (displayState) {
            case PAUSE:
                if (playerSource.type == MediaSource.Type.ARCHIVE
                        && playerSource.show.getPlaylistId().equals(show.getPlaylistId())) {
                    Intents.sendAction(getActivity(), Intents.INTENT_UNPAUSE);
                } // else fall through:
            case STOP:
                Intents.sendAction(getActivity(), Intents.INTENT_PLAY, new MediaSource(show));
                break;
            case BUFFER:
                Intents.sendAction(getActivity(), Intents.INTENT_STOP);
                break;
            case PLAY:
                Intents.sendAction(getActivity(), Intents.INTENT_PAUSE);
                break;
        }
    }

    void updateClock() {
        updateClockHelper(homeScreen.getPlayerPosition());
    }

    private void updateClockHelper(long playerPos) {
        long totalShowTime = homeScreen.getPlayerSource().show.getTotalShowTimeMillis();
        podcastDetails.setText(DateUtil.formatTime(playerPos - show.getHourPaddingTimeMillis()));
        playtimeSeekBar.setMax((int)totalShowTime/100);
        playtimeSeekBar.setProgress((int)playerPos/100);
    }

    public void onWritePermissionResult(boolean wasGranted) {
        if (!wasGranted) {
            return;
        }
        showOfflineDialog();
    }

    @Override
    void onStateChanged(PlayerState state, MediaSource source) {
        switch (state) {
            case PLAY:
                if (source.type == MediaSource.Type.ARCHIVE
                        && show != null
                        && source.show.getPlaylistId().equals(show.getPlaylistId())) {
                    setPlayState();
                } else {
                    setStopState();
                }
                break;
            case PAUSE:
                if (source.type == MediaSource.Type.ARCHIVE
                        && source.show.getPlaylistId().equals(show.getPlaylistId())) {
                    setPauseState();
                } else {
                    setStopState();
                }
                break;
            case STOP:
                setStopState();
                break;
            case BUFFER:
                setBufferState();
                break;
        }
    }

    private void setPlayState() {
        playtimeSeekBar.setEnabled(true);
        fab.setImageResource(R.drawable.ic_pause_white_48dp);
        loadingProgress.setVisibility(View.INVISIBLE);
        startPlayClockUpdater();
        displayState = PlayerState.PLAY;
    }

    private void setPauseState() {
        playtimeSeekBar.setEnabled(false);
        fab.setImageResource(R.drawable.ic_play_arrow_white_48dp);
        updateClock();
        displayState = PlayerState.PAUSE;
    }

    private void setStopState() {
        playtimeSeekBar.setEnabled(false);
        fab.setImageResource(R.drawable.ic_play_arrow_white_48dp);
            loadingProgress.setVisibility(View.INVISIBLE);
        handler.removeCallbacks(playClockUpdater);
        playtimeSeekBar.setProgress(0);
        podcastDetails.setText("");
        displayState = PlayerState.STOP;
    }

    private void setBufferState() {
        playtimeSeekBar.setEnabled(false);
        loadingProgress.setVisibility(View.VISIBLE);
        fab.setImageResource(R.drawable.ic_stop_white_48dp);
        displayState = PlayerState.BUFFER;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    public ShowDetails getShow() {
        return show;
    }
}
