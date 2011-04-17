package fi.harism.curl;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * EGL View.
 * 
 * @author harism
 */
public class CurlView extends GLSurfaceView {

	// Actual renderer.
	private CurlRenderer mCurlRenderer;
	private BitmapHandler mBitmapHandler;

	/**
	 * Default constructor.
	 */
	public CurlView(Context ctx) {
		super(ctx);
		init(ctx);
	}

	/**
	 * Default constructor.
	 */
	public CurlView(Context ctx, AttributeSet attrs) {
		super(ctx, attrs);
		init(ctx);
	}

	/**
	 * Default constructor.
	 */
	public CurlView(Context ctx, AttributeSet attrs, int defStyle) {
		this(ctx, attrs);
	}

	@Override
	public void onSizeChanged(int w, int h, int ow, int oh) {
		super.onSizeChanged(w, h, ow, oh);

		if (h > w) {
			mCurlRenderer.setViewMode(CurlRenderer.SHOW_ONE_PAGE);
		} else {
			mCurlRenderer.setViewMode(CurlRenderer.SHOW_TWO_PAGES);
		}

		requestRender();
	}

	/**
	 * Initialize method.
	 */
	private void init(Context ctx) {
		mBitmapHandler = new BitmapHandler();
		mCurlRenderer = new CurlRenderer(mBitmapHandler);
		setRenderer(mCurlRenderer);
		setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
		this.setOnTouchListener(new TouchListener());
	}

	/**
	 * Update/set bitmap provider.
	 */
	public void setBitmapProvider(CurlBitmapProvider curlBitmapProvider) {
		mBitmapHandler.setBitmapProvider(curlBitmapProvider);
	}

	/**
	 * Touch listener.
	 */
	private class TouchListener implements View.OnTouchListener {

		private static final int DRAG_NONE = 0;
		private static final int DRAG_LEFT = 1;
		private static final int DRAG_RIGHT = 2;

		// ACTION_DOWN coordinates.
		private PointF mStartPos = new PointF();
		private int mDragMode = DRAG_NONE;

		@Override
		public boolean onTouch(View v, MotionEvent me) {
			if (me.getAction() == MotionEvent.ACTION_DOWN) {
				float x = me.getX();
				float y = me.getY();
				if (x > getWidth() / 2) {
					x = getWidth();
					if (mBitmapHandler.updateBitmaps(BitmapHandler.CURL_RIGHT)) {
						mDragMode = DRAG_RIGHT;
					}
				} else {
					x = 0;
					if (mBitmapHandler.updateBitmaps(BitmapHandler.CURL_LEFT)) {
						mDragMode = DRAG_LEFT;
					}
				}
				mStartPos = mCurlRenderer.getPos(x, y);
			} else if (me.getAction() == MotionEvent.ACTION_MOVE) {
				switch (mDragMode) {
				case DRAG_RIGHT: {
					PointF curPos = mCurlRenderer.getPos(me.getX(), me.getY());

					PointF dirVec = new PointF();
					dirVec.x = curPos.x - mStartPos.x;
					dirVec.y = curPos.y - mStartPos.y;
					float dist = (float) Math.sqrt(dirVec.x * dirVec.x
							+ dirVec.y * dirVec.y);
					dirVec.x /= dist;
					dirVec.y /= dist;

					mCurlRenderer.setPointerPos(curPos, dirVec, 0.3f);
					requestRender();
					break;
				}
				case DRAG_LEFT: {
					PointF curPos = mCurlRenderer.getPos(me.getX(), me.getY());
					curPos.x -= 0.3f;

					PointF dirVec = new PointF();
					dirVec.x = curPos.x + mStartPos.x;
					dirVec.y = curPos.y - mStartPos.y;
					float dist = (float) Math.sqrt(dirVec.x * dirVec.x
							+ dirVec.y * dirVec.y);
					dirVec.x /= dist;
					dirVec.y /= dist;

					mCurlRenderer.setCurlPos(curPos, dirVec, 0.3f);
					requestRender();
					break;
				}
			}
			}
			else if (me.getAction() == MotionEvent.ACTION_CANCEL || me.getAction() == MotionEvent.ACTION_UP) {
				mDragMode = DRAG_NONE;
				// TODO: Animate...
			}
			return true;
		}
	}

	private class BitmapHandler implements CurlRenderer.CurlRendererObserver {

		public static final int CURL_NONE = 0;
		public static final int CURL_LEFT = 1;
		public static final int CURL_RIGHT = 2;

		private int mCurrentIndex = 0;
		private int mBitmapWidth = -1;
		private int mBitmapHeight = -1;
		private CurlBitmapProvider mCurlBitmapProvider;

		@Override
		public void onBitmapSizeChanged(int width, int height) {
			mBitmapWidth = width;
			mBitmapHeight = height;
			updateBitmaps(CURL_NONE);
		}

		public void setBitmapProvider(CurlBitmapProvider provider) {
			mCurlBitmapProvider = provider;
			mCurrentIndex = 1;
			updateBitmaps(CURL_NONE);
		}

		private boolean updateBitmaps(int curlMode) {
			if (mBitmapWidth <= 0 || mBitmapHeight <= 0) {
				return false;
			}

			int leftIdx = mCurrentIndex - 1;
			int rightIdx = mCurrentIndex;
			int curlIdx = -1;
			if (curlMode == CURL_RIGHT) {
				curlIdx = mCurrentIndex;
				rightIdx = mCurrentIndex + 1;
			} else if (curlMode == CURL_LEFT) {
				leftIdx = mCurrentIndex - 2;
				curlIdx = mCurrentIndex - 1;
			}

			int max = mCurlBitmapProvider.getBitmapCount();
			if (leftIdx >= 0 && leftIdx < max) {
				Bitmap bitmap = mCurlBitmapProvider.getBitmap(mBitmapWidth,
						mBitmapHeight, leftIdx);
				mCurlRenderer.setBitmap(bitmap, CurlRenderer.PAGE_LEFT);
			} else {
				mCurlRenderer.setBitmap(null, CurlRenderer.PAGE_LEFT);
			}
			if (rightIdx >= 0 && rightIdx < max) {
				Bitmap bitmap = mCurlBitmapProvider.getBitmap(mBitmapWidth,
						mBitmapHeight, rightIdx);
				mCurlRenderer.setBitmap(bitmap, CurlRenderer.PAGE_RIGHT);
			} else {
				mCurlRenderer.setBitmap(null, CurlRenderer.PAGE_RIGHT);
			}
			boolean ret = false;
			if (curlIdx >= 0 && curlIdx < max) {
				Bitmap bitmap = mCurlBitmapProvider.getBitmap(mBitmapWidth,
						mBitmapHeight, curlIdx);
				mCurlRenderer.setBitmap(bitmap, CurlRenderer.PAGE_CURRENT);
				ret = true;
			} else {
				mCurlRenderer.setBitmap(null, CurlRenderer.PAGE_CURRENT);
			}

			requestRender();
			return ret;
		}
	}

}
