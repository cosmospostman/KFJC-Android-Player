package org.kfjc.android.player.fragment;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.kfjc.android.player.R;

public class PodcastPlayerFragment extends Fragment {

    private TextView airName;
    private TextView podcastDetails;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_podcastplayer, container, false);
        airName = (TextView) view.findViewById(R.id.airName);
        podcastDetails = (TextView) view.findViewById(R.id.podcastDetails);

        airName.setText("Fox Populi");
        podcastDetails.setText("April 14th, 2016 6am - 4hrs - 288Mb");
        return view;
    }
}
