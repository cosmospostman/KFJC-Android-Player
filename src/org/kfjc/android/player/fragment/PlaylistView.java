package org.kfjc.android.player.fragment;

import android.app.Activity;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.kfjc.android.player.R;
import org.kfjc.android.player.model.Playlist;

import java.util.List;

public class PlaylistView {
    public static void buildPlaylistLayout(
            Activity activity, LinearLayout layout, List<Playlist.PlaylistEntry> entries) {
        layout.removeAllViews();
        if (entries == null) {
            return;
        }
        for (Playlist.PlaylistEntry e : entries) {
            LayoutInflater inflater = activity.getLayoutInflater();
            View holderView = inflater.inflate(R.layout.list_playlistentry, null);
            FrameLayout emptyGroup = (FrameLayout) holderView.findViewById(R.id.ple_empty);
            LinearLayout nonemptyGroup = (LinearLayout) holderView.findViewById(R.id.ple_nonempty);
            if (isEmptyEntry(e)) {
                emptyGroup.setVisibility(View.VISIBLE);
            } else {
                nonemptyGroup.setVisibility(View.VISIBLE);
                TextView timeView = (TextView) holderView.findViewById(R.id.ple_time);
                TextView trackInfoView = (TextView) holderView.findViewById(R.id.ple_trackinfo);
                timeView.setText(e.getTime());
                Spanned trackInfoSpan = Html.fromHtml(
                        String.format("<b>%s</b> %s", e.getArtist(), e.getTrack()));
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
