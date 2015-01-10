package org.kfjc.android.player.util;

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

}
