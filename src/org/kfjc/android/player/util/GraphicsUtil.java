package org.kfjc.android.player.util;

import org.kfjc.droid.R;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.widget.ImageView;

public class GraphicsUtil {

    private Handler handler = new Handler();
    private Resources resources;
    private Runnable runner;
    private Drawable[] grayRadioDevils;
    public static final int[] imagesOfTheHour = new int[] {
        R.drawable.b0, R.drawable.b0, R.drawable.b2,   // 0 1 2
        R.drawable.b2, R.drawable.b4, R.drawable.b4,    // 3 4 5
        R.drawable.b6, R.drawable.b6, R.drawable.b8,   // 6 7 8
        R.drawable.b8, R.drawable.b10, R.drawable.b10,    // 9 10 11
        R.drawable.b12, R.drawable.b12, R.drawable.b14,   // 12 13 14
        R.drawable.b14, R.drawable.b16, R.drawable.b16,    // 15 16 17
        R.drawable.b18, R.drawable.b18, R.drawable.b20,   // 18 19 20
        R.drawable.b20, R.drawable.b22, R.drawable.b22     // 21 22 23
    };

    public GraphicsUtil(Resources res) {
        this.handler = new Handler();
        this.resources = res;
        this.grayRadioDevils = new Drawable[] {
                resources.getDrawable(R.drawable.radiodevil_10),
                resources.getDrawable(R.drawable.radiodevil_25),
                resources.getDrawable(R.drawable.radiodevil_40),
                resources.getDrawable(R.drawable.radiodevil_55),
                resources.getDrawable(R.drawable.radiodevil_70),
                resources.getDrawable(R.drawable.radiodevil_85) };
    }

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
			bufferImageView.setImageDrawable(grayRadioDevils[radioDevilIndex.intValue()]);
			handler.postDelayed(this, delayTimeMs.intValue());
		}
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