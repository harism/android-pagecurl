/*
   Copyright 2012 Harri Smatt

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

	private int mColorBack;
	private int mColorFront;
	private Bitmap mTextureBack;
	private Bitmap mTextureFront;
	private boolean mTexturesChanged;

	/**
	 * Default constructor.
	 */
	public CurlPage() {
		reset();
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
	 * Getter for textures.
	 * 
	 */
	public Bitmap getTexture(int side) {
		switch (side) {
		case SIDE_FRONT:
			return mTextureFront;
		default:
			return mTextureBack;
		}
	}

	/**
	 * Returns true if textures have changed.
	 */
	public boolean getTexturesChanged() {
		return mTexturesChanged;
	}

	/**
	 * Returns true if back siding texture exists and it differs from front
	 * facing one.
	 */
	public boolean hasBackTexture() {
		return !mTextureFront.equals(mTextureBack);
	}

	/**
	 * Recycles and frees underlying Bitmaps.
	 */
	public void recycle() {
		if (mTextureFront != null) {
			mTextureFront.recycle();
		}
		mTextureFront = Bitmap.createBitmap(1, 1, Bitmap.Config.RGB_565);
		mTextureFront.eraseColor(mColorFront);
		if (mTextureBack != null) {
			mTextureBack.recycle();
		}
		mTextureBack = Bitmap.createBitmap(1, 1, Bitmap.Config.RGB_565);
		mTextureBack.eraseColor(mColorBack);
		mTexturesChanged = false;
	}

	/**
	 * Resets this CurlPage into its initial state.
	 */
	public void reset() {
		mColorBack = Color.WHITE;
		mColorFront = Color.WHITE;
		recycle();
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

	/**
	 * Setter for textures.
	 */
	public void setTexture(Bitmap texture, int side) {
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
			if (mTextureFront != null)
				mTextureFront.recycle();
			mTextureFront = texture;
			break;
		case SIDE_BACK:
			if (mTextureBack != null)
				mTextureBack.recycle();
			mTextureBack = texture;
			break;
		case SIDE_BOTH:
			if (mTextureFront != null)
				mTextureFront.recycle();
			if (mTextureBack != null)
				mTextureBack.recycle();
			mTextureFront = mTextureBack = texture;
			break;
		}
		mTexturesChanged = true;
	}

}
