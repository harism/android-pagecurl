package fi.harism.curl;

import android.graphics.Bitmap;

/**
 * Provider for feeding 'book' with bitmaps which are used for rendering pages.
 * 
 * @author harism
 */
public interface CurlBitmapProvider {

	/**
	 * Called once new bitmap is needed. Width and height are in pixels telling
	 * the size it will be drawn on screen and following them ensures that
	 * aspect ratio remains. But it's possible to return bitmap of any size
	 * though.<br/>
	 * <br/>
	 * Index is a number between 0 and getBitmapCount() - 1.
	 */
	public Bitmap getBitmap(int width, int height, int index);

	/**
	 * Return number of pages/bitmaps available.
	 */
	public int getBitmapCount();
}
