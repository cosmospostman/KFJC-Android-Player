package org.kfjc.android.player.fragment;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.kfjc.android.player.R;
import org.kfjc.android.player.activity.HomeScreenInterface;

public class PlaylistFragment extends Fragment {

    private HomeScreenInterface homeScreen;


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
        homeScreen.setActionbarTitle(getString(R.string.fragment_title_stream));
        View view = inflater.inflate(R.layout.fragment_playlist, container, false);
        return view;
    }

}
