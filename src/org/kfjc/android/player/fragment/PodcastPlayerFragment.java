package org.kfjc.android.player.fragment;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.android.gms.cast.framework.media.RemoteMediaClient;

import org.kfjc.android.player.R;
import org.kfjc.android.player.dialog.OfflineDialog;
import org.kfjc.android.player.dialog.PlaylistDialog;
import org.kfjc.android.player.dialog.SettingsDialog;
import org.kfjc.android.player.model.KfjcMediaSource;
import org.kfjc.android.player.model.ShowDetails;
import org.kfjc.android.player.intent.PlayerState.State;
import org.kfjc.android.player.util.DateUtil;
import org.kfjc.android.player.util.ExternalStorageUtil;
import org.kfjc.android.player.intent.PlayerControl;

public class PodcastPlayerFragment extends PlayerFragment {

    public static final String TAG = PodcastPlayerFragment.class.getSimpleName();
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

    public static PodcastPlayerFragment newInstance(ShowDetails show) {
        PodcastPlayerFragment fragment = new PodcastPlayerFragment();
        Bundle bundle = new Bundle();
        bundle.putParcelable(PodcastPlayerFragment.BROADCAST_SHOW_KEY, show);
        fragment.setArguments(bundle);
        return fragment;
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
            SettingsDialog.newInstance(true).show(getFragmentManager(), "settings");
        }
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

    boolean isSeekBarActive = false;
    SeekBar.OnSeekBarChangeListener onSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onStopTrackingTouch(SeekBar arg0) {
            long seekToMillis = (long) (arg0.getProgress()) * 100;
            if (playerState != State.STOP) {
                updateClockHelper(seekToMillis);
                homeScreen.seekPlayer(seekToMillis);
            }
            isSeekBarActive = false;
        }

        @Override
        public void onStartTrackingTouch(SeekBar arg0) {
            handler.removeCallbacks(playClockUpdater);
            isSeekBarActive = true;
        }

        @Override
        public void onProgressChanged(SeekBar arg0, int progress, boolean arg2) {
            long seekToMillis = (long) (progress) * 100;
            updateClockHelper(seekToMillis);
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
        Bundle bundle = getArguments();
        // Don't do null check. Prefer to crash if show is null.
        this.show = bundle.getParcelable(BROADCAST_SHOW_KEY);
        if (show == null) {
            Log.e("TAG", "null show");
        }
        super.onResume();
        homeScreen.setActionbarTitle(show.getAirName());
        dateTime.setText(show.getTimestampString());

        updateDownloadState();
        bottomControls.setVisibility(View.VISIBLE);
        loadingProgress.setVisibility(View.INVISIBLE);

        homeScreen.setActionBarBackArrow(true);
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
                if (playerSource.type == KfjcMediaSource.Type.ARCHIVE
                        && playerSource.show.getPlaylistId().equals(show.getPlaylistId())) {
                    homeScreen.pausePlayback(true);
                } // else fall through:
            case STOP:
                homeScreen.startPlayback(new KfjcMediaSource(show));
                break;
            case BUFFER:
                homeScreen.stopPlayback();
                break;
            case PLAY:
                homeScreen.pausePlayback(false);
                break;
        }
    }

    void updateClock() {
        updateClockHelper(homeScreen.getPlayerPosition());
    }

    private void updateClockHelper(long playerPos) {
        if (playerSource == null || playerSource.show == null) {
            return;
        }
        long totalShowTime = playerSource.show.getTotalShowTimeMillis();
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
    void onStateChanged(State state, KfjcMediaSource source) {
        if (source != null && source.type == KfjcMediaSource.Type.ARCHIVE
                && show != null
                && source.show.getPlaylistId().equals(show.getPlaylistId())) {
            switch (state) {
                case PLAY:
                    setPlayState();
                    return;
                case PAUSE:
                    setPauseState();
                    return;
                case BUFFER:
                    setBufferState();
                    return;
            }
        }
        setStopState();
    }

    private void setPlayState() {
        playtimeSeekBar.setEnabled(true);
        fab.setImageResource(R.drawable.ic_pause_white_48dp);
        loadingProgress.setVisibility(View.INVISIBLE);
        if (!isSeekBarActive) {
            startPlayClockUpdater();
        }
        displayState = State.PLAY;
    }

    private void setPauseState() {
        playtimeSeekBar.setEnabled(false);
        fab.setImageResource(R.drawable.ic_play_arrow_white_48dp);
        updateClock();
        displayState = State.PAUSE;
    }

    private void setStopState() {
        playtimeSeekBar.setEnabled(false);
        fab.setImageResource(R.drawable.ic_play_arrow_white_48dp);
            loadingProgress.setVisibility(View.INVISIBLE);
        handler.removeCallbacks(playClockUpdater);
        playtimeSeekBar.setProgress(0);
        podcastDetails.setText("");
        displayState = State.STOP;
    }

    private void setBufferState() {
        loadingProgress.setVisibility(View.VISIBLE);
        fab.setImageResource(R.drawable.ic_stop_white_48dp);
        displayState = State.BUFFER;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    public ShowDetails getShow() {
        return show;
    }

    @Override
    public boolean setActionBarBackArrow() {
        return true;
    }
}
