package org.kfjc.android.player.fragment;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;

import org.kfjc.android.player.activity.HomeScreenDrawerActivity;

public abstract class KfjcFragment extends Fragment {

    public static final String TAG = KfjcFragment.class.getSimpleName();

    protected HomeScreenDrawerActivity homeScreen;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            homeScreen = (HomeScreenDrawerActivity) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.getClass().getSimpleName() + " must implement "
                    + HomeScreenDrawerActivity.class.getSimpleName());
        }
    }

    /**
     * Needed for Android versions prior to 23
     */
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            homeScreen = (HomeScreenDrawerActivity) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.getClass().getSimpleName() + " must implement "
                    + HomeScreenDrawerActivity.class.getSimpleName());
        }
    }

    public String getFragmentTag() {
        return TAG;
    }

    public abstract boolean setActionBarBackArrow();
}