package org.kfjc.android.player.dialog;

import android.app.Activity;
import android.app.DialogFragment;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.TypefaceSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.kfjc.android.player.Constants;
import org.kfjc.android.player.R;
import org.kfjc.android.player.model.Playlist;
import org.kfjc.android.player.model.PlaylistJsonImpl;
import org.kfjc.android.player.util.ExternalStorageUtil;
import org.kfjc.android.player.util.HttpUtil;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class PlaylistDialog extends DialogFragment {

    private static final String TAG = PlaylistDialog.class.getSimpleName();
    public static final String PLAYLIST_ID_KEY = "playlistIdKey";
    public static final String DJ_NAME_KEY = "djNameKey";
    public static final String TIME_KEY = "timeKey";

    private TextView djNameView;
    private TextView timestringView;
    private LinearLayout playlistListView;
    private ProgressBar loadingProgress;

    private String playlistId;
    private String djName;
    private String time;

    public static PlaylistDialog newInstance(String djName, String time, String playlistId) {
        PlaylistDialog f = new PlaylistDialog();
        Bundle args = new Bundle();
        args.putString(PLAYLIST_ID_KEY, playlistId);
        args.putString(DJ_NAME_KEY, djName);
        args.putString(TIME_KEY, time);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        playlistId = getArguments().getString(PLAYLIST_ID_KEY);
        djName = getArguments().getString(DJ_NAME_KEY);
        time = getArguments().getString(TIME_KEY);
    }

    @Override
    public void onStart() {
        super.onStart();
        getDialog().getWindow().setLayout(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
    }

    @Override
    public void onPause() {
        super.onPause();
        dismiss();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.layout_playlist, container, false);

        djNameView = (TextView) view.findViewById(R.id.pl_djname);
        djNameView.setText(djName);
        timestringView = (TextView) view.findViewById(R.id.pl_timestring);
        timestringView.setText(time);
        playlistListView = (LinearLayout) view.findViewById(R.id.playlist_list_view);
        loadingProgress = (ProgressBar) view.findViewById(R.id.loadingProgress);

        View closeButton = view.findViewById(R.id.closeButton);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        loadPlaylist(playlistId);
        return view;
    }

    private void loadPlaylist(final String playlistId) {
        new AsyncTask<Void, Void, Playlist>() {

            @Override
            protected Playlist doInBackground(Void... params) {
                File playlistFile = ExternalStorageUtil.getPlaylistFile(playlistId);
                if (playlistFile.exists()) {
                    String playlistString = ExternalStorageUtil.readFile(playlistFile);
                    Playlist playlist = new PlaylistJsonImpl(playlistString);
                    if (! playlist.hasError()) {
                        return playlist;
                    }
                }
                try {
                    String playlistString = HttpUtil.getUrl(Constants.PLAYLIST_URL + playlistId, true);
                    return new PlaylistJsonImpl(playlistString);
                } catch (IOException e) {
                    return new PlaylistJsonImpl("");
                    //TODO: handle errors, display a message
                }
            }

            @Override
            protected void onPostExecute(Playlist playlist) {
                updatePlaylist(playlist);
                loadingProgress.setVisibility(View.GONE);
            }
        }.execute();
    }

    public void updatePlaylist(Playlist playlist) {
        if (!isAdded()) {
            return;
        }
        if (playlist == null || playlist.hasError()) {
            djNameView.setText(R.string.status_playlist_unavailable);
            return;
        }
        djNameView.setText(emptyDefault(playlist.getDjName()));
        playlistListView.setVisibility(View.VISIBLE);
        buildPlaylistLayout(getActivity(), playlistListView, playlist.getTrackEntries());

        Animation fadeIn = new AlphaAnimation(0, 1);
        fadeIn.setDuration(300);
        playlistListView.startAnimation(fadeIn);
    }

    private static String emptyDefault(String s) {
        if (s == null) {
            return "";
        }
        return s;
    }

    private void buildPlaylistLayout(
            Activity activity, LinearLayout layout, List<Playlist.PlaylistEntry> entries) {
        layout.removeAllViews();
        if (entries == null) {
            return;
        }
        for (final Playlist.PlaylistEntry e : entries) {
            LayoutInflater inflater = activity.getLayoutInflater();
            View holderView;
            if (e.isEmpty()) {
                holderView = inflater.inflate(R.layout.list_playlistempty, layout, false);
            } else {
                holderView = inflater.inflate(R.layout.list_playlistentry, layout, false);
                TextView timeView = (TextView) holderView.findViewById(R.id.ple_time);
                TextView trackInfoView = (TextView) holderView.findViewById(R.id.ple_trackinfo);
                if (!TextUtils.isEmpty(e.getTime())) {
                    timeView.setText(e.getTime());
                    timeView.setVisibility(View.VISIBLE);
                }

                String spacer = TextUtils.isEmpty(e.getArtist()) ? "" : "  ";
                SpannableStringBuilder ssb = new SpannableStringBuilder(
                        e.getArtist() + spacer + e.getTrack());
                ssb.setSpan(
                        new TypefaceSpan("sans-serif-thin"),
                        e.getArtist().length(),
                        ssb.length(),
                        SpannableString.SPAN_INCLUSIVE_INCLUSIVE);
                trackInfoView.setText(ssb);

                holderView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showTrackDetails(e);
                    }
                });
            }
            layout.addView(holderView);
        }
    }

    private void showTrackDetails(Playlist.PlaylistEntry entry) {
        TrackDetailsDialog detailsDialog = TrackDetailsDialog.newInstance(entry);
        detailsDialog.show(getActivity().getFragmentManager(), "settings");
    }
}
