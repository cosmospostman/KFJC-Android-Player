package org.kfjc.android.player.fragment;

import android.app.Activity;
import android.database.DataSetObserver;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.kfjc.android.player.Constants;
import org.kfjc.android.player.R;
import org.kfjc.android.player.activity.HomeScreenInterface;
import org.kfjc.android.player.model.Playlist;
import org.kfjc.android.player.model.PlaylistJsonImpl;
import org.kfjc.android.player.model.TrackInfo;
import org.kfjc.android.player.util.HttpUtil;

public class PlaylistFragment extends Fragment {

    private static final String TAG = PlaylistFragment.class.getSimpleName();

    private HomeScreenInterface homeScreen;

    private TextView djNameView;
    private TextView timestringView;
    private LinearLayout playlistListView;
    private ListAdapter playlistListAdapter;

    Playlist playlist;

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

        makeFetchTask().execute();
        return view;
    }

    private AsyncTask<Void, Void, Playlist> makeFetchTask() {
        return new AsyncTask<Void, Void, Playlist>() {
            protected Playlist doInBackground(Void... unusedParams) {
                Log.i(TAG, "Fetching playlist");
                try {
                    String jsonPlaylistString = HttpUtil.getUrl(Constants.PLAYLIST_URL);
                    return new PlaylistJsonImpl(jsonPlaylistString);
                } catch (Exception e) {
                    Log.e(TAG, e.getMessage());
                    return new PlaylistJsonImpl("");
                }
            }

            protected void onPostExecute(Playlist fetchedPlaylist) {
                playlist = fetchedPlaylist;
                djNameView.setText(playlist.getDjName());
                timestringView.setText(playlist.getTime());
                PlaylistView.buildPlaylistLayout(getActivity(), playlistListView, fetchedPlaylist.getTrackEntries());
             }
        };
    }

    public class PlaylistItemViewHolder extends RecyclerView.ViewHolder {

        private TextView timeView;
        private TextView trackInfoView;
        public PlaylistItemViewHolder(View itemView) {
            super(itemView);
            timeView = (TextView) itemView.findViewById(R.id.ple_time);
            trackInfoView = (TextView) itemView.findViewById(R.id.ple_trackinfo);
        }

        public void bindPlaylistEntry(Playlist.PlaylistEntry entry) {
            Spanned trackInfoSpan = Html.fromHtml(
                    String.format("<b>%s</b> %s", entry.getArtist(), entry.getTrack()));
            timeView.setText(entry.getTime());
            trackInfoView.setText(trackInfoSpan);
        }
    }

    public class PlaylistAdapter extends RecyclerView.Adapter<PlaylistItemViewHolder> {

        @Override
        public PlaylistItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
            View view = layoutInflater.inflate(R.layout.list_playlistentry, parent, false);
            return new PlaylistItemViewHolder(view);
        }

        @Override
        public void onBindViewHolder(PlaylistItemViewHolder holder, int position) {
            int adapterPosition = holder.getAdapterPosition();
            Playlist.PlaylistEntry entry = playlist.getTrackEntries().get(adapterPosition);
            holder.bindPlaylistEntry(entry);
        }

        @Override
        public int getItemCount() {
            if (playlist == null || playlist.getTrackEntries() == null) {
                return 0;
            }
            return playlist.getTrackEntries().size();
        }
    }

}
