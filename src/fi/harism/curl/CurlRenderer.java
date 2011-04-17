package fi.harism.curl;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.RectF;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;
import android.opengl.GLUtils;

/**
 * Actual renderer class. Multiple bitmaps should be provided to get proper
 * results.
 * 
 * @author harism
 */
public class CurlRenderer implements GLSurfaceView.Renderer {

	public static final int SHOW_ONE_PAGE = 1;
	public static final int SHOW_TWO_PAGES = 2;

	public static final int PAGE_CURRENT = 0;
	public static final int PAGE_LEFT = 1;
	public static final int PAGE_RIGHT = 2;

	private int mViewMode = SHOW_ONE_PAGE;
	// Rect for render area.
	private RectF mViewRect = new RectF();
	// Screen size.
	private int mViewportWidth;
	private int mViewportHeight;

	// Flag for updating textures from bitmaps.
	private static final int TEXTURE_COUNT = 3;
	private boolean mBitmapsChanged = false;
	private int[] mTextureIds = new int[TEXTURE_COUNT];
	private Bitmap[] mBitmaps = new Bitmap[TEXTURE_COUNT];

	// Curl meshes used for static and dynamic rendering.
	private CurlMesh[] mCurlMeshes;

	private boolean mBackgroundColorChanged = false;
	private int mBackgroundColor;

	private CurlRendererObserver mObserver;

	/**
	 * Basic constructor.
	 */
	public CurlRenderer(CurlRendererObserver observer) {
		mObserver = observer;

		mCurlMeshes = new CurlMesh[TEXTURE_COUNT];
		mCurlMeshes[PAGE_CURRENT] = new CurlMesh(10);
		mCurlMeshes[PAGE_CURRENT].setTexRect(new RectF(0, 0, 1, 1));
		mCurlMeshes[PAGE_CURRENT].reset();
		mCurlMeshes[PAGE_LEFT] = new CurlMesh(10);
		mCurlMeshes[PAGE_LEFT].setTexRect(new RectF(1, 0, 0, 1));
		mCurlMeshes[PAGE_LEFT].reset();
		mCurlMeshes[PAGE_RIGHT] = new CurlMesh(10);
		mCurlMeshes[PAGE_RIGHT].setTexRect(new RectF(0, 0, 1, 1));
		mCurlMeshes[PAGE_RIGHT].reset();
	}

	/**
	 * Calculates curl position from pointerPos, meaning that edge of mesh
	 * follows pointerPos.
	 */
	public void setPointerPos(PointF pointerPos, PointF curlDir, double radius) {
		double curlLen = radius * Math.PI;
		double distX = mViewRect.right - pointerPos.x;
		double distY = distX * curlDir.y;
		double dist = Math.sqrt(distX * distX + distY * distY);

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
	 * Translates screen coordinates into view coordinates.
	 */
	public PointF getPos(float x, float y) {
		PointF ret = new PointF();
		ret.x = mViewRect.left + (mViewRect.width() * x / mViewportWidth);
		ret.y = mViewRect.top - (-mViewRect.height() * y / mViewportHeight);
		return ret;
	}

	/**
	 * Sets curl position.
	 */
	public void setCurlPos(PointF curlPos, PointF curlDir, double radius) {
		mCurlMeshes[PAGE_CURRENT].curl(curlPos, curlDir, radius);
	}

	@Override
	public void onDrawFrame(GL10 gl) {
		if (mBitmapsChanged) {
			loadBitmaps(gl);
			mBitmapsChanged = false;
		}
		if (mBackgroundColorChanged) {
			gl.glClearColorx(Color.red(mBackgroundColor),
					Color.green(mBackgroundColor),
					Color.blue(mBackgroundColor), 255);
		}

		gl.glClear(GL10.GL_COLOR_BUFFER_BIT); // | GL10.GL_DEPTH_BUFFER_BIT);
		gl.glLoadIdentity();

		if (mBitmaps[PAGE_LEFT] != null && mViewMode == SHOW_TWO_PAGES) {
			gl.glBindTexture(GL10.GL_TEXTURE_2D, mTextureIds[PAGE_LEFT]);
			mCurlMeshes[PAGE_LEFT].draw(gl);
		}
		if (mBitmaps[PAGE_RIGHT] != null) {
			gl.glBindTexture(GL10.GL_TEXTURE_2D, mTextureIds[PAGE_RIGHT]);
			mCurlMeshes[PAGE_RIGHT].draw(gl);
		}
		if (mBitmaps[PAGE_CURRENT] != null) {
			gl.glBindTexture(GL10.GL_TEXTURE_2D, mTextureIds[PAGE_CURRENT]);
			mCurlMeshes[PAGE_CURRENT].draw(gl);
		}
	}

	@Override
	public void onSurfaceChanged(GL10 gl, int width, int height) {
		gl.glViewport(0, 0, width, height);
		mViewportWidth = width;
		mViewportHeight = height;

		float ratio = (float) width / height;
		mViewRect.top = 1.0f;
		mViewRect.bottom = -1.0f;
		mViewRect.left = -ratio;
		mViewRect.right = ratio;
		setViewMode(mViewMode);

		gl.glMatrixMode(GL10.GL_PROJECTION);
		gl.glLoadIdentity();
		GLU.gluOrtho2D(gl, mViewRect.left, mViewRect.right, mViewRect.bottom,
				mViewRect.top);

		gl.glMatrixMode(GL10.GL_MODELVIEW);
		gl.glLoadIdentity();
	}

	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		setBackgroundColor(0xFF303030);
		gl.glShadeModel(GL10.GL_SMOOTH);
		gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_NICEST);

