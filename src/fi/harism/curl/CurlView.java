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
 * TODO: Clean up code - a lot.
 * 
 * @author harism
 */
public class CurlView extends GLSurfaceView implements View.OnTouchListener,
		CurlRenderer.CurlRendererObserver {

	public static final int PAGE_CURRENT = 0;
	public static final int PAGE_LEFT = 1;
	public static final int PAGE_RIGHT = 2;

	private static final int CURL_NONE = 0;
	private static final int CURL_LEFT = 1;
	private static final int CURL_RIGHT = 2;
	private int mCurlState = CURL_NONE;

	private int mCurrentIndex = 0;
	private int mBitmapWidth = -1;
	private int mBitmapHeight = -1;

	private PointF mStartPos = new PointF();

	private CurlRenderer mRenderer;
	private CurlBitmapProvider mBitmapProvider;

	private CurlMesh mCurlMeshes[];

	// TODO: This is a DISASTER  :)
	private boolean mAnimate = false;
	private PointF mAnimationSource;
	private PointF mAnimationTarget;
	private long mAnimationStartTime;
	private long mAnimationDurationTime;

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
		PointF pos = new PointF();
		pos.set(mAnimationSource);
		pos.x += (mAnimationTarget.x - mAnimationSource.x)
				* (currentTime - mAnimationStartTime) / mAnimationDurationTime;
		pos.y += (mAnimationTarget.y - mAnimationSource.y)
				* (currentTime - mAnimationStartTime) / mAnimationDurationTime;

		if (currentTime >= mAnimationStartTime + mAnimationDurationTime) {
			if (pos.x > mRenderer.getPos(getWidth() / 2, 0).x) {
				CurlMesh right = mCurlMeshes[PAGE_CURRENT];
				CurlMesh current = mCurlMeshes[PAGE_RIGHT];
				right.setRect(mRenderer.getPageRect(CurlRenderer.PAGE_RIGHT));
				right.reset();
				mRenderer.removeCurlMesh(current);
				mCurlMeshes[PAGE_CURRENT] = current;
				mCurlMeshes[PAGE_RIGHT] = right;
				if (mCurlState == CURL_LEFT) {
					mCurrentIndex--;
				}
			} else {
				CurlMesh left = mCurlMeshes[PAGE_CURRENT];
				CurlMesh current = mCurlMeshes[PAGE_LEFT];
				left.setRect(mRenderer.getPageRect(CurlRenderer.PAGE_LEFT));
				left.reset();
				mRenderer.removeCurlMesh(current);
				mCurlMeshes[PAGE_CURRENT] = current;
				mCurlMeshes[PAGE_LEFT] = left;
				if (mCurlState == CURL_RIGHT) {
					mCurrentIndex++;
				}
			}
			mCurlState = CURL_NONE;
			mAnimate = false;
			requestRender();
			return;
		}

		switch (mCurlState) {
		case CURL_RIGHT: {
			PointF dirVec = new PointF();
			dirVec.x = pos.x - mStartPos.x;
			dirVec.y = pos.y - mStartPos.y;
			float dist = (float) Math.sqrt(dirVec.x * dirVec.x + dirVec.y
					* dirVec.y);
			dirVec.x /= dist;
			dirVec.y /= dist;

			setCurlPos(pos, dirVec, 0.3f, dist);
			requestRender();
			break;
		}
		case CURL_LEFT: {
			PointF dirVec = new PointF();
			dirVec.x = pos.x + mStartPos.x;
			dirVec.y = pos.y - mStartPos.y;
			float dist = (float) Math.sqrt(dirVec.x * dirVec.x + dirVec.y
					* dirVec.y);
			dirVec.x /= dist;
			dirVec.y /= dist;

			setCurlPos(pos, dirVec, 0.3f);
			requestRender();
			break;
		}
		}
	}

	@Override
	public void onSizeChanged(int w, int h, int ow, int oh) {
		super.onSizeChanged(w, h, ow, oh);

		// TODO: This requires some changes in CurlMesh etc.
		// if (h > w) {
		// mRenderer.setViewMode(CurlRenderer.SHOW_ONE_PAGE);
		// } else {
		// mRenderer.setViewMode(CurlRenderer.SHOW_TWO_PAGES);
		// }

		requestRender();
	}

	@Override
	public boolean onTouch(View view, MotionEvent me) {
		if (mAnimate) {
			return false;
		}
		if (me.getAction() == MotionEvent.ACTION_DOWN) {
			float x = me.getX();
			float y = me.getY();
			if (x > view.getWidth() / 2) {
				x = view.getWidth();
				if (mCurrentIndex < mBitmapProvider.getBitmapCount()) {
					for (int i = 0; i < mCurlMeshes.length; ++i) {
						mRenderer.removeCurlMesh(mCurlMeshes[i]);
					}
					CurlMesh curl = mCurlMeshes[PAGE_RIGHT];
					mCurlMeshes[PAGE_RIGHT] = mCurlMeshes[PAGE_CURRENT];
					mCurlMeshes[PAGE_CURRENT] = curl;
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
						mCurlMeshes[PAGE_RIGHT].reset();
						mRenderer.addCurlMesh(mCurlMeshes[PAGE_RIGHT]);
					}
					mCurlMeshes[PAGE_CURRENT].setRect(mRenderer
							.getPageRect(CurlRenderer.PAGE_RIGHT));
					mCurlMeshes[PAGE_CURRENT].reset();
					mRenderer.addCurlMesh(mCurlMeshes[PAGE_CURRENT]);
					mCurlState = CURL_RIGHT;
				}
			} else {
				x = 0;
				if (mCurrentIndex > 0) {
					for (int i = 0; i < mCurlMeshes.length; ++i) {
						mRenderer.removeCurlMesh(mCurlMeshes[i]);
					}
					CurlMesh curl = mCurlMeshes[PAGE_LEFT];
					mCurlMeshes[PAGE_LEFT] = mCurlMeshes[PAGE_CURRENT];
					mCurlMeshes[PAGE_CURRENT] = curl;
					if (mCurrentIndex > 1) {
						Bitmap bitmap = mBitmapProvider.getBitmap(mBitmapWidth,
								mBitmapHeight, mCurrentIndex - 2);
						mCurlMeshes[PAGE_LEFT].setBitmap(bitmap);
						mCurlMeshes[PAGE_LEFT].setRect(mRenderer
								.getPageRect(CurlRenderer.PAGE_LEFT));
						mCurlMeshes[PAGE_LEFT].reset();
						mRenderer.addCurlMesh(mCurlMeshes[PAGE_LEFT]);
					}
					if (mCurrentIndex < mBitmapProvider.getBitmapCount()) {
						mCurlMeshes[PAGE_RIGHT].setRect(mRenderer
								.getPageRect(CurlRenderer.PAGE_RIGHT));
						mCurlMeshes[PAGE_RIGHT].reset();
						mRenderer.addCurlMesh(mCurlMeshes[PAGE_RIGHT]);
					}
					mCurlMeshes[PAGE_CURRENT].setRect(mRenderer
							.getPageRect(CurlRenderer.PAGE_RIGHT));
					mCurlMeshes[PAGE_CURRENT].reset();
					mRenderer.addCurlMesh(mCurlMeshes[PAGE_CURRENT]);
					mCurlState = CURL_LEFT;
				}
			}
			mStartPos = mRenderer.getPos(x, y);
		} else if (me.getAction() == MotionEvent.ACTION_MOVE) {
			switch (mCurlState) {
			case CURL_RIGHT: {
				PointF curPos = mRenderer.getPos(me.getX(), me.getY());

				PointF dirVec = new PointF();
				dirVec.x = curPos.x - mStartPos.x;
				dirVec.y = curPos.y - mStartPos.y;
				float dist = (float) Math.sqrt(dirVec.x * dirVec.x + dirVec.y
						* dirVec.y);
				dirVec.x /= dist;
				dirVec.y /= dist;

				setCurlPos(curPos, dirVec, 0.3f, dist);
				requestRender();
				break;
			}
			case CURL_LEFT: {
				PointF curPos = mRenderer.getPos(me.getX(), me.getY());
				curPos.x -= 0.3f;

				PointF dirVec = new PointF();
				dirVec.x = curPos.x + mStartPos.x;
				dirVec.y = curPos.y - mStartPos.y;
				float dist = (float) Math.sqrt(dirVec.x * dirVec.x + dirVec.y
						* dirVec.y);
				dirVec.x /= dist;
				dirVec.y /= dist;

				setCurlPos(curPos, dirVec, 0.3f);
				requestRender();
				break;
			}
			}
		} else if (me.getAction() == MotionEvent.ACTION_CANCEL
				|| me.getAction() == MotionEvent.ACTION_UP) {
			if (mCurlState == CURL_LEFT || mCurlState == CURL_RIGHT) {
				mAnimationSource = mRenderer.getPos(me.getX(), me.getY());
				mAnimationStartTime = System.currentTimeMillis();
				mAnimationDurationTime = 300;
				if (me.getX() > getWidth() / 2) {
					mAnimationTarget = new PointF();
					mAnimationTarget.set(mStartPos);
					mAnimationTarget.x = mRenderer.getPos(getWidth(), 0).x;
				} else {
					mAnimationTarget = new PointF();
					mAnimationTarget.set(mStartPos);
					mAnimationTarget.x = mRenderer.getPos(0, 0).x;
					if (mCurlState == CURL_RIGHT) {
						mAnimationTarget.x = mRenderer.getPos(-getWidth(), 0).x;
					} else {
						mAnimationSource.x -= 0.3f;
						mAnimationTarget.x -= 0.3f;
					}
				}
				mAnimate = true;
				requestRender();
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
			mCurlMeshes[i].setTexRect(new RectF(0, 0, 1, 1));
		}
	}

	/**
	 * Sets curl position.
	 */
	private void setCurlPos(PointF curlPos, PointF curlDir, double radius) {
		mCurlMeshes[PAGE_CURRENT].curl(curlPos, curlDir, radius);
	}

	/**
	 * Calculates curl position from pointerPos and dist, meaning that edge of
	 * mesh follows pointerPos.
	 */
	private void setCurlPos(PointF pointerPos, PointF curlDir, double radius,
			double dist) {
		double curlLen = radius * Math.PI;
		if (dist >= curlLen) {
			double translate = (dist - curlLen) / 2;
			pointerPos.x -= curlDir.x * translate;
			pointerPos.y -= curlDir.y * translate;
		} else {
			double angle = Math.PI * Math.sqrt(dist / curlLen);
			double translate = radius * Math.sin(angle);
			pointerPos.x += curlDir.x * translate;
			pointerPos.y += curlDir.y * translate;
		}
		setCurlPos(pointerPos, curlDir, radius);
	}

	/**
	 * Updates bitmaps for left and right meshes.
	 */
	private void updateBitmaps() {
		if (mBitmapProvider == null || mBitmapWidth <= 0 || mBitmapHeight <= 0) {
			return;
		}

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

}
