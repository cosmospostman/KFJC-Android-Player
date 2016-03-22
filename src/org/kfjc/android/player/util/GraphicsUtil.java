package org.kfjc.android.player.util;

import android.content.res.Resources;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.os.Handler;
import android.widget.ImageView;

public class GraphicsUtil {

    private Handler handler = new Handler();
    private Runnable runner;

    public GraphicsUtil() {
        this.handler = new Handler();
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
			Double delayTimeMs = 20 + Math.random() * 30;
			bufferImageView.setColorFilter(getMatrix((float)Math.random(), (float)Math.random()));
			handler.postDelayed(this, delayTimeMs.intValue());
		}
	}

	public void radioDevilOff(ImageView imageView) {
		imageView.setColorFilter(getMatrix(0.2f, 0.3f));
	}

	public void radioDevilOn(ImageView imageView) {
		imageView.setColorFilter(new ColorFilter());
	}

	public void bufferDevil(ImageView view, boolean isBuffering) {
		if (isBuffering) {
			this.runner = new BufferImageRunner(view);
			runner.run();
		} else {
			handler.removeCallbacks(this.runner);
		}
	}

	private ColorMatrixColorFilter getMatrix(float b, float alpha) {
		int neg = Math.random() > 0.5 ? -1 : 1;
		ColorMatrix matrix = new ColorMatrix();
		matrix.setSaturation(0);
		float[] mat = new float[]
				{
						1,0,0,0,neg*255*b,
						0,1,0,0,neg*255*b,
						0,0,1,0,neg*255*b,
						0,0,0,alpha,0
				};
		matrix.postConcat(new ColorMatrix(mat));

		return new ColorMatrixColorFilter(matrix);
	}

}