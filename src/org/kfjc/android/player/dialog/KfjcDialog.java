package org.kfjc.android.player.dialog;

import android.app.DialogFragment;
import android.view.WindowManager;
import android.widget.LinearLayout;

public class KfjcDialog extends DialogFragment {
    @Override
    public void onStart() {
        super.onStart();
        getDialog().getWindow().setLayout(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        WindowManager.LayoutParams layoutParams = getDialog().getWindow().getAttributes();
        layoutParams.dimAmount = .6f;
        getDialog().getWindow().setAttributes(layoutParams);
        getDialog().getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
    }

    @Override
    public void onPause() {
        super.onPause();
        dismiss();
    }
}
