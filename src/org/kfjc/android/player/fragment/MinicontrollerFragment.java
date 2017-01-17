package org.kfjc.android.player.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.kfjc.android.player.R;
import org.kfjc.android.player.intent.PlayerState;
import org.kfjc.android.player.model.KfjcMediaSource;
import org.kfjc.android.player.util.DateUtil;

public class MinicontrollerFragment extends PlayerFragment {

    protected PlayerState.State displayState;
    protected PlayerState.State playerState;

    private TextView nowPlayingLabel;
    private TextView clockLabel;
    private FloatingActionButton fab;
    private ProgressBar playProgress;
    private RelativeLayout nowPlayingPanel;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_minicontroller, container, false);

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
                    homeScreen.pausePlayback(true);
                    break;
                case PLAY:
                    homeScreen.pausePlayback(false);
                    break;
            }
        }
    };

    private void setPlayState() {
        nowPlayingPanel.setVisibility(View.VISIBLE);
        playProgress.setVisibility(View.VISIBLE);
        fab.setImageResource(R.drawable.ic_pause_white_48dp);
        startPlayClockUpdater();
        displayState = PlayerState.State.PLAY;
    }

    private void setPauseState() {
        nowPlayingPanel.setVisibility(View.VISIBLE);
        playProgress.setVisibility(View.VISIBLE);
        fab.setImageResource(R.drawable.ic_play_arrow_white_48dp);
        updateClock();
        displayState = PlayerState.State.PAUSE;
    }

    private void setStopState() {
        nowPlayingPanel.setVisibility(View.GONE);
        playProgress.setVisibility(View.GONE);
        displayState = PlayerState.State.STOP;
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
    void onStateChanged(PlayerState.State state, KfjcMediaSource source) {
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

    @Override
    public boolean setActionBarBackArrow() {
        return false;
    }
}