		gl.glGenTextures(TEXTURE_COUNT, mTextureIds, 0);
		for (int i = 0; i < TEXTURE_COUNT; ++i) {
			gl.glBindTexture(GL10.GL_TEXTURE_2D, mTextureIds[i]);
			gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER,
					GL10.GL_LINEAR);
			gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER,
					GL10.GL_LINEAR);
			gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S,
					GL10.GL_CLAMP_TO_EDGE);
			gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T,
					GL10.GL_CLAMP_TO_EDGE);
		}
	}

	/**
	 * Update bitmaps/textures.
	 */
	public void setBitmap(Bitmap bitmap, int page) {
		if (page >= 0 && page <= TEXTURE_COUNT) {
			mBitmaps[page] = bitmap;
			mBitmapsChanged = true;
		}
	}

	/**
	 * Updates textures from bitmaps.
	 */
	private void loadBitmaps(GL10 gl) {
		for (int i = 0; i < TEXTURE_COUNT; ++i) {
			if (mBitmaps[i] != null) {
				gl.glBindTexture(GL10.GL_TEXTURE_2D, mTextureIds[i]);
				GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, mBitmaps[i], 0);
			}
		}
	}

	/**
	 * Change background/clear color.
	 */
	public void setBackgroundColor(int color) {
		mBackgroundColor = color;
		mBackgroundColorChanged = true;
	}

	/**
	 * Sets visible page count to one or two.
	 */
	public void setViewMode(int viewmode) {
		if (viewmode == SHOW_ONE_PAGE) {
			mViewMode = viewmode;

			RectF curlRect = new RectF(mViewRect);
			mCurlMeshes[PAGE_CURRENT].setRect(curlRect);
			mCurlMeshes[PAGE_CURRENT].reset();
			mCurlMeshes[PAGE_RIGHT].setRect(curlRect);
			mCurlMeshes[PAGE_RIGHT].reset();

			mObserver.onBitmapSizeChanged(mViewportWidth, mViewportHeight);
		} else if (viewmode == SHOW_TWO_PAGES) {
			mViewMode = viewmode;

			RectF curlRectLeft = new RectF(mViewRect);
			curlRectLeft.right = 0;
			RectF curlRectRight = new RectF(mViewRect);
			curlRectRight.left = 0;

			mCurlMeshes[PAGE_CURRENT].setRect(curlRectRight);
			mCurlMeshes[PAGE_CURRENT].reset();
			mCurlMeshes[PAGE_RIGHT].setRect(curlRectRight);
			mCurlMeshes[PAGE_RIGHT].reset();
			mCurlMeshes[PAGE_LEFT].setRect(curlRectLeft);
			mCurlMeshes[PAGE_LEFT].reset();

			mObserver.onBitmapSizeChanged((mViewportWidth + 1) / 2,
					mViewportHeight);
		}
	}

	/**
	 * Observer for waiting render engine/state updates.
	 */
	public interface CurlRendererObserver {
		public void onBitmapSizeChanged(int width, int height);
	}
}
