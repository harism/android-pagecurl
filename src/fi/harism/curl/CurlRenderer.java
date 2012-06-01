/*
   Copyright 2012 Harri Smatt

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

package fi.harism.curl;

import java.util.Vector;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.RectF;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

/**
 * Actual renderer class.
 * 
 * @author harism
 */
public class CurlRenderer implements GLSurfaceView.Renderer {

	// Constant for requesting left page rect.
	public static final int PAGE_LEFT = 1;
	// Constant for requesting right page rect.
	public static final int PAGE_RIGHT = 2;
	// Constants for changing view mode.
	public static final int SHOW_ONE_PAGE = 1;
	public static final int SHOW_TWO_PAGES = 2;
	// Set to true for checking quickly how perspective projection looks.
	// private static final boolean USE_PERSPECTIVE_PROJECTION = false;
	// Background color constant.
	private int mBackgroundColor = 0xFF000000;
	// Curl meshes used for static and dynamic rendering.
	private Vector<CurlMesh> mCurlMeshes;
	private RectF mMargins = new RectF();
	private CurlRenderer.Observer mObserver;
	// Page rectangles.
	private final RectF mPageRectLeft;
	private final RectF mPageRectRight;
	// Projection matrix.
	private final float[] mProjectionMatrix = new float[16];
	// Shaders.
	private final CurlShader mShaderShadow = new CurlShader();
	private final CurlShader mShaderTexture = new CurlShader();
	// View mode.
	private int mViewMode = SHOW_ONE_PAGE;
	// Screen size.
	private int mViewportWidth, mViewportHeight;
	// Rect for render area.
	private final RectF mViewRect = new RectF();
	
	// Shaders.
	private final String SHADER_SHADOW_FRAGMENT =
			"precision mediump float;\n" +
			"varying vec4 vColor;\n" +
			"void main() {\n" +
			"  gl_FragColor = vColor;\n" +
			"}\n";
	private final String SHADER_SHADOW_VERTEX =
			"uniform mat4 uProjectionM;\n" +
			"attribute vec3 aPosition;\n" +
			"attribute vec4 aColor;\n" +
			"varying vec4 vColor;\n" +
			"void main() {\n" +
			"  gl_Position = uProjectionM * vec4(aPosition, 1.0);\n" +
			"  vColor = aColor;\n" +
			"}\n";
	private final String SHADER_TEXTURE_FRAGMENT =
			"precision mediump float;\n" +
			"varying vec4 vColor;\n" +
			"varying vec2 vTextureCoord;\n" +
			"uniform sampler2D sTexture;\n" +
			"void main() {\n" +
			"  gl_FragColor = texture2D(sTexture, vTextureCoord);\n" +
			"  gl_FragColor.rgb *= vColor.rgb;\n" +
			"  gl_FragColor = mix(vColor, gl_FragColor, vColor.a);\n" +
			"  gl_FragColor.a = 1.0;\n" +
			"}\n";
	private final String SHADER_TEXTURE_VERTEX =
			"uniform mat4 uProjectionM;\n" +
			"attribute vec3 aPosition;\n" +
			"attribute vec4 aColor;\n" +
			"attribute vec2 aTextureCoord;\n" +
			"varying vec4 vColor;\n" +
			"varying vec2 vTextureCoord;\n" +
			"void main() {\n" +
			"  gl_Position = uProjectionM * vec4(aPosition, 1.0);\n" +
			"  vColor = aColor;\n" +
			"  vTextureCoord = aTextureCoord;\n" +
			"}\n";

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
	 * Returns rect reserved for left or right page. Value page should be
	 * PAGE_LEFT or PAGE_RIGHT.
	 */
	public RectF getPageRect(int page) {
		if (page == PAGE_LEFT) {
			return mPageRectLeft;
		} else if (page == PAGE_RIGHT) {
			return mPageRectRight;
		}
		return null;
	}

	@Override
	public synchronized void onDrawFrame(GL10 unused) {

		mObserver.onDrawFrame();

		GLES20.glClearColor(Color.red(mBackgroundColor) / 255f,
							Color.green(mBackgroundColor) / 255f,
							Color.blue(mBackgroundColor) / 255f,
							Color.alpha(mBackgroundColor) / 255f);
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

		for (int i = 0; i < mCurlMeshes.size(); ++i) {
			mCurlMeshes.get(i).onDrawFrame(mShaderTexture, mShaderShadow);
		}
	}

