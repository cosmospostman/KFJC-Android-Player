package org.kfjc.android.player.fragment;

import android.os.AsyncTask;
import android.os.Bundle;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.kfjc.android.player.Constants;
import org.kfjc.android.player.R;
import org.kfjc.android.player.model.ShowDetails;
import org.kfjc.android.player.model.KfjcMediaSource;
import org.kfjc.android.player.intent.PlayerState.State;
import org.kfjc.android.player.util.DateUtil;
import org.kfjc.android.player.util.ExternalStorageUtil;
import org.kfjc.android.player.util.HttpUtil;
import org.kfjc.android.player.intent.PlayerControl;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PodcastFragment extends PlayerFragment implements PodcastViewHolder.PodcastClickDelegate {

    private static final String TAG = PodcastFragment.class.getSimpleName();

    private RecyclerView recentShowsView;
    private View recentShowsLoadingView;
    private RecyclerView savedShowsView;
    private PodcastRecyclerAdapter recentShowsAdapter;
    private List<ShowDetails> shows = Collections.emptyList();
    private View noSavedShows;
    private View getListError;
    private TextView nowPlayingLabel;
    private TextView clockLabel;
    private FloatingActionButton fab;
    private ProgressBar playProgress;
    private RelativeLayout nowPlayingPanel;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        homeScreen.setActionbarTitle(getString(R.string.fragment_title_podcast));
        homeScreen.setNavigationItemChecked(R.id.nav_podcast);
        View view = inflater.inflate(R.layout.fragment_podcast, container, false);
        recentShowsLoadingView = view.findViewById(R.id.podcastLoading);
        recentShowsView = (RecyclerView) view.findViewById(R.id.podcastRecyclerView);
        recentShowsView.addItemDecoration(new PodcastRecyclerDecorator(getActivity()));
        recentShowsView.setLayoutManager(
                new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false));
        noSavedShows = view.findViewById(R.id.noSavedShows);
        getListError = view.findViewById(R.id.cannotConnect);
        savedShowsView = (RecyclerView) view.findViewById(R.id.savedRecyclerView);
        savedShowsView.setLayoutManager(
                new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
        nowPlayingLabel = (TextView) view.findViewById(R.id.nowPlayingLabel);
        clockLabel = (TextView) view.findViewById(R.id.clockLabel);
        fab = (FloatingActionButton) view.findViewById(R.id.fab);
        fab.setOnClickListener(fabClickListener);
        playProgress = (ProgressBar) view.findViewById(R.id.playProgress);
        nowPlayingPanel = (RelativeLayout) view.findViewById(R.id.nowPlayingPanel);
        nowPlayingPanel.setOnClickListener(nowPlayingClickListener);

        return view;
    }

    private View.OnClickListener nowPlayingClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            homeScreen.loadPodcastPlayer(playerSource.show, true);
        }
    };

    private View.OnClickListener fabClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (displayState) {
                case PAUSE:
                    PlayerControl.sendAction(getActivity(), PlayerControl.INTENT_UNPAUSE);
                    break;
                case PLAY:
                    PlayerControl.sendAction(getActivity(), PlayerControl.INTENT_PAUSE);
                    break;
            }
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        homeScreen.setActionbarTitle(getString(R.string.fragment_title_podcast));
        homeScreen.setNavigationItemChecked(R.id.nav_podcast);

        recentShowsAdapter = new PodcastRecyclerAdapter(
                shows, PodcastRecyclerAdapter.Type.HORIZONTAL, PodcastFragment.this);
        recentShowsView.setAdapter(recentShowsAdapter);

        List<ShowDetails> savedShows = ExternalStorageUtil.getSavedShows(getActivity());
        getListError.setVisibility(View.GONE);
        noSavedShows.setVisibility(savedShows.size() == 0 ? View.VISIBLE : View.GONE);
        PodcastRecyclerAdapter adapter = new PodcastRecyclerAdapter(
                savedShows, PodcastRecyclerAdapter.Type.VERTICAL, PodcastFragment.this);
        savedShowsView.setAdapter(adapter);
        setArchives(shows);

        new GetArchivesTask().execute();
    }

    @Override
    void updateClock() {
        final long playerPos = homeScreen.getPlayerPosition();

        String timeStr = DateUtil.formatTime(playerPos);
        clockLabel.setText(timeStr);
        playProgress.setMax((int) playerSource.show.getTotalShowTimeMillis()/100);
        playProgress.setProgress((int)playerPos/100);
    }

    @Override
    public void onClick(ShowDetails show) {
        homeScreen.loadPodcastPlayer(show, true);
    }

    @Override
    void onStateChanged(State state, KfjcMediaSource source) {
        if (source != null && source.type == KfjcMediaSource.Type.ARCHIVE) {
            nowPlayingLabel.setText(source.show.getAirName());
            switch (state) {
                case PLAY:
                    setPlayState();
                    return;
                case PAUSE:
                    setPauseState();
                    return;
            }
        }
        setStopState();
    }

    private void setPlayState() {
        nowPlayingPanel.setVisibility(View.VISIBLE);
        playProgress.setVisibility(View.VISIBLE);
        fab.setImageResource(R.drawable.ic_pause_white_48dp);
        startPlayClockUpdater();
        displayState = State.PLAY;
    }

    private void setPauseState() {
        nowPlayingPanel.setVisibility(View.VISIBLE);
        playProgress.setVisibility(View.VISIBLE);
        fab.setImageResource(R.drawable.ic_play_arrow_white_48dp);
        updateClock();
        displayState = State.PAUSE;
    }

    private void setStopState() {
        nowPlayingPanel.setVisibility(View.GONE);
        playProgress.setVisibility(View.GONE);
        displayState = State.STOP;
    }

    private class GetArchivesTask extends AsyncTask<Void, Void, List<ShowDetails>> {
        @Override
        protected void onPreExecute() {
            getListError.setVisibility(View.GONE);
            if (shows.size() == 0) {
                recentShowsLoadingView.setVisibility(View.VISIBLE);
                recentShowsView.setVisibility(View.GONE);
            } else {
                recentShowsLoadingView.setVisibility(View.GONE);
                recentShowsView.setVisibility(View.VISIBLE);
            }
        }

        @Override
        protected List<ShowDetails> doInBackground(Void... params) {
            List<ShowDetails> shows = new ArrayList<>();
            try {
                String archiveJson = HttpUtil.getUrl(Constants.ARCHIVES_URL);
                JSONArray showList = new JSONArray(archiveJson);
                for (int i = 0; i < showList.length(); i++) {
                    ShowDetails show = new ShowDetails(showList.getJSONObject(i).toString());
                    if (!show.getPlaylistId().equals("0") && !show.hasError()) {
                        shows.add(show);
                    }
                }
            } catch (JSONException | IOException e) {}
            return shows;
        }

        @Override
        protected void onPostExecute(List<ShowDetails> showDetailses) {
            recentShowsLoadingView.setVisibility(View.GONE);
            if (showDetailses == null || showDetailses.size() == 0) {
                getListError.setVisibility(View.VISIBLE);
                recentShowsView.setVisibility(View.GONE);
            } else {
                getListError.setVisibility(View.GONE);
                recentShowsView.setVisibility(View.VISIBLE);
                setArchives(showDetailses);
            }
        }
    }

    private void setArchives(List<ShowDetails> showDetailses) {
        if (showDetailses == null || shows != null && shows.equals(showDetailses)) {
            Log.i(TAG, "No new shows to add");
            return;
        }
        shows = showDetailses;
        recentShowsAdapter = new PodcastRecyclerAdapter(
                shows, PodcastRecyclerAdapter.Type.HORIZONTAL, PodcastFragment.this);
        recentShowsView.setAdapter(recentShowsAdapter);
        if (recentShowsView.getVisibility() != View.VISIBLE) {
            Animation fadeIn = new AlphaAnimation(0, 1);
            fadeIn.setDuration(300);
            recentShowsView.startAnimation(fadeIn);
        }
    }

    @Override
    public boolean setActionBarBackArrow() {
        return false;
    }
}
