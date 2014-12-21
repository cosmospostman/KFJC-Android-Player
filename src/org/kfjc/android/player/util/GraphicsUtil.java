package org.kfjc.android.player.util;

import org.kfjc.droid.R;

import android.os.Handler;
import android.widget.ImageView;

public class GraphicsUtil {
	
	/**
	 * Runner that takes an image view and at short, random intervals
	 * sets the image to one of the gray radio devils.
	 */
	class BufferImageRunner implements Runnable {
		ImageView bufferImageView;
		
		public BufferImageRunner(ImageView imageView) {
			this.bufferImageView = imageView;
		}
		
		@Override
		public void run() {
			Double radioDevilIndex = Math.random() * grayRadioDevils.length;
			Double delayTimeMs = 20 + Math.random() * 30;
			bufferImageView.setImageResource(grayRadioDevils[radioDevilIndex.intValue()]);
			handler.postDelayed(this, delayTimeMs.intValue());
		}
	}

	private Handler handler = new Handler();
	private Runnable runner;
	private int[] grayRadioDevils = {
			R.drawable.radiodevil_10,
			R.drawable.radiodevil_25,
			R.drawable.radiodevil_40,
			R.drawable.radiodevil_55,
			R.drawable.radiodevil_70,
			R.drawable.radiodevil_85};
	
	public GraphicsUtil() {
		this.handler = new Handler();
	}
	
	public int radioDevilOff() {
		return R.drawable.radiodevil_10;
	}
	
	public int radioDevilOn() {
		return R.drawable.radiodevil;
	}

	public void bufferDevil(ImageView view, boolean isBuffering) {
		if (isBuffering) {
			this.runner = new BufferImageRunner(view);
			runner.run();
		} else {
			handler.removeCallbacks(this.runner);
		}
	}

}