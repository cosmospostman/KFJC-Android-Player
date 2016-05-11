package org.kfjc.android.player.fragment;

import android.app.DownloadManager;
import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import org.kfjc.android.player.Constants;
import org.kfjc.android.player.R;
import org.kfjc.android.player.model.BroadcastShow;
import org.kfjc.android.player.model.Playlist;
import org.kfjc.android.player.model.PlaylistJsonImpl;
import org.kfjc.android.player.model.MediaSource;
import org.kfjc.android.player.util.DateUtil;
import org.kfjc.android.player.util.ExternalStorageUtil;
import org.kfjc.android.player.util.HttpUtil;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class PodcastPlayerFragment extends PlayerFragment {

    public static final String BROADCAST_SHOW_KEY = "broadcastShowKey";
    private static final long PADDING_TIME_MILLIS = 300000; // 5 mins

    private enum FragmentState {
        PREVIEW,
        PLAYER
    }

    private BroadcastShow show;
    private DownloadManager downloadManager;
    private FragmentState fragmentState;
    private Handler handler = new Handler();
    private long totalShowTime;
    private long[] segmentBounds;

    private SeekBar playtimeSeekBar;
    private FloatingActionButton fab;
    private TextView podcastDetails;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        downloadManager = (DownloadManager) getActivity().getSystemService(Context.DOWNLOAD_SERVICE);
    }

    @Override
    public void onPause() {
        super.onPause();
        handler.removeCallbacks(playClockUpdater);
    }

    private Runnable playClockUpdater = new Runnable() {
        @Override public void run() {
            long playerPos = homeScreen.getPlayerPosition();

            int playingSegmentNumber = homeScreen.getPlayerSource().sequenceNumber;
            long segmentOffset = (playingSegmentNumber == 0) ? 0 : segmentBounds[playingSegmentNumber - 1];
            long extra = (playingSegmentNumber == 0) ? 0 : PADDING_TIME_MILLIS;
            long pos = playerPos + segmentOffset - extra;

            podcastDetails.setText(DateUtil.formatTime(pos) + " | " + DateUtil.formatTime(totalShowTime));

            playtimeSeekBar.setMax((int)totalShowTime/100);
            playtimeSeekBar.setProgress((int)pos/100);

            handler.postDelayed(this, 1000);
        }
    };

    private View.OnClickListener pulldownFabClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            getActivity().getFragmentManager().beginTransaction()
                    .setCustomAnimations(R.animator.fade_in_down, R.animator.fade_out_down)
                    .replace(R.id.home_screen_main_fragment, new PodcastFragment())
                    .addToBackStack(null)
                    .commit();
        }
    };

    private View.OnClickListener fabClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (fragmentState) {
                case PREVIEW:
                    homeScreen.requestExternalWritePermission();
                    break;
                case PLAYER:
                    onPlayStopButtonClick();
                    break;
            }
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
            if (isTrackingTouch) {
                handler.removeCallbacks(playClockUpdater);
                seekOverEntireShow(seekToMillis);
            }
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        homeScreen.setActionbarTitle(getString(R.string.fragment_title_podcast));
        View view = inflater.inflate(R.layout.fragment_podcastplayer, container, false);

        FloatingActionButton pullDownFab = (FloatingActionButton) view.findViewById(R.id.pullDownButton);
        TextView airName = (TextView) view.findViewById(R.id.airName);
        TextView dateTime = (TextView) view.findViewById(R.id.podcastDateTime);
        fab = (FloatingActionButton) view.findViewById(R.id.fab);
        playtimeSeekBar = (SeekBar) view.findViewById(R.id.playtimeSeekBar);
        podcastDetails = (TextView) view.findViewById(R.id.podcastDetails);

        pullDownFab.setOnClickListener(pulldownFabClickListener);
        fab.setOnClickListener(fabClickListener);
        playtimeSeekBar.setOnSeekBarChangeListener(onSeekBarChangeListener);

        Bundle bundle = getArguments();
        if (bundle != null) {
            this.show = bundle.getParcelable(BROADCAST_SHOW_KEY);
            airName.setText(show.getAirName());
            dateTime.setText(show.getTimestampString());
            checkState();
        }

        return view;
    }

    private void seekOverEntireShow(long seekToMillis) {
        for (int i = 0; i < segmentBounds.length; i++) {
            if (seekToMillis < segmentBounds[i]) {
                // load segment i
                playArchive(i);
                //seek to adjusted position
                long thisSegmentStart = (i == 0) ? 0 : segmentBounds[i-1];
                long extraSeek = (i == 0) ? 0 : PADDING_TIME_MILLIS;
                long localSeekTo = seekToMillis - thisSegmentStart + extraSeek;
                homeScreen.seekPlayer(localSeekTo);
                return;
            }
        }
    }

    private void checkState() {
        if (! ExternalStorageUtil.getPodcastDir(show.getPlaylistId()).exists()) {
            // Show preview and download state
            fab.setImageResource(R.drawable.ic_file_download_white_48dp);
            podcastDetails.setText(show.getUrls().size() + " hour show, 258Mb download.");
            fragmentState = FragmentState.PREVIEW;
        } else if (ExternalStorageUtil.hasAllContent(show)) {
            // Show play state
            fab.setImageResource(R.drawable.ic_play_arrow_white_48dp);
            countTotalShowTime();
            fragmentState = FragmentState.PLAYER;
        }
    }

    private void onPlayStopButtonClick() {
        switch (displayState) {
            case STOP:
                playArchive(0);
                homeScreen.seekPlayer(PADDING_TIME_MILLIS);
                break;
            case PLAY:
                homeScreen.stopPlayer();
                break;
        }
    }

    private void playArchive(int hourNumber) {
        File f = ExternalStorageUtil.getSavedArchivesForShow(show).get(hourNumber);
        homeScreen.playArchive(new MediaSource(
                MediaSource.Type.ARCHIVE, f.getAbsolutePath(), MediaSource.Format.MP3, hourNumber,
                show.getAirName(), show.getTimestampString()));
    }

    private void countTotalShowTime() {
        List<File> files = ExternalStorageUtil.getSavedArchivesForShow(show);
        long[] bounds = new long[files.size()];
        long totalTime = 0;
        for (int i = 0; i < files.size(); i++) {
            long showTime = countFilePlayTime(files.get(i));

            // Total
            totalTime += showTime - 2 * PADDING_TIME_MILLIS;

            // Bound
            bounds[i] = totalTime + PADDING_TIME_MILLIS;
            if (i == files.size() - 1) {
                bounds[i] += PADDING_TIME_MILLIS;
            }
        }
        this.totalShowTime = totalTime;
        this.segmentBounds = bounds;
    }

    private long countFilePlayTime(File f) {
        MediaMetadataRetriever metaRetriever = new MediaMetadataRetriever();
        metaRetriever.setDataSource(f.getPath());
        String durationString =
                metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        return Long.parseLong(durationString);
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
            String playlistUrl = Constants.PLAYLIST_URL + "?i=" + show.getPlaylistId();
            Playlist playlist = new PlaylistJsonImpl(HttpUtil.getUrl(playlistUrl));
            ExternalStorageUtil.createShowDir(show, playlist);
        }
        for (int i = 0; i < show.getUrls().size(); i++) {
            Uri uri = Uri.parse(show.getUrls().get(i));
            String filename = uri.getLastPathSegment();
            File downloadFile = new File(podcastDir, filename);
            if (! (downloadFile.exists() && downloadFile.length() > 0)) {
                DownloadManager.Request req = new DownloadManager.Request(uri)
                        .setTitle(show.getAirName() + ", part " + (i + 1) + " of " + show.getUrls().size())
                        .setDescription("KFJC Podcast" )
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
                if (source.type == MediaSource.Type.ARCHIVE) {
                    setPlayState();
                } else {
                    setStopState();
                }
                break;
            case STOP:
                setStopState();
                break;
            case BUFFER:
                break;
        }
    }

    private void startPlayClockUpdater() {
        handler.removeCallbacks(playClockUpdater);
        handler.postDelayed(playClockUpdater, 0);
    }

    private void setPlayState() {
        fab.setImageResource(R.drawable.ic_stop_white_48dp);
        startPlayClockUpdater();
        displayState = PlayerState.PLAY;
    }

    private void setStopState() {
        fab.setImageResource(R.drawable.ic_play_arrow_white_48dp);
        handler.removeCallbacks(playClockUpdater);
        displayState = PlayerState.STOP;
    }
}
