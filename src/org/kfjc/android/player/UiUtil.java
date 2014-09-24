package org.kfjc.android.player;

import org.kfjc.droid.R;

import android.content.Context;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ImageView;

public class UiUtil {
	
	/**
	 * @param defaultImage Image used by the button when not being touched.
	 * @param duringTouchImage Image used by the button during touch.
	 * @return An onTouchListener that when attached to an ImageView, will temporarily
	 * swap out the button's image for the duration of the touch. In addition, the touch
	 * will trigger haptic feedback.
	 */
	public static OnTouchListener makeButtonTouchListener(
			final int defaultImage, final int duringTouchImage) {
		return new OnTouchListener() {
			@Override
			public boolean onTouch(View view, MotionEvent event) {
				ImageView imageView = (ImageView) view;
				switch (event.getAction() & MotionEvent.ACTION_MASK) {
	                case MotionEvent.ACTION_DOWN:
	                case MotionEvent.ACTION_POINTER_DOWN:
	                	imageView.setImageResource(duringTouchImage);
	    				view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
	                    return true;
	                case MotionEvent.ACTION_UP:
	                case MotionEvent.ACTION_POINTER_UP:
	                	imageView.setImageResource(defaultImage);
	    				view.performClick();
	                    return true;
                }
            return false;
			}
		};
	};
	
	/**
	 * @param context Context (contains string resources)
	 * @param nowPlaying
	 * @return The DJ Name if non-null/non-empty, otherwise app name.
	 */
	public static String getAppTitle(Context context, NowPlayingInfo nowPlaying) {
		String djName = nowPlaying.getDjAirName();
		if (djName != null && !djName.isEmpty()) {
			return djName;
		}
		return context.getString(R.string.app_name);
	}
}
