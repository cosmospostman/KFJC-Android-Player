package org.kfjc.android.player.util;

import org.kfjc.android.player.NowPlayingInfo;
import org.kfjc.droid.R;

import android.content.Context;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

public class UiUtil {
	
	/**An onTouchListener that triggers haptic feedback.
	 */
	public static OnTouchListener buttonTouchListener = new OnTouchListener() {
		@Override
		public boolean onTouch(View view, MotionEvent event) {
			switch (event.getAction() & MotionEvent.ACTION_MASK) {
			case MotionEvent.ACTION_DOWN:
			case MotionEvent.ACTION_POINTER_DOWN:
				view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
				return true;
			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_POINTER_UP:
				view.performClick();
				return true;
			}
			return false;
		}
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
