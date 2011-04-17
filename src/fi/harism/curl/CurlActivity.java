package fi.harism.curl;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

/**
 * Basic holder for Activity for curl testing.
 * 
 * @author harism
 */
public class CurlActivity extends Activity {

	private CurlView mCurlView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		mCurlView = (CurlView) findViewById(R.id.curl);
		mCurlView.setBitmapProvider(new BitmapProvider());
	}

	@Override
	public void onPause() {
		super.onPause();
		mCurlView.onPause();
	}

	@Override
	public void onResume() {
		super.onResume();
		mCurlView.onResume();
	}

	/**
	 * Bitmap provider.
	 */
	private class BitmapProvider implements CurlBitmapProvider {

		private int[] mBitmapIds = { R.drawable.obama, R.drawable.road_rage,
				R.drawable.taipei_101, R.drawable.world };

		@Override
		public int getBitmapCount() {
			return mBitmapIds.length;
		}

		@Override
		public Bitmap getBitmap(int width, int height, int index) {
			// Create Bitmap for renderer.
			Drawable d = getResources().getDrawable(mBitmapIds[index]);
			d.setBounds(0, 0, width, height);
			Bitmap b = Bitmap.createBitmap(width, height,
					Bitmap.Config.ARGB_8888);
			b.eraseColor(0x00000000);
			Canvas c = new Canvas(b);
			d.draw(c);
			return b;
		}
	}

}