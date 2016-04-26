package org.kfjc.android.player.fragment;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.json.JSONArray;
import org.json.JSONException;
import org.kfjc.android.player.Constants;
import org.kfjc.android.player.R;
import org.kfjc.android.player.activity.HomeScreenInterface;
import org.kfjc.android.player.model.BroadcastArchive;
import org.kfjc.android.player.model.BroadcastHour;
import org.kfjc.android.player.model.BroadcastHourJsonImpl;
import org.kfjc.android.player.model.BroadcastShow;
import org.kfjc.android.player.util.HttpUtil;

import java.io.IOException;
import java.util.List;

public class PodcastFragment extends Fragment {

    private HomeScreenInterface homeScreen;

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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        homeScreen.setActionbarTitle(getString(R.string.fragment_title_podcast));
        homeScreen.setNavigationItemChecked(R.id.nav_podcast);
        View view = inflater.inflate(R.layout.fragment_podcast, container, false);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        homeScreen.setActionbarTitle(getString(R.string.fragment_title_podcast));
        homeScreen.setNavigationItemChecked(R.id.nav_podcast);

        new GetArchivesTask().execute();
    }

    private class GetArchivesTask extends AsyncTask<Void, Void, List<BroadcastShow>> {
        @Override
        protected List<BroadcastShow> doInBackground(Void... params) {
            BroadcastArchive archive = new BroadcastArchive();
            try {
                String archiveJson = HttpUtil.getUrl(Constants.ARCHIVES_URL);
                JSONArray archiveHours = new JSONArray(archiveJson);
                for (int i = 0; i < archiveHours.length(); i++) {
                    BroadcastHour hour = new BroadcastHourJsonImpl(archiveHours.getJSONObject(i));
                    archive.addHour(hour);
                }
            } catch (JSONException | IOException e) {}
            return archive.getShows();
        }

        @Override
        protected void onPostExecute(List<BroadcastShow> broadcastShows) {
            for (BroadcastShow show : broadcastShows) {
                Log.i("podcast", show.getPlaylistId() + " " + show.getAirName());
            }
        }
    }
}
