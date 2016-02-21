package org.kfjc.android.player.dialog;

import android.support.v4.app.DialogFragment;
import android.widget.LinearLayout;

public class KfjcDialog extends DialogFragment {

    @Override
    public void onStart() {
        super.onStart();
        getDialog().getWindow().setLayout(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
    }
}
