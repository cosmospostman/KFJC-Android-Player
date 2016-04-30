package org.kfjc.android.player.fragment;

import android.app.Fragment;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.util.Log;
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
import org.kfjc.android.player.util.ExternalStorageUtil;
import org.kfjc.android.player.util.HttpUtil;

import java.io.File;
import java.io.IOException;

public class PodcastPlayerFragment extends Fragment {

    public static final String BROADCAST_SHOW_KEY = "broadcastShowKey";

    private BroadcastShow show;

    private HomeScreenInterface homeScreen;
    private FloatingActionButton pullDownFab;
    private FloatingActionButton fab;
    private TextView airName;
    private TextView dateTime;
    private TextView podcastDetails;

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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Bundle bundle = getArguments();
        show = bundle.getParcelable(BROADCAST_SHOW_KEY);

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

        fab.setImageResource(R.drawable.ic_file_download_white_48dp);
        airName.setText(show.getAirName());
        dateTime.setText(show.getTimestampString());
        podcastDetails.setText(show.getUrls().size() + " hour show, 258Mb download.");
        return view;
    }

    private void checkState() {
        if (! ExternalStorageUtil.getPodcastDir(show.getPlaylistId()).exists()) {
            // Show preview and download state
        }
    }

    private void onFabClicked() {
        homeScreen.requestExternalWritePermission();
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
    }
}
