/*
   Copyright 2013 Harri Smatt

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

package fi.harism.curl;

import android.graphics.Bitmap;
import android.graphics.Color;

/**
 * Storage class for page textures, blend colors and possibly some other values
 * in the future.
 * 
 * @author harism
 */
public class CurlPage {

	public static final int SIDE_BACK = 2;
	public static final int SIDE_BOTH = 3;
	public static final int SIDE_FRONT = 1;

	private Bitmap mBitmapBack;
	private Bitmap mBitmapFront;
	private boolean mBitmapsChanged;
	private int mColorBack;
	private int mColorFront;

	/**
	 * Default constructor.
	 */
	public CurlPage() {
		reset();
	}

	/**
	 * Getter for bitmap.
	 */
	public Bitmap getBitmap(int side) {
		switch (side) {
		case SIDE_FRONT:
			return mBitmapFront;
		case SIDE_BACK:
			return mBitmapBack;
		default:
			return null;
		}
	}

	public boolean getBitmapsChanged() {
		return mBitmapsChanged;
	}

	/**
	 * Getter for color.
	 */
	public int getColor(int side) {
		switch (side) {
		case SIDE_FRONT:
			return mColorFront;
		default:
			return mColorBack;
		}
	}

	/**
	 * Recycles and frees underlying Bitmaps.
	 */
	public void recycle() {
		if (mBitmapFront != null) {
			mBitmapFront.recycle();
		}
		mBitmapFront = Bitmap.createBitmap(1, 1, Bitmap.Config.RGB_565);
		mBitmapFront.eraseColor(mColorFront);
		if (mBitmapBack != null) {
			mBitmapBack.recycle();
		}
		mBitmapBack = Bitmap.createBitmap(1, 1, Bitmap.Config.RGB_565);
		mBitmapBack.eraseColor(mColorBack);
		mBitmapsChanged = false;
	}

	/**
	 * Resets this CurlPage into its initial state.
	 */
	public void reset() {
		mColorBack = Color.TRANSPARENT;
		mColorFront = Color.TRANSPARENT;
		recycle();
		mBitmapsChanged = true;
	}

	/**
	 * Setter for Bitmaps.
	 */
	public void setBitmap(Bitmap texture, int side) {
		if (texture == null) {
			texture = Bitmap.createBitmap(1, 1, Bitmap.Config.RGB_565);
			if (side == SIDE_BACK) {
				texture.eraseColor(mColorBack);
			} else {
				texture.eraseColor(mColorFront);
			}
		}
		switch (side) {
		case SIDE_FRONT:
			if (mBitmapFront != null)
				mBitmapFront.recycle();
			mBitmapFront = texture;
			break;
		case SIDE_BACK:
			if (mBitmapBack != null)
				mBitmapBack.recycle();
			mBitmapBack = texture;
			break;
		case SIDE_BOTH:
			if (mBitmapFront != null)
				mBitmapFront.recycle();
			if (mBitmapBack != null)
				mBitmapBack.recycle();
			mBitmapFront = mBitmapBack = texture;
			break;
		}
		mBitmapsChanged = true;
	}

	/**
	 * Setter blend color.
	 */
	public void setColor(int color, int side) {
		switch (side) {
		case SIDE_FRONT:
			mColorFront = color;
			break;
		case SIDE_BACK:
			mColorBack = color;
			break;
		default:
			mColorFront = mColorBack = color;
			break;
		}
	}

}
