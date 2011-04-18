package fi.harism.curl;

import android.graphics.Bitmap;

public interface CurlBitmapProvider {
	public Bitmap getBitmap(int width, int height, int index);

	public int getBitmapCount();
}
