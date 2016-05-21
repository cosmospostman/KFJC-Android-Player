package org.kfjc.android.player.fragment;

import android.app.DownloadManager;
import android.content.Context;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.view.ContextThemeWrapper;
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
import org.kfjc.android.player.dialog.PlaylistDialog;
import org.kfjc.android.player.dialog.SettingsDialog;
import org.kfjc.android.player.model.ShowDetails;
import org.kfjc.android.player.model.Playlist;
import org.kfjc.android.player.model.PlaylistJsonImpl;
import org.kfjc.android.player.model.MediaSource;
import org.kfjc.android.player.util.DateUtil;
import org.kfjc.android.player.util.ExternalStorageUtil;
import org.kfjc.android.player.util.HttpUtil;

import java.io.File;
import java.io.IOException;

public class PodcastPlayerFragment extends PlayerFragment {

    public static final String BROADCAST_SHOW_KEY = "broadcastShowKey";

    private ShowDetails show;
    private DownloadManager downloadManager;

    private View playlistButton;
    private TextView dateTime;
    private SeekBar playtimeSeekBar;
    private FloatingActionButton fab;
    private ImageButton downloadButton;
    private View settingsButton;
    private TextView podcastDetails;
    private LinearLayout bottomControls;
    private ProgressBar loadingProgress;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        downloadManager = (DownloadManager) getActivity().getSystemService(Context.DOWNLOAD_SERVICE);
    }

    private View.OnClickListener fabClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            onPlayStopButtonClick();
        }
    };

    private View.OnClickListener settingsButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            SettingsDialog settingsFragment = new SettingsDialog();
            settingsFragment.show(getFragmentManager(), "settings");        }
    };

    private View.OnClickListener showPlaylist = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            PlaylistDialog.newInstance(show.getAirName(), show.getTimestamp(), show.getPlaylistId())
                    .show(getFragmentManager(), "playlist");
        }
    };

    private View.OnClickListener downloadClicklistener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            homeScreen.requestExternalWritePermission();
        }
    };

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
        if (bundle != null) {
            this.show = bundle.getParcelable(BROADCAST_SHOW_KEY);
            homeScreen.setActionbarTitle(show.getAirName());
            dateTime.setText(show.getTimestampString());

            updateDownloadState();
            fab.setImageResource(R.drawable.ic_play_arrow_white_48dp);
            homeScreen.syncState();
            bottomControls.setVisibility(View.VISIBLE);
            loadingProgress.setVisibility(View.INVISIBLE);
        }
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
        switch (displayState) {
            case PAUSE:
                if (playerSource.type == MediaSource.Type.ARCHIVE
                        && playerSource.show.getPlaylistId().equals(show.getPlaylistId())) {
                    homeScreen.unpausePlayer();
                    break;
                } // else fall through:
            case STOP:
                homeScreen.playSource(new MediaSource(show));
                break;
            case PLAY:
                homeScreen.pausePlayer();
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
        makeEnsureDownloadTask().execute();
    }

    private AsyncTask<Void, Void, Void> makeEnsureDownloadTask() {
        return new AsyncTask<Void, Void, Void>() {
            protected Void doInBackground(Void... unusedParams) {
                try {
                    ensureDownloaded();
                } catch (IOException e) {}
                return null;
            }
        };
    }

    private void ensureDownloaded() throws IOException {
        File podcastDir = ExternalStorageUtil.getPodcastDir(show.getPlaylistId());
        File podcastPlaylist = ExternalStorageUtil.getPlaylistFile(show.getPlaylistId());
        boolean podcastDirExists = podcastDir.exists();
        boolean podcastPlaylistExistsAndNotEmpty =
                podcastPlaylist.exists() && podcastPlaylist.length() > 0;
        if (! (podcastDirExists && podcastPlaylistExistsAndNotEmpty)) {
            String playlistUrl = Constants.PLAYLIST_URL + show.getPlaylistId();
            Playlist playlist = new PlaylistJsonImpl(HttpUtil.getUrl(playlistUrl));
            ExternalStorageUtil.createShowDir(show, playlist);
        }
        for (int i = 0; i < show.getUrls().size(); i++) {
            Uri uri = Uri.parse(show.getUrls().get(i));
            String filename = uri.getLastPathSegment();
            File downloadFile = new File(podcastDir, filename);
            if (! (downloadFile.exists() && downloadFile.length() > 0)) {
                DownloadManager.Request req = new DownloadManager.Request(uri)
                        .setTitle(getString(R.string.format_archive_file, show.getAirName(), i+1, show.getUrls().size()))
                        .setDestinationUri(Uri.fromFile(downloadFile));
                long referenceId = downloadManager.enqueue(req);
                homeScreen.registerDownload(referenceId, show);
            }
        }
    }

    @Override
    void onStateChanged(PlayerState state, MediaSource source) {
        switch (state) {
            case PLAY:
                if (source.type == MediaSource.Type.ARCHIVE
                        && source.show.getPlaylistId().equals(show.getPlaylistId())) {
                    setPlayState();
                } else {
                    setStopState();
                }
                break;
            case PAUSE:
                setPauseState();
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
        fab.setImageResource(R.drawable.ic_pause_white_48dp);
        loadingProgress.setVisibility(View.INVISIBLE);
        startPlayClockUpdater();
        displayState = PlayerState.PLAY;
    }

    private void setPauseState() {
        fab.setImageResource(R.drawable.ic_play_arrow_white_48dp);
        updateClock();
        displayState = PlayerState.PAUSE;
    }

    private void setStopState() {
        fab.setImageResource(R.drawable.ic_play_arrow_white_48dp);
            loadingProgress.setVisibility(View.INVISIBLE);
        handler.removeCallbacks(playClockUpdater);
        displayState = PlayerState.STOP;
    }

    private void setBufferState() {
        loadingProgress.setVisibility(View.VISIBLE);
        fab.setImageResource(R.drawable.ic_stop_white_48dp);
        displayState = PlayerState.PLAY;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }
}