	@Override
	public void onSurfaceChanged(GL10 unused, int width, int height) {
		GLES20.glViewport(0, 0, width, height);
		mViewportWidth = width;
		mViewportHeight = height;

		float ratio = (float) width / height;
		mViewRect.top = 1.0f;
		mViewRect.bottom = -1.0f;
		mViewRect.left = -ratio;
		mViewRect.right = ratio;
		updatePageRects();

		Matrix.orthoM(mProjectionMatrix, 0, -ratio, ratio, -1f, 1f, -10f, 10f);
		mShaderTexture.useProgram();
		GLES20.glUniformMatrix4fv(mShaderTexture.getHandle("uProjectionM"), 1, false, mProjectionMatrix, 0);
		mShaderShadow.useProgram();
		GLES20.glUniformMatrix4fv(mShaderShadow.getHandle("uProjectionM"), 1, false, mProjectionMatrix, 0);
	}

	@Override
	public void onSurfaceCreated(GL10 unused, EGLConfig config) {
		GLES20.glDisable(GLES20.GL_DEPTH_TEST);
		GLES20.glDisable(GLES20.GL_CULL_FACE);
		
		try {
			mShaderShadow.setProgram(SHADER_SHADOW_VERTEX, SHADER_SHADOW_FRAGMENT);
			mShaderTexture.setProgram(SHADER_TEXTURE_VERTEX, SHADER_TEXTURE_FRAGMENT);
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		mObserver.onSurfaceCreated();
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
	}

	/**
	 * Set margins or padding. Note: margins are proportional. Meaning a value
	 * of .1f will produce a 10% margin.
	 */
	public synchronized void setMargins(float left, float top, float right,
			float bottom) {
		mMargins.left = left;
		mMargins.top = top;
		mMargins.right = right;
		mMargins.bottom = bottom;
		updatePageRects();
	}

	/**
	 * Sets visible page count to one or two. Should be either SHOW_ONE_PAGE or
	 * SHOW_TWO_PAGES.
	 */
	public synchronized void setViewMode(int viewmode) {
		if (viewmode == SHOW_ONE_PAGE) {
			mViewMode = viewmode;
			updatePageRects();
		} else if (viewmode == SHOW_TWO_PAGES) {
			mViewMode = viewmode;
			updatePageRects();
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
	 * Recalculates page rectangles.
	 */
	private void updatePageRects() {
		if (mViewRect.width() == 0 || mViewRect.height() == 0) {
			return;
		} else if (mViewMode == SHOW_ONE_PAGE) {
			mPageRectRight.set(mViewRect);
			mPageRectRight.left += mViewRect.width() * mMargins.left;
			mPageRectRight.right -= mViewRect.width() * mMargins.right;
			mPageRectRight.top += mViewRect.height() * mMargins.top;
			mPageRectRight.bottom -= mViewRect.height() * mMargins.bottom;

			mPageRectLeft.set(mPageRectRight);
			mPageRectLeft.offset(-mPageRectRight.width(), 0);

			int bitmapW = (int) ((mPageRectRight.width() * mViewportWidth) / mViewRect
					.width());
			int bitmapH = (int) ((mPageRectRight.height() * mViewportHeight) / mViewRect
					.height());
			mObserver.onPageSizeChanged(bitmapW, bitmapH);
		} else if (mViewMode == SHOW_TWO_PAGES) {
			mPageRectRight.set(mViewRect);
			mPageRectRight.left += mViewRect.width() * mMargins.left;
			mPageRectRight.right -= mViewRect.width() * mMargins.right;
			mPageRectRight.top += mViewRect.height() * mMargins.top;
			mPageRectRight.bottom -= mViewRect.height() * mMargins.bottom;

			mPageRectLeft.set(mPageRectRight);
			mPageRectLeft.right = (mPageRectLeft.right + mPageRectLeft.left) / 2;
			mPageRectRight.left = mPageRectLeft.right;

			int bitmapW = (int) ((mPageRectRight.width() * mViewportWidth) / mViewRect
					.width());
			int bitmapH = (int) ((mPageRectRight.height() * mViewportHeight) / mViewRect
					.height());
			mObserver.onPageSizeChanged(bitmapW, bitmapH);
		}
	}

	/**
	 * Observer for waiting render engine/state updates.
	 */
	public interface Observer {
		/**
		 * Called from onDrawFrame called before rendering is started. This is
		 * intended to be used for animation purposes.
		 */
		public void onDrawFrame();

		/**
		 * Called once page size is changed. Width and height tell the page size
		 * in pixels making it possible to update textures accordingly.
		 */
		public void onPageSizeChanged(int width, int height);

		/**
		 * Called from onSurfaceCreated to enable texture re-initialization etc
		 * what needs to be done when this happens.
		 */
		public void onSurfaceCreated();
	}
}
