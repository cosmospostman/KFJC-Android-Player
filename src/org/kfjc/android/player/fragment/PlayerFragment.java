package org.kfjc.android.player.fragment;

import android.app.Fragment;
import android.content.Context;

import org.kfjc.android.player.activity.HomeScreenInterface;
import org.kfjc.android.player.model.MediaSource;

public abstract class PlayerFragment extends Fragment {

    protected HomeScreenInterface homeScreen;
    protected PlayerState playerState;
    protected PlayerState displayState;
    protected MediaSource playerSource;

    public enum PlayerState {
        PLAY,
        STOP,
        BUFFER
    }

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
    public void onResume() {
        super.onResume();
        homeScreen.syncState();
    }

    public void setState(PlayerState state, MediaSource source) {
        playerState = state;
        playerSource = source;
        if (!this.isAdded()) {
            return;
        }
        onStateChanged(state, source);
    }

    abstract void onStateChanged(PlayerState state, MediaSource source);

}
