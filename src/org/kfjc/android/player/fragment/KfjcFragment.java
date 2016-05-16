package org.kfjc.android.player.fragment;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;

import org.kfjc.android.player.activity.HomeScreenInterface;

public class KfjcFragment extends Fragment {

    protected HomeScreenInterface homeScreen;

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

    /**
     * Needed for Android versions prior to 23
     */
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
}