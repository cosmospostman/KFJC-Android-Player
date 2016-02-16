package org.kfjc.android.player.fragment;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.kfjc.android.player.Constants;
import org.kfjc.android.player.R;
import org.kfjc.android.player.activity.HomeScreenInterface;
import org.kfjc.android.player.model.Playlist;
import org.kfjc.android.player.model.PlaylistJsonImpl;
import org.kfjc.android.player.util.HttpUtil;

import java.util.List;

public class PlaylistFragment extends Fragment {

    private static final String TAG = PlaylistFragment.class.getSimpleName();

    private HomeScreenInterface homeScreen;

    private TextView djNameView;
    private TextView timestringView;
    private LinearLayout playlistListView;

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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        homeScreen.setActionbarTitle(getString(R.string.fragment_title_playlist));
        View view = inflater.inflate(R.layout.fragment_playlist, container, false);

        djNameView = (TextView) view.findViewById(R.id.pl_djname);
        timestringView = (TextView) view.findViewById(R.id.pl_timestring);
        playlistListView = (LinearLayout) view.findViewById(R.id.playlist_list_view);

        updatePlaylist(homeScreen.getLatestPlaylist());
        return view;
    }

    public void updatePlaylist(Playlist playlist) {
        if (!isAdded()) {
            return;
        }
        djNameView.setText(playlist.getDjName());
        timestringView.setText(playlist.getTime());
        buildPlaylistLayout(getActivity(), playlistListView, playlist.getTrackEntries());
    }

    public static void buildPlaylistLayout(
            Activity activity, LinearLayout layout, List<Playlist.PlaylistEntry> entries) {
        layout.removeAllViews();
        if (entries == null) {
            return;
        }
        for (Playlist.PlaylistEntry e : entries) {
            LayoutInflater inflater = activity.getLayoutInflater();
            View holderView;
            if (isEmptyEntry(e)) {
                holderView = inflater.inflate(R.layout.list_playlistempty, null);
            } else {
                holderView = inflater.inflate(R.layout.list_playlistentry, null);
                TextView timeView = (TextView) holderView.findViewById(R.id.ple_time);
                TextView trackInfoView = (TextView) holderView.findViewById(R.id.ple_trackinfo);
                if (!TextUtils.isEmpty(e.getTime())) {
                    timeView.setText(e.getTime());
                    timeView.setVisibility(View.VISIBLE);
                }
                String spacer = TextUtils.isEmpty(e.getArtist()) ? "" : " &nbsp ";
                Spanned trackInfoSpan = Html.fromHtml(
                        String.format("<b>%s</b>%s%s", e.getArtist(), spacer, e.getTrack()));
                trackInfoView.setText(trackInfoSpan);
            }
            layout.addView(holderView);
        }
    }

    private static boolean isEmptyEntry(Playlist.PlaylistEntry e) {
        return TextUtils.isEmpty(e.getAlbum())
                && TextUtils.isDigitsOnly(e.getArtist())
                && TextUtils.isEmpty(e.getTime())
                && TextUtils.isEmpty(e.getTrack());
    }
}
