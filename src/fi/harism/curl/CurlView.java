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

	// Shows one page at the center of view.
	public static final int SHOW_ONE_PAGE = 1;
	// Shows two pages side by side.
	public static final int SHOW_TWO_PAGES = 2;
	// One page is the default.
	private int mViewMode = SHOW_ONE_PAGE;

	private boolean mRenderLeftPage = true;
	private boolean mAllowLastPageCurl = true;

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
	private int mPageBitmapWidth = -1;
	private int mPageBitmapHeight = -1;

	// Start position for dragging.
	private PointF mDragStartPos = new PointF();
	private PointF mPointerPos = new PointF();
	private PointF mCurlPos = new PointF();
	private PointF mCurlDir = new PointF();
	
	private float mDoPageTurnCurlPercent = .2f;
	private boolean mAnimate = false;
	private PointF mAnimationSource = new PointF();
	private PointF mAnimationTarget = new PointF();
	private long mAnimationStartTime;
	private long mAnimationDurationTime = 300;
	private int mAnimationTargetEvent;
	private static final int SET_CURL_TO_LEFT = 1;
	private static final int SET_CURL_TO_RIGHT = 2;
	private boolean mAdjustCurlRadiusWithPressure = false;

	private CurlRenderer mRenderer;
	private BitmapProvider mBitmapProvider;
	private SizeChangedObserver mSizeChangedObserver;

	// Texture coordinates. For flipping a page we do not alter its rectangle
	// but flip texture instead.
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

	/**
	 * Set current page index.
	 */
	public int getCurrentIndex() {
		return mCurrentIndex;
	}

	@Override
	public void onDrawFrame() {
		// We are not animating.
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
				if (!mRenderLeftPage) {
					mRenderer.removeCurlMesh(left);
				}
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
			updateCurlPos(mPointerPos, .0f);
		}
	}

	@Override
	public void onPageSizeChanged(int width, int height) {
		mPageBitmapWidth = width;
		mPageBitmapHeight = height;
		updateBitmaps();
		requestRender();
	}

	@Override
	public void onSizeChanged(int w, int h, int ow, int oh) {
		super.onSizeChanged(w, h, ow, oh);
		requestRender();
		if (mSizeChangedObserver != null) {
			mSizeChangedObserver.onSizeChanged(w, h);
		}
	}

	@Override
	public void onSurfaceCreated() {
		// In case surface is recreated, let page meshes drop allocated texture
		// ids and ask for new ones. There's no need to set textures here as
		// onPageSizeChanged should be called later on.
		mPageLeft.resetTexture();
		mPageRight.resetTexture();
		mPageCurl.resetTexture();
	}

	@Override
	public boolean onTouch(View view, MotionEvent me) {
		// No dragging during animation at the moment.
		// TODO: Stop animation on touch event and return to drag mode.
		if (mAnimate || mBitmapProvider == null) {
			return false;
		}

		// We need page rects quite extensively so get them for later use.
		RectF rightRect = mRenderer.getPageRect(CurlRenderer.PAGE_RIGHT);
		RectF leftRect = mRenderer.getPageRect(CurlRenderer.PAGE_LEFT);

		// Store pointer position.
		mPointerPos.x = me.getX();
		mPointerPos.y = me.getY();
		mRenderer.translate(mPointerPos);

		switch (me.getAction()) {
		case MotionEvent.ACTION_DOWN: {

			// Once we receive pointer down event its position is mapped to
			// right or left edge of page and that'll be the position from where
			// user is holding the paper to make curl happen.
			mDragStartPos.set(mPointerPos);

			// First we make sure it's not over or below page. Pages are
			// supposed to be same height so it really doesn't matter do we use
			// left or right one.
			if (mDragStartPos.y > rightRect.top) {
				mDragStartPos.y = rightRect.top;
			} else if (mDragStartPos.y < rightRect.bottom) {
				mDragStartPos.y = rightRect.bottom;
			}

			// Then we have to make decisions for the user whether curl is going
			// to happen from left or right, and on which page.
			if (mViewMode == SHOW_TWO_PAGES) {
				// If we have an open book and pointer is on the left from right
				// page we'll mark drag position to left edge of left page.
				// Additionally checking mCurrentIndex is higher than zero tells
				// us there is a visible page at all.
				if (mDragStartPos.x < rightRect.left && mCurrentIndex > 0) {
					mDragStartPos.x = leftRect.left;
					startCurl(CURL_LEFT);
				}
				// Otherwise check pointer is on right page's side.
				else if (mDragStartPos.x >= rightRect.left
						&& mCurrentIndex < mBitmapProvider.getBitmapCount()) {
					mDragStartPos.x = rightRect.right;
					if (!mAllowLastPageCurl
							&& mCurrentIndex >= mBitmapProvider
									.getBitmapCount() - 1) {
						return false;
					}
					startCurl(CURL_RIGHT);
				}
			} else if (mViewMode == SHOW_ONE_PAGE) {
				float halfX = (rightRect.right + rightRect.left) / 2;
				if (mDragStartPos.x < halfX && mCurrentIndex > 0) {
					mDragStartPos.x = rightRect.left;
					startCurl(CURL_LEFT);
				} else if (mDragStartPos.x >= halfX
						&& mCurrentIndex < mBitmapProvider.getBitmapCount()) {
					mDragStartPos.x = rightRect.right;
					if (!mAllowLastPageCurl
							&& mCurrentIndex >= mBitmapProvider
									.getBitmapCount() - 1) {
						return false;
					}
					startCurl(CURL_RIGHT);
				}
			}
			// If we have are in curl state, let this case clause flow through
			// to next one. We have pointer position and drag position defined
			// and this will create first render request given these points.
			if (mCurlState == CURL_NONE) {
				return false;
			}
		}
		case MotionEvent.ACTION_MOVE: {
			updateCurlPos(mPointerPos, me.getPressure());
			break;
		}
		case MotionEvent.ACTION_CANCEL:
		case MotionEvent.ACTION_UP: {
			if (mCurlState == CURL_LEFT || mCurlState == CURL_RIGHT) {
			// Animation source is the point from where animation starts.
			mAnimationSource.set(mPointerPos);
			mAnimationStartTime = System.currentTimeMillis();

			//Find mDoPageTurnCurlPercent in terms of position
			float curlPointModifier = rightRect.width();
			if(mViewMode == SHOW_TWO_PAGES){
			curlPointModifier += leftRect.width();
			}
			curlPointModifier *= mDoPageTurnCurlPercent;

			mAnimationTarget.set(mDragStartPos);
			//Page turn animation will now occur if a curl drag has reached a certain percentage before release.
			if (mCurlState == CURL_RIGHT && mPointerPos.x > rightRect.right - curlPointModifier) {
			mAnimationTarget.x = rightRect.right;
			mAnimationTargetEvent = SET_CURL_TO_RIGHT;
			} else if ((mCurlState == CURL_LEFT && mPointerPos.x < (mViewMode == SHOW_TWO_PAGES ? leftRect.left : rightRect.left) + curlPointModifier) || mCurlState == CURL_RIGHT) {
			if (mViewMode == SHOW_TWO_PAGES) {
			mAnimationTarget.x = leftRect.left;
			} else {
			mAnimationTarget.x = rightRect.left;
			}
			mAnimationTargetEvent = SET_CURL_TO_LEFT;
			} else {
			mAnimationTarget.x = rightRect.right;
			mAnimationTargetEvent = SET_CURL_TO_RIGHT;
			}
			 
			mAnimate = true;
			requestRender();
			}
			break;
			}
			}

			return true;
	}
	
	/*
	 * Set the percentage a curl drag must reach before release, for a turn to be complete
	 */
	public void setDoPageTurnCurlPercent(float doPageTurnCurlPercent){
		mDoPageTurnCurlPercent = doPageTurnCurlPercent;
	}

	/**
	 * Allow the last page to curl.
	 */
	public void setAllowLastPageCurl(boolean allowLastPageCurl) {
		mAllowLastPageCurl = allowLastPageCurl;
	}

	/**
	 * Sets background color - or OpenGL clear color to be more precise. Color
	 * is a 32bit value consisting of 0xAARRGGBB and is extracted using
	 * android.graphics.Color eventually.
	 */
	@Override
	public void setBackgroundColor(int color) {
		mRenderer.setBackgroundColor(color);
		requestRender();
	}

	/**
	 * Update/set bitmap provider.
	 */
	public void setBitmapProvider(BitmapProvider bitmapProvider) {
		mBitmapProvider = bitmapProvider;
		mCurrentIndex = 0;
		updateBitmaps();
		requestRender();
	}

	/**
	 * Set page index.
	 */
	public void setCurrentIndex(int index) {
		if (mBitmapProvider == null || index <= 0) {
			mCurrentIndex = 0;
		} else {
			mCurrentIndex = Math.min(index,
					mBitmapProvider.getBitmapCount() - 1);
		}
		updateBitmaps();
		requestRender();
	}

	/**
	 * Set margins (or padding). Note: margins are proportional. Meaning a value
	 * of .1f will produce a 10% margin.
	 */
	public void setMargins(float left, float top, float right, float bottom) {
		mRenderer.setMargins(left, top, right, bottom);
	}

	/**
	 * Setter for whether left side page is rendered. This is useful mostly for
	 * situations where right (main) page is aligned to left side of screen and
	 * left page is not visible anyway.
	 */
	public void setRenderLeftPage(boolean renderLeftPage) {
		mRenderLeftPage = renderLeftPage;
	}
	
	/**
	 * Setter for whether touch pressure is taken in to account when calculating
	 * curl radius.
	 */
	public void setAdjustCurlRadiusWithPressure(boolean adjustCurlRadiusWithPressure) {
		mAdjustCurlRadiusWithPressure = adjustCurlRadiusWithPressure;
	}

	/**
	 * Sets SizeChangedObserver for this View. Call back method is called from
	 * this View's onSizeChanged method.
	 */
	public void setSizeChangedObserver(SizeChangedObserver observer) {
		mSizeChangedObserver = observer;
	}

	/**
	 * Sets view mode. Value can be either SHOW_ONE_PAGE or SHOW_TWO_PAGES. In
	 * former case right page is made size of display, and in latter case two
	 * pages are laid on visible area.
	 */
	public void setViewMode(int viewMode) {
		switch (viewMode) {
		case SHOW_ONE_PAGE:
			mViewMode = viewMode;
			mRenderer.setViewMode(CurlRenderer.SHOW_ONE_PAGE);
			break;
		case SHOW_TWO_PAGES:
			mViewMode = viewMode;
			mRenderer.setViewMode(CurlRenderer.SHOW_TWO_PAGES);
			break;
		}
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
				|| (mCurlState == CURL_LEFT && mViewMode == SHOW_ONE_PAGE)) {
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
				if (mRenderLeftPage) {
					mRenderer.addCurlMesh(mPageLeft);
				}
			}

			// If there is new/next available, set it to right page.
			if (mCurrentIndex < mBitmapProvider.getBitmapCount() - 1) {
				Bitmap bitmap = mBitmapProvider.getBitmap(mPageBitmapWidth,
						mPageBitmapHeight, mCurrentIndex + 1);
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
				Bitmap bitmap = mBitmapProvider.getBitmap(mPageBitmapWidth,
						mPageBitmapHeight, mCurrentIndex - 2);
				mPageLeft.setBitmap(bitmap);
				mPageLeft
						.setRect(mRenderer.getPageRect(CurlRenderer.PAGE_LEFT));
				mPageLeft.setTexRect(TEXTURE_RECT_BACK);
				mPageLeft.reset();
				if (mRenderLeftPage) {
					mRenderer.addCurlMesh(mPageLeft);
				}
			}

			// If there is something to show on right page add it to renderer.
			if (mCurrentIndex < mBitmapProvider.getBitmapCount()) {
				mPageRight.setRect(mRenderer
						.getPageRect(CurlRenderer.PAGE_RIGHT));
				mPageRight.reset();
				mRenderer.addCurlMesh(mPageRight);
			}

			// How dragging previous page happens depends on view mode.
			if (mViewMode == SHOW_ONE_PAGE) {
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
		if (mBitmapProvider == null || mPageBitmapWidth <= 0
				|| mPageBitmapHeight <= 0) {
			return;
		}

		// Remove meshes from renderer.
		mRenderer.removeCurlMesh(mPageLeft);
		mRenderer.removeCurlMesh(mPageRight);
		mRenderer.removeCurlMesh(mPageCurl);

		int leftIdx = mCurrentIndex - 1;
		int rightIdx = mCurrentIndex;
		int curlIdx = -1;
		if (mCurlState == CURL_LEFT) {
			curlIdx = leftIdx;
			leftIdx--;
		} else if (mCurlState == CURL_RIGHT) {
			curlIdx = rightIdx;
			rightIdx++;
		}

		if (rightIdx >= 0 && rightIdx < mBitmapProvider.getBitmapCount()) {
			Bitmap bitmap = mBitmapProvider.getBitmap(mPageBitmapWidth,
					mPageBitmapHeight, rightIdx);
			mPageRight.setBitmap(bitmap);
			mPageRight.setRect(mRenderer.getPageRect(CurlRenderer.PAGE_RIGHT));
			mPageRight.reset();
			mRenderer.addCurlMesh(mPageRight);
		}
		if (leftIdx >= 0 && leftIdx < mBitmapProvider.getBitmapCount()) {
			Bitmap bitmap = mBitmapProvider.getBitmap(mPageBitmapWidth,
					mPageBitmapHeight, leftIdx);
			mPageLeft.setBitmap(bitmap);
			mPageLeft.setRect(mRenderer.getPageRect(CurlRenderer.PAGE_LEFT));
			mPageLeft.reset();
			if (mRenderLeftPage) {
				mRenderer.addCurlMesh(mPageLeft);
			}
		}
		if (curlIdx >= 0 && curlIdx < mBitmapProvider.getBitmapCount()) {
			Bitmap bitmap = mBitmapProvider.getBitmap(mPageBitmapWidth,
					mPageBitmapHeight, curlIdx);
			mPageCurl.setBitmap(bitmap);
			if (mCurlState == CURL_RIGHT
					|| (mCurlState == CURL_LEFT && mViewMode == SHOW_TWO_PAGES)) {
				mPageCurl.setRect(mRenderer
						.getPageRect(CurlRenderer.PAGE_RIGHT));
			} else {
				mPageCurl
						.setRect(mRenderer.getPageRect(CurlRenderer.PAGE_LEFT));
			}
			mPageCurl.reset();
			mRenderer.addCurlMesh(mPageCurl);
		}
	}

	/**
	 * Updates curl position.
	 */
	private void updateCurlPos(PointF pointerPos, float pressure) {

		// Default curl radius.
		double radius = mRenderer.getPageRect(CURL_RIGHT).width() / 3;
		if(mAdjustCurlRadiusWithPressure){
			/* Pressure is given as a float ranging between .1, no pressure, to 1 (or higher) on normal pressure.
			 * we'll avoid using 0 so the curl doesn't disappear completely.
			 * We'll rounding the pressure adjustment off to the nearest tenth to attempt prevent flickering. TOOD: Needs additional testing.
			 */
			radius *= (Math.round(Math.max(1f - pressure, .1f)*10.f)/10.0f); 
		}
		mCurlPos.set(pointerPos);

		// If curl happens on right page, or on left page on two page mode,
		// we'll calculate curl position from pointerPos.
		if (mCurlState == CURL_RIGHT
				|| (mCurlState == CURL_LEFT && mViewMode == SHOW_TWO_PAGES)) {

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
			radius = Math.max(Math.min(mCurlPos.x - pageLeftX, radius), 0f);

			float pageRightX = mRenderer.getPageRect(CurlRenderer.PAGE_RIGHT).right;
			mCurlPos.x -= Math.min(pageRightX - mCurlPos.x, radius);
			mCurlDir.x = mCurlPos.x + mDragStartPos.x;
			mCurlDir.y = mCurlPos.y - mDragStartPos.y;

			setCurlPos(mCurlPos, mCurlDir, radius);
		}
	}

	/**
	 * Provider for feeding 'book' with bitmaps which are used for rendering
	 * pages.
	 */
	public interface BitmapProvider {

		/**
		 * Called once new bitmap is needed. Width and height are in pixels
		 * telling the size it will be drawn on screen and following them
		 * ensures that aspect ratio remains. But it's possible to return bitmap
		 * of any size though.<br/>
		 * <br/>
		 * Index is a number between 0 and getBitmapCount() - 1.
		 */
		public Bitmap getBitmap(int width, int height, int index);

		/**
		 * Return number of pages/bitmaps available.
		 */
		public int getBitmapCount();
	}

	/**
	 * Observer interface for handling CurlView size changes.
	 */
	public interface SizeChangedObserver {

		/**
		 * Called once CurlView size changes.
		 */
		public void onSizeChanged(int width, int height);
	}

}
