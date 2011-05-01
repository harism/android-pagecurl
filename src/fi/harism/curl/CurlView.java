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

	// Page meshes. Left and right meshes are 'static' while curl is used to
	// show page flipping.
	private CurlMesh mPageCurl;
	private CurlMesh mPageLeft;
	private CurlMesh mPageRight;

	// Curl state. We are flipping none, left or right page.
	private static final int CURL_NONE = 0;
	private static final int CURL_LEFT = 1;
	private static final int CURL_RIGHT = 2;
	private int mCurlState = CURL_NONE;

	// Current page index. This is always showed on right page.
	private int mCurrentIndex = 0;

	// Bitmap size. These are updated from renderer once it's initialized.
	private int mBitmapWidth = -1;
	private int mBitmapHeight = -1;

	// Start position for dragging.
	private PointF mDragStartPos = new PointF();
	private PointF mPointerPos = new PointF();
	private PointF mCurlPos = new PointF();
	private PointF mCurlDir = new PointF();

	private boolean mAnimate = false;
	private PointF mAnimationSource = new PointF();
	private PointF mAnimationTarget = new PointF();
	private long mAnimationStartTime;
	private long mAnimationDurationTime = 300;
	private int mAnimationTargetEvent;
	private static final int SET_CURL_TO_LEFT = 1;
	private static final int SET_CURL_TO_RIGHT = 2;

	private CurlRenderer mRenderer;
	private CurlBitmapProvider mBitmapProvider;

	private static final RectF TEXTURE_RECT_FRONT = new RectF(0, 0, 1, 1);
	private static final RectF TEXTURE_RECT_BACK = new RectF(1, 0, 0, 1);

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
		// We're not animating.
		if (mAnimate == false) {
			return;
		}

		long currentTime = System.currentTimeMillis();
		// If animation is done.
		if (currentTime >= mAnimationStartTime + mAnimationDurationTime) {
			if (mAnimationTargetEvent == SET_CURL_TO_RIGHT) {
				// Switch curled page to right.
				CurlMesh right = mPageCurl;
				CurlMesh curl = mPageRight;
				right.setRect(mRenderer.getPageRect(CurlRenderer.PAGE_RIGHT));
				right.setTexRect(TEXTURE_RECT_FRONT);
				right.reset();
				mRenderer.removeCurlMesh(curl);
				mPageCurl = curl;
				mPageRight = right;
				// If we were curling left page update current index.
				if (mCurlState == CURL_LEFT) {
					mCurrentIndex--;
				}
			} else if (mAnimationTargetEvent == SET_CURL_TO_LEFT) {
				// Switch curled page to left.
				CurlMesh left = mPageCurl;
				CurlMesh curl = mPageLeft;
				left.setRect(mRenderer.getPageRect(CurlRenderer.PAGE_LEFT));
				left.setTexRect(TEXTURE_RECT_BACK);
				left.reset();
				mRenderer.removeCurlMesh(curl);
				mPageCurl = curl;
				mPageLeft = left;
				// If we were curling right page update current index.
				if (mCurlState == CURL_RIGHT) {
					mCurrentIndex++;
				}
			}
			mCurlState = CURL_NONE;
			mAnimate = false;
			requestRender();
		} else {
			mPointerPos.set(mAnimationSource);
			float t = (float) Math
					.sqrt((double) (currentTime - mAnimationStartTime)
							/ mAnimationDurationTime);
			mPointerPos.x += (mAnimationTarget.x - mAnimationSource.x) * t;
			mPointerPos.y += (mAnimationTarget.y - mAnimationSource.y) * t;
			updateCurlPos(mPointerPos);
		}
	}

	@Override
	public void onSizeChanged(int w, int h, int ow, int oh) {
		super.onSizeChanged(w, h, ow, oh);

		if (h > w) {
			mRenderer.setViewMode(CurlRenderer.SHOW_ONE_PAGE);
			mRenderer.setMargins(.05f, .05f, .05f, .05f);
		} else {
			mRenderer.setViewMode(CurlRenderer.SHOW_TWO_PAGES);
			mRenderer.setMargins(.1f, .05f, .1f, .05f);
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

		RectF rightRect = mRenderer.getPageRect(CurlRenderer.PAGE_RIGHT);
		RectF leftRect = mRenderer.getPageRect(CurlRenderer.PAGE_LEFT);
		mPointerPos.x = me.getX();
		mPointerPos.y = me.getY();
		mRenderer.translate(mPointerPos);
		if (mPointerPos.x > rightRect.right) {
			mPointerPos.x = rightRect.right;
		} else if (mRenderer.getViewMode() == CurlRenderer.SHOW_ONE_PAGE
				&& mPointerPos.x < rightRect.left) {
			mPointerPos.x = rightRect.left;
		} else if (mRenderer.getViewMode() == CurlRenderer.SHOW_TWO_PAGES
				&& mPointerPos.x < leftRect.left) {
			mPointerPos.x = leftRect.left;
		}

		switch (me.getAction()) {
		case MotionEvent.ACTION_DOWN: {
			mDragStartPos.set(mPointerPos);
			if (mDragStartPos.y > rightRect.top) {
				mDragStartPos.y = rightRect.top;
			} else if (mDragStartPos.y < rightRect.bottom) {
				mDragStartPos.y = rightRect.bottom;
			}

			if (mRenderer.getViewMode() == CurlRenderer.SHOW_TWO_PAGES) {
				if (mDragStartPos.x < leftRect.right && mCurrentIndex > 0) {
					mDragStartPos.x = leftRect.left;
					startCurl(CURL_LEFT);
				} else if (mDragStartPos.x > rightRect.left
						&& mCurrentIndex < mBitmapProvider.getBitmapCount()) {
					mDragStartPos.x = rightRect.right;
					startCurl(CURL_RIGHT);
				}
			} else if (mRenderer.getViewMode() == CurlRenderer.SHOW_ONE_PAGE) {
				float halfX = (rightRect.right + rightRect.left) / 2;
				if (mDragStartPos.x < halfX && mCurrentIndex > 0) {
					mDragStartPos.x = rightRect.left;
					startCurl(CURL_LEFT);
				} else if (mDragStartPos.x >= halfX
						&& mCurrentIndex < mBitmapProvider.getBitmapCount()) {
					mDragStartPos.x = rightRect.right;
					startCurl(CURL_RIGHT);
				}
			}
			// Let this case clause flow through to next one.
		}
		case MotionEvent.ACTION_MOVE: {
			updateCurlPos(mPointerPos);
			break;
		}
		case MotionEvent.ACTION_CANCEL:
		case MotionEvent.ACTION_UP: {
			if (mCurlState == CURL_LEFT || mCurlState == CURL_RIGHT) {
				// Animation source is the point from where animation starts.
				mAnimationSource.set(mPointerPos);
				mAnimationStartTime = System.currentTimeMillis();

				if ((mRenderer.getViewMode() == CurlRenderer.SHOW_ONE_PAGE && mPointerPos.x > (rightRect.left + rightRect.right) / 2)
						|| mRenderer.getViewMode() == CurlRenderer.SHOW_TWO_PAGES
						&& mPointerPos.x > rightRect.left) {
					// On right side target is always right page's right border.
					mAnimationTarget.set(mDragStartPos);
					mAnimationTarget.x = mRenderer
							.getPageRect(CurlRenderer.PAGE_RIGHT).right;
					mAnimationTargetEvent = SET_CURL_TO_RIGHT;
				} else {
					// On left side target depends on visible pages.
					mAnimationTarget.set(mDragStartPos);
					if (mCurlState == CURL_RIGHT
							|| mRenderer.getViewMode() == CurlRenderer.SHOW_TWO_PAGES) {
						mAnimationTarget.x = leftRect.left;
					} else {
						mAnimationTarget.x = rightRect.left;
					}
					mAnimationTargetEvent = SET_CURL_TO_LEFT;
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

		// Even though left and right pages are static we have to allocate room
		// for curl on them too as we are switching meshes. Another way would be
		// to swap texture ids only.
		mPageLeft = new CurlMesh(10);
		mPageRight = new CurlMesh(10);
		mPageCurl = new CurlMesh(10);
		mPageLeft.setTexRect(TEXTURE_RECT_BACK);
		mPageRight.setTexRect(TEXTURE_RECT_FRONT);
	}

	/**
	 * Sets mPageCurl curl position.
	 */
	private void setCurlPos(PointF curlPos, PointF curlDir, double radius) {

		// First reposition curl so that page doesn't 'rip off' from book.
		if (mCurlState == CURL_RIGHT
				|| (mCurlState == CURL_LEFT && mRenderer.getViewMode() == CurlRenderer.SHOW_ONE_PAGE)) {
			RectF pageRect = mRenderer.getPageRect(CurlRenderer.PAGE_RIGHT);
			if (curlPos.x < pageRect.left) {
				curlPos.x = pageRect.left;
			}
			if (curlDir.y != 0) {
				float diffX = curlPos.x - pageRect.left;
				float leftY = curlPos.y + (diffX * curlDir.x / curlDir.y);
				if (curlDir.y < 0 && leftY < pageRect.top) {
					curlDir.x = curlPos.y - pageRect.top;
					curlDir.y = pageRect.left - curlPos.x;
				} else if (curlDir.y > 0 && leftY > pageRect.bottom) {
					curlDir.x = pageRect.bottom - curlPos.y;
					curlDir.y = curlPos.x - pageRect.left;
				}
			}
		} else if (mCurlState == CURL_LEFT) {
			RectF pageRect = mRenderer.getPageRect(CurlRenderer.PAGE_LEFT);
			if (curlPos.x > pageRect.right) {
				curlPos.x = pageRect.right;
			}
			if (curlDir.y != 0) {
				float diffX = curlPos.x - pageRect.right;
				float rightY = curlPos.y + (diffX * curlDir.x / curlDir.y);
				if (curlDir.y < 0 && rightY < pageRect.top) {
					curlDir.x = pageRect.top - curlPos.y;
					curlDir.y = curlPos.x - pageRect.right;
				} else if (curlDir.y > 0 && rightY > pageRect.bottom) {
					curlDir.x = curlPos.y - pageRect.bottom;
					curlDir.y = pageRect.right - curlPos.x;
				}
			}
		}

		// Finally normalize direction vector and do rendering.
		double dist = Math.sqrt(curlDir.x * curlDir.x + curlDir.y * curlDir.y);
		if (dist != 0) {
			curlDir.x /= dist;
			curlDir.y /= dist;
			mPageCurl.curl(curlPos, curlDir, radius);
		} else {
			mPageCurl.reset();
		}

		requestRender();
	}

	/**
	 * Switches meshes and loads new bitmaps if available.
	 */
	private void startCurl(int page) {
		switch (page) {

		// Once right side page is curled, first right page is assigned into
		// curled page. And if there are more bitmaps available new bitmap is
		// loaded into right side mesh.
		case CURL_RIGHT: {
			// Remove meshes from renderer.
			mRenderer.removeCurlMesh(mPageLeft);
			mRenderer.removeCurlMesh(mPageRight);
			mRenderer.removeCurlMesh(mPageCurl);

			// We are curling right page.
			CurlMesh curl = mPageRight;
			mPageRight = mPageCurl;
			mPageCurl = curl;

			// If there is something to show on left page, simply add it to
			// renderer.
			if (mCurrentIndex > 0) {
				mPageLeft
						.setRect(mRenderer.getPageRect(CurlRenderer.PAGE_LEFT));
				mPageLeft.reset();
				mRenderer.addCurlMesh(mPageLeft);
			}

			// If there is new/next available, set it to right page.
			if (mCurrentIndex < mBitmapProvider.getBitmapCount() - 1) {
				Bitmap bitmap = mBitmapProvider.getBitmap(mBitmapWidth,
						mBitmapHeight, mCurrentIndex + 1);
				mPageRight.setBitmap(bitmap);
				mPageRight.setRect(mRenderer
						.getPageRect(CurlRenderer.PAGE_RIGHT));
				mPageRight.setTexRect(TEXTURE_RECT_FRONT);
				mPageRight.reset();
				mRenderer.addCurlMesh(mPageRight);
			}

			// Add curled page to renderer.
			mPageCurl.setRect(mRenderer.getPageRect(CurlRenderer.PAGE_RIGHT));
			mPageCurl.setTexRect(TEXTURE_RECT_FRONT);
			mPageCurl.reset();
			mRenderer.addCurlMesh(mPageCurl);

			mCurlState = CURL_RIGHT;
			break;
		}

			// On left side curl, left page is assigned to curled page. And if
			// there are more bitmaps available before currentIndex, new bitmap
			// is loaded into left page.
		case CURL_LEFT: {
			// Remove meshes from renderer.
			mRenderer.removeCurlMesh(mPageLeft);
			mRenderer.removeCurlMesh(mPageRight);
			mRenderer.removeCurlMesh(mPageCurl);

			// We are curling left page.
			CurlMesh curl = mPageLeft;
			mPageLeft = mPageCurl;
			mPageCurl = curl;

			// If there is new/previous bitmap available load it to left page.
			if (mCurrentIndex > 1) {
				Bitmap bitmap = mBitmapProvider.getBitmap(mBitmapWidth,
						mBitmapHeight, mCurrentIndex - 2);
				mPageLeft.setBitmap(bitmap);
				mPageLeft
						.setRect(mRenderer.getPageRect(CurlRenderer.PAGE_LEFT));
				mPageLeft.setTexRect(TEXTURE_RECT_BACK);
				mPageLeft.reset();
				mRenderer.addCurlMesh(mPageLeft);
			}

			// If there is something to show on right page add it to renderer.
			if (mCurrentIndex < mBitmapProvider.getBitmapCount()) {
				mPageRight.setRect(mRenderer
						.getPageRect(CurlRenderer.PAGE_RIGHT));
				mPageRight.reset();
				mRenderer.addCurlMesh(mPageRight);
			}

			// How dragging previous page happens depends on view mode.
			if (mRenderer.getViewMode() == CurlRenderer.SHOW_ONE_PAGE) {
				mPageCurl.setRect(mRenderer
						.getPageRect(CurlRenderer.PAGE_RIGHT));
				mPageCurl.setTexRect(TEXTURE_RECT_FRONT);
			} else {
				mPageCurl
						.setRect(mRenderer.getPageRect(CurlRenderer.PAGE_LEFT));
				mPageCurl.setTexRect(TEXTURE_RECT_BACK);
			}
			mPageCurl.reset();
			mRenderer.addCurlMesh(mPageCurl);

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
		mRenderer.removeCurlMesh(mPageLeft);
		mRenderer.removeCurlMesh(mPageRight);
		mRenderer.removeCurlMesh(mPageCurl);

		if (mCurrentIndex >= 0
				&& mCurrentIndex < mBitmapProvider.getBitmapCount()) {
			Bitmap bitmap = mBitmapProvider.getBitmap(mBitmapWidth,
					mBitmapHeight, mCurrentIndex);
			mPageRight.setBitmap(bitmap);
			mPageRight.setRect(mRenderer.getPageRect(CurlRenderer.PAGE_RIGHT));
			mPageRight.reset();
			mRenderer.addCurlMesh(mPageRight);
		}
		if (mCurrentIndex > 0) {
			Bitmap bitmap = mBitmapProvider.getBitmap(mBitmapWidth,
					mBitmapHeight, mCurrentIndex - 1);
			mPageLeft.setBitmap(bitmap);
			mPageLeft.setRect(mRenderer.getPageRect(CurlRenderer.PAGE_LEFT));
			mPageLeft.reset();
			mRenderer.addCurlMesh(mPageLeft);
		}
	}

	/**
	 * Updates curl position.
	 */
	private void updateCurlPos(PointF pointerPos) {

		// Default curl radius.
		double radius = mRenderer.getPageRect(CURL_RIGHT).width() / 3;
		mCurlPos.set(pointerPos);

		// If curl happens on right page, or on left page on two page mode,
		// we'll calculate curl position from pointerPos.
		if (mCurlState == CURL_RIGHT
				|| (mCurlState == CURL_LEFT && mRenderer.getViewMode() == CurlRenderer.SHOW_TWO_PAGES)) {

			mCurlDir.x = mCurlPos.x - mDragStartPos.x;
			mCurlDir.y = mCurlPos.y - mDragStartPos.y;
			float dist = (float) Math.sqrt(mCurlDir.x * mCurlDir.x + mCurlDir.y
					* mCurlDir.y);

			// Adjust curl radius so that if page is dragged far enough on
			// opposite side, radius gets closer to zero.
			float pageWidth = mRenderer.getPageRect(CurlRenderer.PAGE_RIGHT)
					.width();
			double curlLen = radius * Math.PI;
			if (dist > (pageWidth * 2) - curlLen) {
				curlLen = Math.max((pageWidth * 2) - dist, 0f);
				radius = curlLen / Math.PI;
			}

			// Actual curl position calculation.
			if (dist >= curlLen) {
				double translate = (dist - curlLen) / 2;
				mCurlPos.x -= mCurlDir.x * translate / dist;
				mCurlPos.y -= mCurlDir.y * translate / dist;
			} else {
				double angle = Math.PI * Math.sqrt(dist / curlLen);
				double translate = radius * Math.sin(angle);
				mCurlPos.x += mCurlDir.x * translate / dist;
				mCurlPos.y += mCurlDir.y * translate / dist;
			}

			setCurlPos(mCurlPos, mCurlDir, radius);
		}
		// Otherwise we'll let curl follow pointer position.
		else if (mCurlState == CURL_LEFT) {

			// Adjust radius regarding how close to page edge we are.
			float pageLeftX = mRenderer.getPageRect(CurlRenderer.PAGE_RIGHT).left;
			radius = Math.min(mCurlPos.x - pageLeftX, radius);

			float pageRightX = mRenderer.getPageRect(CurlRenderer.PAGE_RIGHT).right;
			mCurlPos.x -= Math.min(pageRightX - mCurlPos.x, radius);
			mCurlDir.x = mCurlPos.x + mDragStartPos.x;
			mCurlDir.y = mCurlPos.y - mDragStartPos.y;

			setCurlPos(mCurlPos, mCurlDir, radius);
		}
	}

}
