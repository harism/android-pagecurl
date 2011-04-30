package fi.harism.curl;

import java.util.Vector;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.RectF;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;

/**
 * Actual renderer class.
 * 
 * @author harism
 */
public class CurlRenderer implements GLSurfaceView.Renderer {

	public static final int SHOW_ONE_PAGE = 1;
	public static final int SHOW_TWO_PAGES = 2;

	public static final int PAGE_LEFT = 1;
	public static final int PAGE_RIGHT = 2;

	private int mViewMode = SHOW_ONE_PAGE;
	// Rect for render area.
	private RectF mViewRect = new RectF();
	// Screen size.
	private int mViewportWidth;
	private int mViewportHeight;

	// Curl meshes used for static and dynamic rendering.
	private Vector<CurlMesh> mCurlMeshes;

	private boolean mBackgroundColorChanged = false;
	private int mBackgroundColor;

	private CurlRenderer.Observer mObserver;

	private RectF mPageRectLeft;
	private RectF mPageRectRight;

	/**
	 * Basic constructor.
	 */
	public CurlRenderer(CurlRenderer.Observer observer) {
		mObserver = observer;
		mCurlMeshes = new Vector<CurlMesh>();
		mPageRectLeft = new RectF();
		mPageRectRight = new RectF();
	}

	/**
	 * Adds CurlMesh to this renderer.
	 */
	public synchronized void addCurlMesh(CurlMesh mesh) {
		removeCurlMesh(mesh);
		mCurlMeshes.add(mesh);
	}

	/**
	 * Returns rect reserved for left or right page.
	 */
	public RectF getPageRect(int page) {
		if (page == PAGE_LEFT) {
			return mPageRectLeft;
		} else if (page == PAGE_RIGHT) {
			return mPageRectRight;
		}
		return null;
	}

	/**
	 * Getter for current view mode.
	 */
	public int getViewMode() {
		return mViewMode;
	}

	@Override
	public synchronized void onDrawFrame(GL10 gl) {
		if (mBackgroundColorChanged) {
			gl.glClearColor(Color.red(mBackgroundColor) / 255f,
					Color.green(mBackgroundColor) / 255f,
					Color.blue(mBackgroundColor) / 255f,
					Color.alpha(mBackgroundColor) / 255f);
		}

		gl.glClear(GL10.GL_COLOR_BUFFER_BIT); // | GL10.GL_DEPTH_BUFFER_BIT);
		gl.glLoadIdentity();

		for (int i = 0; i < mCurlMeshes.size(); ++i) {
			mCurlMeshes.get(i).draw(gl);
		}

		mObserver.onRenderDone();
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

		gl.glMatrixMode(GL10.GL_PROJECTION);
		gl.glLoadIdentity();
		GLU.gluOrtho2D(gl, mViewRect.left, mViewRect.right, mViewRect.bottom,
				mViewRect.top);

		// TODO: Add more proper margin calculation
		// But for now this hack has to do.
		// mViewRect.inset(.1f, -.1f);
		setViewMode(mViewMode);

		gl.glMatrixMode(GL10.GL_MODELVIEW);
		gl.glLoadIdentity();
	}

	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		setBackgroundColor(0xFF303030);
		gl.glShadeModel(GL10.GL_SMOOTH);
		gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_NICEST);
		gl.glDisable(GL10.GL_DEPTH_TEST);
		gl.glDisable(GL10.GL_CULL_FACE);
	}

	/**
	 * Removes CurlMesh from this renderer.
	 */
	public synchronized void removeCurlMesh(CurlMesh mesh) {
		while (mCurlMeshes.remove(mesh))
			;
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
	public synchronized void setViewMode(int viewmode) {
		if (viewmode == SHOW_ONE_PAGE) {
			mViewMode = viewmode;
			mPageRectRight.set(mViewRect);
			mPageRectLeft.set(mPageRectRight);
			mPageRectLeft.offset(-mPageRectRight.width(), 0);
			mObserver.onBitmapSizeChanged(mViewportWidth, mViewportHeight);
		} else if (viewmode == SHOW_TWO_PAGES) {
			mViewMode = viewmode;
			mPageRectLeft.set(mViewRect);
			mPageRectLeft.right = 0;
			mPageRectRight.set(mViewRect);
			mPageRectRight.left = 0;
			mObserver.onBitmapSizeChanged((mViewportWidth + 1) / 2,
					mViewportHeight);
		}
	}

	/**
	 * Translates screen coordinates into view coordinates.
	 */
	public void translate(PointF pt) {
		pt.x = mViewRect.left + (mViewRect.width() * pt.x / mViewportWidth);
		pt.y = mViewRect.top - (-mViewRect.height() * pt.y / mViewportHeight);
	}

	/**
	 * Observer for waiting render engine/state updates.
	 */
	public interface Observer {
		/**
		 * Called once page size is changed. Width and height tell the page size
		 * in pixels making it possible to update textures accordingly.
		 */
		public void onBitmapSizeChanged(int width, int height);

		/**
		 * Call back method from onDrawFrame called after rendering frame is
		 * done. This is intended to be used for animation purposes.
		 */
		public void onRenderDone();
	}
}
