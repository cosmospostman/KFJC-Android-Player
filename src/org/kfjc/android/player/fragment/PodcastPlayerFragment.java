package org.kfjc.android.player.fragment;

import android.app.DownloadManager;
import android.app.Fragment;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.kfjc.android.player.Constants;
import org.kfjc.android.player.R;
import org.kfjc.android.player.activity.HomeScreenInterface;
import org.kfjc.android.player.model.BroadcastShow;
import org.kfjc.android.player.model.Playlist;
import org.kfjc.android.player.model.PlaylistJsonImpl;
import org.kfjc.android.player.model.Stream;
import org.kfjc.android.player.util.ExternalStorageUtil;
import org.kfjc.android.player.util.HttpUtil;

import java.io.File;
import java.io.IOException;

public class PodcastPlayerFragment extends Fragment {

    public static final String BROADCAST_SHOW_KEY = "broadcastShowKey";

    private enum State {
        PREVIEW,
        SAVED
    }

    private BroadcastShow show;
    private DownloadManager downloadManager;
    private State state;
    private Handler handler = new Handler();

    private HomeScreenInterface homeScreen;
    private FloatingActionButton pullDownFab;
    private FloatingActionButton fab;
    private TextView airName;
    private TextView dateTime;
    private TextView podcastDetails;
    private Runnable playClockUpdater = new Runnable() {
        @Override public void run() {
            long pos = homeScreen.getPlayerPosition();
            long dur = homeScreen.getPlayerDuration();
            podcastDetails.setText(pos + ":" + dur);
            handler.postDelayed(this, 1000);
        }
    };

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            homeScreen = (HomeScreenInterface) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.getClass().getSimpleName() + " must implement "
                    + HomeScreenInterface.class.getSimpleName());
        }
        downloadManager = (DownloadManager) getActivity().getSystemService(Context.DOWNLOAD_SERVICE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        homeScreen.setActionbarTitle(getString(R.string.fragment_title_podcast));
        View view = inflater.inflate(R.layout.fragment_podcastplayer, container, false);

        pullDownFab = (FloatingActionButton) view.findViewById(R.id.pullDownButton);
        fab = (FloatingActionButton) view.findViewById(R.id.fab);
        airName = (TextView) view.findViewById(R.id.airName);
        dateTime = (TextView) view.findViewById(R.id.podcastDateTime);
        podcastDetails = (TextView) view.findViewById(R.id.podcastDetails);

        pullDownFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().getFragmentManager().beginTransaction()
                        .setCustomAnimations(R.animator.fade_in_down, R.animator.fade_out_down)
                        .replace(R.id.home_screen_main_fragment, new PodcastFragment())
                        .addToBackStack(null)
                        .commit();
            }
        });
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onFabClicked();
            }
        });

        Bundle bundle = getArguments();
        if (bundle != null) {
            show = bundle.getParcelable(BROADCAST_SHOW_KEY);
            airName.setText(show.getAirName());
            dateTime.setText(show.getTimestampString());
            checkState();
        }

        return view;
    }

    private void checkState() {
        if (! ExternalStorageUtil.getPodcastDir(show.getPlaylistId()).exists()) {
            // Show preview and download state
            fab.setImageResource(R.drawable.ic_file_download_white_48dp);
            podcastDetails.setText(show.getUrls().size() + " hour show, 258Mb download.");
            state = State.PREVIEW;
        } else if (ExternalStorageUtil.hasAllContent(show)) {
            // Show play state
            fab.setImageResource(R.drawable.ic_play_arrow_white_48dp);
            state = State.SAVED;
        }
    }

    private void onFabClicked() {
        switch (state) {
            case PREVIEW:
                homeScreen.requestExternalWritePermission();
                break;
            case SAVED:
                File f = ExternalStorageUtil.getSavedArchivesForShow(show).get(0);
                try {
                    homeScreen.playArchive(new Stream(f.getCanonicalPath(), Stream.Format.MP3));
                    handler.postDelayed(playClockUpdater, 0);
                } catch (IOException e) {}
                break;
        }
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
}
