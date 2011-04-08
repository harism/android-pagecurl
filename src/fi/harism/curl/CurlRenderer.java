package fi.harism.curl;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.graphics.Bitmap;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;
import android.opengl.GLUtils;

public class CurlRenderer implements GLSurfaceView.Renderer {
	
	private boolean mBitmapsChanged = false;
	private static final int TEXTURE_COUNT = 1;
	private int[] mTextureIds = new int[TEXTURE_COUNT];
	private Bitmap[] mBitmaps = new Bitmap[TEXTURE_COUNT];
	
	private CurlMesh mCurlMesh;
	
	public CurlRenderer() {
		mCurlMesh = new CurlMesh(0.0f, 0.5f, 1.0f, -0.5f, 20);
		mCurlMesh.curl(0, 0, 0, 0);
	}

	@Override
	public void onDrawFrame(GL10 gl) {
		if (mBitmapsChanged) {
			loadBitmaps(gl);
			mBitmapsChanged = false;
		}
		gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
		gl.glLoadIdentity();
		gl.glTranslatef(0.0f, 0.0f, -4.0f);
		
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
	     // for a fixed camera, set the projection too
	     float ratio = (float) width / height;
	     gl.glMatrixMode(GL10.GL_PROJECTION);
	     gl.glLoadIdentity();
	     //gl.glFrustumf(-ratio, ratio, -1, 1, 0, 10);
	     GLU.gluPerspective(gl, 45.0f, ratio, 0.1f, 100.0f);
	     gl.glMatrixMode(GL10.GL_MODELVIEW);
	     gl.glLoadIdentity();
	}
	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		gl.glClearColor(0.5f,0.0f, 07.f, 1.0f);
		gl.glShadeModel(GL10.GL_SMOOTH);
		gl.glClearDepthf(1.0f);
		gl.glEnable(GL10.GL_DEPTH_TEST);
		gl.glDepthFunc(GL10.GL_LEQUAL);
		gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_NICEST);
		
		gl.glGenTextures(TEXTURE_COUNT, mTextureIds, 0);
		for (int i=0; i<TEXTURE_COUNT; ++i) {
			gl.glBindTexture(GL10.GL_TEXTURE_2D, mTextureIds[i]);
			gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
			gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);				
			gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
			gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);
		}
	}
	
	private void loadBitmaps(GL10 gl) {
		for (int i=0; i<TEXTURE_COUNT; ++i) {
			if (mBitmaps[i] != null) {
				gl.glBindTexture(GL10.GL_TEXTURE_2D, mTextureIds[i]);
				GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, mBitmaps[i], 0);
			}
		}
	}
	
	public void curl(float x1, float y1, float x2, float y2) {
		mCurlMesh.curl(x1, y1, x2, y2);
	}
	
	public void setBitmap(Bitmap bitmap) {
		mBitmaps[0] = bitmap;
		mBitmapsChanged = true;
	}
}
