package org.kfjc.android.player;

import org.kfjc.droid.R;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
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
			Double radioDevilIndex = Math.random() * 9;
			Double delayTimeMs = 20 + Math.random() * 30;
			bufferImageView.setImageBitmap(grayRadioDevils[radioDevilIndex.intValue()]);
			handler.postDelayed(this, delayTimeMs.intValue());
		}
	}

	private Handler handler;
	private Runnable runner;
	// array of devil logos at 0, 10 ... 100%
	private Bitmap[] grayRadioDevils = new Bitmap[11];
	private final Bitmap radiodevil;
	
	public GraphicsUtil(Context context) {
		this.radiodevil = BitmapFactory.decodeResource(context.getResources(), R.drawable.radiodevil);
		this.handler = new Handler();
		for (int i = 0; i <= 10; i++) {
			grayRadioDevils[i] = radioDevilBuffer(i * 10);
		}
	}
	
	public Bitmap radioDevilOff() {
		return grayRadioDevils[0];
	}
	
	public Bitmap radioDevilOn() {
		return grayRadioDevils[10];
	}
		
	public void bufferDevil(ImageView view, boolean isBuffering) {
		if (isBuffering) {
			this.runner = new BufferImageRunner(view);
			runner.run();
		} else {
			handler.removeCallbacks(this.runner);
		}
	}
	
    private Bitmap radioDevilBuffer(int bufferPercent) {
    	if (bufferPercent == 100) {
    		return radiodevil;
    	}
    	int alpha = (int) (255 * (0.1 + 0.8 * bufferPercent/100));
    	return grayscale(radiodevil, 0, alpha);
    }
    
	/**
     * Convert bitmap to the grayscale
     * http://stackoverflow.com/questions/3373860/convert-a-bitmap-to-grayscale-in-android/3391061#3391061
     *
     * @param bmpOriginal Original bitmap
     * @return Grayscale bitmap
     */
    private Bitmap grayscale(Bitmap bitmap, float saturation, int alpha) {
    	final int height = bitmap.getHeight();
        final int width = bitmap.getWidth();
        final Bitmap bmpGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        final Canvas c = new Canvas(bmpGrayscale);
        final Paint paint = new Paint();
        paint.setAlpha(alpha);
        final ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(saturation);
        final ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
        paint.setColorFilter(f);
        c.drawBitmap(bitmap, 0, 0, paint);
        return bmpGrayscale;
    }
}