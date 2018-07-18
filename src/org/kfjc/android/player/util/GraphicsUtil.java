package org.kfjc.android.player.util;

import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.os.Handler;
import android.widget.ImageView;

public class GraphicsUtil {

    private static Handler handler;
    private BufferImageRunner runner;

    public GraphicsUtil() {
        this.handler = new Handler();
    }

    /**
	 * Runner that takes an image view and at short, random intervals
	 * sets the image to one of the gray radio devils.
	 */
	static class BufferImageRunner implements Runnable {
		ImageView bufferImageView;
		static boolean continueRunning = true;
		public BufferImageRunner(ImageView imageView) {
			this.bufferImageView = imageView;
		}
		
		@Override
		public void run() {
			if (!this.continueRunning) {
				return;
			}
			Double delayTimeMs = 20 + Math.random() * 30;
			bufferImageView.setColorFilter(getMatrix((float)Math.random(), (float)Math.random()));
			handler.postDelayed(this, delayTimeMs.intValue());
		}

		public void stop() {
			continueRunning = false;
		}

		public void reset() {
			continueRunning = true;
		}
	}

	public void radioDevilOff(ImageView imageView) {
		imageView.setColorFilter(getMatrix(0.2f, 0.2f));
	}

	public void radioDevilOn(ImageView imageView) {
		imageView.clearColorFilter();
	}

	public void bufferDevil(ImageView view, boolean isBuffering) {
		if (isBuffering) {
			this.runner = new BufferImageRunner(view);
			runner.reset();
			runner.run();
		} else {
		    if (this.runner != null) {
                this.runner.stop();
				handler.removeCallbacks(this.runner);
            }
		}
	}

	private static ColorMatrixColorFilter getMatrix(float b, float alpha) {
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