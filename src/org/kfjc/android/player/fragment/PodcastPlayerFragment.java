package org.kfjc.android.player.fragment;

import android.app.Fragment;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.kfjc.android.player.R;
import org.kfjc.android.player.model.BroadcastShow;

public class PodcastPlayerFragment extends Fragment {

    public static final String BROADCAST_SHOW_KEY = "broadcastShowKey";

    private BroadcastShow show;

    private FloatingActionButton pullDownFab;
    private FloatingActionButton fab;
    private TextView airName;
    private TextView dateTime;
    private TextView podcastDetails;

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
        fab.setImageResource(R.drawable.ic_file_download_white_48dp);
        airName.setText(show.getAirName());
        dateTime.setText(show.getStartDateTime());
        podcastDetails.setText("4hrs - 288Mb");
        return view;
    }
}
