package org.kfjc.android.player.fragment;

import android.app.Activity;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
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
        if (playlist.hasError()) {
            djNameView.setText(R.string.status_playlist_unavailable);
            return;
        }
        djNameView.setText(emptyDefault(playlist.getDjName()));
        timestringView.setText(emptyDefault(playlist.getTime()));
        buildPlaylistLayout(getActivity(), playlistListView, playlist.getTrackEntries());
    }

    private static String emptyDefault(String s) {
        if (s == null) {
            return "";
        }
        return s;
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
            }
            layout.addView(holderView);
        }
    }

    private static boolean isEmptyEntry(Playlist.PlaylistEntry e) {
        return TextUtils.isEmpty(e.getAlbum())
                && TextUtils.isEmpty(e.getArtist())
                && TextUtils.isEmpty(e.getTime())
                && TextUtils.isEmpty(e.getTrack());
    }
}
