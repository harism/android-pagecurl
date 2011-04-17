package fi.harism.curl;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.graphics.Bitmap;
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

	// Rect for render area.
	private RectF mViewRect = new RectF();
	// Screen size.
	private int mViewportWidth;
	private int mViewportHeight;

	// Flag for updating textures from bitmaps.
	private boolean mBitmapsChanged = false;
	private static final int TEXTURE_COUNT = 1;
	private int[] mTextureIds = new int[TEXTURE_COUNT];
	private Bitmap[] mBitmaps = new Bitmap[TEXTURE_COUNT];

	// Curl mesh rect.
	private RectF mCurlRect = new RectF(-1.0f, 1.0f, 1.0f, -1.0f);
	private CurlMesh mCurlMesh;

	/**
	 * Basic constructor.
	 */
	public CurlRenderer() {
		mCurlMesh = new CurlMesh(mCurlRect, new RectF(0, 0, 1, 1), 10);
		mCurlMesh.reset();
	}

	public void curl(PointF startPoint, PointF curlPoint) {
		// Map position to 'render' coordinates.
		PointF curlPos = new PointF();
		curlPos.x = mViewRect.left
				+ (mViewRect.width() * curlPoint.x / mViewportWidth);
		curlPos.y = mViewRect.top
				- (-mViewRect.height() * curlPoint.y / mViewportHeight);

		// Map start point to 'render' coordinates.
		PointF startPos = new PointF();
		startPos.x = mViewRect.left
				+ (mViewRect.width() * startPoint.x / mViewportWidth);
		startPos.y = mViewRect.top
				- (-mViewRect.height() * startPoint.y / mViewportHeight);

		// Map startPos to view range.
		startPos.x = startPos.x < mViewRect.left ? mViewRect.left : startPos.x;
		startPos.x = startPos.x > mViewRect.right ? mViewRect.right
				: startPos.x;
		startPos.y = startPos.y < mViewRect.bottom ? mViewRect.bottom
				: startPos.y;
		startPos.y = startPos.y > mViewRect.top ? mViewRect.top : startPos.y;
		if (Math.abs(startPos.x) < Math.abs(startPos.y)) {
			startPos.y = startPos.y < 0 ? mViewRect.bottom : mViewRect.top;
		} else {
			startPos.x = startPos.x < 0 ? mViewRect.left : mViewRect.right;
		}

		PointF directionVec = new PointF(curlPos.x - startPos.x, curlPos.y
				- startPos.y);

		double radius = 0.3f;
		double curlLen = radius * Math.PI;
		double dist = Math.sqrt(directionVec.x * directionVec.x
				+ directionVec.y * directionVec.y);

		// Normalize direction vector.
		directionVec.x = (float) (directionVec.x / dist);
		directionVec.y = (float) (directionVec.y / dist);

		if (dist >= curlLen) {
			double translate = (dist - curlLen) / 2;
			curlPos.x -= directionVec.x * translate;
			curlPos.y -= directionVec.y * translate;
		} else {
			double angle = Math.PI * Math.sqrt(dist / curlLen);
			double translate = radius * Math.sin(angle);
			curlPos.x += directionVec.x * translate;
			curlPos.y += directionVec.y * translate;
		}

		mCurlMesh.curl(curlPos, directionVec, radius);
	}

	@Override
	public void onDrawFrame(GL10 gl) {
		if (mBitmapsChanged) {
			loadBitmaps(gl);
			mBitmapsChanged = false;
		}

		gl.glClear(GL10.GL_COLOR_BUFFER_BIT); // | GL10.GL_DEPTH_BUFFER_BIT);
		gl.glLoadIdentity();

		if (mBitmaps[0] != null) {
			// TODO: Draw left page.
		}
		if (mBitmaps[0] != null) {
			gl.glBindTexture(GL10.GL_TEXTURE_2D, mTextureIds[0]);
			mCurlMesh.draw(gl);
		}
		if (mBitmaps[0] != null) {
			// TODO: Draw right page.
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

		mCurlRect.set(mViewRect);
		// mCurlRect.inset(.2f, -.2f);
		mCurlMesh.setRect(mCurlRect);

		gl.glMatrixMode(GL10.GL_PROJECTION);
		gl.glLoadIdentity();
		GLU.gluOrtho2D(gl, mViewRect.left, mViewRect.right, mViewRect.bottom,
				mViewRect.top);

		gl.glMatrixMode(GL10.GL_MODELVIEW);
		gl.glLoadIdentity();
	}

	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		gl.glClearColor(0.2f, 0.2f, 0.2f, 1.0f);
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
	public void setBitmap(Bitmap bitmap) {
		mBitmaps[0] = bitmap;
		mBitmapsChanged = true;
	}

	private void loadBitmaps(GL10 gl) {
		for (int i = 0; i < TEXTURE_COUNT; ++i) {
			if (mBitmaps[i] != null) {
				gl.glBindTexture(GL10.GL_TEXTURE_2D, mTextureIds[i]);
				GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, mBitmaps[i], 0);
			}
		}
	}
}
