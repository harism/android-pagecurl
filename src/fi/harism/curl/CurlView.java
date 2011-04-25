package fi.harism.curl;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.graphics.RectF;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * OpenGL ES View.
 * 
 * @author harism
 */
public class CurlView extends GLSurfaceView implements View.OnTouchListener,
		CurlRenderer.Observer {

	// Mesh indices.
	public static final int PAGE_CURL = 0;
	public static final int PAGE_LEFT = 1;
	public static final int PAGE_RIGHT = 2;
	private CurlMesh mCurlMeshes[];

	private static final int CURL_NONE = 0;
	private static final int CURL_LEFT = 1;
	private static final int CURL_RIGHT = 2;
	private int mCurlState = CURL_NONE;

	private int mCurrentIndex = 0;
	private int mBitmapWidth = -1;
	private int mBitmapHeight = -1;

	private PointF mStartPos = new PointF();

	private boolean mAnimate = false;
	private PointF mAnimationSource;
	private PointF mAnimationTarget;
	private long mAnimationStartTime;
	private long mAnimationDurationTime;
	private int mAnimationTargetEvent;

	private CurlRenderer mRenderer;
	private CurlBitmapProvider mBitmapProvider;

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
	public void onBitmapSizeChanged(int width, int height) {
		mBitmapWidth = width;
		mBitmapHeight = height;
		updateBitmaps();
		requestRender();
	}

	@Override
	public void onRenderDone() {
		if (mAnimate == false) {
			return;
		}

		long currentTime = System.currentTimeMillis();
		if (currentTime >= mAnimationStartTime + mAnimationDurationTime) {
			if (mAnimationTargetEvent == PAGE_RIGHT) {
				CurlMesh right = mCurlMeshes[PAGE_CURL];
				CurlMesh current = mCurlMeshes[PAGE_RIGHT];
				right.setRect(mRenderer.getPageRect(CurlRenderer.PAGE_RIGHT));
				right.setTexRect(new RectF(0, 0, 1, 1));
				right.reset();
				mRenderer.removeCurlMesh(current);
				mCurlMeshes[PAGE_CURL] = current;
				mCurlMeshes[PAGE_RIGHT] = right;
				if (mCurlState == CURL_LEFT) {
					mCurrentIndex--;
				}
			} else if (mAnimationTargetEvent == PAGE_LEFT) {
				CurlMesh left = mCurlMeshes[PAGE_CURL];
				CurlMesh current = mCurlMeshes[PAGE_LEFT];
				left.setRect(mRenderer.getPageRect(CurlRenderer.PAGE_LEFT));
				left.setTexRect(new RectF(1, 0, 0, 1));
				left.reset();
				mRenderer.removeCurlMesh(current);
				mCurlMeshes[PAGE_CURL] = current;
				mCurlMeshes[PAGE_LEFT] = left;
				if (mCurlState == CURL_RIGHT) {
					mCurrentIndex++;
				}
			}
			mCurlState = CURL_NONE;
			mAnimate = false;
			requestRender();
		} else {
			PointF pos = new PointF();
			pos.set(mAnimationSource);
			pos.x += (mAnimationTarget.x - mAnimationSource.x)
					* (currentTime - mAnimationStartTime)
					/ mAnimationDurationTime;
			pos.y += (mAnimationTarget.y - mAnimationSource.y)
					* (currentTime - mAnimationStartTime)
					/ mAnimationDurationTime;
			updateCurl(pos);
		}
	}

	@Override
	public void onSizeChanged(int w, int h, int ow, int oh) {
		super.onSizeChanged(w, h, ow, oh);

		if (h > w) {
			//mRenderer.setViewMode(CurlRenderer.SHOW_ONE_PAGE);
		} else {
			//mRenderer.setViewMode(CurlRenderer.SHOW_TWO_PAGES);
		}

		requestRender();
	}

	@Override
	public boolean onTouch(View view, MotionEvent me) {
		// No dragging during animation at the moment.
		// TODO: Stop animation on touch event and return to drag mode.
		if (mAnimate) {
			return false;
		}

		switch (me.getAction()) {
		case MotionEvent.ACTION_DOWN: {
			float x = me.getX();
			if (x > getWidth() / 2) {
				if (mCurrentIndex < mBitmapProvider.getBitmapCount()) {
					mStartPos = mRenderer.getPos(getWidth(), me.getY());
					startCurl(CURL_RIGHT);
				}
			} else {
				if (mCurrentIndex > 0) {
					mStartPos = mRenderer.getPos(0, me.getY());
					startCurl(CURL_LEFT);
				}
			}
			break;
		}
		case MotionEvent.ACTION_MOVE: {
			updateCurl(mRenderer.getPos(me.getX(), me.getY()));
			break;
		}
		case MotionEvent.ACTION_CANCEL:
		case MotionEvent.ACTION_UP: {
			if (mCurlState == CURL_LEFT || mCurlState == CURL_RIGHT) {
				mAnimationSource = mRenderer.getPos(me.getX(), me.getY());
				mAnimationStartTime = System.currentTimeMillis();
				mAnimationDurationTime = 300;
				if (me.getX() > getWidth() / 2) {
					mAnimationTarget = new PointF();
					mAnimationTarget.set(mStartPos);
					mAnimationTarget.x = mRenderer
							.getPageRect(CurlRenderer.PAGE_RIGHT).right;
					mAnimationTargetEvent = PAGE_RIGHT;
				} else {
					mAnimationTarget = new PointF();
					mAnimationTarget.set(mStartPos);
					mAnimationTarget.x = mRenderer.getPos(0, 0).x;
					if (mCurlState == CURL_RIGHT
							|| mRenderer.getViewMode() == CurlRenderer.SHOW_TWO_PAGES) {
						mAnimationTarget.x = mRenderer
								.getPageRect(CurlRenderer.PAGE_LEFT).left;
					} else {
						mAnimationSource.x -= 0.3f;
						mAnimationTarget.x -= 0.3f;
					}
					mAnimationTargetEvent = PAGE_LEFT;
				}
				mAnimate = true;
				requestRender();
			}
			break;
		}
		}

		return true;
	}

	/**
	 * Update/set bitmap provider.
	 */
	public void setBitmapProvider(CurlBitmapProvider bitmapProvider) {
		mBitmapProvider = bitmapProvider;
		mCurrentIndex = 0;
		updateBitmaps();
		requestRender();
	}

	/**
	 * Initialize method.
	 */
	private void init(Context ctx) {
		mRenderer = new CurlRenderer(this);
		setRenderer(mRenderer);
		setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
		setOnTouchListener(this);

		mCurlMeshes = new CurlMesh[3];
		for (int i = 0; i < mCurlMeshes.length; ++i) {
			mCurlMeshes[i] = new CurlMesh(10);
		}
		mCurlMeshes[PAGE_LEFT].setTexRect(new RectF(1, 0, 0, 1));
		mCurlMeshes[PAGE_RIGHT].setTexRect(new RectF(0, 0, 1, 1));
	}

	/**
	 * Sets PAGE_CURL mesh curl position.
	 */
	private void setCurlPos(PointF curlPos, PointF curlDir, double radius) {
		// TODO: Calculate curl position at page top and bottom and make sure
		// page doesn't 'tear off'.
		mCurlMeshes[PAGE_CURL].curl(curlPos, curlDir, radius);
		requestRender();
	}

	/**
	 * Switches meshes and loads new bitmaps if available.
	 */
	private void startCurl(int page) {
		switch (page) {

		// Once right side page is curled, first right page is assigned into
		// curled
		// page. And if there are more bitmaps available new bitmap is loaded
		// into right side mesh.
		case CURL_RIGHT: {
			for (int i = 0; i < mCurlMeshes.length; ++i) {
				mRenderer.removeCurlMesh(mCurlMeshes[i]);
			}
			CurlMesh curl = mCurlMeshes[PAGE_RIGHT];
			mCurlMeshes[PAGE_RIGHT] = mCurlMeshes[PAGE_CURL];
			mCurlMeshes[PAGE_CURL] = curl;
			if (mCurrentIndex > 0) {
				mCurlMeshes[PAGE_LEFT].setRect(mRenderer
						.getPageRect(CurlRenderer.PAGE_LEFT));
				mCurlMeshes[PAGE_LEFT].reset();
				mRenderer.addCurlMesh(mCurlMeshes[PAGE_LEFT]);
			}
			if (mCurrentIndex < mBitmapProvider.getBitmapCount() - 1) {
				Bitmap bitmap = mBitmapProvider.getBitmap(mBitmapWidth,
						mBitmapHeight, mCurrentIndex + 1);
				mCurlMeshes[PAGE_RIGHT].setBitmap(bitmap);
				mCurlMeshes[PAGE_RIGHT].setRect(mRenderer
						.getPageRect(CurlRenderer.PAGE_RIGHT));
				mCurlMeshes[PAGE_RIGHT].setTexRect(new RectF(0, 0, 1, 1));
				mCurlMeshes[PAGE_RIGHT].reset();
				mRenderer.addCurlMesh(mCurlMeshes[PAGE_RIGHT]);
			}
			mCurlMeshes[PAGE_CURL].setRect(mRenderer
					.getPageRect(CurlRenderer.PAGE_RIGHT));
			mCurlMeshes[PAGE_CURL].setTexRect(new RectF(0, 0, 1, 1));
			mCurlMeshes[PAGE_CURL].reset();
			mRenderer.addCurlMesh(mCurlMeshes[PAGE_CURL]);
			mCurlState = CURL_RIGHT;
			break;
		}

			// On left side curl, left page is assigned to curled page. And if
			// there are more bitmaps available before currentIndex, new bitmap
			// is loaded into left page.
		case CURL_LEFT: {
			for (int i = 0; i < mCurlMeshes.length; ++i) {
				mRenderer.removeCurlMesh(mCurlMeshes[i]);
			}
			CurlMesh curl = mCurlMeshes[PAGE_LEFT];
			mCurlMeshes[PAGE_LEFT] = mCurlMeshes[PAGE_CURL];
			mCurlMeshes[PAGE_CURL] = curl;
			if (mCurrentIndex > 1) {
				Bitmap bitmap = mBitmapProvider.getBitmap(mBitmapWidth,
						mBitmapHeight, mCurrentIndex - 2);
				mCurlMeshes[PAGE_LEFT].setBitmap(bitmap);
				mCurlMeshes[PAGE_LEFT].setRect(mRenderer
						.getPageRect(CurlRenderer.PAGE_LEFT));
				mCurlMeshes[PAGE_LEFT].setTexRect(new RectF(1, 0, 0, 1));
				mCurlMeshes[PAGE_LEFT].reset();
				mRenderer.addCurlMesh(mCurlMeshes[PAGE_LEFT]);
			}
			if (mCurrentIndex < mBitmapProvider.getBitmapCount()) {
				mCurlMeshes[PAGE_RIGHT].setRect(mRenderer
						.getPageRect(CurlRenderer.PAGE_RIGHT));
				mCurlMeshes[PAGE_RIGHT].reset();
				mRenderer.addCurlMesh(mCurlMeshes[PAGE_RIGHT]);
			}
			if (mRenderer.getViewMode() == CurlRenderer.SHOW_ONE_PAGE) {
				mCurlMeshes[PAGE_CURL].setRect(mRenderer
						.getPageRect(CurlRenderer.PAGE_RIGHT));
				mCurlMeshes[PAGE_CURL].setTexRect(new RectF(0, 0, 1, 1));
			} else {
				mCurlMeshes[PAGE_CURL].setRect(mRenderer
						.getPageRect(CurlRenderer.PAGE_LEFT));
				mCurlMeshes[PAGE_CURL].setTexRect(new RectF(1, 0, 0, 1));
			}
			mCurlMeshes[PAGE_CURL].reset();
			mRenderer.addCurlMesh(mCurlMeshes[PAGE_CURL]);
			mCurlState = CURL_LEFT;
			break;
		}

		}
	}

	/**
	 * Updates bitmaps for left and right meshes.
	 */
	private void updateBitmaps() {
		if (mBitmapProvider == null || mBitmapWidth <= 0 || mBitmapHeight <= 0) {
			return;
		}

		// Remove meshes from renderer.
		for (int i = 0; i < mCurlMeshes.length; ++i) {
			mRenderer.removeCurlMesh(mCurlMeshes[i]);
		}

		if (mCurrentIndex >= 0
				&& mCurrentIndex < mBitmapProvider.getBitmapCount()) {
			Bitmap bitmap = mBitmapProvider.getBitmap(mBitmapWidth,
					mBitmapHeight, mCurrentIndex);
			mCurlMeshes[PAGE_RIGHT].setBitmap(bitmap);
			mCurlMeshes[PAGE_RIGHT].setRect(mRenderer
					.getPageRect(CurlRenderer.PAGE_RIGHT));
			mCurlMeshes[PAGE_RIGHT].reset();
			mRenderer.addCurlMesh(mCurlMeshes[PAGE_RIGHT]);
		}
		if (mCurrentIndex > 0) {
			Bitmap bitmap = mBitmapProvider.getBitmap(mBitmapWidth,
					mBitmapHeight, mCurrentIndex - 1);
			mCurlMeshes[PAGE_LEFT].setBitmap(bitmap);
			mCurlMeshes[PAGE_LEFT].setRect(mRenderer
					.getPageRect(CurlRenderer.PAGE_LEFT));
			mCurlMeshes[PAGE_LEFT].reset();
			mRenderer.addCurlMesh(mCurlMeshes[PAGE_LEFT]);
		}
	}

	/**
	 * Updates curl position.
	 */
	private void updateCurl(PointF pointerPos) {

		// Default curl radius.
		double radius = .3f;

		if (mCurlState == CURL_RIGHT
				|| (mCurlState == CURL_LEFT && mRenderer.getViewMode() == CurlRenderer.SHOW_TWO_PAGES)) {
			PointF dirVec = new PointF();
			dirVec.x = pointerPos.x - mStartPos.x;
			dirVec.y = pointerPos.y - mStartPos.y;
			float dist = (float) Math.sqrt(dirVec.x * dirVec.x + dirVec.y
					* dirVec.y);
			dirVec.x /= dist;
			dirVec.y /= dist;

			double curlLen = radius * Math.PI;
			if (dist >= curlLen) {
				double translate = (dist - curlLen) / 2;
				pointerPos.x -= dirVec.x * translate;
				pointerPos.y -= dirVec.y * translate;
			} else {
				double angle = Math.PI * Math.sqrt(dist / curlLen);
				double translate = radius * Math.sin(angle);
				pointerPos.x += dirVec.x * translate;
				pointerPos.y += dirVec.y * translate;
			}

			setCurlPos(pointerPos, dirVec, radius);
		} else if (mCurlState == CURL_LEFT) {
			pointerPos.x -= radius;

			PointF dirVec = new PointF();
			dirVec.x = pointerPos.x + mStartPos.x;
			dirVec.y = pointerPos.y - mStartPos.y;
			float dist = (float) Math.sqrt(dirVec.x * dirVec.x + dirVec.y
					* dirVec.y);
			dirVec.x /= dist;
			dirVec.y /= dist;

			setCurlPos(pointerPos, dirVec, radius);
		}
	}

}
