package fi.harism.curl;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
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
		//mCurlView.onLandscapeShowTwoPages(false); //false will force one page mode in landscape
		//mCurlView.displayLeftPage(false); //false hide the left page in one page mode
		//mCurlView.allowLastPageCurl(false); //false will prevent the last page from turning.
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
		public Bitmap getBitmap(int width, int height, int index) {
			Bitmap b = Bitmap.createBitmap(width, height,
					Bitmap.Config.ARGB_8888);
			b.eraseColor(0xFFFFFFFF);
			Canvas c = new Canvas(b);
			Drawable d = getResources().getDrawable(mBitmapIds[index]);

			int margin = 7;
			int border = 3;
			Rect r = new Rect(margin, margin, width - margin, height - margin);

			int imageWidth = r.width() - (border * 2);
			int imageHeight = imageWidth * d.getIntrinsicHeight()
					/ d.getIntrinsicWidth();
			if (imageHeight > r.height() - (border * 2)) {
				imageHeight = r.height() - (border * 2);
				imageWidth = imageHeight * d.getIntrinsicWidth()
						/ d.getIntrinsicHeight();
			}

			r.left += ((r.width() - imageWidth) / 2) - border;
			r.right = r.left + imageWidth + border + border;
			r.top += ((r.height() - imageHeight) / 2) - border;
			r.bottom = r.top + imageHeight + border + border;

			Paint p = new Paint();
			p.setColor(0xFFC0C0C0);
			c.drawRect(r, p);
			r.left += border;
			r.right -= border;
			r.top += border;
			r.bottom -= border;

			d.setBounds(r);
			d.draw(c);
			return b;
		}

		@Override
		public int getBitmapCount() {
			return mBitmapIds.length;
		}
	}

}