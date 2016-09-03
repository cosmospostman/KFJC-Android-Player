package org.kfjc.android.player.fragment;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
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
import org.kfjc.android.player.model.ShowListBuilder;
import org.kfjc.android.player.model.BroadcastHour;
import org.kfjc.android.player.model.BroadcastHourJsonImpl;
import org.kfjc.android.player.model.ShowDetails;
import org.kfjc.android.player.model.MediaSource;
import org.kfjc.android.player.service.StreamService.PlayerState;
import org.kfjc.android.player.util.DateUtil;
import org.kfjc.android.player.util.ExternalStorageUtil;
import org.kfjc.android.player.util.HttpUtil;
import org.kfjc.android.player.intent.PlayerControlIntent;


import java.io.IOException;
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
                    PlayerControlIntent.sendAction(getActivity(), PlayerControlIntent.INTENT_UNPAUSE);
                    break;
                case PLAY:
                    PlayerControlIntent.sendAction(getActivity(), PlayerControlIntent.INTENT_PAUSE);
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

        List<ShowDetails> savedShows = ExternalStorageUtil.getSavedShows();
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
        final long padding = playerSource.show.getHourPaddingTimeMillis();

        String timeStr = DateUtil.formatTime(playerPos - padding);
        clockLabel.setText(timeStr);

        playProgress.setMax((int) playerSource.show.getTotalShowTimeMillis()/100);
        playProgress.setProgress((int)playerPos/100);
    }

    @Override
    public void onClick(ShowDetails show) {
        homeScreen.loadPodcastPlayer(show, true);
    }

    @Override
    void onStateChanged(PlayerState state, MediaSource source) {
        if (source != null && source.type == MediaSource.Type.ARCHIVE) {
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
    }

    private void setPauseState() {
        nowPlayingPanel.setVisibility(View.VISIBLE);
        playProgress.setVisibility(View.VISIBLE);
        fab.setImageResource(R.drawable.ic_play_arrow_white_48dp);
        updateClock();
    }

    private void setStopState() {
        nowPlayingPanel.setVisibility(View.GONE);
        playProgress.setVisibility(View.GONE);
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
            ShowListBuilder archiveBuilder = ShowListBuilder.newInstance();
            try {
                String archiveJson = HttpUtil.getUrl(Constants.ARCHIVES_URL);
                JSONArray archiveHours = new JSONArray(archiveJson);
                for (int i = 0; i < archiveHours.length(); i++) {
                    BroadcastHour hour = new BroadcastHourJsonImpl(archiveHours.getJSONObject(i));
                    archiveBuilder.addHour(hour);
                }
            } catch (JSONException | IOException e) {}
            return archiveBuilder.build();
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
}
