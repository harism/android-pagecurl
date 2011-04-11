package fi.harism.curl;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.graphics.RectF;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;
import android.opengl.GLUtils;
import android.util.Log;

public class CurlRenderer implements GLSurfaceView.Renderer {

	private RectF mViewRect = new RectF();
	private int mViewportWidth;
	private int mViewportHeight;

	private boolean mBitmapsChanged = false;
	private static final int TEXTURE_COUNT = 1;
	private int[] mTextureIds = new int[TEXTURE_COUNT];
	private Bitmap[] mBitmaps = new Bitmap[TEXTURE_COUNT];

	private RectF mCurlRect = new RectF(-1.0f, 1.0f, 1.0f, -1.0f);
	private CurlMesh mCurlMesh;

	public CurlRenderer() {
		mCurlMesh = new CurlMesh(mCurlRect, new RectF(0, 0, 1, 1), 10);
	}

	@Override
	public void onDrawFrame(GL10 gl) {
		if (mBitmapsChanged) {
			loadBitmaps(gl);
			mBitmapsChanged = false;
		}

		gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
		gl.glLoadIdentity();
		//gl.glTranslatef(0.0f, 0.0f, -4.0f);

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
				
		mViewRect.top = 1.2f;
		mViewRect.bottom = -1.2f;
		mViewRect.left = -1.2f;
		mViewRect.right = 1.2f;
		
		float gapW = Math.max(0.0f, (mViewRect.width() * (width - height)) / height);
		float gapH = Math.max(0.0f, (-mViewRect.height() * (height - width)) / width);
		
		Log.d ("GAPS", "w=" + gapW + " h=" + gapH);
		
		mViewRect.top += gapH / 2;
		mViewRect.bottom -= gapH / 2;
		mViewRect.left -= gapW / 2;
		mViewRect.right += gapW / 2;
		
		gl.glMatrixMode(GL10.GL_PROJECTION);
		gl.glLoadIdentity();
		GLU.gluOrtho2D(gl, mViewRect.left, mViewRect.right, mViewRect.bottom, mViewRect.top);
		
		gl.glMatrixMode(GL10.GL_MODELVIEW);
		gl.glLoadIdentity();
	}

	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		gl.glClearColor(0.2f, 0.2f, 0.2f, 1.0f);
		gl.glShadeModel(GL10.GL_SMOOTH);
		gl.glClearDepthf(1.0f);
		gl.glEnable(GL10.GL_DEPTH_TEST);
		gl.glDepthFunc(GL10.GL_LEQUAL);
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

	private void loadBitmaps(GL10 gl) {
		for (int i = 0; i < TEXTURE_COUNT; ++i) {
			if (mBitmaps[i] != null) {
				gl.glBindTexture(GL10.GL_TEXTURE_2D, mTextureIds[i]);
				GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, mBitmaps[i], 0);
			}
		}
	}

	public void curl(PointF startPoint, PointF curlPoint) {
		// Normalize direction vector.
		PointF directionVec = new PointF(curlPoint.x - startPoint.x, startPoint.y - curlPoint.y);
		double len = Math.sqrt(directionVec.x * directionVec.x + directionVec.y * directionVec.y);
		directionVec.x = (float)(directionVec.x / len);
		directionVec.y = (float)(directionVec.y / len);
		
		// Map position to 'render' coordinates.
		PointF curlPos = new PointF();
		curlPos.x = mViewRect.left + (mViewRect.width() * curlPoint.x / mViewportWidth);
		curlPos.y = mViewRect.top - (-mViewRect.height() * curlPoint.y / mViewportHeight);
		
		mCurlMesh.curl(curlPos, directionVec, 0.3f);
	}

	public void setBitmap(Bitmap bitmap) {
		mBitmaps[0] = bitmap;
		mBitmapsChanged = true;
	}
}
